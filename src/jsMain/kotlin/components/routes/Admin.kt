package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import components.Spinner
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import viewmodel.LoginViewModel
import viewmodel.State
import viewmodel.routeViewModel


@Composable
fun Admin(
    viewModel: LoginViewModel = remember { LoginViewModel() },
) {
    Div {
        when (viewModel.state) {
            State.Loading -> Spinner(50f)
            State.Ready -> {
                Login(viewModel)
            }
            State.Complete -> {
                Div {
                    Text("Done")
                }
            }
            State.LoggedIn -> {
                H4 {
                    Text("Logged in as ${viewModel.userName}")
                }
                Button(attrs = {
                    onClick {
                        viewModel.logout()
                    }
                }) {
                    Text("Logout")
                }
            }
            State.Failed -> {
                P {
                    Text("Error")
                }
                Spinner(50f)
                viewModel.reload()
            }
        }
    }
}

@Composable
fun Login(
    viewModel: LoginViewModel
) {
    val creator = routeViewModel.getQueryParam("create").isNotEmpty()
    H2 {
        if (creator) {
            Text("Add Login")
        } else {
            Text("Login")
        }
    }
    Input(type = InputType.Text) {
        placeholder("user name")
        onInput {
            viewModel.userName = it.value
        }
        value(viewModel.userName)
    }
    Br()
    Input(type = InputType.Password) {
        placeholder("password")
        onInput {
            viewModel.password = it.value
        }
        value(viewModel.password)
    }
    Br()
    Button(attrs = {
        if (!viewModel.isValid()) {
            disabled()
        }
        onClick {
            if (creator) {
                viewModel.submitNew()
            } else {
                viewModel.login()
            }
        }
    }) {
        Text(if (creator) "Add" else "Login")
    }

}