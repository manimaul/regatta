package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClass
import utils.*

data class EditBoatComposite(
    val boatSkipper: BoatSkipper,
    val people: List<Person>,
)

data class EditBoatState(
    val data: Async<EditBoatComposite> = Uninitialized,
    val operation: Operation = Operation.None,
) : VmState

class EditBoatViewModel(
    val id: Long,
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<EditBoatState>(EditBoatState()) {

    init {
        reload()
    }

    override fun reload() {
        setState {
            EditBoatState(
                data = combineAsync(
                    Api.getBoatSkipper(id),
                    Api.getAllPeople(),
                ) { boat, people ->
                    EditBoatComposite(boat, people)
                }.mapErrorMessage { "error fetching boat id $id" },
                operation = Operation.Fetched
            )
        }
    }

    fun upsertBoat(newBoat: Boat) {
        setState {
            copy(
                data = Api.postBoat(newBoat).toAsync().mapErrorMessage { "error updating boat id $id" }
                    .flatMap { boatSkipper ->
                        data.map { it.copy(boatSkipper = boatSkipper) }
                    },
                operation = Operation.Updated
            )
        }
    }

    fun deleteBoat(boat: Boat) {
        setState {
            copy(
                data = Api.deleteBoat(boat.id).toAsync().mapErrorMessage { "error deleting boat id $id" }.flatMap {
                    data.map { it.copy(boatSkipper = it.boatSkipper.copy(boat = boat)) }
                },
                operation = Operation.Deleted
            )
        }
    }

    fun cancelEdit() {
        routeVm.goBackOrHome()
    }
}
