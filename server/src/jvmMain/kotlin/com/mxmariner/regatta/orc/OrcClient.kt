package com.mxmariner.regatta.orc

import com.mxmariner.regatta.data.OrcCertificate
import com.mxmariner.regatta.data.OrcResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * Lookups against ORC's public data site (data.orc.org). The site sends no CORS headers, so the browser
 * cannot call it; the server does, on behalf of the admin adding a certificate.
 *
 * Two requests make a certificate: the RMS data (`DownBoatRMS`, the rating fields) and the row of the
 * active-certificate list (`activecerts` filtered by country and reference number), which carries the
 * file id `dxtID` that keys the certificate's public page ([OrcCertificate.certificateUrl]). The list
 * only has active certificates, so the id must be captured while the certificate is current.
 */
object OrcClient {

    private const val BASE = "https://data.orc.org/public/WPub.dll"
    private val log = LoggerFactory.getLogger(OrcClient::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val http: HttpClient by lazy {
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build()
    }

    /** The certificate [refNo] with its file id when ORC still lists it as active, or null when ORC has no RMS data for it. */
    suspend fun lookup(refNo: String): OrcCertificate? {
        val cert = fetchCertificate(refNo) ?: return null
        val dxtId = runCatching { fetchDxtId(cert.natAuth, refNo) }
            .onFailure { log.warn("ORC file id lookup failed for $refNo", it) }
            .getOrNull()
        if (dxtId == null) log.info("ORC certificate $refNo has no file id (not on the active list)")
        return cert.copy(dxtId = dxtId)
    }

    suspend fun fetchCertificate(refNo: String): OrcCertificate? {
        val body = get("$BASE?action=DownBoatRMS&RefNo=${encode(refNo)}&ext=json", "application/json")
        return parseCertificate(body)
    }

    suspend fun fetchDxtId(countryId: String, refNo: String): Long? {
        val body = get("$BASE?action=activecerts&CountryId=${encode(countryId)}&RefNo=${encode(refNo)}", "text/xml")
        return parseDxtId(body, refNo)
    }

    /** First certificate of an RMS JSON document; a leading UTF-8 BOM (the country feeds have one) is ignored. */
    internal fun parseCertificate(body: String): OrcCertificate? =
        json.decodeFromString(OrcResponse.serializer(), body.removePrefix("﻿")).rms.firstOrNull()

    /** The `dxtID` of the `ROW` whose `RefNo` is [refNo] in an `activecerts` XML document, or null when there is none. */
    internal fun parseDxtId(xml: String, refNo: String): Long? {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml.removePrefix("﻿"))))
        val rows = document.getElementsByTagName("ROW")
        return (0 until rows.length).asSequence()
            .map { rows.item(it) as Element }
            .firstOrNull { it.text("RefNo").equals(refNo, ignoreCase = true) }
            ?.text("dxtID")
            ?.toLongOrNull()
    }

    private fun Element.text(tag: String): String? =
        getElementsByTagName(tag).item(0)?.textContent?.trim()

    private suspend fun get(url: String, accept: String): String = withContext(Dispatchers.IO) {
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", accept)
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) { "ORC answered ${response.statusCode()} for $url" }
        response.body()
    }

    private fun encode(value: String): String = URLEncoder.encode(value.trim(), Charsets.UTF_8)
}
