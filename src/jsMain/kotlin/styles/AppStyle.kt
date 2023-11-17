package styles

import org.jetbrains.compose.web.css.*

object AppStyle : StyleSheet() {
    private const val marginSize = 8

    val regattaStyle by style {
        margin(marginSize.px)
    }

    val marginVert by style {
        marginTop(marginSize.px)
        marginBottom(marginSize.px)
    }
}