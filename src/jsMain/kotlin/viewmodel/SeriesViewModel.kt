package viewmodel

import com.mxmariner.regatta.data.Series
import com.mxmariner.regatta.moveItem
import kotlinx.coroutines.launch
import utils.*

data class SeriesState(
    val editId: Long? = null,
    val series: Async<List<Series>> = Loading(),
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
                series = Api.postSeries(s).toAsync().flatMap { Api.allSeries().toAsync() },
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

    fun moveUp(series: Series) {
        flow.value.series.value?.moveItem(up = true) { it.id == series.id }?.let { lst ->
            setState {
                lst.forEachIndexed { i, s ->
                    Api.postSeries(s.copy(sort = i))
                }
                copy(series = Api.allSeries().toAsync())
            }
        }
    }

    fun moveDown(series: Series) {
        flow.value.series.value?.moveItem(up = false) { it.id == series.id }?.let { lst ->
            setState {
                lst.forEachIndexed { i, s ->
                    Api.postSeries(s.copy(sort = i))
                }
                copy(series = Api.allSeries().toAsync())
            }
        }
    }
}
