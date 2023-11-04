import androidx.compose.runtime.Composable
import components.routes.*
import org.jetbrains.compose.web.dom.*
import viewmodel.Route
import viewmodel.RouteViewModel
import viewmodel.provideRouteViewModel


@Composable
fun Router(
    viewModel: RouteViewModel = provideRouteViewModel()
) {
    Div {
        H1 {
            Text("Regatta")
        }
        Hr()
        components.Nav()
        when (viewModel.route) {
            Route.Home -> Home()
            Route.Series -> Series()
            Route.People -> People()
            Route.Races -> Races()
            Route.RaceResult -> RaceResults()
            Route.Boats -> Boats()
            Route.Classes -> Classes()
            else -> Text("womp womp, something's missing")
        }
    }
}