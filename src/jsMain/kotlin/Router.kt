import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val state by viewModel.flow.collectAsState()
    Style(AppStyle)
    Div(attrs = {
        classes("container-fluid")
    }) {
        components.Nav()
        Div(attrs = {
            classes(AppStyle.marginVert)
        }) {
            when (state.route) {
                Route.Home -> Home()
                Route.Series -> Series()
                Route.People -> People()
                Route.Races -> Races()
                Route.RaceResult -> RaceResults()
                Route.Boats -> Boats()
                Route.Classes -> Classes()
                Route.Admin -> Admin()
                Route.PeopleEdit -> Text("person edit todo")
                Route.BoatEdit -> Text("boat edit todo")
                Route.NotFound -> Text("womp womp, something's missing")
            }
        }
    }
}