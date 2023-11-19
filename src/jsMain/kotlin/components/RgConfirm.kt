package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle

@Composable
fun RgConfirm(
    msg: String,
    subTitle: String? = null,
    handler: (Boolean) -> Unit
) {
    Div {
        H3 { Text(msg) }
        subTitle?.let { P { Text(it) } }
        RgButton("No", RgButtonStyle.Danger, customClasses = listOf(AppStyle.marginEnd)) {
            handler(false)
        }
        RgButton("Yes", RgButtonStyle.Primary, customClasses = listOf(AppStyle.marginStart)) {
            handler(true)
        }
    }
}
@Composable
fun RgOk(
    title: String,
    subTitle: String? = null,
    handler: () -> Unit
) {
    Div {
        H3 { Text(title) }
        subTitle?.let { P { Text(it) } }
        RgButton("Ok", RgButtonStyle.Primary) {
            handler()
        }
    }
}
