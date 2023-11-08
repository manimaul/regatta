package components.routes

import androidx.compose.runtime.*
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import components.Spinner
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import viewmodel.BoatStateLoaded
import viewmodel.BoatStateLoading
import viewmodel.BoatViewModel

@Composable
fun Boats(
    viewModel: BoatViewModel = remember { BoatViewModel() }
) {
    StyleText()
    when (val state = viewModel.state) {
        BoatStateLoading -> Spinner(50f)
        is BoatStateLoaded -> state.boats.takeIf { it.isNotEmpty() }?.let {
            BoatList(state.boats)
        }
    }
    H4 { Text("Add boat") }
    AddBoat(viewModel)
}

@Composable
fun AddBoat(viewModel: BoatViewModel) {
    var boat by remember { mutableStateOf(Boat()) }
    Div {
        Table {
            BoatTr()
            Tr {
                Td {
                    Input(type = InputType.Text) {
                        placeholder("Name")
                        onInput { boat = boat.copy(name = it.value) }
                        value(boat.name)
                    }
                }
                Td {
                    Input(type = InputType.Text) {
                        placeholder("Sail number")
                        onInput { boat = boat.copy(sailNumber = it.value) }
                        value(boat.sailNumber)
                    }
                }
                Td {
                    Input(type = InputType.Text) {
                        placeholder("Type")
                        onInput { boat = boat.copy(boatType = it.value) }
                        value(boat.boatType)
                    }
                }
                Td {
                    Input(type = InputType.Text) {
                        placeholder("Rating")
                        onInput { boat = boat.copy(boatType = it.value) }
                        value(boat.phrfRating?.toString() ?: "")
                    }
                }
                Td {
                    StyleText()
                }
            }
        }
        Br()
        Hr()
    }
}

@Composable
fun StyleText() {
    Style(BoatStyle)
    P(attrs = {
        classes(BoatStyle.titleText)
    }) {
        Text("This is styled text")
    }

}

@Composable
fun BoatList(boats: List<Boat>) {
    Div {
        BoatTr()
        Table {
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

@Composable
private fun BoatTr() {
    Tr {
        Th { Text("Name") }
        Th { Text("Sail Number") }
        Th { Text("Type") }
        Th { Text("PHRF Rating") }
        Th { Text("Skipper") }
    }
}
private object BoatStyle : StyleSheet() {
    val titleText by style {
        color(rgb(23,24, 28))
        fontSize(50.px)
        property("font-size", 50.px)
        property("letter-spacing", (-1.5).px)
        property("font-weight", 900)
        property("line-height", 58.px)

        property(
            "font-family",
            "Gotham SSm A,Gotham SSm B,system-ui,-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Oxygen,Ubuntu,Cantarell,Droid Sans,Helvetica Neue,Arial,sans-serif"
        )
    }

}
