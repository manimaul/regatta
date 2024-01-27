package viewmodel

import com.mxmariner.regatta.data.RaceReport
import utils.Api
import utils.Async
import utils.Loading
import utils.toAsync

data class RaceReportState(
    val report: Async<RaceReport> = Loading()
) : VmState

class RaceResultViewViewModel(
    private val raceId: Long
) : BaseViewModel<RaceReportState>(RaceReportState()) {
    override fun reload() {
        setState { copy(report = Loading()) }
        setState { copy(report = Api.getReport(raceId).toAsync()) }
    }

    init {
        setState { copy(report = Api.getReport(raceId).toAsync()) }
    }
}