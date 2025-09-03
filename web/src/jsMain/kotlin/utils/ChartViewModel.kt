package utils

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import viewmodel.BaseViewModel
import viewmodel.VmState

private fun mapLocation(): MapLocation? {
//    return routeViewModel.flow.value.current.takeIf {
//        it.route == Route.Enc
//    }?.let { it.params?.values }?.let {
//        val lng = it["lng"]?.toDoubleOrNull()
//        val lat = it["lat"]?.toDoubleOrNull()
//        val z = it["z"]?.toDoubleOrNull()
//        if (lng != null && lat != null && z != null) {
//            MapLocation(lng, lat, z)
//        } else {
//            null
//        }
//    } ?:
    return localStoreGet<MapLocation>()
}

data class ChartState(
    val location: MapLocation = mapLocation() ?: MapLocation(),
    val query: List<dynamic> = emptyList(),
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


class ChartViewModel : BaseViewModel<ChartState>(ChartState()) {
    var controller: ChartController = ChartController()

    init {
        controller.onMoveEnd = { location ->
            println("moveend location: $location")
            setState { copy(location = location) }
        }
        controller.onClick = { point ->
            println("click point: $point")
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
}