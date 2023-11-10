package viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import utils.Scopes
import kotlin.coroutines.CoroutineContext

interface VmState
sealed class Async<out T>(
    val complete: Boolean,
    private val value: T?
) {
    fun loading() : Loading<T> {
        return Loading(value)
    }

    fun error(throwable: Throwable? = null) : Error<T> {
        return Error(value, throwable)
    }
}


fun <T> complete(value: T?) = Complete(value)

object Uninitialized : Async<Nothing>(false, null)
data class Loading<out T>(val value: T? = null) :  Async<T>(false, value)
data class Complete<out T>(val value: T) :  Async<T>(true, value)
data class Error<out T>(val value: T? = null, val error: Throwable? = null) :  Async<T>(true, value)

abstract class BaseViewModel<T : VmState>(
    initialState: T,
    context: CoroutineContext = Scopes.mainScope.coroutineContext
) : CoroutineScope by CoroutineScope(context){
    private val internalState = MutableStateFlow(initialState)
    val flow: StateFlow<T>
        get() = internalState

    protected fun withState(handler: (T) -> Unit) {
        handler(internalState.value)
    }

    protected fun setState(reducer: T.() -> T) {
        internalState.value = reducer(internalState.value)
    }
}