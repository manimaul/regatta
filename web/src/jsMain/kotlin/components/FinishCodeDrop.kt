package components

import androidx.compose.runtime.Composable
import com.mxmariner.regatta.data.FinishCode
import org.jetbrains.compose.web.dom.Div
import utils.finishText

@Composable
fun FinishCodeDrop(
    selected: FinishCode,
    hocPosition: Int?,
    showHoc: Boolean = true,
    customClasses: List<String>? = null,
    handler: (FinishCode) -> Unit
) {
    Div(attrs = {
        customClasses?.toTypedArray()?.let {
            classes(*it)
        }
    }) {
        RgDropdown(
            items = if (showHoc) FinishCode.entries else FinishCode.entries.filter { it != FinishCode.HOC }, selectedItem = selected, name = {
                it.finishText(hocPosition)
            }, handler = handler
        )
    }
}
