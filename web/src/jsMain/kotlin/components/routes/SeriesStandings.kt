package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.StandingsBoatSkipper
import com.mxmariner.regatta.data.StandingsClass
import com.mxmariner.regatta.data.StandingsSeries
import components.*
import org.jetbrains.compose.web.attributes.Scope
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.dom.*
import viewmodel.SeriesStandingsViewModel
import utils.*
import viewmodel.complete

private val col1 = listOf(
    "Boat",
    "Skipper",
    "Rating",
)


@Composable
fun SeriesStandings(
    id: Long?,
    year: Int?,
    viewModel: SeriesStandingsViewModel = remember { SeriesStandingsViewModel(id, year) }
) {
    val state = viewModel.flow.collectAsState()

    state.value.standings.complete(viewModel) { standingsSeries ->
        H1 {
            Text("${standingsSeries.series.name} Standings ${standingsSeries.year}")
        }
        standingsSeries.standings.forEach { standingsClass ->
            Br()
            H4 { Text(standingsClass.raceClass.name) }
            StandingsTable(standingsSeries, standingsClass)

        }
    }
}

@Composable
fun StandingsTable(standingsSeries: StandingsSeries, standingsClass: StandingsClass) {
    val multiBracket = standingsClass.standings.size > 1
    RgTable(stripeColumn = true, color = TableColor.light) {
        RgThead {
            RgTr {
                col1.forEach {
                    RgTh(scope = Scope.Colgroup) { P { Text(it) } }
                }
                standingsSeries.races.forEach { race ->
                    RgTh(scope = Scope.Colgroup) {
                        P { Text(race.name) }
                        if (multiBracket) {
                            Text("(bracket, class)")
                        }
                    }
                }
                RgTh(scope = Scope.Colgroup) {
                    P { Text("Total Score") }
                    if (multiBracket) {
                        Text("(bracket, class)")
                    }
                }

                if (multiBracket) {
                    RgTh(scope = Scope.Colgroup) { P { Text("Place in bracket") } }
                }
                RgTh(scope = Scope.Colgroup) { P { Text("Place in class") } }
            }
        }
        RgTbody {
            standingsClass.standings.forEach { standingsBracket ->
                // (Bootstrap's table-borderless only works on <table>, so it was a no-op on this row)
                RgTr(classes = listOf(TableColor.light.cls)) {
                    RgTdColor(
                        colSpan = col1.size + standingsSeries.races.size + 3,
                        color = TableColor.warning
                    ) {
                        H6 { Text("${standingsBracket.bracket.name} ${standingsBracket.bracket.description ?: ""}") }
                    }
                }
                standingsBracket.standings.forEach { standings ->
                    RgTr {
                        RgTd {
                            BoatLabel(standings)
                        }
                        RgTd {
                            Text(standings.boatSkipper.skipper?.fullName() ?: "")
                        }
                        RgTd {
                            Text(
                                standings.boatSkipper.boat?.ratingType?.label ?: ""
                            )
                        }
                        standings.raceStandings.forEach {
                            RgTd {
                                ScoreText(it.bracketScoreText(), it.bracketScoreClasses())
                                it.bracketCorrectedText()?.let { text ->
                                    ScoreText(text, it.correctedScoreClasses())
                                }
                                if (multiBracket) {
                                    ScoreText(it.classScoreText(), it.classScoreClasses())
                                    it.classCorrectedText()?.let { text ->
                                        ScoreText(text, it.correctedScoreClasses())
                                    }
                                }
                                it.codeSuffix().takeIf { suffix -> suffix.isNotEmpty() }?.let { suffix -> Text(suffix) }
                            }
                        }
                        RgTd {
                            if (multiBracket) {
                                B { Text(standings.totalBracketText()) }
                            }
                            Text(standings.totalClassText())
                        }

                        if (multiBracket) {
                            RgTd {
                                B { Text("${standings.placeInBracket}") }
                            }
                        }

                        RgTd {
                            Text("${standings.placeInClass}")
                        }
                    }
                }
            }
        }
    }
}

/** A bold score with its decoration classes (see utils/StandingsReport.kt). */
@Composable
private fun ScoreText(text: String, classes: List<String>) {
    B(attrs = {
        if (classes.isNotEmpty()) {
            classes(*classes.toTypedArray())
        }
    }) { Text(text) }
}

@Composable
fun BoatLabel(card: StandingsBoatSkipper) {
    Span(attrs = { style { fontWeight("bold") } }) {
        Text(card.boatSkipper.boat?.name ?: "")
    }
    card.boatSkipper.boat?.boatType?.takeIf { it.isNotBlank() }?.let {
        Text(" - $it")
    }
    card.boatSkipper.boat?.sailNumber?.takeIf { it.isNotBlank() }?.let {
        Text(" ($it)")
    }
}
