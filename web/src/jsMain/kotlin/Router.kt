import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import components.*
import components.routes.*
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import utils.currentYear
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
        Alerts()
        RgDiv(id = "route_content", customizer = {
            set(space = RgSpace.m, size = RgSz.s2)
            addCustom("flex-grow-1")
            addCustom("d-flex")
            addCustom("flex-column")
        }) {
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
        Div(attrs = {
            classes("container")
        }) {
            Footer {
                P(attrs = {
                    classes("text-center")
                }) {
                    Text("Build ${VersionInfo.buildDate} ")
                    A(
                        attrs = { classes("link-secondary", "link-underline-opacity-0") },
                        href = "https://github.com/manimaul/regatta/commit/${VersionInfo.gitHash}"
                    ) {
                        Text(VersionInfo.gitHash)
                    }
                    Br { }
                    A(
                        href = "https://github.com/manimaul/regatta",
                        attrs = {
                            classes("link-secondary", "link-underline-opacity-0")
                        }
                    ) {
                        Img(src = "https://img.shields.io/badge/License-Apache_2.0-blue.svg")
                        Text(" ")
                        Img(src = "https://img.shields.io/badge/github-repo-blue?logo=github")
                    }
                }
            }
        }
    }
}
