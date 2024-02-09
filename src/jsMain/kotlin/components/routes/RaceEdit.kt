package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.RaceSchedule
import components.*
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.display
import viewmodel.Operation
import viewmodel.RacesEditViewModel
import viewmodel.complete

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
    }

    RgButton("Save", RgButtonStyle.Success, customClasses = listOf(AppStyle.marginTop)) {
        TODO()
    }
}

@Composable
fun RaceForm(
    editRace: RaceSchedule,
    viewModel: RacesEditViewModel
) {
    val state = viewModel.flow.collectAsState()
    var rs by mutableStateOf(editRace)
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
                    RgInput(label = "name", placeHolder = true, value = rs.race.name) {
                        rs = rs.copy(race = rs.race.copy(name = it))
                    }
                }
                RgTd {
                    state.value.skippers.complete(viewModel) { people ->
                        P {
                            RgSkipperDropdown(people, rs.rc) {
                                rs = rs.copy(rc = it)
                            }
                        }
                    }
                }
                RgTd {
                    state.value.series.complete(viewModel) { series ->
                        P {
                            RgSeriesDropdown(series, rs.series) {
                                rs = rs.copy(series = it, race = rs.race.copy(seriesId = it.id))
                            }
                        }
                    }
                }
                RgTd {
                    RgInput(label = "CF", placeHolder = true, value = rs.race.correctionFactor.toString()) {
                        val cf = it.toIntOrNull() ?: 0
                        rs = rs.copy(race = rs.race.copy(correctionFactor = cf))
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
//            state.value.classes.complete(viewModel) { classBrackets ->
                state.value.race.complete(viewModel) { rs ->
                    rs.schedule.forEach {
                        RgTr {
                            RgTd {
                                H6 { Text(it.raceClass.name) }
                                it.brackets.forEach {
                                    P { Text("${it.name} ${it.maxRating}-${it.maxRating}") }
                                }
                            }
                            RgTd { Text(it.startDate.display()) }
                            RgTd { Text(it.endDate.display()) }
                            RgTd { }
                        }
                    }
                }
                RgAddTime { viewModel.addSchedule(it) }
//            }
        }
    }
}


//@Composable
//fun RgAddRaceTime(
//    categories: List<RaceClassFull>,
//    selected: RaceClass?,
//    added: (RaceTime) -> Unit
//) {
//    var raceClassCat by mutableStateOf(selected)
//    var startDate by remember { mutableStateOf<Instant?>(null) }
//    var endDate by remember { mutableStateOf<Instant?>(null) }
//    var cf by remember { mutableStateOf(correctionFactorDefault) }
//
//    RgRow {
//        RgCol {
//            B { Text("Class Category") }
//            RgClassCatDropDown(categories, raceClassCat) {
//                raceClassCat = it.toRaceClass()
//                println("selected $raceClassCat")
//            }
//        }
//        RgCol {
//            RgDate(label = "Race start", date = startDate, time = true) { st ->
//                if (endDate == null) {
//                    endDate = st.plus(2.5.hours)
//                }
//                startDate = st
//            }
//        }
//        RgCol {
//            RgDate(label = "Race end", date = endDate, time = true) { et ->
//                endDate = et
//            }
//
//        }
//        RgCol {
//            RgInput(label = "Correction", "$cf") {
//                cf = it.toIntOrNull() ?: correctionFactorDefault
//            }
//        }
//        RgCol {
//            RgRow(customizer = { set(space = RgSpace.m, side = RgSide.x, size = RgSz.s3) }) {
//                B { Text("Action") }
//                RgButton(
//                    "Add",
//                    disabled = raceClassCat == null || startDate == null || endDate == null,
//                ) {
//                    added(RaceTime(raceClassCat!!, startDate!!, endDate!!, cf, TODO()))
//                    raceClassCat = null
//                    startDate = startDate?.plus(5.minutes)
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun RgSeriesSelect(
//    allSeries: List<Series>,
//    series: Series?,
//    handler: (Series?) -> Unit
//) {
//    Select(attrs = {
//        classes("form-select")
//        onChange { change ->
//            change.value?.toLongOrNull()?.let { id ->
//                handler(allSeries.firstOrNull {
//                    it.id == id
//                })
//            }
//        }
//    }) {
//        Option("-1", attrs = {
//            if (series == null) {
//                selected()
//            }
//        }) {
//            Text("None")
//        }
//        allSeries.forEach {
//            Option(it.id.toString(), attrs = {
//                if (it.id == series?.id) {
//                    selected()
//                }
//            }) {
//                Text(it.name)
//            }
//        }
//    }
//}
