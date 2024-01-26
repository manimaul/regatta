package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.Instant
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLFormElement
import utils.datePickerInstant
import utils.formattedDateString

private var num = 0

@Composable
fun RgForm(
    content: ContentBuilder<HTMLFormElement>? = null
) {
    Form(attrs = {
        onSubmit {
            it.preventDefault()
        }
    }, content = content)
}

@Composable
fun RgInput(
    label: String,
    value: String,
    placeHolder: Boolean = false,
    listener: (String) -> Unit
) {
    val id = remember { "${++num}_input" }
    if (!placeHolder) {
        Label(id) { B { Text(label) } }
    }
    Input(InputType.Text) {
        id(id)
        if (placeHolder) {
            placeholder(label)
        }
        classes("form-control")
        value(value)
        onInput {
            listener(it.value)
        }
    }
}

@Composable
fun RgDate(
    label: String,
    date: Instant?,
    placeHolder: Boolean = false,
    time: Boolean = false,
    seconds: Boolean = false,
    listener: (Instant) -> Unit
) {
    println("RgDate $label $date")
    val id = remember { "${++num}_input" }
    if (!placeHolder) {
        Label(id) { B { Text(label) } }
    }
    Input(if (time) InputType.DateTimeLocal else InputType.Date) {
        id(id)
        if (placeHolder) {
            placeholder(label)
        }
        classes("form-control")
        if (seconds) {
            attr("step", "1")
        }
        date?.let {
            val ts = it.formattedDateString(time)
            value(ts)
        }
        onInput {
            it.value.takeIf { it.isNotBlank() }?.let {
                listener(it.datePickerInstant())
            }
        }
    }
}

@Composable
fun RgCheck(
    label: String,
    checked: Boolean,
    bold: Boolean = true,
    customClasses: List<String>? = null,
    listener: (Boolean) -> Unit
) {
    val id = remember { "${++num}_input" }
    Div(attrs = {
        customClasses?.let {
            classes(it)
        } ?: classes("form-check")
    }) {
        Label(id, attrs = { classes("form-check-label") }) {
            if (bold) {
                B { Text(label) }
            } else {
                Text(label)
            }
        }
        CheckboxInput(
            attrs = {
                classes("form-check-input")
                id(id)
                checked(checked)
                onChange {
                    listener(it.value)
                }
            }
        )
    }
}
