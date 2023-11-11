package viewmodel

import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.launch
import utils.Api

data class PeopleState(
    val people: Async<List<Person>> = Uninitialized,
    val editPerson: Person? = null
) : VmState


class PeopleViewModel : BaseViewModel<PeopleState>(PeopleState()) {
    init {
        launch { fetchAllPeople() }
    }

    private fun fetchAllPeople() {
        launch {
            setState { copy(people = people.loading()) }
            val response = Api.getAllPeople()
            setState {
                copy(people = response.body?.let {
                    Complete(it)
                } ?: people.error(response.error))
            }
        }
    }

    fun upsertPerson(person: Person) {
        setEditPerson(null)
        launch {
            Api.postPerson(person)
            fetchAllPeople()
        }
    }

    fun delete(person: Person) {
        setEditPerson(null)
        person.id?.let {
            setState {
                val delete = Api.deletePerson(person.id)
                if (delete.ok) {
                    copy(people = Api.getAllPeople().toAsync(people))
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
}
