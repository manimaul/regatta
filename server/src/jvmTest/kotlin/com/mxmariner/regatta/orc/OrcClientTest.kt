package com.mxmariner.regatta.orc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Parsing of ORC's answers; the fixtures are copies of real responses from data.orc.org (2026-10-15). */
class OrcClientTest {

    private val activeRow = this::class.java.getResourceAsStream("/orc/activecerts_0456000518S.xml")!!
        .bufferedReader().readText()

    private val noRows = """<?xml version="1.0" encoding="UTF-8"?>
        <?xml-stylesheet type="text/xsl" href="activecerts.xsl" ?>
        <ROOT><script xmlns="http://www.w3.org/1999/xhtml" src="/xslt-polyfill.min.js"/>
          <DATA/>
          <ERRORS/>
        </ROOT>"""

    @Test
    fun fileIdOfTheRowWithTheReferenceNumber() {
        assertEquals(264182, OrcClient.parseDxtId(activeRow, "0456000518S"))
        assertEquals(264182, OrcClient.parseDxtId(activeRow, "0456000518s"), "reference numbers compare case-insensitively")
    }

    @Test
    fun noRowMeansNoFileId() {
        assertNull(OrcClient.parseDxtId(noRows, "04560003WR9"), "an expired certificate is not on the active list")
        assertNull(OrcClient.parseDxtId(activeRow, "04560003WR9"), "a row for another certificate does not count")
    }

    @Test
    fun rmsDocumentWithByteOrderMarkAndUnknownKeys() {
        val body = "﻿{\"rms\":[{\"NatAuth\":\"USA\",\"RefNo\":\"0456000518S\",\"YachtName\":\"FLICKA\",\"Unknown\":1}],\"Countries\":[]}"
        val cert = OrcClient.parseCertificate(body)!!
        assertEquals("0456000518S", cert.refNo)
        assertEquals("USA", cert.natAuth)
        assertNull(cert.dxtId)
        assertNull(OrcClient.parseCertificate("{\"rms\":[]}"))
    }
}
