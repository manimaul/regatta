package styles

import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.px

object AppStyle : StyleSheet() {
    val regattaStyle by style {
        margin(8.px)
    }
}