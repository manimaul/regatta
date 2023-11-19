package viewmodel

import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.launch
import utils.*
import utils.Error

data class PeopleEditState(
    val person: Async<Person> = Uninitialized,
    val operation: Operation = Operation.None
) : VmState

class PeopleEditViewModel(
    val personId: Long?,
    val routeVm: RouteViewModel = routeViewModel
) : BaseViewModel<PeopleEditState>(PeopleEditState()) {

    init {
        launch {
            personId?.let {
                setState {
                    PeopleEditState(
                        Api.getPerson(personId).toAsync().mapErrorMessage { "error fetching user id $personId" },
                        Operation.Fetched
                    )
                }
            } ?: setState {
                PeopleEditState(
                    Error(),
                    Operation.Fetched
                )
            }
        }
    }

    fun delete(person: Person) {
        person.id?.let {
            setState {
                copy(
                    person = Api.deletePerson(person.id).toAsync().map { person }
                        .mapErrorMessage { "error deleting ${person.first}, ${person.last}" },
                    operation = Operation.Deleted
                )
            }
        }
    }

    fun cancelEdit() {
        routeVm.goBackOrHome()
    }

    fun upsertPerson(person: Person) {
        setState {
            copy(
                person = Api.postPerson(person).toAsync()
                    .mapErrorMessage { "error updating ${person.first}, ${person.last}" },
                operation = Operation.Updated
            )
        }
    }

}
