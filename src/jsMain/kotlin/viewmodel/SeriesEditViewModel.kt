package viewmodel

import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.launch
import utils.*

data class SeriesEditState(
    val series: Async<Series> = Uninitialized,
    val operation: Operation = Operation.None,
) : VmState

class SeriesEditViewModel(
    val id: Long?,
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<SeriesEditState>(SeriesEditState()) {
    init {
        reload()
    }

    override fun reload() {
        id?.let {
            setState {
                SeriesEditState(
                    series = Api.getSeries(id).toAsync(),
                    operation = Operation.Fetched
                )
            }
        }
    }

    fun cancelEdit() {
        routeVm.goBackOrHome()
    }

    fun upsert(newSeries: Series) {
        setState {
            copy(
                series = Api.postSeries(newSeries).toAsync(),
                operation = Operation.Updated
            )
        }
    }

    fun delete(series: Series) {
        series.id?.let { id ->
            setState {
                copy(
                    series = Api.deleteSeries(id).toAsync().map { series },
                    operation = Operation.Deleted
                )
            }
        }
    }
}