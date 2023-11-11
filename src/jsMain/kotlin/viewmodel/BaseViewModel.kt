package viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import utils.NetworkResponse
import utils.Scopes
import kotlin.coroutines.CoroutineContext

interface VmState

sealed class Async<out T>(
    val complete: Boolean,
    open val value: T?
) {

    fun loading(): Loading<T> {
        return Loading(value)
    }

    fun error(throwable: Throwable? = null): Error<T> {
        return Error(value, throwable)
    }
}

suspend fun <T, R> Async<T>.flatMap(handler: suspend (T) -> Async<R>) : Async<R> {
    return value?.let { handler(it) } ?: Error()
}

suspend fun <T, R> Async<T>.map(handler: suspend (T) -> R) : Async<R> {
    return value?.let { Complete(handler(it)) } ?: Error()
}

fun <T> NetworkResponse<T>.toAsync(previous: Async<T>? = null): Async<T> {
    return body?.let {
        Complete(it)
    } ?: Error(previous?.value, error = error)
}

suspend fun <T, R, O> combine(
    one: NetworkResponse<T>,
    two: NetworkResponse<R>,
    reducer: suspend (T, R) -> O
): O? {
    val oneAsync = one.toAsync()
    val twoAsync = two.toAsync()
    return if (oneAsync is Complete && twoAsync is Complete) {
        reducer(oneAsync.value, twoAsync.value)
    } else {
        null
    }
}
suspend fun <T, R, O> combineAsync(
    one: NetworkResponse<T>,
    two: NetworkResponse<R>,
    reducer: suspend (T, R) -> O
): Async<O> {
    val oneAsync = one.toAsync()
    val twoAsync = two.toAsync()
    return if (oneAsync is Complete && twoAsync is Complete) {
        Complete(reducer(oneAsync.value, twoAsync.value))
    } else {
        Error()
    }
}

//suspend fun <A, B, C> combineAsync(one: Async<A>, two: Async<B>, handler: suspend (A, B) -> C) : Async<C> {
//    return one.value?.let { a ->
//        two.value?.let { b ->
//            Complete(handler(a, b))
//        }
//    } ?: Error()
//}

object Uninitialized : Async<Nothing>(false, null)
data class Loading<out T>(override val value: T? = null) : Async<T>(false, value)
data class Complete<out T>(override val value: T) : Async<T>(true, value)
data class Error<out T>(override val value: T? = null, val error: Throwable? = null) : Async<T>(true, value)

abstract class BaseViewModel<T : VmState>(
    initialState: T,
    context: CoroutineContext = Scopes.mainScope.coroutineContext
) : CoroutineScope by CoroutineScope(context) {
    private val internalState = MutableStateFlow(initialState)
    val flow: StateFlow<T>
        get() = internalState

    protected fun withState(handler: (T) -> Unit) {
        handler(internalState.value)
    }

//    protected fun <R> setState(
//        network: suspend () -> NetworkResponse<R>?,
//        reducer: suspend T.(R?) -> T,
//    ) {
//        launch {
//            val response = network()
//            internalState.value = reducer(internalState.value, response?.body)
//        }
//    }

//    protected fun <A, B> setState(
//        network: suspend () -> NetworkResponse<A>?,
//        network2: suspend () -> NetworkResponse<B>?,
//        reducer: suspend T.(A?, B?) -> T,
//    ) {
//        launch {
//            internalState.value = reducer(internalState.value, network()?.body, network2()?.body)
//        }
//    }

    protected fun setState(reducer: suspend T.() -> T) {
        launch {
            internalState.value = reducer(internalState.value)
        }
    }
}
