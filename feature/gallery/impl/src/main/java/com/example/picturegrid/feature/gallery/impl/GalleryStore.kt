package com.example.picturegrid.feature.gallery.impl

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import com.example.picturegrid.feature.gallery.api.domain.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.domain.GalleryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import com.arkivanov.mvikotlin.core.rx.Observer

internal interface GalleryStore : Store<GalleryStore.Intent, GalleryStore.State, Nothing> {

    val stateFlow: StateFlow<State>

    data class State(
        val items: List<GalleryItem> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val refreshError: String? = null,
    )

    sealed interface Intent {
        data object Retry : Intent
        data object ReloadCached : Intent

        /**
         * Повторная загрузка списка после восстановления сети.
         *
         * Фоновая синхронизация обновляет сохранённый список, но экран
         * должен увидеть новые данные сам: без этого интента список на
         * экране остался бы прежним до перезапуска приложения.
         */
        data object Refresh : Intent
    }
}

internal class GalleryStoreFactory(
    private val storeFactory: StoreFactory,
    private val repository: GalleryRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val syncScheduler: () -> Unit = {},
) {

    fun create(): GalleryStore {
        val delegate = storeFactory.create<GalleryStore.Intent, Action, Msg, GalleryStore.State, Nothing>(
            name = "GalleryStore",
            initialState = GalleryStore.State(),
            bootstrapper = SimpleBootstrapper(Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        )
        return StateFlowGalleryStore(delegate)
    }

    private class StateFlowGalleryStore(
        private val delegate: Store<GalleryStore.Intent, GalleryStore.State, Nothing>,
    ) : GalleryStore, Store<GalleryStore.Intent, GalleryStore.State, Nothing> by delegate {
        private val _state = MutableStateFlow(GalleryStore.State())
        override val stateFlow: StateFlow<GalleryStore.State> = _state.asStateFlow()
        private val observer = delegate.states(object : Observer<GalleryStore.State> {
            override fun onNext(value: GalleryStore.State) {
                _state.update { value }
            }

            override fun onComplete() = Unit
        })

        override fun dispose() {
            observer.dispose()
            delegate.dispose()
        }
    }

    private sealed interface Action {
        data object Load : Action
    }

    private sealed interface Msg {
        data object LoadingStarted : Msg
        data class Loaded(val items: List<GalleryItem>) : Msg
        data class LoadFailed(val message: String, val cached: List<GalleryItem>?) : Msg
        data object RefreshingStarted : Msg
    }

    private object ReducerImpl : Reducer<GalleryStore.State, Msg> {
        override fun GalleryStore.State.reduce(msg: Msg): GalleryStore.State = when (msg) {
            Msg.LoadingStarted -> copy(isLoading = true, error = null, refreshError = null)
            is Msg.Loaded -> copy(
                items = msg.items,
                isLoading = false,
                isRefreshing = false,
                error = null,
                refreshError = null,
            )
            is Msg.LoadFailed -> {
                if (msg.cached != null) {
                    copy(
                        items = msg.cached,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        refreshError = msg.message,
                    )
                } else {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = msg.message,
                        refreshError = null,
                    )
                }
            }
            Msg.RefreshingStarted -> copy(isRefreshing = true, refreshError = null)
        }
    }

    private inner class ExecutorImpl :
        CoroutineExecutor<GalleryStore.Intent, Action, GalleryStore.State, Msg, Nothing>(mainDispatcher) {

        override fun executeAction(action: Action) {
            when (action) {
                Action.Load -> load()
            }
        }

        override fun executeIntent(intent: GalleryStore.Intent) {
            when (intent) {
                GalleryStore.Intent.Retry -> load()
                GalleryStore.Intent.ReloadCached -> reloadCached()
                GalleryStore.Intent.Refresh -> refresh()
            }
        }

        private fun load() {
            scope.launch {
                val cached = withContext(ioDispatcher) { repository.getCached() }
                if (cached != null) {
                    dispatch(Msg.Loaded(cached))
                    dispatch(Msg.RefreshingStarted)
                } else {
                    dispatch(Msg.LoadingStarted)
                }
                val result = withContext(ioDispatcher) { repository.refresh() }
                when (result) {
                    is GalleryRefreshResult.Success -> dispatch(Msg.Loaded(result.items))
                    is GalleryRefreshResult.Failure -> {
                        if (result.error.retryable) {
                            syncScheduler()
                        }
                        dispatch(
                            Msg.LoadFailed(
                                message = result.error.toMessage(),
                                cached = result.cached,
                            ),
                        )
                    }
                }
            }
        }

        private fun reloadCached() {
            scope.launch {
                val cached = withContext(ioDispatcher) { repository.getCached() }
                if (cached != null) {
                    dispatch(Msg.Loaded(cached))
                }
            }
        }

        /**
         * Загружает список заново, не показывая кэш: он и так уже на экране.
         */
        private fun refresh() {
            scope.launch {
                when (val result = withContext(ioDispatcher) { repository.refresh() }) {
                    is GalleryRefreshResult.Success -> dispatch(Msg.Loaded(result.items))
                    is GalleryRefreshResult.Failure -> {
                        if (result.error.retryable) {
                            syncScheduler()
                        }
                        dispatch(
                            Msg.LoadFailed(
                                message = result.error.toMessage(),
                                cached = result.cached,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun com.example.picturegrid.feature.gallery.api.domain.GalleryError.toMessage(): String =
    when (this) {
        is com.example.picturegrid.feature.gallery.api.domain.GalleryError.Network -> "Сеть: ${message}"
        is com.example.picturegrid.feature.gallery.api.domain.GalleryError.Storage -> "Хранилище: ${message}"
    }
