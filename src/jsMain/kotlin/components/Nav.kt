package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import viewmodel.*

@Composable
fun Nav(
    viewModel: RouteViewModel = routeViewModel,
    loginVm: LoginViewModel = loginViewModel,
) {
    P {
        (loginVm.loginResponse?.let {
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

        if (loginVm.loginStatus == LoginStatus.LoggedIn) {
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