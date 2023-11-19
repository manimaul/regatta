package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClassCategory
import kotlinx.coroutines.launch
import utils.*

data class EditBoatComposite(
    val boat: Boat,
    val people: List<Person>,
    val raceClass: List<RaceClassCategory>,
)

data class EditBoatState(
    val data: Async<EditBoatComposite> = Uninitialized,
    val operation: Operation = Operation.None,
) : VmState

class EditBoatViewModel(
    val id: Long?,
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<EditBoatState>(EditBoatState()) {

    init {
        launch {
            id?.let {
                setState {
                    EditBoatState(
                        data = combineAsync(
                            Api.getBoat(id),
                            Api.getAllPeople(),
                            Api.getAllCategories()
                        ) { boat, people, cat ->
                            EditBoatComposite(boat, people, cat)
                        }.mapErrorMessage { "error fetching boat id $id" },
                        operation = Operation.Fetched
                    )
                }
            }
        }
    }

    fun upsertBoat(newBoat: Boat) {
        setState {
            copy(
                data = Api.postBoat(newBoat).toAsync().mapErrorMessage { "error updating boat id $id" }.map { boat ->
                    EditBoatComposite(boat, emptyList(), emptyList())
                },
                operation = Operation.Updated
            )
        }
    }
    fun deleteBoat(boat: Boat) {
        boat.id?.let { id ->
            setState {
                copy(
                    data = Api.deleteBoat(id).toAsync().mapErrorMessage { "error deleting boat id $id" }.map {
                        EditBoatComposite(boat, emptyList(), emptyList())
                    },
                    operation = Operation.Deleted
                )
            }
        }
    }
    fun cancelEdit() {
        routeVm.goBackOrHome()
    }
}
