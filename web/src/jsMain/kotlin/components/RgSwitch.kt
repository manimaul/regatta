package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import utils.salt

@Composable
fun RgSwitch(
    id: String,
    num: Int,
    label: String,
    check: () -> Boolean,
    change: (Boolean) -> Unit,
) {
    Div(attrs = {
        classes("form-check", "form-switch")
    }) {
        Input(InputType.Checkbox) {
            classes("form-check-input")
            id("$id$num")
            onChange { change(it.value) }
            checked(check())
        }
        Label(attrs = {
            classes("form-check-label")
            attr("for", "$id$num")
        }) {
            Text(label)
        }
    }
}

@Composable
fun <T> RgSwitches(
    items: List<T>,
    check: (T) -> Boolean,
    label: (T) -> String,
    change: (T, Boolean) -> Unit,
) {
    items.forEachIndexed { i, t ->
        RgSwitch(salt(), i, label(t), { check(t) }) {
            change(t, it)
        }
    }
}