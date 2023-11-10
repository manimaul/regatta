package viewmodel

import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import utils.Api
import utils.Scopes.mainScope


sealed interface PeopleState

data object PeopleStateLoading : PeopleState

data class DeletePerson(
    val person: Person
) : PeopleState

data class PeopleStateLoaded(
    val people: List<Person>
) : PeopleState

class PeopleViewModel {
    private val peopleState = MutableStateFlow<PeopleState>(PeopleStateLoading)

    val flow: StateFlow<PeopleState>
        get() = peopleState

    init {
        mainScope.launch { fetchAllPeople() }
    }

    suspend fun fetchAllPeople() {
        Api.getAllPeople().body?.let {
            peopleState.value = PeopleStateLoaded(it)
        }
    }

    fun reload() {
        mainScope.launch {
            peopleState.value = PeopleStateLoading
            fetchAllPeople()
        }
    }

    fun upsertPerson(person: Person) {
        mainScope.launch {
            Api.postPerson(person)
            fetchAllPeople()
        }
    }

    fun delete(person: Person) {
        mainScope.launch {
            person.id?.let {
                peopleState.value = PeopleStateLoading
                Api.deletePerson(person.id)
                fetchAllPeople()
            }
        }
    }
    fun setDeletePerson(person: Person) {
        peopleState.value = DeletePerson(person)
    }
}
