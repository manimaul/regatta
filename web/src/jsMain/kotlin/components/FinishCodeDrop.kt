package components

import androidx.compose.runtime.Composable
import com.mxmariner.regatta.data.FinishCode
import org.jetbrains.compose.web.dom.Div

@Composable
fun FinishCodeDrop(
    selected: FinishCode,
    hocPosition: Int?,
    customClasses: List<String>? = null,
    handler: (FinishCode) -> Unit
) {
    Div(attrs = {
        customClasses?.toTypedArray()?.let {
            classes(*it)
        }
    }) {
        RgDropdown(
            items = FinishCode.entries, selectedItem = selected, name = {
                when (it) {
                    FinishCode.TIME,
                    FinishCode.RET,
                    FinishCode.DNF,
                    FinishCode.NSC -> it.name

                    FinishCode.HOC -> "${it.name}${hocPosition?.let { " $it" } ?: ""}"
                }
            }, handler = handler
        )
    }
}
