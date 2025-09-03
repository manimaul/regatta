import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import components.*
import components.routes.*
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.events.Event
import styles.AppStyle
import viewmodel.Route
import viewmodel.RouteViewModel
import viewmodel.routeViewModel

@Serializable
data class SizeCast(val rh: Int, val rw: Int)
@Composable
fun Router(
    viewModel: RouteViewModel = routeViewModel
) {
    LaunchedEffect(window) {
        val resizeHandler = { _: Event ->
            window.parent.postMessage(SizeCast(rh = window.innerHeight, rw = window.innerWidth), "*"); // '*' for any origin, or specify parent origin for security
        }
        window.addEventListener("resize", resizeHandler)
        resizeHandler.invoke(Event("initial"))
    }
    val state by viewModel.flow.collectAsState()
    Style(AppStyle)
    RgGrid(RgContainerType.container_fluid) {
        NavBar()
        Alerts()
        RgDiv(id = "route_content", customizer = {
            set(space = RgSpace.m, size = RgSz.s2)
            addCustom("flex-grow-1")
            addCustom("d-flex")
            addCustom("flex-column")
        } ) {
            when (state.current.route) {
                Route.Home -> Home()
                Route.Series -> Series()
                Route.Course -> Course()
                Route.People -> People()
                Route.Races -> Races()
                Route.RaceCreate -> RaceEdit()
                Route.RaceEdit -> RaceEdit(state.current.args?.get("id")?.toLongOrNull() ?: 0L)
                Route.RaceResult -> RaceResults()
                Route.RaceResultView -> RaceResultsView(state.current.args?.get("id")?.toLongOrNull())
                Route.RaceResultEdit -> RaceResultsEdit(state.current.args?.get("id")?.toLongOrNull())
                Route.SeriesStandingsView -> SeriesStandings(
                    state.current.args?.get("id")?.toLongOrNull(),
                    state.current.args?.get("year")?.toIntOrNull(),
                )

                Route.Rc -> Rc()
                Route.Boats -> Boats()
                Route.Classes -> Classes()
                Route.Admin -> Admin(create = false)
                Route.AdminCreate -> Admin(create = true)
                Route.NotFound -> Text("womp womp, something's missing")
            }
        }
    }
}
