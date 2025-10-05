package utils

import kotlinx.browser.document
import kotlinx.serialization.ExperimentalSerializationApi
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalSerializationApi::class)
class ChartController {
    var mapView: MapLibre.Map? = null
    var onMoveEnd: ((MapLocation) -> Unit)? = null
    var onClick: ((MapPoint) -> Unit)? = null
    var onClickLocation: ((MapLibre.LngLat) -> Unit)? = null
    var onLoad: (() -> Unit)? = null

    fun move(location: MapLocation) {
        val options = js("{}")
        options.center = arrayOf(location.longitude, location.latitude)
        options.zoom = location.zoom
        mapView?.jumpTo(options)
    }

    fun popUp(mark: Mark) {
        mapView?.let { mv ->
            val popUp = MapLibre.Popup(
                options = PopupOptions(
                    closeOnClick = false,
                    closeButton = true
                )
            ).setLngLat(mark.position)
                .setHTML("<p><strong>${mark.name}</strong> - ${mark.desc}</p>")
                .addTo(mv)
        }
    }

    private var showing: MapLibre.Popup? = null

    fun addMarkPopup(mark: Mark) {
        mapView?.let { mv ->
            val popUp = MapLibre.Popup(
                options = PopupOptions(
                    closeOnClick = false,
                    closeButton = true
                )
            ).setLngLat(mark.position)
                .setHTML("""<h4>${mark.name}</h4>
                    <h6>${mark.desc}</h6>
                """.trimIndent())

            val el = document.createElement("div") as HTMLElement
            el.innerHTML = "<span>${mark.letter}</span>"
            el.style.color = "red"
            el.style.fontSize = "40px"
            el.onclick = {
                showing?.remove()
                showing = popUp
                popUp.addTo(mv)
            }
            MapLibre.Marker(MarkerOptions(element = el))
                .setLngLat(mark.position)
                .addTo(mv)
        }
    }

    fun getBounds(): List<MapLocation> {
        println("get bounds called")
        mapView?.getBounds()?.let { bounds ->
            println("bounds north = ${bounds.getNorth()}")
            println("bounds south = ${bounds.getSouth()}")
            println("bounds west = ${bounds.getWest()}")
            println("bounds east = ${bounds.getEast()}")
            //MapLocation(it[0], it[1], it[2])
        }
        return emptyList()
    }

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
                val pt = MapPoint(x, y)
                onClick?.invoke(pt)
                onClickLocation?.let { cb ->
                    mapView?.unproject(arrayOf(x, y))?.let { cb(it) }
                }
            }
            mv.on("load") {
                onLoad?.invoke()
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
        val obj = js("""{"style": "https://openenc.com/v1/style/feet/day"}""")
        obj["container"] = container
        obj["center"] = arrayOf(-122.44511255700473, 47.28327102060851)
        obj["zoom"] = 10.5
        obj["attributionControl"] = false
        obj["maxBounds"] = arrayOf(
            arrayOf(-123.66528922827865, 47.098629350626396), //southwest
            arrayOf(-121.34828434446568, 47.57621259351356) //northeast
        )
        return obj
    }

    fun unproject(mapPoint: MapPoint): MapLibre.LngLat? {
        return mapView?.unproject(arrayOf(mapPoint.x, mapPoint.y))
    }

    fun project(mapLocation: MapLocation): MapPoint? {
        return mapView?.let { mapView ->
            val p = mapView.project(arrayOf(mapLocation.longitude, mapLocation.latitude))
            MapPoint(p.x, p.y)
        }
    }
}
