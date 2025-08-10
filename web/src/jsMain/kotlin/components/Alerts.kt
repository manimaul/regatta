package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import org.jetbrains.compose.web.svg.height
import org.jetbrains.compose.web.svg.width
import styles.AppStyle
import viewmodel.AlertsViewModel

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun Alerts(
    alertsViewModel: AlertsViewModel = viewmodel.alertsViewModel
) {
    val state by alertsViewModel.flow.collectAsState()
    state.message?.let { message ->
        Div(attrs = {
            classes("alert", "alert-success", "d-flex", "align-items-center")
            attr("role", "alert")
        }) {
            Svg(attrs = {
                classes("bi", "bi-check2-circle")
                width(16)
                height(16)
            }) {
                Path(d = "M2.5 8a5.5 5.5 0 0 1 8.25-4.764.5.5 0 0 0 .5-.866A6.5 6.5 0 1 0 14.5 8a.5.5 0 0 0-1 0 5.5 5.5 0 1 1-11 0")
                Path(d = "M15.354 3.354a.5.5 0 0 0-.708-.708L8 9.293 5.354 6.646a.5.5 0 1 0-.708.708l3 3a.5.5 0 0 0 .708 0z")
            }
            Div(attrs = { classes(AppStyle.marginStart) }) {
                Text(message)
            }
        }
    }
}
