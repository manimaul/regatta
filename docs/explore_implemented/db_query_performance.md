# Database Indexes and Query Performance

Survey of the PostgreSQL indexes behind `server/src/jvmMain/kotlin/com/mxmariner/regatta/db/`
and of what actually costs time when the server answers a request. The index inventory, row
counts, `EXPLAIN ANALYZE` plans and per-endpoint statement counts below were taken on 2026-10-14
against the local Postgres 16 container loaded with the scrubbed production snapshot of
2026-10-13 (`server/src/jvmTest/resources/golden/regatta-2026-10-13.sql`), with the server built
from `main` at 552229f and `log_statement = 'all'` turned on for the measurement.

Status: implemented on 2026-10-14 (R1–R6 below), re-measured, and the golden regression re-run
at every step. The "Results" section at the end has the after numbers and the two places where
the implementation departed from the plan.

## Summary

- **The tables are small and every query already runs in well under a millisecond.** The
  largest table, `raceresults`, has 2,094 rows in 472 kB. The planner picks a sequential scan
  for most lookups and is right to: the whole database fits in a few hundred shared buffers.
- **The cost is the number of round trips, not the plans.** A series standings page issues
  5,080 SQL statements. A single race report issues 1,027. A season's race list issues 979.
  Each statement is a Netty → JDBC → Postgres round trip plus an Exposed transaction's share
  of overhead, so a standings page takes about 1.2 s on localhost and more over the network.
- **Most of those statements are duplicates.** The race report re-reads the race's full
  schedule (brackets, classes, start times, series, RC, image) once per result row, and every
  hydrated `Bracket`, `RaceClass`, `Series` and `Boat` runs an extra `COUNT(*)` that the
  report never displays.
- **Index changes are cheap insurance, not a fix.** Adding the missing foreign-key indexes and
  two uniqueness constraints is worth doing for correctness and for when the data grows, but
  on the current data it will not move the numbers above. Fix the N+1 loaders first (R1–R3),
  then add the indexes (R6) in the same migration bump.

## What exists today

### Index inventory (from `pg_indexes`, 2026-10-14)

