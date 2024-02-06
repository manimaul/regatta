package com.mxmariner.regatta.db

import org.jetbrains.exposed.sql.Table

object SkipperBoatJunction : Table() {
    val skipper = (long("skipper_id") references PersonTable.id)
    val boat = (long("boat_id") references BoatTable.id)
}
