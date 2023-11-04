package viewmodel

import utils.Network
import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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
        val people = minimumDelay(1500) {
            Network.fetch<List<Person>>("people")
        }
        peopleState.value = PeopleStateLoaded(people)
    }

    fun upsertPerson(person: Person) {
        mainScope.launch {
            peopleState.value = PeopleStateLoading
            Network.post<Person>("person", person)
            fetchAllPeople()
        }
    }
}

suspend fun <T> minimumDelay(ms: Long, action: suspend () -> T): T {
    val start = Clock.System.now()
    val result = action()
    val elapsed = Clock.System.now().minus(start)
    (ms - elapsed.inWholeMilliseconds).takeIf { it > 0 }?.let {
        delay(it)
    }
    return result
}