package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import utils.Api


data class BoatPeopleComposite(
    val boats: List<Boat>,
    val people: List<Person>,
)

data class BoatState(
    val response: Async<BoatPeopleComposite> = Uninitialized,
    val deleteBoat: Boat? = null,
) : VmState

class BoatViewModel : BaseViewModel<BoatState>(BoatState()) {

    init {
        getAllBoats()
    }

    private fun getAllBoats() {
        setState {
            copy(
                response = combineAsync(Api.getAllBoats(), Api.getAllPeople()) { boats, people ->
                    BoatPeopleComposite(boats, people)
                }
            )
        }
    }

    fun addBoat(boat: Boat) {
        setState {
            val boats = Api.postBoat(boat).toAsync().flatMap { Api.getAllBoats().toAsync() }
            copy(
                response = response.flatMap { comp ->
                    boats.map { boats ->
                        comp.copy(boats = boats)
                    }
                }
            )
        }
    }

    fun deleteBoat(boat: Boat) {
        setDeleteBoat(null)
        boat.id?.let {id ->
            setState {
                val boats = Api.deleteBoat(id).toAsync().flatMap { Api.getAllBoats().toAsync() }
                copy(
                    response = response.flatMap { comp ->
                        boats.map { boats ->
                            comp.copy(boats = boats)
                        }
                    }
                )
            }
        }
    }

    fun setDeleteBoat(boat: Boat?) {
        setState {
            copy(deleteBoat = boat)
        }
    }
}
