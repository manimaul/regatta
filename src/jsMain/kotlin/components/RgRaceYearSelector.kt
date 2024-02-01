package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import components.routes.RgYearSelect
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import utils.*
import viewmodel.BaseViewModel
import viewmodel.VmState
import viewmodel.complete

data class RgRaceYearState(
    val year: Int? = currentYear().toInt(),
    val years: Async<List<String>> = Loading()
) : VmState

class RgRaceYearViewModel : BaseViewModel<RgRaceYearState>(RgRaceYearState()) {

    init {
        setState {
            copy(years = Api.getYears().toAsync())
        }
    }

    fun selectedYear(): Int? = flow.value.year

    fun selectYear(year: String?): Int? {
        return year?.toIntOrNull()?.let { y ->
            setState {
                copy(
                    year = y,
                )
            }
            y
        }
    }

    override fun reload() {
        setState { copy(years = Loading()) }
        setState {
            copy(years = Api.getYears().toAsync())
        }
    }
}

private val rgRaceYearViewModel = RgRaceYearViewModel()

fun selectedYear(): Int? = rgRaceYearViewModel.flow.value.year

@Composable
fun RgRaceYearSelector(
    viewModel: RgRaceYearViewModel = rgRaceYearViewModel,
    onYearSelect: ((Int?) -> Unit)? = null,
) {
    val state = viewModel.flow.collectAsState()
    Div(attrs = { style { property("width", "fit-content") } }) {
        state.value.years.complete(viewModel, loading = {
            Text("...")
        }) { yearList ->
            if (yearList.isNotEmpty()) {
                RgYearSelect(year = "${state.value.year}", years = yearList) {
                    val y = viewModel.selectYear(it)
                    onYearSelect?.invoke(y)
                }
            }
        }
    }
}
