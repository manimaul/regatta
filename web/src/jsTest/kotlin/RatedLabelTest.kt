import com.mxmariner.regatta.data.RatingType
import components.RatedLabel
import org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi
import org.jetbrains.compose.web.testutils.runTest
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The rating cell: the ORC reference number links to the certificate's public page in a new tab when its id is known. */
@OptIn(ComposeWebExperimentalTestsApi::class)
class RatedLabelTest {

    private val url = "https://data.orc.org/public/WPub.dll/CC/264182"

    private fun HTMLElement.anchor(): HTMLAnchorElement? = querySelector("a") as? HTMLAnchorElement

    @Test
    fun orcReferenceLinksToTheCertificate() = runTest {
        composition { RatedLabel(RatingType.ORC, null, "0456000518S", url) }
        assertEquals(RatingType.ORC.ratedLabel(null, "0456000518S"), root.textContent, "the text is the plain label")
        val a = root.anchor()!!
        assertEquals("0456000518S", a.textContent)
        assertEquals(url, a.getAttribute("href"))
        assertEquals("_blank", a.getAttribute("target"))
        assertEquals("noopener noreferrer", a.getAttribute("rel"))
    }

    @Test
    fun orcPhrfKeepsThePhrfPartAsText() = runTest {
        composition { RatedLabel(RatingType.ORC_PHRF, 48, "0456000518S", url) }
        assertEquals("PHRF (48), ORC (0456000518S)", root.textContent)
        assertEquals("0456000518S", root.anchor()!!.textContent)
    }

    @Test
    fun noUrlMeansPlainText() = runTest {
        composition { RatedLabel(RatingType.ORC_PHRF, 48, "04560003WR9", null) }
        assertEquals("PHRF (48), ORC (04560003WR9)", root.textContent)
        assertNull(root.anchor(), "an unresolved (e.g. expired) certificate is not linked")
    }

    @Test
    fun phrfAndCruisingAreUnchanged() = runTest {
        composition { RatedLabel(RatingType.PHRF, 48, null, null) }
        assertEquals("PHRF (48)", root.textContent)
        assertNull(root.anchor())
    }
}
