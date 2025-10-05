@file:OptIn(ExperimentalJsExport::class)

package utils

import io.madrona.njord.geojson.GeoJsonObject
import org.w3c.dom.HTMLElement
import kotlin.js.Promise

@JsModule("maplibre-gl")
@JsName("maplibre")
@JsNonModule
external class MapLibre {

    class LngLat(
        val lng: Double,
        val lat: Double,
    )

    class LngLatBounds {
        fun getCenter(): LngLat
        fun getNorth(): Double
        fun getSouth(): Double
        fun getWest(): Double
        fun getEast(): Double
        fun getSouthWest(): LngLat
        fun getSouthEast(): LngLat
        fun getNorthEast(): LngLat
        fun getNorthWest(): LngLat
    }

    interface PointLike {
        val x: Number
        val y: Number
    }

    // https://github.com/maplibre/maplibre-gl-js/blob/main/src/ui/marker.ts
    // https://maplibre.org/maplibre-gl-js/docs/API/classes/Marker/
    class Marker {
        constructor()
        constructor(options: MarkerOptions)
        fun addTo(map: Map): Marker
        fun remove(): Marker
        fun getLngLat(): LngLat
        fun setLngLat(lngLat: LngLat) : Marker
        fun setPopup(popup: Popup) : Marker
    }


    // https://github.com/maplibre/maplibre-gl-js/blob/main/src/ui/popup.ts
    // https://maplibre.org/maplibre-gl-js/docs/API/classes/Popup/
    class Popup{
        constructor()
        constructor(options: PopupOptions)
        fun addTo(map: Map): Popup
        fun isOpen(): Boolean
        fun remove(): Popup
        fun getLngLat(): LngLat
        fun setLngLat(lngLat: LngLat): Popup
        fun trackPointer(): Popup
        fun getElement() : HTMLElement
        fun setText(text: String): Popup
        fun setHTML(html: String): Popup
    }

    // https://maplibre.org/maplibre-gl-js/docs/API/classes/Map/
    class Map(args: dynamic) {
        fun on(type: dynamic, listener: (dynamic) -> dynamic)
        fun queryRenderedFeatures(box: Array<Array<Int>>): dynamic
        fun setStyle(url: String)
        fun remove()
        fun jumpTo(options: dynamic)
        fun fitBounds(bounds: Array<Array<Double>>)
        fun getBounds() : LngLatBounds
        fun addSource(id: String, source: dynamic)
        fun project(lngLat: Array<Double>): dynamic
        fun unproject(point: dynamic): LngLat
        fun loadImage(url: String) : Promise<GetResourceResponse>
        fun addImage(id: String, image: dynamic)
        fun addSource(id: String, source: Source)
        fun addLayer(options: dynamic)
    }
}

@JsExport
class GetResourceResponse(
    val data: dynamic
)

@JsExport
class Source(
    val type: String = "geojson",
    val data: GeoJsonObject,
)
@JsExport
class Layer(
    val id: String,
    val type: String,
    val source: String,
    val layout: Layout,
)

@JsExport
class Layout(
    @JsName("icon-image")
    val iconImage: String,
    @JsName("icon-overlap")
    val iconOverlap: String,
    @JsName("text-overlap")
    val textOverlap: String,
)

@JsExport
class MapStyleImageMissingEvent(
    val id: String,
)


