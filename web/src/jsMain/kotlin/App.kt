import org.jetbrains.compose.web.renderComposable

@JsExport
@ExperimentalJsExport
abstract class AppController {
    abstract fun notifyReady()
    abstract fun dispose()
}

/**
 * @param rootId - an id of an HTML element which is already added into the DOM.
 * Compose will manage the content of this element.
 *
 * @return instance of [AppController] to control the composition in non-compose code.
 */
@JsExport
@ExperimentalJsExport
fun ComposeApp(rootId: String): AppController {
    val composition = renderComposable(rootElementId = rootId) { Router() }

    return object : AppController() {
        override fun notifyReady() {
            println("compose app ready")
        }

        override fun dispose() {
            composition.dispose()
        }
    }
}
