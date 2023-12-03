package viewmodel

import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.launch
import utils.*

data class SeriesState(
    val series: Async<List<Series>> = Loading(),
    val newSeries: Series = Series(),
) : VmState

class SeriesViewModel(
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<SeriesState>(SeriesState()) {

    init {
        setState {
            copy(series = Api.allSeries().toAsync())
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

    fun setNewSeriesName(name: String) {
        setState { copy(newSeries = newSeries.copy(name = name)) }
    }

    override fun reload() {
        setState { SeriesState() }
        setState { copy(series = Api.allSeries().toAsync()) }
    }

    fun editSeries(id: Long?) {
        id?.let { routeVm.pushRoute("/series/$id") }
    }
}
