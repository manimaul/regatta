package viewmodel

import androidx.compose.runtime.Composable
import components.ErrorDisplay
import components.RgSpinner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import utils.*

interface VmState

abstract class BaseViewModel<T : VmState>(
    initialState: T,
) : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private val internalState = MutableStateFlow(initialState)
    val flow: StateFlow<T>
        get() = internalState

    protected fun <A> withState(handler: (T) -> A): A {
        return handler(internalState.value)
    }

    protected fun <A> MutableStateFlow<A>.setState(reducer: suspend A.() -> A) {
        launch {
            value = reducer(value)
        }
    }

    protected fun setState(reducer: suspend T.() -> T) {
        launch {
            internalState.value = reducer(internalState.value)
        }
    }

    protected fun <A, B> setState(
        n1: suspend () -> Async<A>,
        n2: suspend () -> Async<B>,
        reducer: suspend T.(Async<A>, Async<B>) -> T
    ) {
        launch {
            internalState.value = reducer(internalState.value, n1(), n2())
        }
    }

    protected fun <A> setState(
        n: suspend () -> Async<A>,
        reducer: suspend T.(Async<A>) -> T
    ) {
        launch {
            internalState.value = reducer(internalState.value, n())
        }
    }

    abstract fun reload()

}

@Composable
fun <A> Async<A>.complete(viewModel: BaseViewModel<*>, handler: @Composable (A) -> Unit) {
    when (val event = this) {
        is Complete -> handler(event.value)
        is Error -> ErrorDisplay(event) {
            viewModel.reload()
        }

        is Loading -> RgSpinner()
        Uninitialized -> Unit
    }
}
