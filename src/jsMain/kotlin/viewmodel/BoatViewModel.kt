package viewmodel

import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.Boat
import kotlinx.coroutines.launch
import utils.Api
import utils.Scopes.mainScope

sealed interface BoatState

data object BoatStateLoading : BoatState

data class BoatStateLoaded(
    val boats: List<Boat>
) : BoatState

class BoatViewModel(
    val peopleVm: PeopleViewModel = PeopleViewModel()
) {
    private val internalState = mutableStateOf<BoatState>(BoatStateLoading)

    val state: BoatState
        get() = internalState.value

    private fun setState(state: BoatState) {
        internalState.value = state
    }

    fun addBoat(boat: Boat) {
       mainScope.launch {
           setState(BoatStateLoading)
           internalState.value = BoatStateLoading
           Api.postBoat(boat)
           setState(
               BoatStateLoaded(
                   Api.getAllBoats().body ?: emptyList()
               )
           )
       }
    }

    init {
        mainScope.launch {
            setState(
                BoatStateLoaded(
                    Api.getAllBoats().body ?: emptyList()
                )
            )
        }
    }
}