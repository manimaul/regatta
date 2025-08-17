package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RatingType
import com.mxmariner.regatta.data.Windseeker
import com.mxmariner.regatta.ratingDefault
import kotlinx.coroutines.launch
import utils.*


data class BoatPeopleComposite(
    val boats: List<BoatSkipper>,
    val people: List<Person>,
)

data class AddEditBoatState(
    val addBoat: Boat = Boat(),
    val addSkipper: Person? = null,
)

enum class AddEditBoatMode {
    Editing,
    Adding;

    fun label() = when (this) {
        Editing -> "Edit Boat"
        Adding -> "Add Boat"
    }
}

data class BoatState(
    val addEditState: AddEditBoatState = AddEditBoatState(),
    val addEditMode: AddEditBoatMode = AddEditBoatMode.Adding,
    val response: Async<BoatPeopleComposite> = Loading(),
) : VmState


val boatViewModel = BoatViewModel()

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

    fun deleteBoat(boat: Boat) {
        setState {
            val boats = Api.deleteBoat(boat.id).toAsync().flatMap { Api.getAllBoats().toAsync() }
            copy(
                response = response.flatMap { comp ->
                    boats.map { boats ->
                        comp.copy(boats = boats)
                    }
                }
            )
        }
    }

    fun setAddEditMode(mode: AddEditBoatMode, boatSkipper: BoatSkipper? = null) {
        setState {
            copy(
                addEditMode = mode,
                addEditState = boatSkipper.addState()
            )
        }
    }

    private fun BoatSkipper?.addState() : AddEditBoatState {
        return AddEditBoatState(
            addBoat = this?.boat?.copy(skipperId = skipper?.id) ?: Boat(skipperId = this?.skipper?.id),
            addSkipper = this?.skipper
        )
    }

    override fun reload() {
        setState { BoatState() }
        getAllBoatsAndPeople()
    }

    fun saveEditedBoat() {
        withState {
            val boat = it.addEditState.addBoat
            setState {
                val boats = Api.postBoat(boat).toAsync().flatMap { Api.getAllBoats().toAsync() }
                copy(
                    response = response.flatMap { comp ->
                        boats.map { boats ->
                            comp.copy(boats = boats)
                        }
                    },
                    addEditState = AddEditBoatState()
                )
            }
        }
    }

    fun clearEditBoat() {
        setState { copy(addEditState = AddEditBoatState()) }
    }

    fun setEditBoatName(name: String) {
        setState {
            val boat = addEditState.addBoat.copy(name = name)
            copy(addEditState = addEditState.copy(addBoat = boat))
        }
    }

    fun setEditBoatSkipper(person: Person?) {
        setState {
            val boat = addEditState.addBoat.copy(skipperId = person?.id)
            copy(
                addEditState = addEditState.copy(
                    addBoat = boat,
                    addSkipper = person
                )
            )
        }
    }

    fun setEditBoatSailNumber(sailNumber: String) {
        setState {
            val boat = addEditState.addBoat.copy(sailNumber = sailNumber)
            copy(addEditState = addEditState.copy(addBoat = boat))
        }
    }

    fun setSetEditBoatType(type: String) {
        setState {
            val boat = addEditState.addBoat.copy(boatType = type)
            copy(addEditState = addEditState.copy(addBoat = boat))
        }
    }

    fun setEditBoatRatingType(ratingType: RatingType) {
        withState {
            var boat = it.addEditState.addBoat
            if (boat.ratingType() != ratingType) {
                when (ratingType) {
                    RatingType.PHRF -> boat = boat.copy(windseeker = null, phrfRating = ratingDefault.toInt())
                    RatingType.Windseeker -> boat = boat.copy(windseeker = Windseeker(), phrfRating = null)
                }
                setState {
                    copy(addEditState = addEditState.copy(addBoat = boat))
                }
            }
        }
    }

    fun setEditBoatPhrfRating(rating: String) {
        setState {
            val boat = addEditState.addBoat.copy(phrfRating = rating.toIntOrNull())
            copy(addEditState = addEditState.copy(addBoat = boat))
        }
    }

    fun setEditBoatWsRating(wsRating: String) {
        setState {
            val boat = addEditState.addBoat.copy(
                windseeker = addEditState.addBoat.windseeker?.copy(
                    rating = wsRating.toIntOrNull() ?: ratingDefault.toInt()
                )
            )
            copy(addEditState = addEditState.copy(addBoat = boat))
        }
    }

    fun setEditBoatWsFlying(isFlying: Boolean) {
        setState {
            val boat = addEditState.addBoat.copy(
                windseeker = addEditState.addBoat.windseeker?.copy(
                    flyingSails = isFlying
                )
            )
            copy(addEditState = addEditState.copy(addBoat = boat))
        }
    }
}

