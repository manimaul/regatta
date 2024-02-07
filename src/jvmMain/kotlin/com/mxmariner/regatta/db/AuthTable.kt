package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.AuthRecord
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object AuthTable : Table() {
    val id = long("id").autoIncrement()

    //hmac sha512 hash of user's password - hashed client side
    val hash = varchar("hash", 128)
    val userName = varchar("user_name", 128).uniqueIndex("user_name_idx")
    override val primaryKey = PrimaryKey(id)

    fun getAuth(name: String): AuthRecord? {
        return AuthTable.select { userName eq name }.singleOrNull()?.let(::resultRowToAuth)
    }

    fun getAuth(authId: Long): AuthRecord? {
        return AuthTable.select { id eq authId }.singleOrNull()?.let(::resultRowToAuth)
    }

    fun saveAuth(record: AuthRecord): AuthRecord? {
        return upsert {
            it[hash] = record.hash
            it[userName] = record.userName
        }.resultedValues?.singleOrNull()?.let(::resultRowToAuth)
    }

    private fun resultRowToAuth(row: ResultRow): AuthRecord {
        return AuthRecord(
            id = row[id],
            hash = row[hash],
            userName = row[userName],
        )
    }
}
