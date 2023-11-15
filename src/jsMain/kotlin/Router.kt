import androidx.compose.runtime.Composable
import components.routes.*
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import viewmodel.Route
import viewmodel.RouteViewModel
import viewmodel.routeViewModel


@Composable
fun Router(
    viewModel: RouteViewModel = routeViewModel
) {
    Style(AppStyle)
    Div(attrs = {
        classes(AppStyle.regattaStyle)
    }) {
        components.Nav()
        when (viewModel.route) {
            Route.Home -> Home()
            Route.Series -> Series()
            Route.People -> People()
            Route.Races -> Races()
            Route.RaceResult -> RaceResults()
            Route.Boats -> Boats()
            Route.Classes -> Classes()
            Route.Admin -> Admin()
            else -> Text("womp womp, something's missing")
        }
    }
}