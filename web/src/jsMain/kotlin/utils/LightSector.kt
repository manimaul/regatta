package utils

import viewmodel.Theme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/**
 * Light sector images for the OpenENC chart style, ported from njord's `SvgLightSector.kt`
 * (mxmariner.com / openenc.com draw them the same way).
 *
 * The style's `LIGHTS_sector` layer uses the feature's `SI` property as its icon id, and the id
 * encodes everything needed to draw the sector:
 * `sector_<sectr1>_<sectr2>_<day>_<dusk>_<night>_<dayLine>_<duskLine>_<nightLine>_<radius>`.
 * MapLibre raises `styleimagemissing` for it, and [ChartController] answers with the rasterized
 * [sectorSvg] in the palette of the current theme.
 */
data class SectorImage(
    val sectr1: Double,
    val sectr2: Double,
    val dayColor: String,
    val duskColor: String,
    val nightColor: String,
    val dayLineColor: String,
    val duskLineColor: String,
    val nightLineColor: String,
    val radius: Double,
) {
    /** Arc and sector-line colors for the site theme: day in light mode, dusk in dark mode. */
    fun colors(theme: Theme): Pair<String, String> = when (theme) {
        Theme.Light -> dayColor to dayLineColor
        Theme.Dark -> duskColor to duskLineColor
    }

    fun svg(theme: Theme): String {
        val (fill, line) = colors(theme)
        return sectorSvg(sectr1, sectr2, fill, line, radius)
    }
}

const val SECTOR_IMAGE_PREFIX = "sector_"

/** Parses a `LIGHTS_sector` icon id, or null when it is not one. */
fun parseSectorImageId(id: String): SectorImage? {
    if (!id.startsWith(SECTOR_IMAGE_PREFIX)) return null
    val parts = id.removePrefix(SECTOR_IMAGE_PREFIX).split("_")
    if (parts.size < 9) return null
    return SectorImage(
        sectr1 = parts[0].toDoubleOrNull() ?: return null,
        sectr2 = parts[1].toDoubleOrNull() ?: return null,
        dayColor = parts[2],
        duskColor = parts[3],
        nightColor = parts[4],
        dayLineColor = parts[5],
        duskLineColor = parts[6],
        nightLineColor = parts[7],
        radius = parts[8].toDoubleOrNull() ?: 80.0,
    )
}

/**
 * A 200×200 SVG of the light's visible arc with dashed sector limit lines from the light at the
 * center. SECTR1/SECTR2 are bearings *from seaward toward the light*, so they are reversed to
 * draw from the light outward; equal bearings mean an all-round light, drawn as a full circle.
 */
fun sectorSvg(
    sectr1: Double,
    sectr2: Double,
    fillColor: String,
    lineColor: String,
    radius: Double = 80.0,
): String {
    val sector1 = (sectr1 + 180.0) % 360.0
    val sector2 = (sectr2 + 180.0) % 360.0
    val cx = 100.0
    val cy = 100.0

    val a1 = sector1 * PI / 180.0
    val a2 = sector2 * PI / 180.0

    val x1 = cx + radius * sin(a1)
    val y1 = cy - radius * cos(a1)
    val x2 = cx + radius * sin(a2)
    val y2 = cy - radius * cos(a2)

    val span = (sector2 - sector1 + 360.0) % 360.0
    val arcElement = if (span == 0.0) {
        """<circle cx="$cx" cy="$cy" r="${radius.fmt()}" fill="none" stroke="$fillColor" stroke-width="4"/>"""
    } else {
        val largeArc = if (span > 180.0) 1 else 0
        """<path d="M ${x1.fmt()} ${y1.fmt()} A ${radius.fmt()} ${radius.fmt()} 0 $largeArc 1 ${x2.fmt()} ${y2.fmt()}"
      fill="none" stroke="$fillColor" stroke-width="4" stroke-linecap="butt"/>"""
    }

    val sectorLinesElement = if (span == 0.0) {
        ""
    } else {
        """  <line x1="$cx" y1="$cy" x2="${x1.fmt()}" y2="${y1.fmt()}"
        stroke="$lineColor" stroke-width="2"
        stroke-dasharray="4 6" stroke-linecap="butt"/>
  <line x1="$cx" y1="$cy" x2="${x2.fmt()}" y2="${y2.fmt()}"
        stroke="$lineColor" stroke-width="2"
        stroke-dasharray="4 6" stroke-linecap="butt"/>"""
    }

    return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
  $arcElement
  $sectorLinesElement
</svg>"""
}

private fun Double.fmt(): String = round(this * 10).toLong().let { "${it / 10}.${abs(it % 10)}" }
