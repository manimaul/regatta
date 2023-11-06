package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import viewmodel.Route
import viewmodel.RouteViewModel
import viewmodel.routeViewModel

@Composable
fun Nav(
    viewModel: RouteViewModel = routeViewModel
) {
    P {

        arrayOf(
            Route.Home,
            Route.People,
            Route.Boats,
            Route.Classes,
            Route.Series,
            Route.Races,
            Route.RaceResult,
        ).forEach { route ->
            if (route != Route.NotFound) {
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
        }
    }
}