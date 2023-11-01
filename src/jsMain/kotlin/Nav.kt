import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import viewmodel.Route
import viewmodel.RouteViewModel
import viewmodel.provideRouteViewModel

@Composable
fun Nav(
    viewModel: RouteViewModel = provideRouteViewModel()
) {
    P {

        Route.entries.forEach { route ->
            if (route != Route.NotFound) {
                Button(attrs = {
                    onClick {
                        viewModel.setRoute(route)
                    }
                }) {
                    Text(route.name)
                }
                Text("${Typography.nbsp}")
            }
        }
    }
}