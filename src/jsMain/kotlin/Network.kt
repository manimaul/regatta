import com.mxmariner.regatta.versionedApi
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json

object Network {
    suspend inline fun <reified T> fetch(api: String) : T {
        val response = window.fetch(api.versionedApi())
            .await()
            .text()
            .await()
        return Json.decodeFromString(response)
    }
}