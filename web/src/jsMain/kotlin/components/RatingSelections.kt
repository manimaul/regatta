package components

import androidx.compose.runtime.Composable
import com.mxmariner.regatta.data.RatingType
import com.mxmariner.regatta.ratingDefault

@Composable
fun RatingSelections(
    boatType: RatingType,
    phrfRating: Int?,
    typeChange: (RatingType, Int) -> Unit,
) {
    RgDropdown(RatingType.entries, boatType, { it.label }) {
        typeChange(it, phrfRating ?: ratingDefault.toInt())
    }
    when (boatType) {
        RatingType.ORC-> {
            OrcFetch()
        }
        RatingType.ORC_PHRF -> {
            RgNumberInput(
                label = "PHRF Rating",
                value = phrfRating,
                placeHolder = false,
            ) {
                typeChange(boatType, it.toInt())
            }
            OrcFetch()
        }
        RatingType.PHRF -> {
            RgNumberInput(
                label = "PHRF Rating",
                value = phrfRating,
                placeHolder = false,
            ) {
                typeChange(boatType, it.toInt())
            }
        }
        else -> {}
    }
}

//https://data.orc.org/public/WPub.dll?action=DownBoatRMS&RefNo=04560003WR9&ext=json
@Composable
fun OrcFetch() {
    RgInput(
        label = "ORC Reference Number",
        value = ""
    ) {

    }
}