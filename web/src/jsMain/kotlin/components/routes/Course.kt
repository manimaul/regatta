package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import components.RgDropdown
import org.jetbrains.compose.web.dom.Div
import styles.AppStyle
import utils.chartViewModel
import utils.marks

@Composable
fun Course() {
    val state by chartViewModel.flow.collectAsState()

    Div(attrs = {
        classes(AppStyle.marginBot)
    }) {
        RgDropdown(
            items = marks,
            selectedItem = state.mark,
            name = { "${it?.letter} ${it?.name} - ${it?.desc}" },
            handler = {
                chartViewModel.selectMark(it)
            })
    }
    Div(
        attrs = {
            classes("flex-grow-1")
            ref { element ->
                chartViewModel.controller.createMapView(element)
                object : DisposableEffectResult {
                    override fun dispose() {
                        chartViewModel.controller.disposeMapView()
                    }
                }
            }
        }
    )
}