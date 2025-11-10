package utils

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import viewmodel.BaseViewModel
import viewmodel.VmState

private fun mapLocation(): MapLocation? {
    return localStoreGet<MapLocation>()
}

data class ChartState(
    val location: MapLocation = mapLocation() ?: MapLocation(),
    val query: List<dynamic> = emptyList(),
    val mark: Mark? = null,
) : VmState

@Serializable
data class MapLocation(
    val longitude: Double = -118.512,
    val latitude: Double = 33.96832,
    val zoom: Double = 12.0,
)

data class MapPoint(
    val x: Int,
    val y: Int,
)


val chartViewModel = ChartViewModel()


data class Mark(
    val name: String,
    val desc: String,
    val letter: String,
    val position: MapLibre.LngLat,
)

val marks = listOf(
    Mark(
        name = "TTPYC Mark 'A'",
        desc = "White Column",
        letter = "A",
        position = MapLibre.LngLat(-122.33305114731311, 47.39704811163327)
    ),
    Mark(
        name = "Blair Waterway",
        desc = "Yellow Perm. Can",
        letter = "B",
        position = MapLibre.LngLat(-122.42167447164488, 47.27810246077564)
    ),
    Mark(
        name = "Chinese Reconciliation Park (Pagoda)",
        desc = "Temp",
        letter = "C",
        position = MapLibre.LngLat(-122.460895, 47.276678)
    ),
    Mark(
        name = "Dashpoint Temp",
        desc = "Temp",
        letter = "D",
        position = MapLibre.LngLat(-122.43615006110504, 47.32174845199978)
    ),
    Mark(
        name = "Poverty Bay",
        desc = "Temp",
        letter = "E",
        position = MapLibre.LngLat(-122.34450922624008, 47.345379233271075)
    ),
    Mark(
        name = "Foss Waterway",
        desc = "Temp",
        letter = "F",
        position = MapLibre.LngLat(-122.4379856307234, 47.26191956366969)
    ),
    Mark(
        name = "TTPYC Mark 'I'",
        desc = "White Column",
        letter = "I",
        position = MapLibre.LngLat(-122.33392193748179, 47.386890766454115)
    ),
    Mark(
        name = "TTPYC Mark 'J'",
        desc = "White Column",
        letter = "J",
        position = MapLibre.LngLat(-122.37664522730665, 47.383257011839646)
    ),
    Mark(
        name = "Lighthouse at Brown's Pt.",
        desc = "Temp",
        letter = "L",
        position = MapLibre.LngLat(-122.44710740743537, 47.306778710579266)
    ),
    Mark(
        name = "Madronas",
        desc = "Temp",
        letter = "M",
        position = MapLibre.LngLat(-122.44721754644462, 47.29943077416439)
    ),
    Mark(
        name = "Neil Point",
        desc = "Temp",
        letter = "N",
        position = MapLibre.LngLat(-122.48771988020714, 47.329219485017916)
    ),
    Mark(
        name = "Outfall Buoy",
        desc = "Yellow Perm. Mark",
        letter = "O",
        position = MapLibre.LngLat(-122.48416224770311, 47.28729287090877)
    ),
    Mark(
        name = "Piner Point",
        desc = "Temp",
        letter = "P",
        position = MapLibre.LngLat(-122.45365267698233, 47.34233563126003)
    ),
    Mark(
        name = "Arrrmy Dock",
        desc = "Temp",
        letter = "R",
        position = MapLibre.LngLat(-122.4158178975889, 47.28649587180013)
    ),
    Mark(
        name = "Quartermaster Harbor",
        desc = "Temp",
        letter = "Q",
        position = MapLibre.LngLat(-122.47008809457841, 47.382987204303305)
    ),
    Mark(
        name = "TYC White Buoy",
        desc = "Perm. Mark",
        letter = "T",
        position = MapLibre.LngLat(-122.50489938041565, 47.30572288135093)
    ),
    Mark(
        name = "Mauri Island",
        desc = "Temp",
        letter = "U",
        position = MapLibre.LngLat(-122.42235618134569, 47.36942826972913)
    ),
    Mark(
        name = "Manzzzzanita",
        desc = "Red Perm. Can",
        letter = "Z",
        position = MapLibre.LngLat(-122.4794968763011, 47.34899776773085)
    )
)

class ChartViewModel : BaseViewModel<ChartState>(ChartState()) {
    var controller: ChartController = ChartController()

    init {
        controller.onLoad = {
            marks.forEach { ea ->
                controller.addMarkPopup(
                    mark = ea,
                    onSelect = { mark ->
                        selectMark(mark)
                    },
                    onClose = { mark ->
                        withState { state ->
                            if (state.mark == mark) {
                                setState { copy(mark = null) }
                            }
                        }
                    }
                )
            }
        }

        controller.onMoveEnd = { location ->
            println("moveend location: $location")
            controller.getBounds()
            setState { copy(location = location) }
        }
        controller.onClick = { point ->
            println("click point: $point")
        }

        controller.onClickLocation = { location ->
        }

        launch {
            flow.map { it.location }.collect {
                println("location change: $it")
                localStoreSet(it)
            }
        }
    }

    override fun reload() {
        setState { ChartState() }
    }

    fun selectMark(mark: Mark?) {
        withState { state ->
            state.mark?.let { controller.hidePopup(it)}
        }
        setState { copy(mark = mark) }
        mark?.let { controller.showPopup(it) }
    }
}

