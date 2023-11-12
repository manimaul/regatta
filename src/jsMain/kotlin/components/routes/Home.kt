package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import components.RgButton
import components.RgButtonStyle
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import utils.token
import viewmodel.HomeViewModel
import viewmodel.LoginStatus

@Composable
fun Home(
    viewModel: HomeViewModel = remember { HomeViewModel() },
) {
    val flowState by viewModel.flow.collectAsState()
    val loginFlowState by viewModel.loginVm.flow.collectAsState()
    if (loginFlowState.state == LoginStatus.LoggedIn) {
        H4 {
            Text("Logged in as ${loginFlowState.auth.userName}")
        }
        H4 {
            Text("Authorization expires: ${loginFlowState.login?.expires ?: "?"}")
        }
        RgButton("Copy auth token", RgButtonStyle.Primary) {
            val token = token()
            window.navigator.clipboard.writeText(token)
            window.alert("Auth token copied to clipboard\n\n$token")
        }
    }
    Div {
        H4 { Text(flowState.clock) }
    }
}

