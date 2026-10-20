package com.example.picturegrid.feature.viewer.impl

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.picturegrid.core.images.OriginalImageLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

interface ViewerStore : Store<ViewerStore.Intent, ViewerStore.State, Nothing> {

    data class State(
        val url: String,
        val isLoading: Boolean = true,
        val file: File? = null,
        val error: String? = null,
        val zoom: Zoom = Zoom(),
    )

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
    }
}

class ViewerStoreFactory(
    private val storeFactory: StoreFactory,
    private val loader: OriginalImageLoader,
    private val url: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

    fun create(): ViewerStore =
        object : ViewerStore, Store<ViewerStore.Intent, ViewerStore.State, Nothing> by storeFactory.create(
            name = "ViewerStore",
            initialState = ViewerStore.State(url = url),
            bootstrapper = SimpleBootstrapper(Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

    private sealed interface Action { data object Load : Action }
    private sealed interface Msg {
        data object Loading : Msg
        data class Loaded(val file: File) : Msg
        data class Failed(val message: String) : Msg
        data class Zoomed(val zoom: ViewerStore.Zoom) : Msg
    }

    private object ReducerImpl : Reducer<ViewerStore.State, Msg> {
        override fun ViewerStore.State.reduce(msg: Msg): ViewerStore.State = when (msg) {
            Msg.Loading -> copy(isLoading = true, error = null)
            // Новый оригинал показывается в исходном масштабе.
            is Msg.Loaded -> copy(isLoading = false, file = msg.file, error = null, zoom = ViewerStore.Zoom())
            is Msg.Failed -> copy(isLoading = false, error = msg.message)
            is Msg.Zoomed -> copy(zoom = msg.zoom)
        }
    }

    private inner class ExecutorImpl :
        CoroutineExecutor<ViewerStore.Intent, Action, ViewerStore.State, Msg, Nothing>(mainDispatcher) {

        override fun executeAction(action: Action) {
            when (action) {
                Action.Load -> load()
            }
        }

        override fun executeIntent(intent: ViewerStore.Intent) {
            when (intent) {
                ViewerStore.Intent.Retry -> load()
                is ViewerStore.Intent.ZoomGesture -> dispatch(
                    Msg.Zoomed(
                        ViewerZoom.apply(
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
                ViewerStore.Intent.ToggleZoom -> dispatch(Msg.Zoomed(ViewerZoom.toggle(state().zoom)))
            }
        }

        private fun load() {
            scope.launch {
                dispatch(Msg.Loading)
                val result = withContext(ioDispatcher) { loader.load(state().url) }
                val file = result.getOrNull()
                if (file != null) {
                    dispatch(Msg.Loaded(file))
                } else {
                    dispatch(Msg.Failed(result.exceptionOrNull()?.message ?: "Load failed"))
                }
            }
        }
    }
}
