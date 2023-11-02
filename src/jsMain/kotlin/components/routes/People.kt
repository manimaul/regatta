package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.dom.*
import viewmodel.PeopleViewModel

@Composable
fun People(
    viewModel: PeopleViewModel = remember { PeopleViewModel() }
) {
    Div {
        H4 {
            Text("People")
        }
        Table {
            Tr {
                Th{ Text("First") }
                Th { Text("Last") }
                Th { Text("Boat") }
                Th { Text("Member") }
            }
            viewModel.people.forEach { person ->
                Tr {
                    Td { Text(person.first) }
                    Td { Text(person.last) }
                    Td { Text("?") }
                    Td { CheckboxInput(person.clubMember) }
                }
            }

        }
    }
}