package com.mxmariner.regatta.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

object ImageTable : Table() {
    val fileName = varchar("hash", 128)
    val raceId = (long("race_id") references RaceTable.id)
    val data = binary("data")
    override val primaryKey = PrimaryKey(fileName)

    fun getImage(name: String): ByteArray? {
        return ImageTable.selectAll().where { fileName eq name }.singleOrNull()?.let { row ->
            return row[data]
        }
    }

    fun getRaceReportImageName(id: Long): String? {
        return ImageTable.selectAll().where { raceId.eq(id) }.singleOrNull()?.let { row ->
            return row[fileName]
        }
    }

    fun saveRaceReportImage(id: Long, name: String, image: ByteArray) {
        ImageTable.deleteWhere { raceId.eq(id) }
        ImageTable.insert {
            it[raceId] = id
            it[fileName] = name
            it[data] = image
        }
    }
}
