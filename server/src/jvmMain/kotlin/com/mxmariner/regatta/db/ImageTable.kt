package com.mxmariner.regatta.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

object ImageTable : Table() {
    val fileName = varchar("hash", 128)
    val raceId = (long("race_id") references RaceTable.id)
    val data = binary("data")
    override val primaryKey = PrimaryKey(fileName)

    init {
        uniqueIndex("image_race_id_unique", raceId)
    }

    fun getImage(name: String): ByteArray? {
        return ImageTable.selectAll().where { fileName eq name }.singleOrNull()?.let { row ->
            return row[data]
        }
    }

    /** The report image name of race [id], without reading the image bytes. */
    fun getRaceReportImageName(id: Long): String? = reportImageNames(listOf(id))[id]

    /** The report image name of each race in [raceIds] that has exactly one, in one query that skips the bytes. */
    fun reportImageNames(raceIds: Collection<Long>): Map<Long, String> {
        if (raceIds.isEmpty()) return emptyMap()
        return select(fileName, raceId).where { raceId inList raceIds }
            .map { it[raceId] to it[fileName] }
            .groupBy({ it.first }, { it.second })
            .mapNotNull { (race, names) -> names.singleOrNull()?.let { race to it } }
            .toMap()
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
