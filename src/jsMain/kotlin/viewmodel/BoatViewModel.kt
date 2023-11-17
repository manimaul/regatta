package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RaceClass
import com.mxmariner.regatta.data.RaceClassCategory
import kotlinx.coroutines.launch
import utils.*


data class BoatPeopleComposite(
    val boats: List<Boat>,
    val people: List<Person>,
    val raceClass: List<RaceClassCategory>,
)

data class BoatState(
    val response: Async<BoatPeopleComposite> = Uninitialized,
    val editPerson: Person? = null,
    val editBoat: Boat? = null,
) : VmState

class BoatViewModel : BaseViewModel<BoatState>(BoatState()) {

    init {
        getAllBoatsAndPeople()
    }

    private fun getAllBoatsAndPeople() {
        setState {
            copy(
                response = combineAsync(Api.getAllBoats(), Api.getAllPeople(), Api.getAllCategories()) { boats, people, raceClasses ->
                    BoatPeopleComposite(boats, people, raceClasses)
                }
            )
        }
    }

    fun findBoatName(person: Person, composite: BoatPeopleComposite): String {
        return composite.boats.firstOrNull {
            it.skipper?.id == person.id
        }?.name ?: "-"
    }

    fun upsertPerson(person: Person) {
        setEditPerson(null)
        launch {
            Api.postPerson(person)
            getAllBoatsAndPeople()
        }
    }

    fun delete(person: Person) {
        setEditPerson(null)
        person.id?.let {
            setState {
                val delete = Api.deletePerson(person.id)
                if (delete.ok) {
                    Api.getAllPeople().body?.let { people ->
                        copy(response = response.map { it.copy(people = people) })

                    } ?: this
                } else {
                    this
                }
            }
        }
    }

    fun setEditPerson(person: Person?) {
        setState {
            copy(editPerson = person)
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
        setEditBoat(null)
        boat.id?.let { id ->
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

   fun setEditBoat(boat: Boat?) {
       setState {
           copy(editBoat = boat)
       }
   }

    fun upsertBoat(newBoat: Boat) {
        setEditBoat(null)
        launch {
            Api.postBoat(newBoat)
            getAllBoatsAndPeople()
        }
    }
}
