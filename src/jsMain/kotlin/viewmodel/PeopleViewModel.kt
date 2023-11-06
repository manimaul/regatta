package viewmodel

import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.launch
import utils.Network
import utils.Scopes.mainScope


sealed interface PeopleState

data object PeopleStateLoading : PeopleState

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
        Network.get<List<Person>>("people").body?.let {
            peopleState.value = PeopleStateLoaded(it)
        }
    }

    fun upsertPerson(person: Person) {
        mainScope.launch {
            peopleState.value = PeopleStateLoading
            Network.post<Person, Person>("person", person)
            fetchAllPeople()
        }
    }
}
