import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import components.*
import components.routes.*
import org.jetbrains.compose.web.css.Style
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
    RgGrid(RgContainerType.container_fluid) {
        NavBar()
        RgDiv(customizer = { set(space = RgSpace.m, size = RgSz.s2) }) {
            when (state.current.route) {
                Route.Home -> Home()
                Route.Series -> Series()
                Route.People -> People()
                Route.Races -> Races()
                Route.RaceCreate -> RaceEdit()
                Route.RaceEdit -> RaceEdit(state.current.args?.get("id")?.toLongOrNull() ?: 0L)
                Route.RaceResult -> RaceResults()
                Route.RaceResultView -> RaceResultsView(state.current.args?.get("id")?.toLongOrNull())
                Route.RaceResultEdit -> RaceResultsEdit(state.current.args?.get("id")?.toLongOrNull())
                Route.SeriesStandingsView -> SeriesStandings(
                    state.current.args?.get("id")?.toLongOrNull(),
                    state.current.args?.get("year")?.toIntOrNull()
                )
                Route.Boats -> Boats()
                Route.Classes -> Classes()
                Route.Admin -> Admin(create = false)
                Route.AdminCreate -> Admin(create = true)
                Route.PeopleEdit -> PeopleEdit(state.current.args?.get("id")?.toLongOrNull())
                Route.BoatEdit -> BoatEdit(state.current.args?.get("id")?.toLongOrNull())
                Route.NotFound -> Text("womp womp, something's missing")
            }
        }
    }
}