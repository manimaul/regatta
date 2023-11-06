package utils

import com.mxmariner.regatta.versionedApi
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.w3c.fetch.RequestInit
import kotlin.js.json

object Network {
    suspend inline fun <reified T> fetch(api: String): T {
        val response = window.fetch(api.versionedApi())
            .await()
            .text()
            .await()
        return Json.decodeFromString(response)
    }
    suspend inline fun <reified T> post(api: String, item: T): T? {
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
        return if (response.ok) {
            val body = response.text().await()
            return Json.decodeFromString(body)

        } else {
            null
        }

    }

    suspend fun delete(api: String, params: Map<String, String>): Int {
        return window.fetch(api.versionedApi(params), RequestInit(method = "DELETE"))
            .await()
            .status.toInt()
    }
}