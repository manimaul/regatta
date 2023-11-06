package components.routes

import androidx.compose.runtime.Composable
import components.Spinner
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import viewmodel.LoginStatus
import viewmodel.LoginViewModel
import viewmodel.loginViewModel
import viewmodel.routeViewModel


@Composable
fun Admin(viewModel: LoginViewModel = loginViewModel) {
    Div {
        when (viewModel.loginStatus) {
            LoginStatus.Loading -> Spinner(50f)
            LoginStatus.Ready -> {
                Login()
            }
            LoginStatus.Complete -> {
                Div {
                    Text("Done")
                }
            }
            LoginStatus.LoggedIn -> {
                H4 {
                    Text("Logged in as ${viewModel.userName}")
                }
                val creator = routeViewModel.getQueryParam("create").isNotEmpty()
                if (creator) {
                    Login()
                }
            }
            LoginStatus.Failed -> {
                P {
                    Text(viewModel.state.errorMessage ?: "")
                }
                viewModel.reload()
            }
        }
    }
}

@Composable
fun Login(viewModel: LoginViewModel = loginViewModel) {
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