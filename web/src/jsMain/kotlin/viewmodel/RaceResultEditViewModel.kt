package viewmodel

import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.RaceReport
import utils.*
import kotlin.math.max

data class RaceResultEditState(
    val report: Async<RaceReport> = Loading(),
    val boats: Async<List<BoatSkipper>> = Loading(),
    val maxHoc: Int = 1,
) : VmState

class RaceResultEditViewModel(
    private val raceId: Long,
    val addViewModel: RaceResultAddViewModel = RaceResultAddViewModel(raceId),
) : BaseViewModel<RaceResultEditState>(RaceResultEditState()) {
    init {
        reload()
    }

    override fun reload() {
        setState {
            copy(
                report = Api.getReport(raceId).toAsync(),
                boats = Api.getAllBoats().toAsync(),
            )
        }
    }

    fun addResult(value: RaceResultAddState) {
        addViewModel.setCard()
        value.asPost()?.let { post ->
            setState(
                { Api.postResult(post).toAsync() },
                { Api.getReport(raceId).toAsync() }
            ) { _, report ->
                copy(
                    report = report,
                    maxHoc = findMaxHoc(maxHoc, report.value),
                )
            }
        }
    }

    private fun findMaxHoc(currentHoc: Int, value: RaceReport?): Int {
        var hoc = currentHoc
        value?.classReports?.forEach { cat ->
            cat.bracketReport.forEach { cls ->
                cls.cards.forEach { card ->
                    card.hocPosition?.let {
                        hoc = max(it, hoc)
                    }
                }
            }
        }
        return hoc
    }

    fun delete(id: Long) {
        setState(
            { Api.deleteResult(id).toAsync() },
            { Api.getReport(raceId).toAsync() }
        ) { _, report ->
            copy(
                report = report,
                maxHoc = findMaxHoc(maxHoc, report.value),
            )
        }
        addViewModel.setCard()
    }
}