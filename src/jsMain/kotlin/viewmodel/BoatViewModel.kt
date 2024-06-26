package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.launch
import utils.*


data class BoatPeopleComposite(
    val boats: List<BoatSkipper>,
    val people: List<Person>,
)

data class BoatState(
    val response: Async<BoatPeopleComposite> = Loading(),
) : VmState

class BoatViewModel(
    val routeVm: RouteViewModel = routeViewModel,
) : BaseViewModel<BoatState>(BoatState()) {

    init {
        getAllBoatsAndPeople()
    }

    private fun getAllBoatsAndPeople() {
        setState {
            copy(
                response = combineAsync(Api.getAllBoats(), Api.getAllPeople()) { boats, people ->
                    BoatPeopleComposite(boats, people)
                }
            )
        }
    }

    fun findBoatName(person: Person, composite: BoatPeopleComposite): String {
        return composite.boats.firstOrNull {
            it.skipper?.id == person.id
        }?.boat?.name ?: "-"
    }

    fun upsertPerson(person: Person) {
        setEditPerson(null)
        launch {
            Api.postPerson(person)
            getAllBoatsAndPeople()
        }
    }

    fun setEditPerson(person: Person?) {
        person?.id?.let {
            routeVm.pushRoute("/people/$it")
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


   fun setEditBoat(boat: Boat?) {
       boat?.id?.let {
           routeVm.pushRoute("/boat/${boat.id}")
       }
   }

    override fun reload() {
       setState { BoatState() }
        getAllBoatsAndPeople()
    }
}
