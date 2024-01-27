package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.RaceReportClass
import com.mxmariner.regatta.display
import components.*
import org.jetbrains.compose.web.dom.*
import utils.display
import viewmodel.RaceResultViewViewModel
import viewmodel.complete

@Composable
fun RaceResultsView(
    raceId: Long?,
    viewModel: RaceResultViewViewModel = remember { RaceResultViewViewModel(raceId ?: 0L) }
) {
    val state = viewModel.flow.collectAsState()
    H1 {
        Text("Race Results")
    }
    state.value.report.complete(viewModel) { report ->
        RgTable {
            RgThead {
                RgTr {
                    RgTh { Text("Boat Name") }
                    RgTh { Text("Skipper") }
                    RgTh { Text("Sail Number") }
                    RgTh { Text("Boat Type") }
                    RgTh { Text("PHRF Rating") }
                    RgTh { Text("Start Time") }
                    RgTh { Text("Finish Time") }
                    RgTh { Text("Elapsed Time") }
                    RgTh { Text("Elapsed Seconds") }
                    RgTh { Text("Correction Factor") }
                    RgTh { Text("Corrected Time") }
                    RgTh { Text("Corrected Time Seconds") }
                    RgTh { Text("Place In Bracket") }
                    RgTh { Text("Place In Class") }
                    RgTh { Text("Place Overall") }
                }
            }
            RgTbody {
                report.categories.forEach {
                    RgTr {
                        RgTd(colSpan = 15) {
                            H4 { Text(it.category.name) }
                            Text("CF - ${it.correctionFactor}")
                        }
                    }
                    it.classes.forEach { classReport ->
                        RgTr {
                            RgTd(colSpan = 15) {
                                H6 { Text("${classReport.raceClass.name} ${classReport.raceClass.description ?: ""}") }
                            }
                        }
                        classReport.cards.forEach { card ->
                            RgTr {
                                RgTd { Text(card.boatName) }
                                RgTd { Text(card.skipper) }
                                RgTd { Text(card.sail) }
                                RgTd { Text(card.boatType) }
                                RgTd { Text(card.phrfRating?.toString() ?: "n/a") }
                                RgTd { Text(card.startTime?.display() ?: "DNS") }
                                RgTd {
                                    Text(card.finishTime?.display()?.takeIf { card.startTime != null }
                                        ?: "DNF".takeIf { card.startTime != null } ?: "")
                                }
                                RgTd { Text(card.elapsedTime?.display() ?: "n/a") }
                                RgTd { Text(card.elapsedTime?.inWholeSeconds?.toString() ?: "n/a") }
                                RgTd { Text("${card.correctionFactor.asDynamic().toFixed(3)}") }
                                RgTd { Text(card.correctedTime?.display() ?: "n/a") }
                                RgTd { Text(card.correctedTime?.inWholeSeconds?.toString() ?: "n/a") }
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
