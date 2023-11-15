package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import components.RgButton
import components.RgButtonStyle
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import utils.token
import viewmodel.LoginStatus
import viewmodel.LoginViewModel
import viewmodel.loginViewModel

@Composable
fun Home(
    viewModel: LoginViewModel = loginViewModel,
) {
    val state by viewModel.flow.collectAsState()
    if (state.loginStatus == LoginStatus.LoggedIn) {
        H4 {
            Text("Logged in as ${state.auth.userName}")
        }
        RgButton("Copy auth token", RgButtonStyle.Primary) {
            val token = token()
            window.navigator.clipboard.writeText(token)
            window.alert("Auth token copied to clipboard\n\n$token")
        }
    }
}

