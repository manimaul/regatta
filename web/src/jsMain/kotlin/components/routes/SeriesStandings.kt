package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.StandingsBoatSkipper
import com.mxmariner.regatta.data.StandingsClass
import com.mxmariner.regatta.data.StandingsSeries
import com.mxmariner.regatta.ratingLabel
import components.*
import org.jetbrains.compose.web.attributes.Scope
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.dom.*
import viewmodel.SeriesStandingsViewModel
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
                RgTr(classes = listOf("table-light", "table-borderless")) {
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
                                ratingLabel(
                                    standings.boatSkipper.boat?.phrfRating,
                                    standings.boatSkipper.boat?.windseeker,
                                    false
                                )
                            )
                        }
                        standings.raceStandings.forEach {
                            RgTd {
                                B(attrs = {
                                    if (it.placeInBracketCorrected != null && it.throwOut) {
                                        classes("text-danger", "text-decoration-line-through")
                                    } else if (it.throwOut) {
                                        classes("text-danger")
                                    } else if (it.placeInBracketCorrected != null) {
                                        classes("text-decoration-line-through")
                                    }
                                }) { Text("${it.placeInBracket}") }
                                it.placeInBracketCorrected?.let { pbc ->
                                    B(attrs = { if (it.throwOut) classes("text-danger") }) {
                                        Text(" $pbc")
                                    }
                                }
                                if (multiBracket) {
                                    B(attrs = {
                                        if (it.placeInClassCorrected != null && it.throwOut) {
                                            classes("text-danger", "text-decoration-line-through")
                                        } else if (it.throwOut) {
                                            classes("text-danger")
                                        } else if (it.placeInClassCorrected != null) {
                                            classes("text-decoration-line-through")
                                        }
                                    }) { Text(", ${it.placeInClass}") }
                                    it.placeInClassCorrected?.let { pcc ->
                                        B(attrs = { if (it.throwOut) classes("text-danger") }) {
                                            Text(" $pcc")
                                        }
                                    }
                                }
                                when (val code = it.finishCode) {
                                    FinishCode.TIME -> Unit
                                    FinishCode.RET,
                                    FinishCode.DNF,
                                    FinishCode.NSC -> Text(" ${code.name}")

                                    FinishCode.HOC -> Text(" ${code.name}${it.hocPosition}")
                                    FinishCode.DNS_RC -> Text(" DNS RC Volunteer")
                                    null -> Text(" DNS")
                                }
                            }
                        }
                        RgTd {
                            if (multiBracket) {
                                B { Text("${standings.totalScoreBracket}, ") }
                            }
                            Text("${standings.totalScoreClass}")
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
