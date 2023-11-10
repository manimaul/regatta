package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import components.RgButton
import components.RgButtonStyle
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import utils.token
import viewmodel.HomeViewModel
import viewmodel.LoginStatus

@Composable
fun Home(
    viewModel: HomeViewModel = remember { HomeViewModel() },
) {
    val loginVm = viewModel.loginVm
    if (loginVm.loginStatus == LoginStatus.LoggedIn) {
        H4 {
            Text("Logged in as ${loginVm.userName}")
        }
        H4 {
            Text("Authorization expires: ${viewModel.state.expires}")
        }
        RgButton("Copy auth token", RgButtonStyle.Primary) {
            val token = token()
            window.navigator.clipboard.writeText(token)
            window.alert("Auth token copied to clipboard\n\n$token")
        }
    }
    Div {
        H4 { Text(viewModel.state.clock) }
    }
}

