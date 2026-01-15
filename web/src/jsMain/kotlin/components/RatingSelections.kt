package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.RatingType
import com.mxmariner.regatta.ratingDefault
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import styles.AppStyle
import viewmodel.OrcViewModel

@Composable
fun RatingSelections(
    boatType: RatingType,
    phrfRating: Int?,
    typeChange: (RatingType, Int) -> Unit,
) {
    P {
        Label(attrs = {
            classes(AppStyle.marginEnd)
        }) { B { Text("Rating Type") } }
        RgDropdown(RatingType.entries, boatType, { it.label }) {
            typeChange(it, phrfRating ?: ratingDefault.toInt())
        }
    }
    when (boatType) {
        RatingType.ORC -> {
            P {
                OrcFetch()
            }
        }

        RatingType.ORC_PHRF -> {
            P {
                RgNumberInput(
                    label = "PHRF Rating",
                    value = phrfRating,
                ) {
                    typeChange(boatType, it.toInt())
                }
            }
            P {
                OrcFetch()
            }
        }

        RatingType.PHRF -> {
            P {
                RgNumberInput(
                    label = "PHRF Rating",
                    value = phrfRating,
                ) {
                    typeChange(boatType, it.toInt())
                }
            }
        }

        else -> {}
    }
}

//https://data.orc.org/public/WPub.dll?action=DownBoatRMS&RefNo=04560003WR9&ext=json
@Composable
fun OrcFetch(
    viewModel: OrcViewModel = remember { OrcViewModel() }
) {
    val state by viewModel.flow.collectAsState()
    RgInputWithButton(
        label = "ORC Reference Number",
        btnLabel = "Check",
        value = state.refNumber,
    ) { change, clicked ->
        if (clicked) {
            //todo:
        } else {
            viewModel.refNumber(change)
        }
    }
}