| Table | Index | Definition | Declared in |
|---|---|---|---|
| auth | auth_pkey | UNIQUE (id) | `AuthTable.primaryKey` |
| auth | user_name_idx | UNIQUE (user_name) | `AuthTable.userName.uniqueIndex` |
| boat | boat_pkey | UNIQUE (id) | `BoatTable.primaryKey` |
| bracket | raceclass_pkey | UNIQUE (id) | `BracketTable.primaryKey` (name is a leftover from the table's old name) |
| image | image_pkey | UNIQUE (hash) | `ImageTable.primaryKey` |
| metadata | metadata_pkey | UNIQUE (name) | `Migration.kt` raw SQL |
| orc | orc_pkey | UNIQUE (orc_ref) | `OrcTable.primaryKey` |
| person | person_pkey | UNIQUE (id) | `PersonTable.primaryKey` |
| person | person_first_last_unique | UNIQUE (first, last) | `PersonTable.compoundIdx` |
| race | race_pkey | UNIQUE (id) | `RaceTable.primaryKey` |
| raceclass | raceclasscategory_pkey | UNIQUE (id) | `RaceClassTable.primaryKey` (old table name) |
| raceresults | raceresults_pkey | UNIQUE (id) | `RaceResultsTable.primaryKey` |
| raceresults | idx_boat_race_unique | UNIQUE (boat_id, race_id) | `RaceResultsTable.init` |
| raceresults | raceresults_end_date | (end_date) | `RaceResultsTable.finish.index()` |
| series | series_pkey | UNIQUE (id) | `SeriesTable.primaryKey` |
| series | nameidx | UNIQUE (name) | `SeriesTable.name.uniqueIndex` |

Tables with **no index beyond the primary key**: `boat`, `bracket`, `race`, `raceclass`.
Tables with **no index at all**: `racebracketjunction`, `racetime` (neither has a primary key).

### Row counts and sizes

| Table | Rows | Total size |
|---|---|---|
| raceresults | 2,094 | 472 kB |
| racebracketjunction | 581 | 56 kB |
| racetime | 425 | 56 kB |
| race | 119 | 64 kB |
| person | 80 | 40 kB |
| boat | 79 | 32 kB |
| bracket | 21 | 32 kB |
| raceclass | 13 | 24 kB |
| series | 9 | 48 kB |
| orc | 8 | 64 kB |
| image | 0 | 16 kB |
| checkin | 0 | 0 bytes |

`checkin` exists in production and in the snapshot but no Kotlin code references it. It is
not in the `tables` array in `RegattaDatabase.init()` and can be dropped in a migration step
when convenient.

### Observations on the existing indexes

- **Postgres does not index foreign-key columns automatically.** Every `references` column in
  the Exposed tables gets a FK *constraint* (`fk_raceresults_race_id__id` and friends) but no
  index. That is normal, but it means `raceresults.race_id`, `racebracketjunction.race_id`,
  `racetime.race_id`, `orc.boat_id`, `image.race_id`, `race.series_id` and `boat.skipper_id`
  are all unindexed even though they are the columns the code filters on most.
- **`raceresults_end_date` is never used.** No query in the `db` package filters or sorts on
  `end_date`; the column is only read. `pg_stat_user_indexes` shows `idx_scan = 0` on it versus
  22,193 on `idx_boat_race_unique`. It costs a little on every result write and nothing else.
  Drop it.
- **`idx_boat_race_unique` serves the wrong lookups.** Its leading column is `boat_id`, so it
  answers `raceCount(boatId)` (used once per boat on the boats page) but not
  `WHERE race_id = ?`, which is the hot lookup for results. `EXPLAIN` confirms: the per-race
  results query is a sequential scan that discards 2,085 of 2,094 rows (0.36 ms today).
- **Two tables rely on uniqueness the schema does not enforce.** `RaceTimeTable.findByRaceAndClassId`
  calls `singleOrNull()`, and `ImageTable.getRaceReportImageName` does too, but nothing stops a
  second `racetime` row for the same race and class or a second image for a race. The code
  deletes before inserting, so duplicates only appear if two admins save at once, but then the
  schedule page throws. `racebracketjunction` likewise has no key, so the same bracket can be
  attached to a race twice and `selectBrackets` would return it twice.
- **Exposed will not create a new index on an existing table.** `RegattaDatabase.init()` calls
  `SchemaUtils.create` (skips tables that already exist) and `addMissingColumnsStatements`
  (verified in the Exposed 1.3.0 sources: adds columns, primary keys and FK constraints only).
  Missing indexes are only logged. Every index below therefore needs a
  `CREATE INDEX IF NOT EXISTS` in a new `dbVersion4To5Upgrade` step **and** the matching
  `.index()` / `uniqueIndex(...)` declaration in the Table object so fresh databases get it too.
  `common/.../Versions.kt` pins Exposed 1.3.0; the CLAUDE.md table still says 1.0.0.

## Measurements

SQL statements and wall time per request, one warm-up call then one measured call,
localhost, `podman logs` statement count delta. The `allBoat` endpoint needs an admin token
and was not measured; from the code it is 1 join + 1 select + 2 statements per boat, about
160 for the 79 boats.

| Endpoint | Statements | Wall time |
|---|---|---|
| `GET /v1/api/series/standings?id=15&year=2025` (5 races, 143 results) | 5,080 | 1,224 ms |
| `GET /v1/api/report?raceId=69` (30 results, 4 classes, 6 brackets) | 1,027 | 259 ms |
| `GET /v1/api/allRaces?year=2025` (34 races) | 979 | 242 ms |
| `GET /v1/api/raceSchedule?id=69` | 35 | 36 ms |
| `GET /v1/api/allClasses` (13 classes, 21 brackets) | 53 | 41 ms |
| `GET /v1/api/allSeries` (9 series) | 15 | 30 ms |
| `GET /v1/api/years` | 6 | 29 ms |

Statement shapes behind the 1,027 for one race report (parameters normalised):

| Count | Statement | Source |
|---|---|---|
| 180 | `SELECT COUNT(DISTINCT race_id) FROM racebracketjunction WHERE bracket_id = ?` | `BracketTable.resultRowToBracket` → `numberOfRaces` |
| 180 | `SELECT ... FROM bracket WHERE id = ?` | `RaceBracketJunction.selectBrackets`, one per junction row |
| 120 | `SELECT ... FROM racetime WHERE class_id = ? AND race_id = ?` | `RaceTable.findRaceSchedule`, one per class |
| 120 | `SELECT ... FROM raceclass WHERE id = ?` | `RaceClassTable.selectById`, one per class |
| 120 | `SELECT COUNT(DISTINCT race_id) FROM racebracketjunction WHERE class_id = ?` | `RaceClassTable.resultRowToClass` → `numberOfRaces` |
| 59 | `SELECT hash, race_id, data FROM image WHERE race_id = ?` | `RaceTable.rowToRace` → `ImageTable.getRaceReportImageName` |
| 30 | `SELECT ... FROM series WHERE id = ?` | `findRaceSchedule` |
| 30 | `SELECT COUNT(*) FROM race WHERE series_id = ?` | `SeriesTable.resultRowToSeries` → `raceCount` |
| 30 | `SELECT ... FROM racebracketjunction WHERE race_id = ?` | `findRaceSchedule` |
| 30 | `SELECT ... FROM race WHERE id = ?` | `findRaceSchedule` |
| 30 | `SELECT COUNT(*) FROM raceresults WHERE race_id = ?` | `findRaceSchedule` → `resultCount` |
| 29 | `SELECT ... FROM person WHERE id = ?` | skipper, one per result |
| 29 | `SELECT ... FROM orc WHERE boat_id = ?` | `BoatTable.resultRowToBoat` → `orcCerts` |
| 29 | `SELECT COUNT(*) FROM raceresults WHERE boat_id = ?` | `BoatTable.resultRowToBoat` → `numberOfRaces` |

The report has 30 result rows, and every number in that table is a multiple of 30 (or 29
boats). The cause is one line: `RaceResultsTable.rowToRaceResultBoatBracket` calls
`findRaceSchedule(it.id)` for each row, and `findRaceSchedule` is itself ~33 statements for
this race (1 junction select + 6 brackets × 2 + 4 classes × 3 + race + image + result count +
series + series race count + RC person). `RaceResultReporter.getReport` had already loaded the
same schedule one line earlier and passes it to `reduceToCard`, so the per-row copy is thrown
away. The standings endpoint is five of these reports plus one `seriesRaces` query, which is
exactly the 5,080.

`allRaces(year)` has the same shape: one raw query for the year's race ids, then
`findRaceSchedule` per race (34 × ~28).

`EXPLAIN (ANALYZE, BUFFERS)` for the individual statements, same data:

| Statement | Plan | Actual time |
|---|---|---|
| `raceresults WHERE race_id = 50` | Seq Scan, 2,085 rows filtered out | 0.36 ms |
| `raceresults WHERE boat_id = 10` | Index Scan `idx_boat_race_unique` | 0.11 ms |
| `racetime WHERE race_id = 50 AND class_id = 3` | Seq Scan, 424 filtered | 0.07 ms |
| `racebracketjunction WHERE race_id = 50` | Seq Scan, 577 filtered | 0.14 ms |
| `COUNT(DISTINCT race_id) ... WHERE bracket_id = 3` | Seq Scan + Sort + Aggregate | 0.13 ms |
| `DISTINCT race_id FROM racetime WHERE start_date in 2025` | Seq Scan (134 of 425 match) + HashAggregate | 0.06 ms |
| `orc WHERE boat_id = 10` | Seq Scan, 8 rows | 0.02 ms |

With 1,027 statements at roughly 0.1 ms each the database spends about 100 ms on the report;
the other ~160 ms is driver, connection and coroutine overhead per statement. Faster plans
cannot help much when the plan is already a tenth of a millisecond.

## Recommendations

In priority order. R1 is a one-line change with the largest effect; R2 and R3 are small
refactors of the loaders; R4 and R5 are hygiene; R6 is the index migration. Expected statement
counts are estimates from the call graph, to be confirmed by re-measuring (see Verification).

### R1. Stop re-reading the schedule per result row

`RaceResultsTable.rowToRaceResultBoatBracket(row)` should take the `RaceSchedule` as a
parameter instead of calling `findRaceSchedule(rowToRace(row).id)` itself.
`resultsBoatBracketByRaceId(rId)` becomes `resultsBoatBracketByRaceId(rId, schedule)` and
`RaceResultReporter.getReport` passes the schedule it already has. The inner join with
`RaceTable` in that query is then unnecessary too (nothing else reads the race columns).

Effect: report 1,027 → about 125 statements (1 schedule ≈ 33, 1 results join, 3 per result
for skipper, ORC certs and boat race count). Standings 5,080 → about 630.

### R2. Load a race schedule in one or two statements

`RaceTable.findRaceSchedule` fans out into 1 + 2·brackets + 3·classes + 6 statements. Replace
the body with:

1. One join: `racebracketjunction ⋈ bracket ⋈ raceclass ⋈ racetime` filtered on `race_id`,
   grouped in Kotlin by class. This yields every `ClassSchedule` with its brackets, start and
   end in a single result set.
2. One query for the race row left-joined to `series` and `person` (RC), plus the
   `COUNT(*)` of results as a subquery or a second statement.

For the race list, add a `findRaceSchedules(raceIds: List<Long>)` that runs the same two
statements with `race_id IN (...)` and groups by race, so `allRaces(year)` is 3 statements
instead of 979, and `getStandingsReport` loads its 5 schedules in one go.

Effect: `raceSchedule` 35 → 2–3; `allRaces` 979 → 3; report (after R1) ~125 → ~95.

### R3. Take the derived counts off the hot path

`numberOfRaces` on `Bracket` and `RaceClass`, `raceCount` on `Series` and `numberOfRaces` on
`Boat` are computed with a `COUNT` every time a row is hydrated. They exist so the admin pages
(`Classes.kt`, `Series`, `Boats`) can show the count and block edits or deletes of anything in
use; the reporter and the public race pages never read them. Two acceptable shapes:

- **Grouped count on the list endpoints only.** `allClasses()`, `allSeries()` and
  `selectAllBoats()` each run one `SELECT fk, COUNT(DISTINCT ...) GROUP BY fk` and stitch the
  numbers into the rows. Every other loader fills the field with 0 (or the field moves to a
  wrapper type for those endpoints). The simplest version keeps the API shape unchanged.
- **Bulk-load the children.** `allClasses()` currently does 1 + 13 brackets-per-class
  selects, each bracket then counting; one join of `raceclass ⋈ bracket` plus one grouped
  count over `racebracketjunction` replaces all 53 statements with 3.

Same for `BoatTable.resultRowToBoat`, which runs `raceCount` and `findCertificates` per boat:
`selectAllBoats()` should left-join `person` (replacing the inner join + second query for
skipperless boats) and load all ORC certificates in one `WHERE boat_id IN (...)`.

Effect: report (after R1 + R2) ~95 → ~5; standings → ~30; `allClasses` 53 → 3; `allBoat`
~160 → 3.

### R4. Do not select the image bytes to learn the file name

`ImageTable.getRaceReportImageName` runs `selectAll()` on `image` and reads only `hash`, so
each call pulls the whole `bytea` across the wire. Production has no images today (the scrub
script refuses to run if it ever does), so the cost is currently zero, but the first uploaded
report image would be re-read on every `rowToRace` for that race, which is once per result
row until R1 lands. Use `select(fileName)`. With R2 this becomes a left join in the race
query.

### R5. Smaller things

- `RaceTimeTable.allYears()` loads every `racetime` row and converts both timestamps in
  Kotlin. `SELECT DISTINCT EXTRACT(YEAR FROM start_date AT TIME ZONE 'America/Los_Angeles')`
  (and the same for `end_date`) returns the handful of years directly. 6 statements → 2; the
  bigger win is not shipping 425 rows.
- `BoatTable.findBoatSkipper` runs an inner join and, when the boat has no skipper, a second
  plain select. A single left join handles both.
- `RaceBracketJunction.selectBrackets` selects the junction rows and then `findBracket` per
  row. Folded into R2.
- `dbQuery` wraps `suspendTransaction { withContext(Dispatchers.IO) { ... } }`. Not a query
  issue, but note that the dispatcher hop happens once per `dbQuery`, and
  `getStandingsReport` makes ~12 separate `dbQuery` calls per race. After R1–R3 consider one
  transaction per request.

### R6. Index and constraint changes

All of these go in a new `dbVersion4To5Upgrade` in `Migration.kt` (bump `currentDbVersion`
to 5) **and** in the Table objects. None are expected to change timings on today's data
(every affected scan is under 0.4 ms); they are for correctness (the two unique constraints),
for the FK checks that run on every delete, and so that the sequential scans do not become a
problem when `raceresults` is 10× larger.

| Table | Change | Why | SQL (migration) |
|---|---|---|---|
| raceresults | Add index on `race_id` | Hot lookup (`resultsByRaceId`, `count`, the results join); today a seq scan discarding 99.6 % of the table. Alternative: flip the unique index to `(race_id, boat_id)` and add a plain `boat_id` index, since `boat_id`-only lookups are rare. | `CREATE INDEX IF NOT EXISTS raceresults_race_id ON raceresults (race_id);` |
| raceresults | Drop `raceresults_end_date` | Never used (`idx_scan = 0`); remove `.index()` from `finish`. | `DROP INDEX IF EXISTS raceresults_end_date;` |
| racetime | Add **unique** `(race_id, class_id)` | Enforces what `findByRaceAndClassId.singleOrNull()` assumes; serves that lookup and `deleteRace`. Declare as `uniqueIndex("racetime_race_class_unique", raceId, classId)`. | `CREATE UNIQUE INDEX IF NOT EXISTS racetime_race_class_unique ON racetime (race_id, class_id);` |
| racebracketjunction | Add **unique** `(race_id, class_id, bracket_id)` | The table has no key at all; prevents a bracket being attached twice; leading `race_id` serves `selectBrackets` and both delete paths. | `CREATE UNIQUE INDEX IF NOT EXISTS racebracketjunction_unique ON racebracketjunction (race_id, class_id, bracket_id);` |
| racebracketjunction | Add index on `bracket_id` and on `class_id` | `raceCountForBracket` / `raceCountForClass` (180 + 120 calls per report today; a few after R3). Also the FK check when a bracket or class is deleted. | `CREATE INDEX IF NOT EXISTS racebracketjunction_bracket_id ON racebracketjunction (bracket_id); CREATE INDEX IF NOT EXISTS racebracketjunction_class_id ON racebracketjunction (class_id);` |
| image | Add **unique** on `race_id` | One report image per race is what `saveRaceReportImage` (delete then insert) and `getRaceReportImageName.singleOrNull()` assume. | `CREATE UNIQUE INDEX IF NOT EXISTS image_race_id_unique ON image (race_id);` |
| orc | Add index on `boat_id` | `findCertificates(boat)`; 8 rows today, irrelevant until there are hundreds of certificates, but it is the FK check on boat delete. | `CREATE INDEX IF NOT EXISTS orc_boat_id ON orc (boat_id);` |
| race | Add index on `series_id` | `raceCountForSeries`, the `seriesRaces` join, FK check on series delete. | `CREATE INDEX IF NOT EXISTS race_series_id ON race (series_id);` |
| boat | Add index on `skipper_id` | `findBoatForSkipper`, `removePerson`, FK check on person delete. | `CREATE INDEX IF NOT EXISTS boat_skipper_id ON boat (skipper_id);` |

Deliberately **not** recommended:

- An index on `racetime.start_date` for the year-range queries. A season is roughly a third
  of the table (134 of 425 rows in 2025), so the planner would keep the sequential scan;
  revisit after ten or more seasons.
- Any index on `person.first` / `person.last` / `series.name` for the `ILIKE '%x%'` searches.
  A B-tree cannot serve a leading-wildcard pattern; `pg_trgm` could, but the tables have 80
  and 9 rows.
- Partial or covering indexes on `raceresults`. Nothing in the result set is narrow enough to
  benefit, and the table is 28 pages.

Before applying the two unique constraints in production, check the snapshot for existing
duplicates; on the 2026-10-13 snapshot both queries return zero rows:

```sql
SELECT race_id, class_id, COUNT(*) FROM racetime GROUP BY 1, 2 HAVING COUNT(*) > 1;
SELECT race_id, class_id, bracket_id, COUNT(*) FROM racebracketjunction GROUP BY 1, 2, 3 HAVING COUNT(*) > 1;
```

## Verification

- **Correctness.** `./gradlew :server:jvmGoldenTest` scores every race and series in the
  snapshot and diffs against `golden/approved`. R1–R3 rewrite the loaders the reporter depends
  on, so the golden files must not change at all (`deviations.json` stays empty). Run it after
  each of R1, R2 and R3, not just at the end.
- **Statement counts.** Repeat the measurement: `ALTER DATABASE regatta SET
  log_statement = 'all'`, start the server, warm up each endpoint once, then diff
  `podman logs database_postgres_1 | grep -c "LOG:  execute\|LOG:  statement"` around one
  call. Reset with `ALTER DATABASE regatta RESET log_statement`. Targets after R1–R3: standings
  under 50 statements, report and `allRaces` under 10, `raceSchedule` under 5.
- **Index use.** After R6, `EXPLAIN` on `SELECT * FROM raceresults WHERE race_id = ?` should
  show an index scan, and `pg_stat_user_indexes` should show the new indexes accumulating
  `idx_scan` while `raceresults_end_date` is gone.

## Results (implemented 2026-10-14)

Same data, same method, server built from the implementation:

| Endpoint | Statements before | Statements after | Wall before | Wall after |
|---|---|---|---|---|
| `series/standings?id=15&year=2025` (5 races, 143 results) | 5,080 | 74 | 1,224 ms | 101 ms |
| `report?raceId=69` (30 results) | 1,027 | 25 | 259 ms | 48 ms |
| `allRaces?year=2025` (34 races) | 979 | 18 | 242 ms | 36 ms |
| `raceSchedule?id=69` | 35 | 16 | 36 ms | 33 ms |
| `allClasses` | 53 | 9 | 41 ms | 41 ms |
| `allSeries` | 15 | 7 | 30 ms | 29 ms |
| `years` | 6 | 6 | 29 ms | 30 ms |

The remaining statements are mostly fixed per request: a schedule is 14 statements however many
races are loaded, a race's results are 4, and each Exposed transaction adds a few session
statements that the count includes (`years` is a single `SELECT` and still shows 6).

What was done, by recommendation:

- **R1.** `RaceResultsTable.resultsBoatBracketByRaceId(raceId, raceSchedule)` takes the schedule
  the reporter already has. The results query keeps the exact SQL it had (`raceresults ⋈ race ⋈
  boat`, no `ORDER BY`), because tied cards keep their input order through the stable sorts in
  the reporter and a different row order would reorder them in the golden files.
- **R2.** `RaceTable.findRaceSchedules(raceIds)` loads any number of schedules in 14 statements
  (races, image names, junctions, brackets, classes, class times, result counts, series, RC
  people, plus one grouped count each for brackets, classes, series and results).
  `findRaceSchedule(id)` is the one-element case; `allRaces(year)` and `getStandingsReport` use
  the bulk form. Classes are now ordered by class sort then id, and brackets by min rating
  then id, instead of by whatever order the junction rows are stored in. That was necessary
  for R6: with a unique index led by `race_id` on the junction table the planner would read it in
  index order, which matches the stored order in only 17 of 118 races.
- **R3.** The derived counts stay in the API (the golden files and the admin pages use them) but
  come from one `GROUP BY` query per hydrated batch: `RaceBracketJunction.raceCountsByBracket`
  / `raceCountsByClass`, `RaceTable.raceCountsBySeries`, `RaceResultsTable.countsByRace` /
  `raceCountsByBoat`. Single-item lookups (`findBracket`, `selectById`, upserts) go through the
  same batch loaders with a one-element list. The boats page loads boats with a left join to
  `person` and one query each for certificates and race counts; `BoatTable.findBoatSkippers(ids)`
  does the same for the schedule-results endpoint. The reporter looks up the ORC certificates of
  a race's results in one `IN` query instead of one per card.
- **R4.** `ImageTable.getRaceReportImageName` / `reportImageNames` select `hash` and `race_id`
  only.
- **R5.** `allYears()` selects only the two timestamp columns rather than whole rows. It still
  converts to Pacific years in Kotlin, not with `EXTRACT`: the column is `timestamp without time
  zone` and the JVM-side conversion is what defines the stored value, so an SQL expression would
  have to replicate that convention for a saving of one statement. `findBoatSkipper` is one left
  join.
- **R6.** `dbVersion4To5Upgrade` creates the indexes in the table above, drops
  `raceresults_end_date` and the empty `checkin` table, and `currentDbVersion` is 5. The Table
  objects declare the same indexes for fresh databases. `RaceBracketJunction.setBrackets` now
  de-duplicates its input so the new unique index cannot reject a double-attached bracket.
  Verified on the dev database after startup: schema version 5, all ten indexes present,
  `EXPLAIN` on `raceresults WHERE race_id = ?` is a bitmap scan on `raceresults_race_id`.

**Golden regression.** Run after R1–R3 with the stored row order kept: 118 race reports and 22
standings byte-identical to the approved files. Then the explicit ordering of R2 was added and
exactly eight files changed, all order-only (checked by comparing the files with every list
sorted): `race-37`, `race-46`, `race-91`, `race-103`, `race-109` list bracket I before II (or III
before IV) where they had been stored the other way round; `race-77` lists its two Windseekers
classes in class-sort order; `standings-16-2026` and `standings-17-2026` follow the same bracket
order. Those eight were re-approved on 2026-10-14 and `database/audit_scoring.py` still writes
an empty `deviations.json`. The run after R6 (which applies the migration to the snapshot and
checks the schema version) and `:server:jvmTest` are green.

## Tasks

- [x] R1: thread `RaceSchedule` through `resultsBoatBracketByRaceId`. Golden test.
- [x] R2: `findRaceSchedules(ids)` in a fixed number of statements; used by `allRaces(year)` and `getStandingsReport`. Golden test.
- [x] R3: grouped counts for brackets, classes, series, boats and results; bulk-loaded certificates and skippers. Golden test.
- [x] R4: `ImageTable` selects only the name.
- [x] R5: `allYears()` narrow select (Kotlin conversion kept, see above); `findBoatSkipper` as one left join.
- [x] R6: `dbVersion4To5Upgrade`, `currentDbVersion = 5`, matching Table declarations, duplicate check on the snapshot (none).
- [x] Re-measured and recorded above; moved to `explore_implemented/`.
- [x] Exposed version in `CLAUDE.md` corrected to 1.3.0.
