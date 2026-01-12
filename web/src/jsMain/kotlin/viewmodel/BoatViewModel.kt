package viewmodel

import OrcCertificate
import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.BoatSkipper
import com.mxmariner.regatta.data.Person
import com.mxmariner.regatta.data.RatingType
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
    val addEditPerson: Person = Person(),
    val showConfirmDeletePerson: Boolean = false,
    val addEditMode: AddEditBoatMode = AddEditBoatMode.Adding,
    val response: Async<BoatPeopleComposite> = Loading(),
) : VmState


val boatViewModel = BoatViewModel()

class BoatViewModel : BaseViewModel<BoatState>(BoatState()) {

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

    fun upsertPerson() {
        withState {
            launch {
                Api.postPerson(it.addEditPerson)
                getAllBoatsAndPeople()
            }
            setEditPerson(Person())
        }
    }

    fun deletePerson() {
        withState {
            launch {
                Api.deletePerson(it.addEditPerson.id)
                getAllBoatsAndPeople()
            }
            setEditPerson(Person())
        }
    }

    fun confirmDeletePerson(show: Boolean = true) {
        setState {
            copy(showConfirmDeletePerson = show)
        }
    }

    fun setEditPerson(person: Person) {
        setState {
            copy(
                addEditPerson = person,
                showConfirmDeletePerson = false
            )
        }
    }

    fun setEditPersonFirst(name: String) {
        setState {
            copy(addEditPerson = addEditPerson.copy(first = name))
        }
    }

    fun setEditPersonLast(name: String) {
        setState {
            copy(addEditPerson = addEditPerson.copy(last = name))
        }
    }

    fun setEditPersonMember(member: Boolean) {
        setState {
            copy(addEditPerson = addEditPerson.copy(clubMember = member))
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

    private fun BoatSkipper?.addState(): AddEditBoatState {
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

    fun setEditBoatRatingType(ratingType: RatingType, rating: Int) {
        withState {
            val boat = when (ratingType) {
                RatingType.ORC_PHRF-> it.addEditState.addBoat.copy(
                    ratingType = ratingType,
                    phrfRating = rating,
                    orcCerts = listOf(OrcCertificate())
                )
                RatingType.ORC -> it.addEditState.addBoat.copy(
                    ratingType = ratingType,
                    phrfRating = null,
                    orcCerts = listOf(OrcCertificate())
                )
                RatingType.PHRF -> it.addEditState.addBoat.copy(
                    ratingType = ratingType,
                    phrfRating = rating
                )
                RatingType.CruisingFlyingSails -> it.addEditState.addBoat.copy(
                    ratingType = ratingType,
                    phrfRating = null
                )
                RatingType.CruisingNonFlyingSails -> it.addEditState.addBoat.copy(
                    ratingType = ratingType,
                    phrfRating = null
                )
            }
            setState {
                copy(addEditState = addEditState.copy(addBoat = boat))
            }
        }
    }
}

