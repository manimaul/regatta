package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

@Composable
fun Row(content: ContentBuilder<HTMLDivElement>? = null) {
    Div(attrs = { classes("row")}, content = content)
}
@Composable
fun Col4(content: ContentBuilder<HTMLDivElement>? = null) {
    Div(attrs = { classes("col-4")}, content = content)
}

@Composable
fun Column(content: ContentBuilder<HTMLDivElement>? = null) {
    Div(attrs = { classes("col")}, content = content)
}
