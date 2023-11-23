package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import components.RgButton
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import viewmodel.ResultsViewModel

@Composable
fun RaceResultsEdit(id: Long?) {

}

@Composable
fun RaceResultsCreate() {


}

@Composable
fun RaceResults(
    viewModel: ResultsViewModel = remember { ResultsViewModel() }
) {
    H4 {
        Text("Race Results")
    }
    RgButton("Add result") {
        viewModel.addResult()
    }
}