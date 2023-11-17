package utils

sealed class Async<out T>(
    val complete: Boolean,
    open val value: T?,
    open val error: List<Throwable>? = null,
) {

    fun loading(): Loading<T> {
        return Loading(value)
    }

    fun error(throwable: Throwable? = null): Error<T> {
        return Error.from(throwable, value = value)
    }

    override fun toString(): String {
        val prefix = when (this) {
            is Complete -> "Complete"
            is Error -> "Error"
            is Loading -> "Loading"
            Uninitialized -> "Uninitialized"
        }
        return "$prefix:Async(complete=$complete, value=$value)"
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
    } ?: Error.from(error, value = previous?.value)
}

object Uninitialized : Async<Nothing>(false, null)
data class Loading<out T>(override val value: T? = null) : Async<T>(false, value)
data class Complete<out T>(override val value: T) : Async<T>(true, value)
data class Error<out T>(
    override val value: T? = null,
    override val error: List<Throwable> = emptyList(),
    val message: String? = null,
) : Async<T>(true, value) {
    companion object {
        fun <T> from(vararg errors: Throwable?,  value: T? = null, message: String? = null) : Error<T> {
            val error = errors.filterNotNull()
            return Error(value, error, message)
        }
        fun <T> from(vararg errors: List<Throwable>?,  value: T? = null, message: String? = null) : Error<T> {
            val error = errors.filterNotNull().flatten()
            return Error(value, error, message)
        }
    }
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
suspend fun <A, B, R> combineAsync(
    one: NetworkResponse<A>,
    two: NetworkResponse<B>,
    reducer: suspend (A, B) -> R
): Async<R> {
    val oneAsync = one.toAsync()
    val twoAsync = two.toAsync()
    return if (oneAsync is Complete && twoAsync is Complete) {
        Complete(reducer(oneAsync.value, twoAsync.value))
    } else {
        Error.from(oneAsync.error, twoAsync.error)
    }
}
suspend fun <A, B, C, R> combineAsync(
    one: NetworkResponse<A>,
    two: NetworkResponse<B>,
    three: NetworkResponse<C>,
    reducer: suspend (A, B, C) -> R
): Async<R> {
    val oneAsync = one.toAsync()
    val twoAsync = two.toAsync()
    val threeAsync = three.toAsync()
    return if (oneAsync is Complete && twoAsync is Complete && threeAsync is Complete) {
        Complete(reducer(oneAsync.value, twoAsync.value, threeAsync.value))
    } else {
        Error.from(oneAsync.error, twoAsync.error, threeAsync.error)
    }
}
