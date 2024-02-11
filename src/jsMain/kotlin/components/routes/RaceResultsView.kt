package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import components.*
import org.jetbrains.compose.web.attributes.Scope
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.year
import viewmodel.*

val colums = listOf(
    "Boat Name",
    "Skipper",
    "Sail Number",
    "Boat Type",
    "PHRF Rating",
    "Start Time",
    "Finish Time",
    "Elapsed Time",
    "Elapsed Seconds",
    "Correction Factor",
    "Corrected Time",
    "Corrected Time Seconds",
    "Place In Bracket",
    "Place In Class",
    "Place Overall"
)

@Composable
fun RaceResultsView(
    raceId: Long?,
    viewModel: RaceResultViewViewModel = remember { RaceResultViewViewModel(raceId ?: 0L) }
) {
    val state = viewModel.flow.collectAsState()
    val loginState = loginViewModel.flow.collectAsState()
    state.value.report.complete(viewModel) { report ->
        H1 {
            Text("${report.raceSchedule.startTime?.year() ?: ""} - ${report.raceSchedule.race.name} - Results")
        }
        if (loginState.value.loginStatus == LoginStatus.LoggedIn) {
            RgButton("Editor", customClasses = listOf(AppStyle.marginVert)) {
                routeViewModel.pushRoute("/races/results/${raceId}")
            }
        }
        RgTable(stripeColumn = true, color = TableColor.light) {
            RgThead {
                RgTr {
                    colums.forEach {
                        RgTh(scope = Scope.Colgroup) { Text(it) }
                    }
                }
            }
            RgTbody {
                report.categories.forEach { reportCategory ->
                    RgTr(classes = listOf("table-light", "table-borderless")) {
                        RgTdColor(colSpan = 15, color = TableColor.info) {
                            H4 { Text(reportCategory.category.name) }
                            Text("CF - ${reportCategory.correctionFactor}")
                        }
                    }
                    reportCategory.brackets.forEach { classReport ->
                        RgTr(classes = listOf("table-light", "table-borderless")) {
                            RgTdColor(colSpan = 15, color = TableColor.warning) {
                                H6 { Text("${classReport.bracket.name} ${classReport.bracket.description ?: ""}") }
                            }
                        }
                        classReport.cards.forEach { card ->
                            RgTr {
                                RgTd { Text(card.boatName) }
                                RgTd { Text(card.skipper) }
                                RgTd { Text(card.sail) }
                                RgTd { Text(card.boatType) }
                                RgTd { Text(card.phrfText()) }
                                RgTd { Text(card.startText()) }
                                RgTd { Text(card.finishText()) }
                                RgTd { Text(card.elapsedText()) }
                                RgTd { Text(card.elapsedSecText()) }
                                RgTd { Text(card.cfText()) }
                                RgTd { Text(card.corTimeText()) }
                                RgTd { Text(card.corTimeSecText()) }
                                RgTd { Text(card.placeInBracket.toString()) }
                                RgTd { Text(card.placeInClass.toString()) }
                                RgTd { Text(card.placeOverall.toString()) }
                            }
                        }
                    }
                }
            }
        }
    }
}
