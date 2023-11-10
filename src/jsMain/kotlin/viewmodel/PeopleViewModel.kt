package viewmodel

import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import utils.Api
import utils.Scopes.mainScope

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

    fun reload() {
        fetchAllPeople()
    }

    fun upsertPerson(person: Person) {
        setEditPerson(null)
        mainScope.launch {
            Api.postPerson(person)
            fetchAllPeople()
        }
    }

    fun delete(person: Person) {
        setEditPerson(null)
        launch {
            person.id?.let {
                Api.deletePerson(person.id)
                fetchAllPeople()
            }
        }
    }

    fun setEditPerson(person: Person?) {
        setState {
            copy(editPerson = person)
        }
    }
}
