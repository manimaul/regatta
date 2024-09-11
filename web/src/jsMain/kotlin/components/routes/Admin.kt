package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mxmariner.regatta.data.Boat
import components.*
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.dom.*
import viewmodel.LoginStatus
import viewmodel.LoginViewModel
import viewmodel.loginViewModel
import viewmodel.routeViewModel


@Composable
fun Admin(
    create: Boolean,
    viewModel: LoginViewModel = loginViewModel) {
    val flowState by viewModel.flow.collectAsState()
    Div {
        when (flowState.loginStatus) {
            LoginStatus.Loading -> Progress { }
            LoginStatus.Ready -> {
                Login(create)
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
                if (create) {
                    Login(create)
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
fun Login(
    create: Boolean,
    viewModel: LoginViewModel = loginViewModel) {
    val flowState by viewModel.flow.collectAsState()
    H2 {
        if (create) {
            Text("Add Login")
        } else {
            Text("Login")
        }
    }
    Form(attrs = {
        onSubmit { it.preventDefault() }
    }) {
        RgDiv(customizer = { set(RgSpace.m, RgSide.b, RgSz.s3) }) {
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
        RgDiv(customizer = { set(RgSpace.m, RgSide.b, RgSz.s3) }) {
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
        val label = if (create) "Add" else "Login"
        RgButton(label, RgButtonStyle.Primary, flowState.pass.length < 4) {
            if (create) {
                viewModel.submitNew()
            } else {
                viewModel.login()
            }
        }
    }
}
