package com.mxmariner.regatta

import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.OrcResponse
import com.mxmariner.regatta.data.RaceReportCard
import com.mxmariner.regatta.data.findPreferred
import com.mxmariner.regatta.data.orcCertificateUrl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrcTest {

    @Test
    fun testEncode() {
        val withUnknownKeys = Json { ignoreUnknownKeys = true }
        val content = this::class.java.getResourceAsStream("/waymaker.json")
        val response = withUnknownKeys.decodeFromStream(OrcResponse.serializer(),content)
        assertEquals(response.rms.first().refNo, "04560003WR9")
        assertNull(response.rms.first().dxtId, "the RMS data carries no file id")
    }

    @Test
    fun certificateUrlFromFileId() {
        assertEquals("https://data.orc.org/public/WPub.dll/CC/264182", OrcCertificate(refNo = "0456000518S", dxtId = 264182).certificateUrl())
        assertNull(OrcCertificate(refNo = "0456000518S").certificateUrl())
        assertNull(orcCertificateUrl(null))
        assertEquals("https://data.orc.org/public/WPub.dll/CC/7", RaceReportCard(orcDxtId = 7).orcCertUrl())
        assertNull(RaceReportCard().orcCertUrl())
    }

    /** The golden files are written with encodeDefaults = true and must not change for certificates without an id. */
    @Test
    fun fileIdIsNeverEncodedWhileNull() {
        val json = Json { encodeDefaults = true }
        assertFalse(json.encodeToString(OrcCertificate.serializer(), OrcCertificate(refNo = "001")).contains("dxtId"))
        assertTrue(json.encodeToString(OrcCertificate.serializer(), OrcCertificate(refNo = "001", dxtId = 264182)).contains("\"dxtId\":264182"))
        assertFalse(json.encodeToString(RaceReportCard.serializer(), RaceReportCard()).contains("orcDxtId"))
        assertTrue(json.encodeToString(RaceReportCard.serializer(), RaceReportCard(orcDxtId = 264182)).contains("\"orcDxtId\":264182"))
        // a stored certificate without the key decodes to null; one with it keeps the id
        assertNull(Json.decodeFromString(OrcCertificate.serializer(), "{\"RefNo\":\"001\"}").dxtId)
        assertEquals(264182, Json.decodeFromString(OrcCertificate.serializer(), "{\"RefNo\":\"001\",\"dxtId\":264182}").dxtId)
    }

    @Test
    fun testPref() {
        val certs = listOf(
            OrcCertificate(
                refNo = "001",
                cType = "NSCL"
            ),
            OrcCertificate(
                refNo = "002",
                cType = "CLUB"
            ),
            OrcCertificate(
                refNo = "003",
                cType = "DHCL"
            ),
        )
        assertEquals("002", certs.findPreferred()?.refNo)
    }
}