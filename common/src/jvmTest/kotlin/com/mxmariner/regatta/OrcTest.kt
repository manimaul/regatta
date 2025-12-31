package com.mxmariner.regatta

import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.OrcResponse
import com.mxmariner.regatta.data.findPreferred
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlin.test.Test
import kotlin.test.assertEquals

class OrcTest {

    @Test
    fun testEncode() {
        val withUnknownKeys = Json { ignoreUnknownKeys = true }
        val content = this::class.java.getResourceAsStream("/waymaker.json")
        val response = withUnknownKeys.decodeFromStream(OrcResponse.serializer(),content)
        assertEquals(response.rms.first().refNo, "04560003WR9")
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