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
    )

    sealed interface Intent {
        data object Retry : Intent
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
    }

    private object ReducerImpl : Reducer<ViewerStore.State, Msg> {
        override fun ViewerStore.State.reduce(msg: Msg): ViewerStore.State = when (msg) {
            Msg.Loading -> copy(isLoading = true, error = null)
            is Msg.Loaded -> copy(isLoading = false, file = msg.file, error = null)
            is Msg.Failed -> copy(isLoading = false, error = msg.message)
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
