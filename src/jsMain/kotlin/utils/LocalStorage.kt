package utils

import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.LoginResponse
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


fun localStoreSet(key: String, value: String) {
    window.localStorage.setItem(key, value)
}

fun localStoreGet(key: String): String? {
    return window.localStorage.getItem(key)
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

fun token(): String {
    return localStoreGet<LoginResponse>()?.token() ?: "none"
}
