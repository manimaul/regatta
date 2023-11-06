package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import components.Clock
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import viewmodel.HomeViewModel

@Composable
fun Home(
    viewModel: HomeViewModel = remember { HomeViewModel() }
) {
    viewModel.loggedInPerson?.let {
        H4 {
            Text("Logged in as ${it.first} ${it.last}")
        }
        Clock(viewModel)
    } ?: run {
        Clock(viewModel)
    }
}
