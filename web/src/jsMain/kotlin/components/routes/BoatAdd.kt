package components.routes

import androidx.compose.runtime.Composable
import com.mxmariner.regatta.data.BoatSkipper
import components.*
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.*


@Composable
fun BoatAdd(
) {
    RgTable {
        RgThead {
            RgTr {
                Th { Text("Boat Name") }
                Th { Text("Skipper") }
                Th { Text("Sail Number") }
                Th { Text("Type") }
                Th { Text("Rating") }
                Th { Text("Action") }
            }
        }
    }
}

