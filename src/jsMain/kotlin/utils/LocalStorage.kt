package utils

import com.mxmariner.regatta.data.AuthRecord
import com.mxmariner.regatta.data.LoginResponse
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> localStoreSet(item: T?) {
    T::class.simpleName?.let { key ->
        val value = item?.let { Json.encodeToString(it) } ?: ""
        window.localStorage.setItem(key, value)
    }
}

inline fun <reified T> localStoreGet() : T? {
    return T::class.simpleName?.let { key ->
        window.localStorage.getItem(key)?.let { value ->
           Json.decodeFromString(value)
        }
    }
}

fun token() : String {
    return localStoreGet<LoginResponse>()?.token() ?: "none"
}
