package com.mxmariner.regatta.db

import com.mxmariner.regatta.data.RatingType
import com.mxmariner.regatta.ratingDefault
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

const val currentDbVersion = 5

private const val sqlDbMetaDataSetup = """
CREATE TABLE IF NOT EXISTS metadata (
    name varchar(128) PRIMARY KEY,
    value varchar(128) NOT NULL
);
INSERT INTO metadata (name, value) 
VALUES ('version', '${currentDbVersion + 10}')
ON CONFLICT (name) DO NOTHING;
"""

private fun dbVersionSetup(database: Database) {
    return transaction(database) { exec(sqlDbMetaDataSetup) }
}

private fun dbVersion(database: Database): Int {
    val version = transaction(database) {
        requireNotNull(exec("SELECT cast(value as INT) from metadata where name='version';") { rs ->
            require(rs.next()) { "metadata version next was not present"}
            rs.getInt(1)
        }) { "metadata version was null" }
    }
    println("db migration version = $version")
    return version
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
        exec("ALTER TABLE raceclass ADD COLUMN IF NOT EXISTS rating_type varchar(128) DEFAULT 'CruisingNonFlyingSails' NOT NULL;")
        exec("UPDATE raceclass SET rating_type = 'PHRF' WHERE phrf = true;")
        exec("UPDATE raceclass SET rating_type = 'CruisingNonFlyingSails' WHERE phrf = false AND wsf = false;")
        exec("UPDATE raceclass SET rating_type = 'CruisingFlyingSails' WHERE phrf = false AND wsf = true;")
        exec("ALTER TABLE raceclass DROP COLUMN IF EXISTS phrf;")
        exec("ALTER TABLE raceclass DROP COLUMN IF EXISTS wsf;")
    }
    transaction(database) {
        exec("ALTER TABLE boat ADD COLUMN IF NOT EXISTS rating_type varchar(128) DEFAULT 'CruisingNonFlyingSails' NOT NULL;")
        exec("UPDATE boat SET rating_type = 'PHRF' WHERE phrf_rating IS NOT NULL;")
        exec("UPDATE boat SET rating_type = 'CruisingFlyingSails' WHERE phrf_rating IS NULL AND ws_flying = true;")
        exec("ALTER TABLE boat DROP COLUMN IF EXISTS ws_flying;")
        exec("ALTER TABLE boat DROP COLUMN IF EXISTS ws_rating;")
    }
    transaction(database) {
        exec("ALTER TABLE raceresults ADD COLUMN IF NOT EXISTS rating_type varchar(128) DEFAULT 'CruisingNonFlyingSails' NOT NULL;")
        exec("ALTER TABLE raceresults ADD COLUMN IF NOT EXISTS orc_ref varchar(128);")
        exec("UPDATE raceresults SET rating_type = 'PHRF' WHERE phrf_rating IS NOT NULL;")
        exec("UPDATE raceresults SET rating_type = 'CruisingFlyingSails' WHERE phrf_rating IS NULL AND ws_flying = true;")
        exec("ALTER TABLE raceresults DROP COLUMN IF EXISTS ws_flying;")
        exec("ALTER TABLE raceresults DROP COLUMN IF EXISTS ws_rating;")
    }
    updateVersion(database, 2)
}

private fun dbVersion2To3Upgrade(database: Database) {
    transaction(database) {
        exec("ALTER TABLE boat DROP COLUMN IF EXISTS orc_id;")
    }
    transaction(database) {
        exec("DROP TABLE IF EXISTS orc")
    }
    updateVersion(database, 3)
}

private fun dbVersion3To4Upgrade(database: Database) {
    transaction(database) {

        RaceResultsTable.selectAll().where { RaceResultsTable.raceClass.isNull() }.forEach { row ->
            val race = RaceTable.findRaceSchedule(row[RaceResultsTable.raceId])
            val ratingType = RatingType.valueOf(row[RaceResultsTable.ratingType])
            val resultId = row[RaceResultsTable.id]
            val phrfRating = row[RaceResultsTable.phrfRating]
            val rating = phrfRating ?: ratingDefault
            race?.schedule?.firstOrNull { classSchedule ->
                if (classSchedule.raceClass.ratingType == ratingType) {
                    val bracket = classSchedule.brackets.firstOrNull { b ->
                        rating >= b.minRating && rating <= b.maxRating
                    }
                    if (bracket != null) {
                        exec("UPDATE raceresults SET bracket_id = ${bracket.id} WHERE id = ${resultId};")
                        exec("UPDATE raceresults SET class_id = ${classSchedule.raceClass.id} WHERE id = ${resultId};")
                    }
                    bracket != null
                } else {
                    false
                }
            }
        }

        RaceResultsTable.selectAll().where { RaceResultsTable.bracket.isNull() }.forEach { row ->
            val race = RaceTable.findRaceSchedule(row[RaceResultsTable.raceId])
            val ratingType = RatingType.valueOf(row[RaceResultsTable.ratingType])
            val resultId = row[RaceResultsTable.id]
            val phrfRating = row[RaceResultsTable.phrfRating]
            val rating = phrfRating ?: ratingDefault
            race?.schedule?.firstOrNull { classSchedule ->
                if (classSchedule.raceClass.ratingType == ratingType) {
                    val bracket = classSchedule.brackets.firstOrNull { b ->
                        rating >= b.minRating && rating <= b.maxRating
                    }
                    if (bracket != null) {
                        exec("UPDATE raceresults SET bracket_id = ${bracket.id} WHERE id = ${resultId};")
                    }
                    bracket != null
                } else {
                    false
                }
            }
        }
    }
    updateVersion(database, 4)
}

/**
 * Indexes for the lookups the loaders make and the uniqueness they assume, the unused `end_date`
 * index, and the orphan `checkin` table. The same indexes are declared on the Table objects for
 * fresh databases; Exposed only adds columns and constraints to existing tables, never indexes.
 * See docs/explore_implemented/db_query_performance.md, R6.
 */
private fun dbVersion4To5Upgrade(database: Database) {
    transaction(database) {
        exec("CREATE INDEX IF NOT EXISTS raceresults_race_id ON raceresults (race_id);")
        exec("DROP INDEX IF EXISTS raceresults_end_date;")
        exec("CREATE UNIQUE INDEX IF NOT EXISTS racetime_race_class_unique ON racetime (race_id, class_id);")
        exec("CREATE UNIQUE INDEX IF NOT EXISTS racebracketjunction_unique ON racebracketjunction (race_id, class_id, bracket_id);")
        exec("CREATE INDEX IF NOT EXISTS racebracketjunction_bracket_id ON racebracketjunction (bracket_id);")
        exec("CREATE INDEX IF NOT EXISTS racebracketjunction_class_id ON racebracketjunction (class_id);")
        exec("CREATE UNIQUE INDEX IF NOT EXISTS image_race_id_unique ON image (race_id);")
        exec("CREATE INDEX IF NOT EXISTS orc_boat_id ON orc (boat_id);")
        exec("CREATE INDEX IF NOT EXISTS race_series_id ON race (series_id);")
        exec("CREATE INDEX IF NOT EXISTS boat_skipper_id ON boat (skipper_id);")
        exec("DROP TABLE IF EXISTS checkin;")
    }
    updateVersion(database, currentDbVersion)
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
            dbVersion2To3Upgrade(database)
        }
        if (dbVersion(database) == 3) {
            dbVersion3To4Upgrade(database)
        }
        if (dbVersion(database) == 4) {
            dbVersion4To5Upgrade(database)
        }
        println("migrations complete")
        println("database version ${dbVersion(database)}")
    }
}