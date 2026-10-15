package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.ClassReportCards
import com.mxmariner.regatta.data.PhrfBFactor
import com.mxmariner.regatta.data.RaceReport
import com.mxmariner.regatta.data.RaceReportCard
import com.mxmariner.regatta.data.RatingType
import components.*
import org.jetbrains.compose.web.attributes.Scope
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.*
import styles.AppStyle
import utils.*
import viewmodel.*

@Composable
fun RaceResultsView(
    raceId: Long?,
    viewModel: RaceResultViewViewModel = remember { RaceResultViewViewModel(raceId ?: 0L) }
) {
    val state by viewModel.flow.collectAsState()
    val loginState = loginViewModel.flow.collectAsState()
    state.report.complete(viewModel) { report ->
        H1 {
            Text("${report.raceSchedule.startTime.year()} - ${report.raceSchedule.race.name} - Results")
        }
        if (loginState.value.loginStatus == LoginStatus.LoggedIn) {
            // the page actions side by side under the title, like the races page and the results editor
            Div(attrs = { classes("flex", "flex-wrap", "items-end", "gap-4") }) {
                RgButton("Editor", customClasses = listOf(AppStyle.marginBot)) {
                    routeViewModel.pushRoute("/races/results/${raceId}")
                }
            }
        }
        report.classReports.forEachIndexed { i, classReportCards ->
            RaceResultsClassTable(i > 0, report, classReportCards)
        }
    }
}

@Composable
fun RaceResultsClassTable(
    addDivider: Boolean,
    report: RaceReport,
    classReportCards: ClassReportCards,
) {
    val totalBracketInClassCount = classReportCards.bracketReport.size
    val showOrc = classReportCards.raceClass.ratingType.isORC
    if (addDivider) {
        Div(attrs = { style { marginBottom(32.px) } })
    }
    Div(attrs = {
        classes("border-2", "border-line")
    }) {
        H4 { Text(classReportCards.raceClass.label()) }
        H6 {
            Text(" Start time - ${report.classStart(classReportCards.raceClass.id)?.timeStr() ?: "None"}")
        }
        H6 {
            Text("PHRF B - ${classReportCards.phrfBFactor} - ${PhrfBFactor.fromBFactor(classReportCards.phrfBFactor).label}")
        }
        if (showOrc) {
            H6 {
                Text(
                    "ORC ${classReportCards.orcScoringOption.label}${
                        classReportCards.orcBandLabel()?.let { " - $it" } ?: ""
                    }")
            }
        }
    }
    val headers = if (classReportCards.raceClass.ratingType.isORCorPHRF) {
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
                // (Bootstrap's table-borderless only works on <table>, so it was a no-op on this row)
                RgTr(classes = listOf(TableColor.light.cls)) {
                    RgTdColor(colSpan = 15, color = TableColor.warning) {
                        H6 { Text("${classReport.bracket.name} ${classReport.bracket.description ?: ""}") }
                    }
                }
                classReport.cards.forEach { card ->
                    RgTr {
                        RgTd { BoatLabel(card) }
                        RgTd { Text(card.skipper) }
                        RgTd { Text(card.ratingType().ratedLabel(card.phrfRating, card.resultRecord.result.orcRef)) }
                        RgTd { Text(card.finishText()) }
                        RgTd { Text(card.elapsedText()) }
                        if (classReportCards.raceClass.ratingType.isORCorPHRF) {
                            RgTd {
                                if (card.ratingType().isPHRF) {
                                    Text(card.corTimePhrfText())
                                }
                                if (card.ratingType() == RatingType.ORC_PHRF) {
                                    Br { }
                                }
                                if (showOrc && card.ratingType().isORC) {
                                    Text(card.corTimeOrcText())
                                }
                            }
                        }
                        if (totalBracketInClassCount > 1) {
                            RgTd { CellLines(card.bracketPlaceLines(showOrc)) }
                            RgTd { CellLines(card.classPlaceLines(showOrc)) }
                        } else {
                            RgTd { CellLines(card.singleBracketPlaceLines()) }
                        }
                    }
                }
            }
        }
    }
}

/** Renders the lines of a place cell, separated by line breaks. */

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
