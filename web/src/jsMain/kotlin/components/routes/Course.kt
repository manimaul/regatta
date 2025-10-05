package components.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffectResult
import org.jetbrains.compose.web.dom.Div
import utils.chartViewModel

@Composable
fun Course() {
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
        })
}