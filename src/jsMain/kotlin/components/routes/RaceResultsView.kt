package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.RaceReport
import com.mxmariner.regatta.display
import components.*
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Tr
import utils.display
import viewmodel.RaceResultViewViewModel
import viewmodel.complete

@Composable
fun RaceResultsView(
    raceId: Long?,
    viewModel: RaceResultViewViewModel = remember { RaceResultViewViewModel(raceId ?: 0L)}
) {
    val state = viewModel.flow.collectAsState()
    H1 {
        Text("Race Results")
    }
    state.value.report.complete(viewModel) { report ->
       RaceReportTable(report)
    }
}

@Composable
fun RaceReportTable(report: RaceReport) {
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
        Tbody {
            report.cards.forEach {  card ->
                Tr {
                    RgTd { Text(card.boatName) }
                    RgTd { Text(card.skipper) }
                    RgTd { Text(card.sail) }
                    RgTd { Text(card.boatType) }
                    RgTd { Text(card.phrfRating?.toString() ?: "") }
                    RgTd { Text(card.startTime?.display() ?: "DNS") }
                    RgTd { Text(card.finishTime?.display() ?: "DNF") }
                    RgTd { Text(card.elapsedTime?.display() ?: "") }
                    RgTd { Text(card.elapsedTime?.inWholeSeconds?.toString() ?: "") }
                    RgTd { Text(card.correctionFactor.toString()) }
                    RgTd { Text(card.correctedTime?.display() ?: "") }
                    RgTd { Text(card.correctedTime?.inWholeSeconds?.toString() ?: "") }
                    RgTd { Text(card.placeInBracket.toString()) }
                    RgTd { Text(card.placeInClass.toString()) }
                    RgTd { Text(card.placeOverall.toString()) }
                }
            }
        }
    }
}