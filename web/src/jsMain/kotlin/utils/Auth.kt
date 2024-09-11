package utils

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

private val crypto: dynamic
    get() =
        js("crypto")

private val subtle: dynamic get() = crypto.subtle

@OptIn(ExperimentalEncodingApi::class)
private suspend fun hashInternal(data: String) : String {
    val digest = (subtle.digest("SHA-512", data.encodeToByteArray()) as Promise<ArrayBuffer>).await()
    val hash = Int8Array(digest).unsafeCast<ByteArray>()
    return Base64.encode(hash)
}

fun salt() : String {
    return crypto.randomUUID()
}
suspend fun hash(vararg data: String) : String {
    var result = ""
    data.forEach {
        result = hashInternal(it + result)
    }
    return result
}
