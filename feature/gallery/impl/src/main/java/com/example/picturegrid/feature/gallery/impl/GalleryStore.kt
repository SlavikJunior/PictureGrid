package com.example.picturegrid.feature.gallery.impl

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface GalleryStore : Store<GalleryStore.Intent, GalleryStore.State, Nothing> {

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
    }
}

class GalleryStoreFactory(
    private val storeFactory: StoreFactory,
    private val repository: GalleryRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val syncScheduler: () -> Unit = {},
) {

    fun create(): GalleryStore =
        object : GalleryStore, Store<GalleryStore.Intent, GalleryStore.State, Nothing> by storeFactory.create(
            name = "GalleryStore",
            initialState = GalleryStore.State(),
            bootstrapper = SimpleBootstrapper(Action.Load),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl,
        ) {}

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
    }
}

private fun com.example.picturegrid.feature.gallery.api.GalleryError.toMessage(): String =
    when (this) {
        is com.example.picturegrid.feature.gallery.api.GalleryError.Network -> "Network: ${message}"
        is com.example.picturegrid.feature.gallery.api.GalleryError.Storage -> "Storage: ${message}"
    }