@JsExport
class PopupOptions(
    /**
     * If `true`, a close button will appear in the top right corner of the popup.
     * @defaultValue true
     */
    val closeButton: Boolean? = null,
    /**
     * If `true`, the popup will closed when the map is clicked.
     * @defaultValue true
     */
    val closeOnClick: Boolean? = null,
    /**
     * If `true`, the popup will closed when the map moves.
     * @defaultValue false
     */
    val closeOnMove: Boolean? = null,
    /**
     * If `true`, the popup will try to focus the first focusable element inside the popup.
     * @defaultValue true
     */
    val focusAfterOpen: Boolean? = null,
    /**
     * A string indicating the part of the Popup that should
     * be positioned closest to the coordinate set via {@link Popup.setLngLat}.
     * Options are `'center'`, `'top'`, `'bottom'`, `'left'`, `'right'`, `'top-left'`,
     * `'top-right'`, `'bottom-left'`, and `'bottom-right'`. If unset the anchor will be
     * dynamically set to ensure the popup falls within the map container with a preference
     * for `'bottom'`.
     */
    //export type PositionAnchor = 'center' | 'top' | 'bottom' | 'left' | 'right' | 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
    val anchor: String? = null,

    /**
     * A pixel offset applied to the popup's location
     */
    val offset: Number? = null,

    /**
     * Space-separated CSS class names to add to popup container
     */
    className: String? = null,
    /**
     * A string that sets the CSS property of the popup's maximum width, eg `'300px'`.
     * To ensure the popup resizes to fit its content, set this property to `'none'`.
     * Available values can be found here: https://developer.mozilla.org/en-US/docs/Web/CSS/max-width
     * @defaultValue '240px'
     */
    maxWidth: String? = null,
    /**
     * If `true`, rounding is disabled for placement of the popup, allowing for
     * subpixel positioning and smoother movement when the popup is translated.
     * @defaultValue false
     */
    val subpixelPositioning: Boolean? = null,    /**
     * Optional opacity when the location is behind the globe.
     * Note that if a number is provided, it will be converted to a string.
     * @defaultValue undefined
     */
    locationOccludedOpacity: String? = null,
)

@JsExport
class MarkerOptions(
    /**
     * DOM element to use as a marker. The default is a light blue, droplet-shaped SVG marker.
     */
    val element: HTMLElement? = null,
    /**
     * Space-separated CSS class names to add to marker element.
     */
    val className: String? = null,
    /**
     * The offset in pixels as a PointLike object to apply relative to the element's center. Negatives indicate left and up.
     */
    val offset: PointLike? = null,
    /**
     * A string indicating the part of the Marker that should be positioned closest to the coordinate set via Marker.setLngLat.
     * Options are 'center', 'top', 'bottom', 'left', 'right', 'top-left', 'top-right', 'bottom-left', and 'bottom-right'.
     * @defaultValue 'center'
     */
    val anchor: PositionAnchor? = null,
    /**
     * The color to use for the default marker if options.element is not provided. The default is light blue.
     * @defaultValue '#3FB1CE'
     */
    val color: String? = null,
    /**
     * The scale to use for the default marker if options.element is not provided. The default scale corresponds to a height of `41px` and a width of `27px`.
     * @defaultValue 1
     */
    val scale: Float? = null,
    /**
     * A boolean indicating whether or not a marker is able to be dragged to a new position on the map.
     * @defaultValue false
     */
    val draggable: Boolean? = null,
    /**
     * The max number of pixels a user can shift the mouse pointer during a click on the marker for it to be considered a valid click (as opposed to a marker drag). The default is to inherit map's clickTolerance.
     * @defaultValue 0
     */
    val clickTolerance: Int? = null,
    /**
     * The rotation angle of the marker in degrees, relative to its respective `rotationAlignment` setting. A positive value will rotate the marker clockwise.
     * @defaultValue 0
     */
    val rotation: Int? = null,
    /**
     * `map` aligns the `Marker`'s rotation relative to the map, maintaining a bearing as the map rotates. `viewport` aligns the `Marker`'s rotation relative to the viewport, agnostic to map rotations. `auto` is equivalent to `viewport`.
     * @defaultValue 'auto'
     */
    val rotationAlignment: Alignment? = null,
    /**
     * `map` aligns the `Marker` to the plane of the map. `viewport` aligns the `Marker` to the plane of the viewport. `auto` automatically matches the value of `rotationAlignment`.
     * @defaultValue 'auto'
     */
    val pitchAlignment: Alignment? = null,
    /**
     * Marker's opacity when it's in clear view (not behind 3d terrain)
     * @defaultValue 1
     */
    val opacity: String? = null,
    /**
     * Marker's opacity when it's behind 3d terrain
     * @defaultValue 0.2
     */
    val opacityWhenCovered: String? = null,
    /**
     * If `true`, rounding is disabled for placement of the marker, allowing for
     * subpixel positioning and smoother movement when the marker is translated.
     * @defaultValue false
     */
    val subpixelPositioning: Boolean? = null,
)

// Example of the PointLike type (for context)
// A common representation in Kotlin/JS for JavaScript objects.
external interface PointLike {
    val x: Number
    val y: Number
}

// Example of the PositionAnchor type (for context)
// This can be represented by a simple typealias for a string.
typealias PositionAnchor = String

// Example of the Alignment type (for context)
// This can also be a simple typealias for a string.
typealias Alignment = String
