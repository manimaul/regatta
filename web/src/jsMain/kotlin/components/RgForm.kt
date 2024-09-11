package components

import androidx.compose.runtime.*
import kotlinx.datetime.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLFormElement
import utils.*
import viewmodel.RgTimeViewModel

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
    customClasses: List<String>? = null,
    listener: (String) -> Unit
) {
    val id = remember { "${++num}_input" }
    if (!placeHolder) {
        Label(id) { B { Text(label) } }
    }
    Input(InputType.Text) {
        id(id)
        customClasses?.let { classes(it) }
        if (placeHolder) {
            placeholder(label)
        }
        classes("form-control")
        value(value)
        onInput {
            listener(it.value.trimStart())
        }
    }
}

@Composable
fun RgTimeButton(
    modalTitle: String,
    date: Instant,
    listener: (Instant) -> Unit
) {
    var saveDate by remember { mutableStateOf(date) }
    RgModal(
        buttonLabel = date.timeStr(),
        modalTitle = modalTitle,
        openAction = {
            saveDate = date
        },
        content = {
            RgTime(date = saveDate, showSeconds = true, listener = {
                saveDate = it
            })
        }, footer = {
            Button(attrs = {
                classes(*RgButtonStyle.PrimaryOutline.classes)
                attr("data-bs-dismiss", "modal")
            }) {
                Text("Cancel")
            }
            Button(attrs = {
                classes(*RgButtonStyle.Success.classes)
                attr("data-bs-dismiss", "modal")
                onClick {
                    listener(saveDate)
                }
            }) {
                Text("Done")
            }
        })

}

@Composable
fun RgTime(
    label: String? = null,
    date: Instant,
    showSeconds: Boolean = false,
    showDate: Boolean = false,
    viewModel: RgTimeViewModel = remember { RgTimeViewModel(date, showSeconds) },
    listener: (Instant) -> Unit
) {
    val state = viewModel.flow.collectAsState()
    viewModel.setInstant(date)
    if (showDate) {
        Div(attrs = {
            style {
                maxWidth(if (showDate) 600.px else 400.px)
            }
            classes("input-group", "input-group-sm")
        }) {
            label?.let {
                Span(attrs = { classes("input-group-text") }) { Text(label) }
            }
            Input(InputType.Date) {
                classes("form-control")
                value(state.value.localTime.inputStr())
                onInput {
                    viewModel.setInputDate(it.value)?.let(listener)
                }
            }
        }
    }
    Div(attrs = {
        style {
            maxWidth(if (showDate) 600.px else 400.px)
        }
        classes("input-group", "input-group-sm")
    }) {
        label?.takeIf { !showDate }?.let {
            Span(attrs = { classes("input-group-text") }) { Text(label) }
        }
        Input(InputType.Number) {
            classes("form-control")
            value(state.value.localTime.hour.doubleDigit())
            onInput {
                viewModel.setInputHour(it.value)?.let(listener)
            }
        }
        Span(attrs = { classes("input-group-text") }) { Text(":") }
        Input(InputType.Number) {
            classes("form-control")
            value(state.value.localTime.minute.doubleDigit())
            onInput {
                viewModel.setInputMinute(it.value)?.let(listener)
            }
        }
        if (showSeconds) {
            Span(attrs = { classes("input-group-text") }) { Text(":") }
            Input(InputType.Number) {
                classes("form-control")
                value(state.value.localTime.second.doubleDigit())
                onInput {
                    viewModel.setInputSecond(it.value)?.let(listener)
                }
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
