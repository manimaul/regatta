package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.RaceReportCard
import com.mxmariner.regatta.ratingLabel
import components.*
import org.jetbrains.compose.web.attributes.Scope
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.*
import viewmodel.*

val columns = listOf(
    "Boat",
    "Skipper",
    "Rating",
    "Finish",
    "Elapsed Time",
    "Corrected Time",
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
            Text("${report.raceSchedule.startTime.year()} - ${report.raceSchedule.race.name} - Results")
        }
        if (loginState.value.loginStatus == LoginStatus.LoggedIn) {
            RgButton("Editor", customClasses = listOf(AppStyle.marginVert)) {
                routeViewModel.pushRoute("/races/results/${raceId}")
            }
        }
        RgTable(stripeColumn = true, color = TableColor.light) {
            RgThead {
                RgTr {
                    columns.forEach {
                        RgTh(scope = Scope.Colgroup) { Text(it) }
                    }
                }
            }
            RgTbody {
                report.classReports.forEach { reportCategory ->
                    RgTr(classes = listOf("table-light", "table-borderless")) {
                        RgTdColor(colSpan = 15, color = TableColor.info) {
                            H4 { Text(reportCategory.raceClass.name) }
                            Text("CF - ${reportCategory.correctionFactor}")
                            Br()
                            Text("Start time - ${report.classStart(reportCategory.raceClass.id)?.timeStr() ?: "None"}")
                        }
                    }
                    reportCategory.bracketReport.forEach { classReport ->
                        RgTr(classes = listOf("table-light", "table-borderless")) {
                            RgTdColor(colSpan = 15, color = TableColor.warning) {
                                H6 { Text("${classReport.bracket.name} ${classReport.bracket.description ?: ""}") }
                            }
                        }
                        classReport.cards.forEach { card ->
                            RgTr {
                                RgTd { BoatLabel(card) }
                                RgTd { Text(card.skipper) }
                                RgTd { Text(ratingLabel(card.phrfRating, card.windseeker, false)) }
                                RgTd { Text(card.finishText()) }
                                RgTd { Text(card.elapsedText()) }
                                RgTd { Text(card.corTimeText()) }
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

@Composable
fun BoatLabel(card: RaceReportCard) {
   Span(attrs = { style { fontWeight("bold") } }) {
       Text(card.boatName)
   }
    card.boatType.takeIf { it.isNotBlank() }?.let {
        Text(" - $it")
    }
    card.sail.takeIf { it.isNotBlank() }?.let {
        Text(" ($it)")
    }
}
