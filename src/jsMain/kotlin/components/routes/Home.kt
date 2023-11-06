package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import components.Clock
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import viewmodel.*

@Composable
fun Home(
    viewModel: HomeViewModel = remember { HomeViewModel() },
    loginViewModel: LoginViewModel = remember { LoginViewModel() }

) {
    if (loginViewModel.state == State.LoggedIn) {
        H4 {
            Text("Logged in as ${loginViewModel.userName}")
        }
        Text("Login expires: ${loginViewModel.loginResponse?.expires}")
        Br()
        Button(attrs = {
            onClick {
                loginViewModel.logout()
            }
        }) {
            Text("Logout")
        }
    } else {
        Button(attrs = {
            onClick {
                routeViewModel.setRoute(Route.Admin)
            }
        }) {
            Text("Login")
        }
    }
    Clock(viewModel)
}
