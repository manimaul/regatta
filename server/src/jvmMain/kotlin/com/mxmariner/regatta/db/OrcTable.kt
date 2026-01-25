package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.OrcCertificate
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.json.jsonb


object OrcTable : Table() {
    val refNo = varchar("orc_ref", 128)
    val boatId = (long("boat_id") references BoatTable.id).nullable()
    val cert = jsonb<OrcCertificate>("cert", Json)
    override val primaryKey = PrimaryKey(refNo)

    fun upsertCert(boat: Long, certificate: OrcCertificate) {
        if (validate(certificate)) {
            upsert(refNo) {
                it[refNo] = certificate.refNo
                it[boatId] = boat
                it[cert] = certificate
            }
        } else {
            throw IllegalArgumentException("invalid certificate properties")
        }
    }

    fun findCertificate(ref: String?): OrcCertificate? {
        return ref?.let {
            OrcTable.selectAll().where { refNo eq ref }.map { ref ->
                ref[cert]
            }.singleOrNull()
        }
    }

    fun findCertificates(boat: Long): List<OrcCertificate> {
        return OrcTable.selectAll().where { boatId eq boat }.map { ref ->
            ref[cert]
        }
    }

    fun unlinkCerts(boat: Long) {
        update(where = { boatId eq boat }) {
            it[boatId] = null
        }
    }

    private fun validate(cert: OrcCertificate) : Boolean {
        return cert.allPurposeTod != 0.0 &&
                cert.allPurposeTot != 0.0 &&
                cert.wlSingleNumberTod != 0.0 &&
                cert.wlSingleNumberTot != 0.0 &&
                cert.tripleNumberAllPurposeLowTod != 0.0 &&
                cert.tripleNumberAllPurposeLowTot != 0.0 &&
                cert.tripleNumberAllPurposeMedTod != 0.0 &&
                cert.tripleNumberAllPurposeMedTot != 0.0 &&
                cert.tripleNumberAllPurposeHiTod != 0.0 &&
                cert.tripleNumberAllPurposeHiTot != 0.0 &&
                cert.tripleNumberWlLowTot != 0.0 &&
                cert.tripleNumberWlMedTot != 0.0 &&
                cert.tripleNumberWlHiTot != 0.0 &&
                cert.singleNumberPredominantUpwindTot != 0.0 &&
                cert.singleNumberPredominantUpwindTod != 0.0 &&
                cert.singleNumberPredominantReachingTot != 0.0 &&
                cert.singleNumberPredominantReachingTod != 0.0 &&
                cert.singleNumberPredominantDownwindTot != 0.0 &&
                cert.singleNumberPredominantDownwindTod != 0.0 &&
                cert.predominantUpwindLowTot != 0.0 &&
                cert.predominantUpwindLowTod != 0.0 &&
                cert.predominantUpwindLowMedTot != 0.0 &&
                cert.predominantUpwindLowMedTod != 0.0 &&
                cert.predominantUpwindMedTot != 0.0 &&
                cert.predominantUpwindMedTod != 0.0 &&
                cert.predominantUpwindMedHiTot != 0.0 &&
                cert.predominantUpwindMedHiTod != 0.0 &&
                cert.predominantUpwindHiTot != 0.0 &&
                cert.predominantUpwindHiTod != 0.0 &&
                cert.predominantDownwindLowTot != 0.0 &&
                cert.predominantDownwindLowTod != 0.0 &&
                cert.predominantDownwindLowMedTot != 0.0 &&
                cert.predominantDownwindLowMedTod != 0.0 &&
                cert.predominantDownwindMedTot != 0.0 &&
                cert.predominantDownwindMedTod != 0.0 &&
                cert.predominantDownwindMedHiTot != 0.0 &&
                cert.predominantDownwindMedHiTod != 0.0 &&
                cert.predominantDownwindHiTot != 0.0 &&
                cert.predominantDownwindHiTod != 0.0 &&
                cert.predominantReachingLowTot != 0.0 &&
                cert.predominantReachingLowTod != 0.0 &&
                cert.predominantReachingLowMedTot != 0.0 &&
                cert.predominantReachingLowMedTod != 0.0 &&
                cert.predominantReachingMedTot != 0.0 &&
                cert.predominantReachingMedTod != 0.0 &&
                cert.predominantReachingMedHiTot != 0.0 &&
                cert.predominantReachingMedHiTod != 0.0 &&
                cert.predominantReachingHiTot != 0.0 &&
                cert.predominantReachingHiTod != 0.0 &&
                cert.fiveBandWlLowTot != 0.0 &&
                cert.fiveBandWlLowTod != 0.0 &&
                cert.fiveBandWlLowMedTot != 0.0 &&
                cert.fiveBandWlLowMedTod != 0.0 &&
                cert.fiveBandWlMedTot != 0.0 &&
                cert.fiveBandWlMedTod != 0.0 &&
                cert.fiveBandWlMedHiTot != 0.0 &&
                cert.fiveBandWlMedHiTod != 0.0 &&
                cert.fiveBandWlHiTot != 0.0 &&
                cert.fiveBandWlHiTod != 0.0 &&
                cert.usWl6040LTod != 0.0 &&
                cert.usWl6040LTot != 0.0 &&
                cert.usWl6040LmTod != 0.0 &&
                cert.usWl6040LmTot != 0.0 &&
                cert.usWl6040MTod != 0.0 &&
                cert.usWl6040MTot != 0.0 &&
                cert.usWl6040MhTod != 0.0 &&
                cert.usWl6040MhTot != 0.0 &&
                cert.usWl6040HTod != 0.0 &&
                cert.usWl6040HTot != 0.0 &&
                cert.fiveBandAllPurposeLowTot != 0.0 &&
                cert.fiveBandAllPurposeLowTod != 0.0 &&
                cert.fiveBandAllPurposeLowMedTot != 0.0 &&
                cert.fiveBandAllPurposeLowMedTod != 0.0 &&
                cert.fiveBandAllPurposeMedTot != 0.0 &&
                cert.fiveBandAllPurposeMedTod != 0.0 &&
                cert.fiveBandAllPurposeMedHiTot != 0.0 &&
                cert.fiveBandAllPurposeMedHiTod != 0.0 &&
                cert.fiveBandAllPurposeHiTot != 0.0 &&
                cert.fiveBandAllPurposeHiTod != 0.0
    }
}