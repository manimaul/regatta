package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import viewmodel.*

@Composable
fun Nav(
    viewModel: RouteViewModel = routeViewModel,
    loginVm: LoginViewModel = loginViewModel,
) {
    val state by viewModel.flow.collectAsState()
    val loginFlowState by loginVm.flow.collectAsState()
    val clockState by loginVm.clockFlow.collectAsState()
    H4 { Text("Regatta ${clockState.display}") }
    P { Text("Login authorization expires in: ${clockState.expiresDisplay}") }
    RgButtonGroup {
        (loginFlowState.login?.let {
            arrayOf(
                Route.Home,
                Route.People,
                Route.Boats,
                Route.Classes,
                Route.Series,
                Route.Races,
                Route.RaceResult,
            )
        } ?: arrayOf(
            Route.RaceResult,
        )).forEach { route ->
            val style = if (state.current.route == route) {
                RgButtonStyle.Primary
            } else {
                RgButtonStyle.PrimaryOutline
            }
            RgButton(route.name, style) {
                println("clicked $route")
                viewModel.pushRoute(route)
            }
        }

        val style = if (state.current.route == Route.Admin) {
            RgButtonStyle.Success
        } else {
            RgButtonStyle.SuccessOutline
        }
        if (loginFlowState.loginStatus== LoginStatus.LoggedIn) {
            RgButton("Logout", style) {
                loginVm.logout()
            }
        } else {
            RgButton("Admin", style) {
                viewModel.pushRoute(Route.Admin)
            }
        }
    }
}