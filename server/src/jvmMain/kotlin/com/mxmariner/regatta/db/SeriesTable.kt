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

    fun resultRowToSeries(row: ResultRow): Series {
        val seriesId = row[id]
        val raceCount = RaceTable.raceCountForSeries(seriesId)
        return Series(
            id = seriesId,
            name = row[name],
            sort = row[sort],
            raceCount = raceCount,
            active = row[active]
        )
    }

    fun selectAllSeries(): List<Series> {
        return selectAll().orderBy(sort).map(::resultRowToSeries)
    }

    fun selectSeries(seriesId: Long): Series? {
        return selectAll().where { id eq seriesId }.map(::resultRowToSeries).singleOrNull()
    }

    fun deleteSeries(seriesId: Long): Int {
        return deleteWhere { id eq seriesId }
    }

    fun selectByName(name: String): List<Series> {
        return selectAll().where { SeriesTable.name ilike LikePattern("%$name%") }.map(::resultRowToSeries)
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
            }.resultedValues?.singleOrNull()?.let(::resultRowToSeries)
        }
    }
}
