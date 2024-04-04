package viewmodel

import com.mxmariner.regatta.data.Series
import utils.*

data class SeriesState(
    val editId: Long? = null,
    val series: Async<List<Series>> = Loading(),
    val sortMode: Boolean = false,
) : VmState

class SeriesViewModel(
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<SeriesState>(SeriesState()) {

    init {
        setState {
            copy(series = Api.allSeries().toAsync())
        }
    }

    fun nextSort(): Int {
        return flow.value.series.value?.maxByOrNull { it.sort }?.let { it.sort + 1 } ?: 0
    }

    override fun reload() {
        setState { SeriesState() }
        setState { copy(series = Api.allSeries().toAsync()) }
    }

    fun editSeries(id: Long?) {
        setState { copy(editId = id) }
    }


    fun upsert(series: Series) {
        val s = if (series.id > 0) {
            series
        } else {
            series.copy(sort = nextSort())
        }
        setState {
            copy(
                series = Api.postSeries(listOf(s)).toAsync().flatMap { Api.allSeries().toAsync() },
            )
        }
    }

    fun delete(series: Series) {
        setState {
            copy(
                series = Api.deleteSeries(series.id).toAsync().flatMap { Api.allSeries().toAsync() },
            )
        }
    }

    fun sortMode(b: Boolean) {
        setState { copy(sortMode = b) }
    }

    fun saveOrder(order: List<Series>) {
        setState { copy(
            series = series.loading(),
            sortMode = false,
        ) }
        setState {
            copy(
                series = Api.postSeries(order).toAsync().flatMap { Api.allSeries().toAsync() },
            )
        }
    }
}
