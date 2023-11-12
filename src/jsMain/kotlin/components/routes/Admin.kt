package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import components.RgButton
import components.RgButtonStyle
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.*
import viewmodel.LoginStatus
import viewmodel.LoginViewModel
import viewmodel.loginViewModel
import viewmodel.routeViewModel


@Composable
fun Admin(viewModel: LoginViewModel = loginViewModel) {
    val flowState by viewModel.flow.collectAsState()
    Div {
        when (flowState.state) {
            LoginStatus.Loading -> Progress {  }
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
                Progress {  }
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
    Input(type = InputType.Text) {
        placeholder("user name")
        onInput {
            viewModel.setUserName(it.value)
        }
        value(flowState.auth.userName)
    }
    Br()
    Input(type = InputType.Password) {
        placeholder("password")
        onInput {
            viewModel.setPassword(it.value)
        }
        value(flowState.pass)
    }
    Br()
    val label = if (creator) "Add" else "Login"
    RgButton(label, RgButtonStyle.Primary, flowState.pass.length < 4) {
        if (creator) {
            viewModel.submitNew()
        } else {
            viewModel.login()
        }
    }
}
