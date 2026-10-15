package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Series
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

object SeriesTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 1024).uniqueIndex("nameIdx")
    val sort = integer("sort")
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)

    fun resultRowToSeries(row: ResultRow, raceCount: Long): Series {
        return Series(
            id = row[id],
            name = row[name],
            sort = row[sort],
            raceCount = raceCount,
            active = row[active]
        )
    }

    /** Hydrates [rows] with their race counts from one grouped query. */
    private fun toSeries(rows: List<ResultRow>): List<Series> {
        val counts = RaceTable.raceCountsBySeries(rows.map { it[id] })
        return rows.map { resultRowToSeries(it, counts[it[id]] ?: 0L) }
    }

    fun selectAllSeries(): List<Series> {
        return toSeries(selectAll().orderBy(sort).toList())
    }

    fun selectSeries(seriesId: Long): Series? = findSeries(listOf(seriesId))[seriesId]

    /** The series with [seriesIds], by id; two statements for any number of series. */
    fun findSeries(seriesIds: Collection<Long>): Map<Long, Series> {
        if (seriesIds.isEmpty()) return emptyMap()
        return toSeries(selectAll().where { id inList seriesIds }.toList()).associateBy { it.id }
    }

    fun deleteSeries(seriesId: Long): Int {
        return deleteWhere { id eq seriesId }
    }

    fun selectByName(name: String): List<Series> {
        return toSeries(selectAll().where { SeriesTable.name ilike LikePattern("%$name%") }.toList())
    }

    fun upsertSeries(seriesList: List<Series>): List<Series> {
        return seriesList.mapNotNull { series ->
            upsert {
                if (series.id > 0) {
                    it[id] = series.id
                }
                it[name] = series.name.trim()
                it[sort] = series.sort
                it[active] = series.active
            }.resultedValues?.singleOrNull()?.let { resultRowToSeries(it, RaceTable.raceCountForSeries(it[id])) }
        }
    }
}
