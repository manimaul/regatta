package com.mxmariner.regatta.db

import com.mxmariner.regatta.correctionFactorDefault
import com.mxmariner.regatta.data.RaceResultFull
import com.mxmariner.regatta.data.RaceTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RaceTimeTable: Table() {
    val raceClassCategory = (long("class_cat_id") references RaceClassCategoryTable.id)
    val raceId = (long("race_id") references RaceTable.id)
    val startDate = timestamp("start_date")
    val endDate = timestamp("end_date")
    val correctionFactor = integer("correction_factor").nullable()

    fun allYears(): List<String>  {
        val years = mutableSetOf<Int>()
        RaceTimeTable.selectAll().map {row ->
            val startDate = row[startDate]
            val endDate = row[endDate]
            startDate.toString().substring(0, 4).toIntOrNull()?.let {
                years.add(it)
            }
            endDate.toString().substring(0, 4).toIntOrNull()?.let {
                years.add(it)
            }
        }
        return years.sortedDescending().map { "$it" }
    }

    fun updateRaceTimes(rId: Long, times: List<RaceTime>)   {
        RaceTimeTable.deleteWhere { raceId eq rId }
        times.forEach { time ->
            RaceTimeTable.insert {
                it[raceId] = rId
                it[raceClassCategory] = time.raceClassCategory.id!!
                it[startDate] = time.startDate
                it[endDate] = time.endDate
                it[correctionFactor] = time.correctionFactor
            }
        }
    }

    fun selectByRaceId(raceId: Long) : List<RaceTime> {
        return select { RaceTimeTable.raceId eq RaceTimeTable.raceId }.map {
            RaceTime(
                raceClassCategory = RaceClassCategoryTable.selectById(it[raceClassCategory])!!,
                startDate = it[startDate],
                endDate = it[endDate],
                correctionFactor = it[correctionFactor] ?: correctionFactorDefault,
                brackets = RaceBracketJunction.selectBrackets(raceId)
            )
        }
    }
}
