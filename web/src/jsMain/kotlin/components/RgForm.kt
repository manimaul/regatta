package components

import androidx.compose.runtime.*
import kotlinx.datetime.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLFormElement
import styles.AppStyle
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

enum class NumberType {
    NumberInt, NumberFloat, NumberDouble
}

@Composable
fun RgNumberInput(
    label: String,
    value: Number?,
    placeHolder: Boolean = false,
    customClasses: List<String>? = null,
    numberType: NumberType = NumberType.NumberInt,
    listener: (Number) -> Unit
) {
    var locked by remember { mutableStateOf(false)}
    var rNumber by remember { mutableStateOf(value?.toString() ?: "")}
    if (!locked) {
        val numberValue = when (numberType) {
            NumberType.NumberInt -> value?.toInt()
            NumberType.NumberFloat -> value?.toFloat()
            NumberType.NumberDouble -> value?.toDouble()
    }
        rNumber = numberValue?.toString() ?: ""
    }
    val id = remember { "${++num}_input" }
    Label(id) { B { Text(label) } }
    Input(InputType.Text) {
        id(id)
        customClasses?.let { classes(it) }
        if (placeHolder) {
            placeholder(label)
        }
        classes("form-control")
        inputMode(InputMode.Numeric)
        value(rNumber)
        onChange {
            locked = false
        }
        onInput { input ->
            locked = true
            rNumber = input.value
            when (numberType) {
                NumberType.NumberInt -> input.value.toIntOrNull()
                NumberType.NumberFloat -> input.value.toFloatOrNull()
                NumberType.NumberDouble -> input.value.toDoubleOrNull()
            }.takeIf { input.value.isNotBlank() && input.value != "-" }?.let {
                listener(it)
            }
        }
    }
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
    Label(id, attrs = {
        classes(AppStyle.marginEnd)
    }) { B { Text(label) } }
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
fun RgInputWithButton(
    label: String? = null,
    value: String,
    btnLabel: String,
    disabled: Boolean = false,
    btnDisabled: Boolean = false,
    listener: (String, Boolean) -> Unit,
) {
    val id = remember { "${++num}_input" }
    label?.let {
        Label(id, attrs = {
            classes(AppStyle.marginEnd)
        }) { B { Text(label) } }
    }
    Div(attrs = {
        classes("input-group", "mb-3")
    }) {
        Input(type = InputType.Text) {
            classes("form-control")
            id(id)
            label?.let { placeholder(it) }
            if (disabled) {
                disabled()
            }
            value(value)
            onInput {
                listener(it.value.trim(), false)
            }
        }
        Button(attrs = {
            classes("btn", "btn-outline-primary")
            if (btnDisabled) {
                disabled()
            }
            onClick {
                listener(value.trim(), true)
            }
        }) {
            Text(btnLabel)
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
