package utils

import com.mxmariner.regatta.data.LoginResponse
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


fun localStoreSet(key: String, value: String) {
    window.localStorage.setItem(key, value)
}

fun localStoreGet(key: String): String? {
    return window.localStorage.getItem(key)
}

inline fun <reified T> localStoreSetById(id: String, item: T?) {
    T::class.simpleName?.let { key ->
        val value = item?.let { Json.encodeToString(it) } ?: ""
        window.localStorage.setItem("${id}_$key", value)
    }
}

inline fun <reified T> localStoreGetById(id: String): T? {
    return T::class.simpleName?.let { key ->
        window.localStorage.getItem("${id}_$key")?.let { value ->
            try {
                val decode = Json.decodeFromString<T>(value)
                decode
            } catch (e: Exception) {
                null
            }
        }
    }
}

inline fun <reified T> localStoreSet(item: T?) {
    T::class.simpleName?.let { key ->
        val value = item?.let { Json.encodeToString(it) } ?: ""
        window.localStorage.setItem(key, value)
    }
}

inline fun <reified T> localStoreGet(): T? {
    return T::class.simpleName?.let { key ->
        window.localStorage.getItem(key)?.let { value ->
            try {
                Json.decodeFromString(value)
            } catch (e: Exception) {
                null
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
inline fun <reified T> localStoreGetEncoded(): String? {
    return T::class.simpleName?.let { key ->
        window.localStorage.getItem(key)?.takeIf { it.isNotBlank() }?.let { value ->
            try {
                Base64.encode(value.encodeToByteArray())
            } catch (e: Exception) {
                null
            }
        }
    }
}

fun token(): String {
    return localStoreGetEncoded<LoginResponse>() ?: "none"
}
fun LoginResponse.isExpired() : Boolean {
    return expires.minus(kotlinx.datetime.Clock.System.now()).isNegative()
}
