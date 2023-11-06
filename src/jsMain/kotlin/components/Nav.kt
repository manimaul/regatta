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
            Button(attrs = {
                onClick {
                    println("clicked $route")
                    viewModel.setRoute(route)
                }
            }) {
                Text(route.name)
            }
            Text("${Typography.nbsp}")
        }

        if (loginVm.loginStatus == LoginStatus.LoggedIn) {
            Button(attrs = {
                onClick {
                    loginVm.logout()
                }
            }) {
                Text("Logout")
            }
        } else {
            Button(attrs = {
                onClick {
                    viewModel.setRoute(Route.Admin)
                }
            }) {
                Text("Login")
            }
        }
    }
}