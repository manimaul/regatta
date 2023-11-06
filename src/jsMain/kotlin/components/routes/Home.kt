package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
        Button(attrs = {
            onClick { window.navigator.clipboard.writeText(token()) }
        }) {
            Text("Copy auth token")
        }
    }
    Div {
        H4 { Text(viewModel.state.clock) }
    }
}
