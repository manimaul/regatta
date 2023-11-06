package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import viewmodel.HomeViewModel
import viewmodel.LoginStatus
import viewmodel.Route

@Composable
fun Home(
    viewModel: HomeViewModel = remember { HomeViewModel() },
) {
    val loginVm = viewModel.loginVm
    val routeVm = viewModel.routeVm
    if (loginVm.loginStatus == LoginStatus.LoggedIn) {
        H4 {
            Text("Logged in as ${loginVm.userName}")
        }
        H4 {
            Text("Expires: ${viewModel.state.expires}")
        }
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
                routeVm.setRoute(Route.Admin)
            }
        }) {
            Text("Login")
        }
    }
    Div {
        H4 { Text(viewModel.state.clock) }
    }
}
