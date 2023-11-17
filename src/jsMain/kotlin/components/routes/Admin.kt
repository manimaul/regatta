package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import components.RgButton
import components.RgButtonStyle
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.dom.*
import viewmodel.LoginStatus
import viewmodel.LoginViewModel
import viewmodel.loginViewModel
import viewmodel.routeViewModel


@Composable
fun Admin(viewModel: LoginViewModel = loginViewModel) {
    val flowState by viewModel.flow.collectAsState()
    Div {
        when (flowState.loginStatus) {
            LoginStatus.Loading -> Progress { }
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
                    Text("Logged in as ${flowState.auth.userName}")
                }
                val creator = routeViewModel.getQueryParam("create").isNotEmpty()
                if (creator) {
                    Login()
                }
            }

            LoginStatus.Failed -> {
                Progress { }
                P {
                    Text(flowState.errorMessage ?: "")
                    viewModel.reload()
                }
            }
        }
    }
}

@Composable
fun Login(viewModel: LoginViewModel = loginViewModel) {
    val creator = routeViewModel.getQueryParam("create").isNotEmpty()
    val flowState by viewModel.flow.collectAsState()
    H2 {
        if (creator) {
            Text("Add Login")
        } else {
            Text("Login")
        }
    }
    Form(attrs = {
        onSubmit { it.preventDefault() }
    }) {
        Div(attrs = { classes("mb-3") }) {
            Label("username") {
                Text("Username")
            }
            Input(type = InputType.Text) {
                id("username")
                classes("form-control")
                onInput {
                    viewModel.setUserName(it.value)
                }
                value(flowState.auth.userName)
            }

        }
        Div(attrs = { classes("mb-3") }) {
            Label("password") {
                Text("Password")
            }
            Input(type = InputType.Password) {
                id("password")
                classes("form-control")
                onInput {
                    viewModel.setPassword(it.value)
                }
                value(flowState.pass)
            }

        }
        val label = if (creator) "Add" else "Login"
        RgButton(label, RgButtonStyle.Primary, flowState.pass.length < 4) {
            if (creator) {
                viewModel.submitNew()
            } else {
                viewModel.login()
            }
        }
    }
}
