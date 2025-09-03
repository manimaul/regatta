package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import components.RgButton
import components.RgButtonStyle
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import utils.token
import viewmodel.*

@Composable
fun Home(
    routeVm: RouteViewModel = routeViewModel,
    viewModel: LoginViewModel = loginViewModel,
) {
    val state by viewModel.flow.collectAsState()
    val clockState by viewModel.clockFlow.collectAsState()
    if (state.loginStatus == LoginStatus.LoggedIn) {
        H4 {
            Text("Logged in as ${state.auth.userName}")
        }
        P {
            Text(" Login expires in ${clockState.expiresDisplay}")
        }
        Div(attrs = { classes("flex-wrap") }) {
            RgButton(
                label = "Copy auth token",
                style = RgButtonStyle.PrimaryOutline,
            ) {
                val token = token()
                window.navigator.clipboard.writeText(token)
                window.alert("Auth token copied to clipboard\n\n$token")
            }
        }
    } else {
        routeVm.pushRoute(Route.RaceResult)
    }
}

