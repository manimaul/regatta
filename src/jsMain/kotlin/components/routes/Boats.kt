package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.Boat
import components.Spinner
import org.jetbrains.compose.web.dom.*
import viewmodel.BoatStateLoaded
import viewmodel.BoatStateLoading
import viewmodel.BoatViewModel

@Composable
fun Boats(
    viewModel: BoatViewModel = remember { BoatViewModel() }
) {
    H4 {
        Text("Boats")
    }
    when (val state = viewModel.state) {
        BoatStateLoading -> Spinner(50f)
        is BoatStateLoaded -> BoatList(state.boats)
    }
    Br()
    Hr()
    H4 { Text("Add boat") }
    AddBoat(viewModel)
}

@Composable
fun AddBoat(viewModel: BoatViewModel) {

}

@Composable
fun BoatList(boats: List<Boat>) {
   Div {

       Table {
           Tr {
               Th { Text("Name") }
               Th { Text("Sail Number") }
               Th { Text("Type") }
               Th { Text("PHRF Rating") }
               Th { Text("Skipper") }
           }
           boats.forEach { boat ->
               Tr {
                   Td { Text(boat.name) }
                   Td { Text(boat.sailNumber) }
                   Td { Text(boat.boatType) }
                   Td { Text(boat.phrfRating?.let { "$it" } ?: "-") }
                   Td {
                       Text(
                           "${boat.skipper.first} ${boat.skipper.last}"
                       )
                   }
               }
           }
       }
   }
}
