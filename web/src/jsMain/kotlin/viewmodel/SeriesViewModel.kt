package viewmodel

import com.mxmariner.regatta.data.Series
import utils.*

data class SeriesState(
    val series: Async<List<Series>> = Loading(),
    val addEditSeries: Series = Series(),
    val sortMode: Boolean = false,
) : VmState

class SeriesViewModel() : BaseViewModel<SeriesState>(SeriesState()) {

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

    fun editSeries(series: Series) {
        setState { copy(addEditSeries = series) }
    }

    fun editSeriesName(name: String) {
        setState { copy(addEditSeries = addEditSeries.copy(name = name)) }
    }

    fun upsert(series: Series, onSuccess: (Series) -> Unit) {
        val s = if (series.id > 0) {
            series
        } else {
            series.copy(sort = nextSort())
        }
        setState {
            copy(
                series = Api.postSeries(listOf(s)).toAsync().flatMap {
                    onSuccess(s)
                    Api.allSeries().toAsync()
                },
            )
        }
    }

    fun delete(series: Series, onSuccess: (Series) -> Unit) {
        setState {
            copy(
                series = Api.deleteSeries(series.id).toAsync().flatMap {
                    onSuccess(series)
                    Api.allSeries().toAsync()
                },
            )
        }
    }

    fun sortMode(b: Boolean) {
        setState { copy(sortMode = b) }
    }

    fun saveOrder(order: List<Series>) {
        setState {
            copy(
                series = series.loading(),
                sortMode = false,
            )
        }
        setState {
            copy(
                series = Api.postSeries(order).toAsync().flatMap { Api.allSeries().toAsync() },
            )
        }
    }
}

