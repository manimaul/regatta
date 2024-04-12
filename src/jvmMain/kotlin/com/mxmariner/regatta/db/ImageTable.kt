package com.mxmariner.regatta.db

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

object ImageTable : Table() {
    val fileName = varchar("hash", 128)
    val raceId = (long("race_id") references RaceTable.id)
    val data = binary("data")
    override val primaryKey = PrimaryKey(fileName)

    fun getImage(name: String): ByteArray? {
        return ImageTable.select { fileName.eq(name) }.singleOrNull()?.let { row ->
            return row[data]
        }
    }

    fun getRaceReportImageName(id: Long): String? {
        return ImageTable.select { raceId.eq(id) }.singleOrNull()?.let { row ->
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
