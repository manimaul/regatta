package components.routes

import androidx.compose.runtime.*
import components.Clock
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import viewmodel.ClockViewModel
import viewmodel.HomeViewModel

@Composable
fun Home(
    viewModel: HomeViewModel = remember { HomeViewModel() }
) {
    viewModel.loggedInPerson?.let {
        H4 {
            Text("Logged in as ${it.first} ${it.last}")
        }
        Clock(viewModel)
    } ?: run {
        Clock(viewModel)
        Login { userName, password ->

        }
    }
}

@Composable
fun Login(
    onSubmit: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    Div {
        Input(type = InputType.Text) {
            placeholder("user name")
            onInput {
                name = it.value
            }
            value(name)
        }
        Br()
        Input(type = InputType.Password) {
            placeholder("password")
            onInput {
                name = it.value
            }
            value(name)
        }
        Br()
        Button(attrs = {
            onClick {
                onSubmit(name, pass)
                name = ""
            }
        }) {
            Text("Login")
        }
    }

}
