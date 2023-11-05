package components.routes

import androidx.compose.runtime.*
import components.Clock
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
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
        Login(viewModel)
    }
}

@Composable
fun Login(
    viewModel: HomeViewModel
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
                pass = it.value
                viewModel.password(pass)
            }
            value(pass)
        }
        CheckboxInput {
            checked(viewModel.admin)
            id("Admin")
            name("Admin")
            onChange {
                viewModel.admin = it.value
            }
        }
        Label("Admin") { Text("Admin") }
        viewModel.hash.takeIf { it.isNotBlank() }?.let {
            Br()
            Br()
            Text(viewModel.hash)
            Br()
        }
        Br()
        Button(attrs = {
            onClick {
//                name = ""
            }
        }) {
            Text("Login")
        }
    }

}
