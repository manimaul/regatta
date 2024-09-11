package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.Race
import com.mxmariner.regatta.data.RaceSchedule
import components.*
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import utils.display
import viewmodel.Operation
import viewmodel.RacesEditViewModel
import viewmodel.RacesViewModel
import viewmodel.complete

@Composable
fun Races(
    viewModel: RacesViewModel = remember { RacesViewModel() }
) {
    val state = viewModel.flow.collectAsState()
    H1 { Text("Races") }
    B { Text("Year") }
    RgRaceYearSelector { viewModel.selectYear(it) }
    Br()
    state.value.races.complete(viewModel) { RaceList(it, viewModel) }
}

@Composable
fun RaceList(schedules: List<RaceSchedule>, viewModel: RacesViewModel) {
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Series") }
                RgTh { Text("Name") }
                RgTh { Text("Race start") }
                RgTh { Text("Race finish") }
                RgTh { Text("Race committee") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            schedules.forEach { sch ->
                RgTr {
                    RgTd { Text(sch.series?.name ?: "-") }
                    RgTd { Text(sch.race.name) }
                    RgTd { Text(sch.startTime.display()) }
                    RgTd { Text(sch.endTime.display()) }
                    RgTd { Text(sch.rc?.let { "${it.first} ${it.last}" } ?: "-") }
                    RgTd {
                        RgButton("Edit", RgButtonStyle.PrimaryOutline, customClasses = listOf("float-start")) {
                            viewModel.editRace(sch.race)
                        }
                    }
                }
            }
            RgTr {
                RgTd(5) { }
                RgTd {
                    RgButton("Create Race", RgButtonStyle.SuccessOutline, customClasses = listOf("float-start")) {
                        viewModel.createRace()
                    }
                }
            }
        }
    }
}
