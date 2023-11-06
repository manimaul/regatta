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

object Network {

    suspend inline fun <reified T> Response.networkResponse() : NetworkResponse<T> {
        var body: T? = null
        var error: Exception? = null
        try {
            body = Json.decodeFromString(text().await())
        } catch (e: Exception) {
            error = e
        }
        return NetworkResponse(url, ok, body, status, statusText, error)
    }
    suspend inline fun <reified T> get(api: String): NetworkResponse<T> {
        val response = window.fetch(
            api.versionedApi(),
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

    suspend inline fun <reified T, reified R> post(api: String, item: T): NetworkResponse<R> {
        val response = window.fetch(
            api.versionedApi(),
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

    suspend fun delete(api: String, params: Map<String, String>): Int {
        return window.fetch(
            api.versionedApi(params), RequestInit(
                method = "DELETE",
                headers = json("Authorization" to "Bearer ${token()}"),
            )
        ).await().status.toInt()
    }
}