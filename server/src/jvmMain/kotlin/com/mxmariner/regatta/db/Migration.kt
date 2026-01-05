package com.mxmariner.regatta.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

private const val sqlDbMetaDataSetup = """
CREATE TABLE IF NOT EXISTS metadata (
    name varchar(128) PRIMARY KEY,
    value varchar(128) NOT NULL
);
INSERT INTO metadata (name, value) 
VALUES ('version', '0')
ON CONFLICT (name) DO NOTHING;
"""

private fun dbVersionSetup(database: Database) {
    return transaction(database) { exec(sqlDbMetaDataSetup) }
}

private fun dbVersion(database: Database): Int {
    return transaction(database) {
        requireNotNull(exec("SELECT cast(value as INT) from metadata where name='version';") { rs ->
            require(rs.next()) { "metadata version next was not present"}
            rs.getInt(1)
        }) { "metadata version was null" }
    }
}

private fun updateVersion(database: Database, newVersion: Int) {

    transaction(database) {
        exec("UPDATE metadata SET value = '$newVersion' WHERE name = 'version'; ")
    }
    require(dbVersion(database) == newVersion) { }
}

private fun dbVersion0To1Upgrade(database: Database) {
    transaction(database) {

//            exec("ALTER TABLE IF EXISTS raceresults DROP COLUMN IF EXISTS start_date;")
//            exec("ALTER TABLE IF EXISTS series ADD COLUMN IF NOT EXISTS sort INTEGER DEFAULT 0 NOT NULL;")
//            exec("ALTER TABLE IF EXISTS bracket ADD COLUMN IF NOT EXISTS race_class BIGINT DEFAULT 1 NOT NULL;")
//            exec("ALTER TABLE IF EXISTS bracket ADD COLUMN IF NOT EXISTS min_r FLOAT DEFAULT 0 NOT NULL;")
//            exec("ALTER TABLE IF EXISTS bracket ADD COLUMN IF NOT EXISTS max_r FLOAT DEFAULT 0 NOT NULL;")
//            exec("ALTER TABLE IF EXISTS raceclass ADD COLUMN IF NOT EXISTS sort INTEGER DEFAULT 0 NOT NULL;")
//            exec("ALTER TABLE IF EXISTS raceclass ADD COLUMN IF NOT EXISTS phrf BOOLEAN DEFAULT false NOT NULL;")
//            exec("ALTER TABLE IF EXISTS raceclass ADD COLUMN IF NOT EXISTS wsf BOOLEAN DEFAULT false NOT NULL;")
//            exec("ALTER TABLE IF EXISTS raceresults DROP COLUMN IF EXISTS bracket_id;")
        exec("ALTER TABLE IF EXISTS racetime DROP COLUMN IF EXISTS correction_factor;")
        //nuke
        //raceresults, racetime, raceclasscategory, raceclass
        //alter table boat drop column if exists class_id
        //alter table boat drop column if exists class_id
//            SchemaUtils.create(*tables)
//            execInBatch(
//                SchemaUtils.addMissingColumnsStatements(*tables, withLogs = true)
//            )
        exec("UPDATE raceresults SET finish_code='HOC' WHERE hoc > 0 AND end_date IS NULL;")
//            exec(
//                "alter table raceresults drop column if exists name"
//            )
    }
    updateVersion(database, 1)
}


private fun dbVersion1To2Upgrade(database: Database) {
    transaction(database) {
        exec("ALTER TABLE raceclass ADD COLUMN IF NOT EXISTS ratingType varchar(128) DEFAULT 'CruisingNonFlyingSails' NOT NULL;")
        exec("UPDATE raceclass SET ratingType = 'PHRF' WHERE phrf = true;")
        exec("UPDATE raceclass SET ratingType = 'CruisingNonFlyingSails' WHERE phrf = false AND wsf = false;")
        exec("UPDATE raceclass SET ratingType = 'CruisingFlyingSails' WHERE phrf = false AND wsf = true;")
        exec("ALTER TABLE raceclass DROP COLUMN IF EXISTS phrf;")
        exec("ALTER TABLE raceclass DROP COLUMN IF EXISTS wsf;")
    }
    updateVersion(database, 2)
}

private fun dbVersion1To3Upgrade(database: Database) {
    transaction(database) {
        exec("ALTER TABLE boat DROP COLUMN IF EXISTS orc_id;")
    }
    transaction(database) {
        exec("DROP TABLE IF EXISTS orc")
    }
    updateVersion(database, 3)
}

object Migration {
    fun runMigration(database: Database) {
        dbVersionSetup(database)
        if (dbVersion(database) == 0) {
            dbVersion0To1Upgrade(database)
        }
        if (dbVersion(database) == 1) {
            dbVersion1To2Upgrade(database)
        }
        if (dbVersion(database) == 2) {
            dbVersion1To3Upgrade(database)
        }
        println("migrations complete")
        println("database version ${dbVersion(database)}")
    }

}