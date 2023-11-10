package viewmodel

import com.mxmariner.regatta.data.Boat
import com.mxmariner.regatta.data.Person
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import utils.Api
import utils.Scopes.mainScope

sealed interface BoatState

data object BoatStateLoading : BoatState

data class BoatStateLoaded(
    val boats: List<Boat>,
    val people: List<Person>,
) : BoatState

class BoatViewModel {
    private val internalState = MutableStateFlow<BoatState>(BoatStateLoading)

    val flow: StateFlow<BoatState>
        get() = internalState

    init {
        getAllBoats()
    }

    private fun getAllBoats() {
        mainScope.launch {
            val boats: List<Boat> = Api.getAllBoats().body ?: emptyList()
            val people = Api.getAllPeople().body ?: emptyList()
            setState(BoatStateLoaded(boats, people))
        }

    }

    private fun setState(state: BoatState) {
        internalState.value = state
    }

    fun addBoat(boat: Boat) {
       mainScope.launch {
           setState(BoatStateLoading)
           internalState.value = BoatStateLoading
           Api.postBoat(boat)
           getAllBoats()
       }
    }

    fun setDeleteBoat(boat: Boat) {
        mainScope.launch {
            boat.id?.let {
                setState(BoatStateLoading)
                Api.deleteBoat(boat.id)
                getAllBoats()
            }
        }
    }

}