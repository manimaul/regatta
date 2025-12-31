package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.ClassReportCards
import com.mxmariner.regatta.data.RaceReport
import com.mxmariner.regatta.data.RaceReportCard
import com.mxmariner.regatta.data.RatingType
import com.mxmariner.regatta.ratingLabel
import components.*
import org.jetbrains.compose.web.attributes.Scope
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.*
import viewmodel.*

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
        report.classReports.forEach { classReportCards ->
            RaceResultsClassTable(report, classReportCards)
            Br()
        }
    }
}

@Composable
fun RaceResultsClassTable(report: RaceReport, classReportCards: ClassReportCards) {
    val totalBracketInClassCount = classReportCards.bracketReport.size

    Div(attrs = {
        classes("border-top", "border-2", "border")
    }) {
        H4 { Text(classReportCards.raceClass.name) }
        Text("CF - ${classReportCards.correctionFactor}")
        Text("Start time - ${report.classStart(classReportCards.raceClass.id)?.timeStr() ?: "None"}")
    }
    val headers = if (classReportCards.raceClass.ratingType == RatingType.PHRF) {
        listOf(
            "Boat",
            "Skipper",
            "Rating",
            "Finish",
            "Elapsed Time",
            "Corrected Time",
        )
    } else {
        listOf(
            "Boat",
            "Skipper",
            "Rating",
            "Finish",
            "Elapsed Time",
        )
    }
    RgTable(stripeColumn = true, color = TableColor.light) {
        RgThead {
            RgTr {
                headers.forEach {
                    RgTh(scope = Scope.Colgroup) { Text(it) }
                }
                if (totalBracketInClassCount > 1) {
                    RgTh(scope = Scope.Colgroup) { Text("Place In Bracket") }
                    RgTh(scope = Scope.Colgroup) { Text("Place In Class") }
                } else {
                    RgTh(scope = Scope.Colgroup) { Text("Place In Class") }
                }
            }
        }
        RgTbody {
            classReportCards.bracketReport.forEach { classReport ->
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
                        if (classReportCards.raceClass.ratingType == RatingType.PHRF) {
                            RgTd { Text(card.corTimeText()) }
                        }
                        if (totalBracketInClassCount > 1) {
                            RgTd { Text(card.placeInBracket.toString()) }
                            RgTd { Text(card.placeInClass.toString()) }
                        } else {
                            RgTd { Text(card.placeInBracket.toString()) }
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
