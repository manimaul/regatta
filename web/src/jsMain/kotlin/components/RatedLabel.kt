package components

import androidx.compose.runtime.Composable
import com.mxmariner.regatta.data.RatingType
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text

/**
 * The rating cell of a results table: [RatingType.ratedLabel] with the ORC reference number linked to the
 * certificate's public page when [orcUrl] is known (a new tab). Without a URL, or when the label does not
 * contain the reference, the plain label is rendered exactly as before.
 */
@Composable
fun RatedLabel(ratingType: RatingType, phrfRating: Int?, orcRef: String?, orcUrl: String?) {
    val label = ratingType.ratedLabel(phrfRating, orcRef)
    val at = if (orcUrl == null || orcRef.isNullOrBlank()) -1 else label.indexOf(orcRef)
    if (at < 0 || orcRef == null || orcUrl == null) {
        Text(label)
        return
    }
    Text(label.substring(0, at))
    A(
        attrs = {
            attr("target", "_blank")
            attr("rel", "noopener noreferrer")
        },
        href = orcUrl,
    ) {
        Text(orcRef)
    }
    Text(label.substring(at + orcRef.length))
}
