package com.example.picturegrid.feature.viewer.impl.presentation.state

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.core.rx.Observer
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.picturegrid.core.images.ImageLoadError
import com.example.picturegrid.core.images.ImageLoadResult
import com.example.picturegrid.core.images.OriginalImageLoader
import com.example.picturegrid.feature.viewer.api.domain.ViewerError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import com.example.picturegrid.feature.viewer.impl.presentation.ui.ViewerZoom

internal interface ViewerStore : Store<ViewerStore.Intent, ViewerStore.State, Nothing> {

    val stateFlow: StateFlow<State>

    /**
     * @param urls изображения для перелистывания.
     * @param index позиция открытого изображения в списке.
     */
    data class State(
        val urls: List<String>,
        val index: Int,
        val isLoading: Boolean = true,
        val file: File? = null,
        val error: ViewerError? = null,
        val zoom: Zoom = Zoom(),
        val isFullscreen: Boolean = false,
    ) {

        /** URL открытого изображения. */
        val url: String get() = urls.getOrElse(index) { "" }
    }

    /**
     * Масштаб и смещение изображения.
     *
     * Смещение задаётся в пикселях контейнера: правила ограничения описаны
     * в [ViewerZoom].
     */
    data class Zoom(
        val scale: Float = ViewerZoom.MIN_SCALE,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
    )

    sealed interface Intent {
        data object Retry : Intent

        /**
         * Жест масштабирования или перетаскивания.
         *
         * Размеры контейнера и изображения передаются вместе с жестом: без
         * них нельзя удержать точку под пальцем и ограничить сдвиг границами
         * видимой области.
         */
        data class ZoomGesture(
            val zoomChange: Float,
            val panX: Float,
            val panY: Float,
            val centroidX: Float,
            val centroidY: Float,
            val containerWidth: Float,
            val containerHeight: Float,
            val imageWidth: Float,
            val imageHeight: Float,
        ) : Intent

        /** Двойное нажатие: переключение между исходным и увеличенным масштабом. */
        data object ToggleZoom : Intent

        /** Нажатие по изображению или пустому месту: переключение полноэкранного режима. */
        data object ToggleFullscreen : Intent

        /** Перелистывание: открыто другое изображение из списка. */
        data class Select(val index: Int) : Intent
    }
}

internal class ViewerStoreFactory(
    private val storeFactory: StoreFactory,
    private val loader: OriginalImageLoader,
    private val urls: List<String>,
    private val initialIndex: Int = 0,
    private val zoomRules: ViewerZoom = ViewerZoom(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

    fun create(): ViewerStore {
        val delegate = storeFactory.create<ViewerStore.Intent, Action, Msg, ViewerStore.State, Nothing>(
            name = "ViewerStore",
            initialState = ViewerStore.State(
                urls = urls,
                index = initialIndex.coerceIn(0, (urls.size - 1).coerceAtLeast(0)),
            ),
            bootstrapper = SimpleBootstrapper(Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        )
        return StateFlowViewerStore(delegate)
    }

    private class StateFlowViewerStore(
        private val delegate: Store<ViewerStore.Intent, ViewerStore.State, Nothing>,
    ) : ViewerStore, Store<ViewerStore.Intent, ViewerStore.State, Nothing> by delegate {
        private val _state = MutableStateFlow(delegate.state)
        override val stateFlow: StateFlow<ViewerStore.State> = _state.asStateFlow()
        private val observer = delegate.states(object : Observer<ViewerStore.State> {
            override fun onNext(value: ViewerStore.State) {
                _state.update { value }
            }

            override fun onComplete() = Unit
        })

        override fun dispose() {
            observer.dispose()
            delegate.dispose()
        }
    }

    private sealed interface Action { data object Load : Action }
    private sealed interface Msg {
        data class Loading(val index: Int) : Msg
        data class Loaded(val index: Int, val file: File) : Msg
        data class Failed(val index: Int, val error: ViewerError) : Msg
        data class Zoomed(val zoom: ViewerStore.Zoom) : Msg
        data class FullscreenChanged(val isFullscreen: Boolean) : Msg
        data class Selected(val index: Int) : Msg
    }

    private object ReducerImpl : Reducer<ViewerStore.State, Msg> {
        override fun ViewerStore.State.reduce(msg: Msg): ViewerStore.State = when (msg) {
            is Msg.Loading -> if (msg.index != index) {
                this
            } else {
                copy(isLoading = true, error = null)
            }
            // Новый оригинал показывается в исходном масштабе.
            is Msg.Loaded -> if (msg.index != index) {
                this
            } else {
                copy(isLoading = false, file = msg.file, error = null, zoom = ViewerStore.Zoom())
            }
            is Msg.Failed -> if (msg.index != index) {
                this
            } else {
                copy(isLoading = false, error = msg.error)
            }
            is Msg.Zoomed -> copy(zoom = msg.zoom)
            is Msg.FullscreenChanged -> copy(isFullscreen = msg.isFullscreen)
            is Msg.Selected -> copy(index = msg.index, isLoading = true, file = null, error = null, zoom = ViewerStore.Zoom())
        }
    }

    private inner class ExecutorImpl :
        CoroutineExecutor<ViewerStore.Intent, Action, ViewerStore.State, Msg, Nothing>(mainDispatcher) {

        override fun executeAction(action: Action) {
            when (action) {
                Action.Load -> load(state().index)
            }
        }

        override fun executeIntent(intent: ViewerStore.Intent) {
            when (intent) {
                ViewerStore.Intent.Retry -> load(state().index)
                is ViewerStore.Intent.ZoomGesture -> dispatch(
                    Msg.Zoomed(
                        zoomRules.apply(
                            zoom = state().zoom,
                            zoomChange = intent.zoomChange,
                            panX = intent.panX,
                            panY = intent.panY,
                            centroidX = intent.centroidX,
                            centroidY = intent.centroidY,
                            containerWidth = intent.containerWidth,
                            containerHeight = intent.containerHeight,
                            imageWidth = intent.imageWidth,
                            imageHeight = intent.imageHeight,
                        ),
                    ),
                )
                ViewerStore.Intent.ToggleZoom -> dispatch(Msg.Zoomed(zoomRules.toggle(state().zoom)))
                ViewerStore.Intent.ToggleFullscreen -> dispatch(
                    Msg.FullscreenChanged(!state().isFullscreen),
                )
                is ViewerStore.Intent.Select -> select(intent.index)
            }
        }

        private fun select(index: Int) {
            val current = state()
            if (index !in current.urls.indices || index == current.index) return
            scope.launch {
                dispatch(Msg.Selected(index))
                load(index)
            }
        }

        private fun load(index: Int) {
            val url = state().urls.getOrNull(index) ?: return
            scope.launch {
                dispatch(Msg.Loading(index))
                when (val result = withContext(ioDispatcher) { loader.load(url) }) {
                    is ImageLoadResult.Success -> dispatch(Msg.Loaded(index, result.file))
                    is ImageLoadResult.Failure -> dispatch(Msg.Failed(index, result.error.toViewerError()))
                }
            }
        }
    }
}

/**
 * Переводит причину отказа загрузки в контракт экрана просмотра.
 *
 * Текст ошибки здесь не формируется: его выбирает UI (INV-14).
 */
private fun ImageLoadError.toViewerError(): ViewerError = when (this) {
    is ImageLoadError.Network -> ViewerError.Network
    ImageLoadError.NotAnImage -> ViewerError.NotAnImage
    ImageLoadError.Corrupted -> ViewerError.Corrupted
    ImageLoadError.Storage -> ViewerError.Storage
}
