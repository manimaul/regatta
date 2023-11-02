package viewmodel

import utils.Network
import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.launch
import utils.Scopes.mainScope

class PeopleViewModel {
    private val peopleState = mutableStateOf<List<Person>>(emptyList())
    val people: List<Person>
        get() = peopleState.value

    init {
        mainScope.launch {
            peopleState.value = Network.fetch("people")
        }
    }

    fun upsertPerson(person: Person) {
        mainScope.launch {
            val newPerson = Network.post<Person>("person", person)
            peopleState.value.toMutableList().also { list ->
                list.indexOf(person).takeIf { it >= 0 }?.let { i ->
                    list[i] = newPerson
                } ?: run {
                   list.add(newPerson)
                }
                peopleState.value = list
            }
        }
    }
}