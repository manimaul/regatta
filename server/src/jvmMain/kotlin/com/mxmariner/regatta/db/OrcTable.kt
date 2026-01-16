package com.mxmariner.regatta.db

import OrcCertificate
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update


object OrcTable : Table() {
    val refNo = varchar("ref", 128)
    val boatId = (long("boat_id") references BoatTable.id).nullable()
    val cert = jsonb<OrcCertificate>("cert", Json)
    override val primaryKey = PrimaryKey(refNo)

    fun upsertCert(boat: Long, certificate: OrcCertificate) {
        deleteCert(certificate.refNo)
        insert {
            it[refNo] = certificate.refNo
            it[boatId] = boat
            it[cert] = certificate
        }
    }

    fun findCertificates(boat: Long): List<OrcCertificate> {
        return OrcTable.select { boatId eq boat }.map { ref ->
            ref[cert]
        }
    }

    fun unlinkCerts(boat: Long) {
        update(where = { boatId eq boat }) {
            it[boatId] = null
        }
    }

    fun deleteCert(ref: String): Int {
        return OrcTable.deleteWhere {
            refNo eq ref
        }
    }
}