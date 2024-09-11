package viewmodel

import com.mxmariner.regatta.data.*
import components.selectedYear
import kotlinx.coroutines.launch
import utils.*

data class SeriesStandingsState(
    val standings: Async<StandingsSeries> = Loading()
) : VmState

class SeriesStandingsViewModel(
    val id: Long?,
    val year: Int?,
) : BaseViewModel<SeriesStandingsState>(SeriesStandingsState()) {

    init {
        reload()
    }

    override fun reload() {
        if (id != null && year != null) {
            setState {
                SeriesStandingsState(
                    standings = Api.getSeriesStanding(id, year).toAsync()
                )
            }
        }
    }
}
