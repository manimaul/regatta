package viewmodel

import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.launch
import utils.Api
import utils.Network
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
    private val peopleState = mutableStateOf<PeopleState>(PeopleStateLoading)
    val state: PeopleState
        get() = peopleState.value

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
            peopleState.value = PeopleStateLoading
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
