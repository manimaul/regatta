package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceSchedule
import components.*
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.display
import viewmodel.*

@Composable
fun RaceEdit(
    raceId: Long = 0,
    viewModel: RacesEditViewModel = remember { RacesEditViewModel(raceId = raceId) }
) {
    val state by viewModel.flow.collectAsState()

    state.race.complete(viewModel) { schedule ->
        when (state.operation) {
            Operation.None -> Unit
            Operation.Fetched -> RaceForm(schedule, viewModel = viewModel)
            Operation.Updated -> RgOk("Race", "${schedule.race.name} updated!") {
                viewModel.cancelCreate()
            }

            Operation.Deleted -> RgOk("Race", "${schedule.race.name} deleted!") {
                viewModel.cancelCreate()
            }
        }
        RgButton(
            label = "Save",
            style = RgButtonStyle.Success,
            customClasses = listOf(AppStyle.marginTop, AppStyle.marginEnd),
            disabled = !schedule.validate()
        ) {
            viewModel.save(schedule)
        }
        RgButton(label = "Cancel", style = RgButtonStyle.Primary, customClasses = listOf(AppStyle.marginTop)) {
            routeViewModel.goBackOrHome()
        }
    }

}

@Composable
fun RaceForm(
    raceSchedule: RaceSchedule,
    viewModel: RacesEditViewModel
) {
    val state = viewModel.flow.collectAsState()
    val tState = viewModel.timeVm.flow.collectAsState()
    H1 { Text("Edit Race") }
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Name") }
                RgTh { Text("RC") }
                RgTh { Text("Series") }
                RgTh { Text("CF") }
            }
        }
        Tbody {
            RgTr {
                RgTd {
                    RgInput(label = "name", placeHolder = true, value = raceSchedule.race.name) {
                        viewModel.setRaceName(it)
                    }
                }
                RgTd {
                    state.value.skippers.complete(viewModel) { people ->
                        P {
                            RgSkipperDropdown(people, raceSchedule.rc) {
                                viewModel.setRC(it)
                            }
                        }
                    }
                }
                RgTd {
                    state.value.series.complete(viewModel) { series ->
                        P {
                            RgSeriesDropdown(series, raceSchedule.series) {
                                viewModel.setSeries(it)
                            }
                        }
                    }
                }
                RgTd {
                    RgInput(label = "CF", placeHolder = true, value = raceSchedule.race.correctionFactor.toString()) {
                        viewModel.setCF(it.toIntOrNull())
                    }
                }
            }

        }
    }
    H6 { Text("Race Start Time(s)") }
    RgTable {
        RgThead {
            RgTr {
                RgTh { Text("Class") }
                RgTh { Text("Start") }
                RgTh { Text("End") }
                RgTh { Text("Action") }
            }
        }
        RgTbody {
            state.value.race.complete(viewModel) { rs ->
                rs.schedule.forEach { schedule ->
                    if (schedule.raceClass.id == tState.value.focus?.raceClass?.id) {
                        RgAddTime(viewModel.timeVm) { viewModel.addSchedule(it) }
                    } else {
                        RgTr {
                            RgTd {
                                H6 { Text(schedule.raceClass.name) }
                                schedule.brackets.forEach {
                                    P { Text(it.label()) }
                                }
                            }
                            RgTd { Text(schedule.raceStart()?.display() ?: "") }
                            RgTd { Text(schedule.raceEnd()?.display() ?: "") }
                            RgTd {
                                RgButton(label = "Edit", customClasses = listOf(AppStyle.marginEnd)) {
                                    viewModel.timeVm.editSchedule(schedule)
                                }
                                RgButton(
                                    label = "Remove",
                                    style = RgButtonStyle.Danger,
                                    customClasses = listOf(AppStyle.marginEnd)
                                ) {
                                    viewModel.removeSchedule(schedule)
                                    viewModel.timeVm.resetOption(schedule.raceClass.id)
                                }
                            }
                        }
                    }
                }
            }
            RgAddTime(viewModel.timeVm) { viewModel.addSchedule(it) }
        }
    }
}
