package utils

import kotlinx.serialization.ExperimentalSerializationApi
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalSerializationApi::class)
class ChartController {
    var mapView: MapLibre.Map? = null
    var onMoveEnd: ((MapLocation) -> Unit)? = null
    var onClick: ((MapPoint) -> Unit)? = null

    fun move(location: MapLocation) {
        val options = js("{}")
        options.center = arrayOf(location.longitude, location.latitude)
        options.zoom = location.zoom
        mapView?.jumpTo(options)
    }

//    fun fitBounds(bounds: Bounds) {
//        val topLeft = arrayOf(bounds.leftLng, bounds.topLat)
//        val botRight = arrayOf(bounds.rightLng, bounds.bottomLat)
//        mapView?.fitBounds(arrayOf(topLeft, botRight))
//    }

    fun createMapView(container: HTMLDivElement) {
        mapView = MapLibre.Map(mapLibreArgs(container)).also { mv ->
            mv.on("moveend") { event ->
                val center = event.target.getCenter()
                val zoom = event.target.getZoom() as Double
                val lat: Double = center.lat as Double
                val lng: Double = center.lng as Double
                onMoveEnd?.invoke(MapLocation(lng, lat, zoom))
            }
            mv.on("click") { event ->
                val x: Int = event.point.x as Int
                val y: Int = event.point.y as Int
                onClick?.invoke(MapPoint(x, y))
            }
        }
    }

    fun disposeMapView() {
        mapView?.remove()
        mapView = null
    }

    private fun mapLibreArgs(
        container: HTMLDivElement,
    ): dynamic {
        val state = chartViewModel.flow.value
        val obj = js("{}")
        obj["container"] = container
        obj["style"] = "https://openenc.com/v1/style/feet/day"
        obj["center"] = arrayOf(state.location.longitude, state.location.latitude)
        obj["zoom"] = state.location.zoom
        obj["attributionControl"] = false
        return obj
    }

    fun project(mapLocation: MapLocation): MapPoint? {
        return mapView?.let { mapView ->
            val p = mapView.project(arrayOf(mapLocation.longitude, mapLocation.latitude))
            MapPoint(p.x, p.y)
        }
    }
}
