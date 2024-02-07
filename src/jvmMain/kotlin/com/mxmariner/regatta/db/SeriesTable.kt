package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.Series
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object SeriesTable : Table() {
    val id = long("id").autoIncrement()
    val name = varchar("name", 1024).uniqueIndex("nameIdx")
    val active = bool("active")
    override val primaryKey = PrimaryKey(id)

    fun resultRowToSeries(row: ResultRow) = Series(
        id = row[id], name = row[name], active = row[active]
    )

    fun selectAllSeries(): List<Series> {
        return selectAll().map(::resultRowToSeries)
    }

    fun selectSeries(seriesId: Long): Series? {
        return select { id eq seriesId }.map(::resultRowToSeries).singleOrNull()
    }

    fun deleteSeries(seriesId: Long): Int {
        return deleteWhere { id eq seriesId }
    }

    fun selectByName(name: String): List<Series> {
        return select { SeriesTable.name ilike LikePattern("%$name%") }.map(::resultRowToSeries)
    }

    fun upsertSeries(series: Series): Series? {
        return upsert {
            it[name] = series.name.trim()
            it[active] = series.active
        }.resultedValues?.singleOrNull()?.let(::resultRowToSeries)
    }
}