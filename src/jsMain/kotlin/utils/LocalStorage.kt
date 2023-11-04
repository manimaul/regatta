package utils

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


fun foo() {
//    function sha512(str) {
//        return crypto.subtle.digest("SHA-512", new TextEncoder("utf-8").encode(str)).then(buf => {
//            return Array.prototype.map.call(new Uint8Array(buf), x=>(('00'+x.toString(16)).slice(-2))).join('');
//        });
//    }
}
