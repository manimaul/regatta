package utils

import com.mxmariner.regatta.versionedApi
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.js.json


data class NetworkResponse<T>(
    val url: String? = null,
    val ok: Boolean,
    val body: T? = null,
    val status: Short? = null,
    val statusText: String? = null,
    val error: Exception? = null
)

fun <T, R> NetworkResponse<T>.map(mapper: (T) -> R) : NetworkResponse<R> {
    return body?.let {
        NetworkResponse(url, ok, mapper(it), status, statusText, error)
    } ?: NetworkResponse(url, ok, null, status, statusText, error)
}

object Network {

    suspend inline fun <reified T> Response.networkResponse(setBody: T? = null): NetworkResponse<T> {
        var body: T? = setBody
        var error: Exception? = null
        if (body == null) {
            try {
                body = Json.decodeFromString(text().await())
            } catch (e: Exception) {
                error = e
            }
        }
        return NetworkResponse(url, ok, body, status, statusText, error)
    }

    suspend inline fun <reified T> get(api: String, params: Map<String, String>? = null): NetworkResponse<T> {
        val response = window.fetch(
            api.versionedApi(params = params),
            RequestInit(
                method = "GET",
                headers = json(
                    "Accept" to "application/json",
                    "Authorization" to "Bearer ${token()}"
                ),
            )
        ).await()
        return response.networkResponse()
    }

    suspend inline fun <reified T, reified R> post(api: String, item: T, params: Map<String, String>? = null): NetworkResponse<R> {
        val response = window.fetch(
            api.versionedApi(params = params),
            RequestInit(
                method = "POST",
                headers = json(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json",
                    "Authorization" to "Bearer ${token()}"
                ),
                body = Json.encodeToJsonElement(item)
            )
        ).await()
        return response.networkResponse()
    }

    suspend fun delete(api: String, params: Map<String, String>): NetworkResponse<Unit> {
        val response = window.fetch(
            api.versionedApi(params = params), RequestInit(
                method = "DELETE",
                headers = json("Authorization" to "Bearer ${token()}"),
            )
        ).await()
        return response.networkResponse(Unit)
    }
}