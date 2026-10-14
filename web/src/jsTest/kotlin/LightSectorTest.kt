import utils.parseSectorImageId
import utils.sectorSvg
import viewmodel.Theme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Light sector icon ids and SVGs (utils/LightSector.kt), as the OpenENC style's `LIGHTS_sector` layer uses them. */
class LightSectorTest {

    private val id = "sector_90.0_270.0_#FF0000_#B30000_#500000_#000000_#8CA6B3_#555555_68.0"

    @Test
    fun parses_the_style_icon_id_and_picks_theme_colors() {
        val img = parseSectorImageId(id)!!
        assertEquals(90.0, img.sectr1)
        assertEquals(270.0, img.sectr2)
        assertEquals(68.0, img.radius)
        assertEquals("#FF0000" to "#000000", img.colors(Theme.Light))
        assertEquals("#B30000" to "#8CA6B3", img.colors(Theme.Dark))
    }

    @Test
    fun rejects_ids_that_are_not_sectors() {
        assertNull(parseSectorImageId("LIGHTS11"))
        assertNull(parseSectorImageId("sector_1_2_3"))
        assertNull(parseSectorImageId("sector_x_270.0_a_b_c_d_e_f_80"))
    }

    @Test
    fun radius_defaults_to_80_when_unparseable() {
        assertEquals(80.0, parseSectorImageId("sector_0.0_0.0_a_b_c_d_e_f_nope")!!.radius)
    }

    @Test
    fun equal_bearings_draw_an_all_round_light_without_sector_lines() {
        val svg = sectorSvg(0.0, 0.0, "#FFD700", "#000000", 80.0)
        assertTrue(svg.contains("""<circle cx="100" cy="100" r="80.0""""), svg)
        assertTrue(svg.contains("""stroke="#FFD700""""), svg)
        assertTrue(!svg.contains("<line"), svg)
    }

    @Test
    fun bearings_are_reversed_to_draw_from_the_light_outward() {
        // SECTR1 90 (seen from the east) starts the arc west of the light, SECTR2 270 ends it east
        val svg = sectorSvg(90.0, 270.0, "#FF0000", "#000000", 68.0)
        assertTrue(svg.contains("""<path d="M 32.0 100.0 A 68.0 68.0 0 0 1 168.0 100.0""""), svg)
        assertTrue(svg.contains("""x2="32.0" y2="100.0""""), svg)
        assertTrue(svg.contains("""x2="168.0" y2="100.0""""), svg)
    }

    @Test
    fun spans_over_180_degrees_use_the_large_arc_flag() {
        // SECTR1 0 / SECTR2 270 reverses to 180 -> 90 clockwise, a 270 degree arc
        val svg = sectorSvg(0.0, 270.0, "#FF0000", "#000000", 80.0)
        assertTrue(svg.contains(" 0 1 1 "), svg)
        assertTrue(!sectorSvg(0.0, 90.0, "#FF0000", "#000000", 80.0).contains(" 0 1 1 "))
    }
}
