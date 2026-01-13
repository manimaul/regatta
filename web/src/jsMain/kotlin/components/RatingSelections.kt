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
            //todo: ORC
        }
        RatingType.ORC_PHRF -> {
            RgNumberInput(
                label = "PHRF Rating",
                value = phrfRating,
                placeHolder = false,
            ) {
                typeChange(boatType, it.toInt())
            }
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