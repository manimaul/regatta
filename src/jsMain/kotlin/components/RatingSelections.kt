package components

import androidx.compose.runtime.Composable
import styles.AppStyle
import utils.digits
import viewmodel.BoatType

@Composable
fun RatingSelections(
    boatType: BoatType,
    phrfRating: String,
    wsRating: String,
    wsFlying: Boolean,
    typeChagne: (BoatType) -> Unit,
    phrfChange: (String) -> Unit,
    wsRatingChange: (String) -> Unit,
    wsFlyingChange: (Boolean) -> Unit,
) {
    RgDropdown(BoatType.entries, boatType, { it.name }) {
        typeChagne(it)
    }
    when (boatType) {
        BoatType.PHRF -> {
            RgInput(
                label = "PHRF Rating",
                value = phrfRating,
                placeHolder = false,
            ) {
                phrfChange(it.digits(4))
            }
        }

        BoatType.Windseeker -> {
            RgInput(
                label = "WindSeeker Rating",
                value = wsRating,
                placeHolder = false,
                customClasses = listOf(AppStyle.marginBot),
            ) {
                wsRatingChange(it.digits(4))
            }
            RgSwitch("wsflying", 0, "Flying Sails", check = { wsFlying }) {
                wsFlyingChange(it)
            }
        }
    }
}