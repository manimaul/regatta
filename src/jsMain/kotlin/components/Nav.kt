package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.dom.H5
import viewmodel.*

@Composable
fun Nav(
    viewModel: RouteViewModel = routeViewModel,
    loginVm: LoginViewModel = loginViewModel,
) {
    val loginFlowState by loginVm.flow.collectAsState()
    val clockState by loginVm.clockFlow.collectAsState()
    H4 { Text("Regatta ${clockState.display}") }
    H5 { Text("Login authorization expires in: ${clockState.expiresDisplay}") }
    Hr()
    P {
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
            Route.Home,
            Route.RaceResult,
        )).forEach { route ->
            val style = if (viewModel.route == route) {
                RgButtonStyle.PrimaryOutline
            } else {
                RgButtonStyle.SecondaryOutline
            }
            RgButton(route.name, style) {
                println("clicked $route")
                viewModel.setRoute(route)
            }
            Text("${Typography.nbsp}")
        }

        if (loginFlowState.loginStatus== LoginStatus.LoggedIn) {
            RgButton("Logout", RgButtonStyle.Primary) {
                loginVm.logout()
            }
        } else {
            RgButton("Login", RgButtonStyle.Primary) {
                viewModel.setRoute(Route.Admin)
            }
        }
    }
}