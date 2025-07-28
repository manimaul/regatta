package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.FinishCode
import com.mxmariner.regatta.data.StandingsBoatSkipper
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

private val col2 = listOf(
    "Place in bracket",
    "Place in class",
)
@Composable
fun SeriesStandings(
    id: Long?,
    year: Int?,
    viewModel: SeriesStandingsViewModel = remember { SeriesStandingsViewModel(id, year) }
) {
    val state = viewModel.flow.collectAsState()

    state.value.standings.complete(viewModel) { ss ->
        H1 {
            Text("${ss.series.name} Standings ${ss.year}")
        }
        RgTable(stripeColumn = true, color = TableColor.light) {
            RgThead {
                RgTr {
                    col1.forEach {
                        RgTh(scope = Scope.Colgroup) { P { Text(it) } }
                    }
                    ss.races.forEach { race ->
                        RgTh(scope = Scope.Colgroup) {
                            P { Text(race.name) }
                            Text("(bracket, class)")
                        }
                    }
                    RgTh(scope = Scope.Colgroup) {
                        P { Text("Total Score") }
                        Text("(bracket, class)")
                    }
                    col2.forEach {
                        RgTh(scope = Scope.Colgroup) { P { Text(it) } }
                    }
                }
            }
            RgTbody {
                ss.standings.forEach { sc ->
                    RgTr(classes = listOf("table-light", "table-borderless")) {
                        RgTdColor(colSpan = col1.size + col2.size + ss.races.size + 1, color = TableColor.info) {
                            H4 { Text(sc.raceClass.name) }
                        }
                    }
                    sc.standings.forEach { sb ->
                        RgTr(classes = listOf("table-light", "table-borderless")) {
                            RgTdColor(colSpan = col1.size + col2.size + ss.races.size + 1, color = TableColor.warning) {
                                H6 { Text("${sb.bracket.name} ${sb.bracket.description ?: ""}") }
                            }
                        }
                        sb.standings.forEach { ss ->
                            RgTr {
                                RgTd {
                                    BoatLabel(ss)
                                }
                                RgTd {
                                    Text(ss.boatSkipper.skipper?.fullName() ?: "")
                                }
                                RgTd { Text(ratingLabel(ss.boatSkipper.boat?.phrfRating, ss.boatSkipper.boat?.windseeker, false)) }
                                ss.raceStandings.forEach {
                                    RgTd(classes = if (it.throwOut) listOf("text-danger") else null) {
                                        B { Text("${it.placeInBracket}, ") }
                                        Text("${it.placeInClass}")
                                        when (val code = it.finishCode) {
                                            FinishCode.TIME -> Unit
                                            FinishCode.RET,
                                            FinishCode.DNF,
                                            FinishCode.NSC -> Text(" ${code.name}")
                                            FinishCode.HOC -> Text(" ${code.name}${it.hocPosition}")
                                            null -> Text(" DNS")
                                        }
                                    }
                                }
                                RgTd {
                                    B {   Text("${ss.totalScoreBracket}, ") }
                                    Text("${ss.totalScoreClass}")
                                }
                                RgTd {
                                    B { Text("${ss.placeInBracket}") }
                                }
                                RgTd {
                                    Text("${ss.placeInClass}")
                                }
//                                RgTd {
//                                    I { Text("${ss.placeOverall}") }
//                                }
                            }
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
