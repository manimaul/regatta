package viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.launch
import utils.Api
import utils.Scopes.mainScope

data class SeriesState(
    val series: Async<List<Series>> = Uninitialized,
    val deleteSeries: Series? = null,
    val newSeries: Series = Series(),
) : VmState

class SeriesViewModel : BaseViewModel<SeriesState>(SeriesState()) {

    init {
        launch {
            setState {
                copy(series = Api.allSeries().toAsync())
            }
        }
    }

    fun deleteSeries(series: Series) {
        launch {
            series.id?.let {
                setState {
                    copy(
                        deleteSeries = null,
                        series = Api.deleteSeries(it).toAsync().flatMap {
                            Api.allSeries().toAsync()
                        }
                    )
                }
            }
        }
    }

    fun addSeries() {
        launch {
            setState {
                copy(
                    series = Api.postSeries(newSeries).toAsync().map { series.value?.plus(it) ?: emptyList() },
                    newSeries = Series(),
                )
            }
        }
    }

    fun confirmDeleteSeries(series: Series?) {
        setState { copy(deleteSeries = series) }
    }

    fun setNewSeriesName(name: String) {
        setState { copy(newSeries = newSeries.copy(name = name)) }
    }
}
