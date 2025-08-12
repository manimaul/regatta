import org.jetbrains.compose.web.renderComposable

external fun require(module: String): dynamic

fun main() {
    require("bootstrap/dist/css/bootstrap.css")
    require("bootstrap/dist/js/bootstrap.bundle.js")
    renderComposable(rootElementId = "root") { Router() }
}
