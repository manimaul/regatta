package com.mxmariner.regatta.db

import org.jetbrains.exposed.sql.Table

object OrcTable : Table() {
    val id = long("id").autoIncrement()
    val url = varchar("url", 2048)
    val windwardLeewardTot = double("windwardLeewardTot")
    override val primaryKey = PrimaryKey(id)
}