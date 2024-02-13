package components

import androidx.compose.runtime.Composable
import com.mxmariner.regatta.data.RatingType
import styles.AppStyle
import utils.digits

@Composable
fun RatingSelections(
    boatType: RatingType,
    phrfRating: String,
    wsRating: String,
    wsFlying: Boolean,
    typeChagne: (RatingType) -> Unit,
    phrfChange: (String) -> Unit,
    wsRatingChange: (String) -> Unit,
    wsFlyingChange: (Boolean) -> Unit,
) {
    RgDropdown(RatingType.entries, boatType, { it.name }) {
        typeChagne(it)
    }
    when (boatType) {
        RatingType.PHRF -> {
            RgInput(
                label = "PHRF Rating",
                value = phrfRating,
                placeHolder = false,
            ) {
                phrfChange(it.digits(4))
            }
        }

        RatingType.Windseeker -> {
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