package com.mxmariner.regatta.db

import org.jetbrains.exposed.sql.*

//https://www.postgresql.org/docs/current/functions-matching.html
infix fun <T : String?> Expression<T>.ilike(pattern: LikePattern): ILikeEscapeOp =
    ILikeEscapeOp(this, stringParam(pattern.pattern),  pattern.escapeChar)

class ILikeEscapeOp(expr1: Expression<*>, expr2: Expression<*>, val escapeChar: Char?) :
    ComparisonOp(expr1, expr2, "ILIKE") {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        super.toQueryBuilder(queryBuilder)
        if (escapeChar != null) {
            with(queryBuilder) {
                + " ESCAPE "
                + stringParam(escapeChar.toString())
            }
        }
    }
}
