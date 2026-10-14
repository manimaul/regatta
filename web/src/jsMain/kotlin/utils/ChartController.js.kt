package utils

import kotlinx.browser.document
import kotlinx.serialization.ExperimentalSerializationApi
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import viewmodel.Theme
import viewmodel.themeViewModel

external fun encodeURIComponent(str: String): String

/** OpenENC chart style for the site theme: the day palette in light mode, dusk in dark mode. */
fun chartStyleUrl(theme: Theme): String = when (theme) {
    Theme.Light -> "https://openenc.com/v1/style/feet/day"
    Theme.Dark -> "https://openenc.com/v1/style/feet/dusk"
}

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

    private val markPopupMap = mutableMapOf<Mark, MapLibre.Popup>()

    fun showPopup(mark: Mark) {
        markPopupMap[mark]?.let { popup ->
            mapView?.let { mv ->
                popup.addTo(mv)
            }
        }
    }

    fun hidePopup(mark: Mark) {
        markPopupMap[mark]?.remove()
    }

    fun addMarkPopup(mark: Mark, onSelect: (Mark?) -> Unit, onClose: (Mark) -> Unit) {
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
            popUp.on("close") {
                println("popup ${mark.letter} closed")
                onClose(mark)
            }

            markPopupMap[mark] = popUp
            val el = document.createElement("div") as HTMLElement
            el.innerHTML = "<span>${mark.letter}</span>"
            el.style.color = "red"
            el.style.fontSize = "40px"
            el.onclick = {
                onSelect(mark)
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
            mv.on("styleimagemissing") { event ->
                (event.id as? String)?.let { addSectorImage(mv, it) }
            }
        }
    }

    /**
     * Swaps the chart style in place; markers and popups are DOM elements and survive it. The
     * light sector images are drawn in the theme's palette under theme-independent ids, so they
     * are dropped first and `styleimagemissing` redraws them for the new theme.
     */
    fun setTheme(theme: Theme) {
        mapView?.let { mv ->
            // listImages throws "Style is not done loading" mid-load; there is nothing to drop then
            if (mv.isStyleLoaded()) {
                mv.listImages().filter { it.startsWith(SECTOR_IMAGE_PREFIX) }.forEach { mv.removeImage(it) }
            }
            mv.setStyle(chartStyleUrl(theme))
        }
    }

    /** Rasterizes a `LIGHTS_sector` icon (see [SectorImage]) and registers it under its id. */
    private fun addSectorImage(map: MapLibre.Map, id: String) {
        val sector = parseSectorImageId(id) ?: return
        val svg = sector.svg(themeViewModel.flow.value.theme)
        val img = js("new Image(200, 200)")
        img.onload = {
            if (!map.hasImage(id)) map.addImage(id, img)
        }
        img.src = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(svg)
    }

    fun disposeMapView() {
        markPopupMap.clear()
        mapView?.remove()
        mapView = null
    }

    private fun mapLibreArgs(
        container: HTMLDivElement,
    ): dynamic {
        val obj = js("{}")
        obj["style"] = chartStyleUrl(themeViewModel.flow.value.theme)
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
