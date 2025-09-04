import androidx.compose.runtime.DisposableEffectResult
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.renderComposable
import utils.ResizeObserver

external fun require(module: String): dynamic

val resizeHandler = ResizeObserver { entries, observer ->
    val wh = window.innerHeight
    val h = entries[0].contentBoxSize[0].blockSize.toInt()
    println("resize wh=$wh h = $h")
    window.parent.postMessage(
        JSON.parse("{\"regatta_scroll_height\": $h}"),
        "*"
    )
}

fun main() {
    require("maplibre-gl/dist/maplibre-gl.css")
    require("bootstrap/dist/css/bootstrap.css")
    require("bootstrap/dist/js/bootstrap.bundle.js")
    renderComposable(rootElementId = "root") {
        Div(attrs = {
            ref {
                resizeHandler.observe(it)
                object : DisposableEffectResult {
                    override fun dispose() {
                        resizeHandler.unobserve(it)
                    }
                }
            }
        }) { Router() }
    }
}
