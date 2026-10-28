# Database Indexes and Query Performance

Survey of the PostgreSQL indexes behind the data layer (`crates/service/src/db/`)
and of what actually costs time when the server answers a request. The index inventory, row
counts, `EXPLAIN ANALYZE` plans and per-endpoint statement counts below were taken on 2026-10-14
against the local Postgres 16 container loaded with the scrubbed production snapshot of
2026-10-13 (the golden snapshot in `crates/scoring/tests/golden/`), with the server built
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
  Each statement is a driver → Postgres round trip plus its transaction's share
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

| Table | Index | Definition |
|---|---|---|
| auth | auth_pkey | UNIQUE (id) |
| auth | user_name_idx | UNIQUE (user_name) |
| boat | boat_pkey | UNIQUE (id) |
| bracket | raceclass_pkey | UNIQUE (id) (name is a leftover from the table's old name) |
| image | image_pkey | UNIQUE (hash) |
| metadata | metadata_pkey | UNIQUE (name) |
| orc | orc_pkey | UNIQUE (orc_ref) |
| person | person_pkey | UNIQUE (id) |
| person | person_first_last_unique | UNIQUE (first, last) |
| race | race_pkey | UNIQUE (id) |
| raceclass | raceclasscategory_pkey | UNIQUE (id) (old table name) |
| raceresults | raceresults_pkey | UNIQUE (id) |
| raceresults | idx_boat_race_unique | UNIQUE (boat_id, race_id) |
| raceresults | raceresults_end_date | (end_date) |
| series | series_pkey | UNIQUE (id) |
| series | nameidx | UNIQUE (name) |

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

`checkin` was in production and in the snapshot, but no code references it. R6 drops it, and
the baseline schema (`migrations/0001_baseline.sql`) does not have it.

### Observations on the existing indexes

- **Postgres does not index foreign-key columns automatically.** Every foreign-key column in
  the schema has a FK *constraint* (`fk_raceresults_race_id__id` and friends) but no
  index. That is normal, but it means `raceresults.race_id`, `racebracketjunction.race_id`,
  `racetime.race_id`, `orc.boat_id`, `image.race_id`, `race.series_id` and `boat.skipper_id`
  are all unindexed even though they are the columns the code filters on most.
- **`raceresults_end_date` is never used.** No query in `crates/service/src/db/` filters or sorts on
  `end_date`; the column is only read. `pg_stat_user_indexes` shows `idx_scan = 0` on it versus
  22,193 on `idx_boat_race_unique`. It costs a little on every result write and nothing else.
  Drop it.
- **`idx_boat_race_unique` serves the wrong lookups.** Its leading column is `boat_id`, so it
  answers `race_results::race_count(boat_id)` (used once per boat on the boats page) but not
  `WHERE race_id = ?`, which is the hot lookup for results. `EXPLAIN` confirms: the per-race
  results query is a sequential scan that discards 2,085 of 2,094 rows (0.36 ms today).
- **Two tables rely on uniqueness the schema does not enforce.** The schedule loader expects at
  most one `racetime` row per race and class, and `image::get_race_report_image_name` at most one
  image per race, but nothing stops a second `racetime` row for the same race and class or a
  second image for a race. The code deletes before inserting, so duplicates only appear if two
  admins save at once, but then the schedule page fails. `racebracketjunction` likewise has no
  key, so the same bracket can be attached to a race twice and the schedule would list it twice.
- **Startup never adds an index to an existing database.** `startup::prepare`
  (`crates/service/src/startup.rs`) applies `migrations/0001_baseline.sql` to an empty database
  only, and otherwise just checks `metadata.version`. Every index below therefore belongs to a
  schema version: a `CREATE INDEX IF NOT EXISTS` in the upgrade step **and** the same index in
  the baseline so fresh databases get it too.

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
| 180 | `SELECT COUNT(DISTINCT race_id) FROM racebracketjunction WHERE bracket_id = ?` | bracket hydration → `Bracket.number_of_races` |
| 180 | `SELECT ... FROM bracket WHERE id = ?` | the race's brackets, one per junction row |
| 120 | `SELECT ... FROM racetime WHERE class_id = ? AND race_id = ?` | `find_race_schedule`, one per class |
| 120 | `SELECT ... FROM raceclass WHERE id = ?` | `race_class::select_by_id`, one per class |
| 120 | `SELECT COUNT(DISTINCT race_id) FROM racebracketjunction WHERE class_id = ?` | class hydration → `RaceClass.number_of_races` |
| 59 | `SELECT hash, race_id, data FROM image WHERE race_id = ?` | race row → `image::get_race_report_image_name` |
| 30 | `SELECT ... FROM series WHERE id = ?` | `find_race_schedule` |
| 30 | `SELECT COUNT(*) FROM race WHERE series_id = ?` | series hydration → `Series.race_count` |
| 30 | `SELECT ... FROM racebracketjunction WHERE race_id = ?` | `find_race_schedule` |
| 30 | `SELECT ... FROM race WHERE id = ?` | `find_race_schedule` |
| 30 | `SELECT COUNT(*) FROM raceresults WHERE race_id = ?` | `find_race_schedule` → `RaceSchedule.result_count` |
| 29 | `SELECT ... FROM person WHERE id = ?` | skipper, one per result |
| 29 | `SELECT ... FROM orc WHERE boat_id = ?` | boat hydration → `Boat.orc_certs` |
| 29 | `SELECT COUNT(*) FROM raceresults WHERE boat_id = ?` | boat hydration → `Boat.number_of_races` |

The report has 30 result rows, and every number in that table is a multiple of 30 (or 29
boats). The cause is one line: the results row mapper calls
`find_race_schedule` for each row, and `find_race_schedule` is itself ~33 statements for
this race (1 junction select + 6 brackets × 2 + 4 classes × 3 + race + image + result count +
series + series race count + RC person). `reporter::get_report` had already loaded the
same schedule one line earlier and passes it to `reduce_to_card`, so the per-row copy is thrown
away. The standings endpoint is five of these reports plus one `series_races` query, which is
exactly the 5,080.

`all_races(year)` has the same shape: one raw query for the year's race ids, then
`find_race_schedule` per race (34 × ~28).

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
the other ~160 ms is driver and connection overhead per statement. Faster plans
cannot help much when the plan is already a tenth of a millisecond.

## Recommendations

In priority order. R1 is a one-line change with the largest effect; R2 and R3 are small
refactors of the loaders; R4 and R5 are hygiene; R6 is the index migration. Expected statement
counts are estimates from the call graph, to be confirmed by re-measuring (see Verification).

### R1. Stop re-reading the schedule per result row

The results row mapper should take the `RaceSchedule` as a parameter instead of calling
`find_race_schedule` for the row's race itself.
`results_boat_bracket_by_race_id(race_id)` becomes
`results_boat_bracket_by_race_id(race_id, race_schedule)` and `reporter::get_report` passes the
schedule it already has. The inner join with `race` in that query is then unnecessary too
(nothing else reads the race columns).

Effect: report 1,027 → about 125 statements (1 schedule ≈ 33, 1 results join, 3 per result
for skipper, ORC certs and boat race count). Standings 5,080 → about 630.

### R2. Load a race schedule in one or two statements

`race::find_race_schedule` fans out into 1 + 2·brackets + 3·classes + 6 statements. Replace
the body with:

1. One join: `racebracketjunction ⋈ bracket ⋈ raceclass ⋈ racetime` filtered on `race_id`,
   grouped by class in the server. This yields every `ClassSchedule` with its brackets, start and
   end in a single result set.
2. One query for the race row left-joined to `series` and `person` (RC), plus the
   `COUNT(*)` of results as a subquery or a second statement.

For the race list, add a `find_race_schedules(race_ids: &[i64])` that runs the same two
statements with `race_id IN (...)` and groups by race, so `all_races(year)` is 3 statements
instead of 979, and `get_standings_report` loads its 5 schedules in one go.

Effect: `raceSchedule` 35 → 2–3; `allRaces` 979 → 3; report (after R1) ~125 → ~95.

### R3. Take the derived counts off the hot path

`number_of_races` on `Bracket` and `RaceClass`, `race_count` on `Series` and `number_of_races` on
`Boat` are computed with a `COUNT` every time a row is hydrated. They exist so the admin pages
(Classes, Series and Boats in `crates/app/src/routes/`) can show the count and block edits or deletes of anything in
use; the reporter and the public race pages never read them. Two acceptable shapes:

- **Grouped count on the list endpoints only.** `race_class::all_classes`, `series::select_all_series` and
  `boat::select_all_boats` each run one `SELECT fk, COUNT(DISTINCT ...) GROUP BY fk` and stitch the
  numbers into the rows. Every other loader fills the field with 0 (or the field moves to a
  wrapper type for those endpoints). The simplest version keeps the API shape unchanged.
- **Bulk-load the children.** `all_classes` currently does 1 + 13 brackets-per-class
  selects, each bracket then counting; one join of `raceclass ⋈ bracket` plus one grouped
  count over `racebracketjunction` replaces all 53 statements with 3.

Same for boat hydration, which runs a race count and a certificate lookup per boat:
`select_all_boats` should left-join `person` (replacing the inner join + second query for
skipperless boats) and load all ORC certificates in one `WHERE boat_id IN (...)`.

Effect: report (after R1 + R2) ~95 → ~5; standings → ~30; `allClasses` 53 → 3; `allBoat`
~160 → 3.

### R4. Do not select the image bytes to learn the file name

`image::get_race_report_image_name` selects every column of `image` and reads only `hash`, so
each call pulls the whole `bytea` across the wire. Production has no images today (the scrub
script refuses to run if it ever does), so the cost is currently zero, but the first uploaded
report image would be re-read on every race row read for that race, which is once per result
row until R1 lands. Select only `hash`. With R2 this becomes a left join in the race
query.

### R5. Smaller things

- `race_time::all_years` loads every `racetime` row and converts both timestamps in
  the server. `SELECT DISTINCT EXTRACT(YEAR FROM start_date AT TIME ZONE 'America/Los_Angeles')`
  (and the same for `end_date`) returns the handful of years directly. 6 statements → 2; the
  bigger win is not shipping 425 rows.
- `boat::find_boat_skipper` runs an inner join and, when the boat has no skipper, a second
  plain select. A single left join handles both.
- The race's brackets are read as the junction rows and then `bracket::find_bracket` per
  row. Folded into R2.

### R6. Index and constraint changes

All of these belong to schema version 5 (`startup::SCHEMA_VERSION`): the upgrade step for
existing databases **and** `migrations/0001_baseline.sql` for fresh ones. None are expected to change timings on today's data
(every affected scan is under 0.4 ms); they are for correctness (the two unique constraints),
for the FK checks that run on every delete, and so that the sequential scans do not become a
problem when `raceresults` is 10× larger.

| Table | Change | Why | SQL (migration) |
|---|---|---|---|
| raceresults | Add index on `race_id` | Hot lookup (`race_results::results_by_race_id`, `race_results::count`, the results join); today a seq scan discarding 99.6 % of the table. Alternative: flip the unique index to `(race_id, boat_id)` and add a plain `boat_id` index, since `boat_id`-only lookups are rare. | `CREATE INDEX IF NOT EXISTS raceresults_race_id ON raceresults (race_id);` |
| raceresults | Drop `raceresults_end_date` | Never used (`idx_scan = 0`). | `DROP INDEX IF EXISTS raceresults_end_date;` |
| racetime | Add **unique** `(race_id, class_id)` | Enforces the one class time per race and class that the schedule loader assumes; serves that lookup and `race_time::delete_race`. | `CREATE UNIQUE INDEX IF NOT EXISTS racetime_race_class_unique ON racetime (race_id, class_id);` |
| racebracketjunction | Add **unique** `(race_id, class_id, bracket_id)` | The table has no key at all; prevents a bracket being attached twice; leading `race_id` serves `race_bracket_junction::select_for_races` and both delete paths. | `CREATE UNIQUE INDEX IF NOT EXISTS racebracketjunction_unique ON racebracketjunction (race_id, class_id, bracket_id);` |
| racebracketjunction | Add index on `bracket_id` and on `class_id` | `race_count_for_bracket` / `race_count_for_class` (180 + 120 calls per report today; a few after R3). Also the FK check when a bracket or class is deleted. | `CREATE INDEX IF NOT EXISTS racebracketjunction_bracket_id ON racebracketjunction (bracket_id); CREATE INDEX IF NOT EXISTS racebracketjunction_class_id ON racebracketjunction (class_id);` |
| image | Add **unique** on `race_id` | One report image per race is what `image::save_race_report_image` (delete then insert) and `image::get_race_report_image_name` assume. | `CREATE UNIQUE INDEX IF NOT EXISTS image_race_id_unique ON image (race_id);` |
| orc | Add index on `boat_id` | `orc_table::find_certificates_of_boat`; 8 rows today, irrelevant until there are hundreds of certificates, but it is the FK check on boat delete. | `CREATE INDEX IF NOT EXISTS orc_boat_id ON orc (boat_id);` |
| race | Add index on `series_id` | `race::race_count_for_series`, the `series_races` join, FK check on series delete. | `CREATE INDEX IF NOT EXISTS race_series_id ON race (series_id);` |
| boat | Add index on `skipper_id` | `boat::find_boat_for_skipper`, `boat::remove_person`, FK check on person delete. | `CREATE INDEX IF NOT EXISTS boat_skipper_id ON boat (skipper_id);` |

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

- **Correctness.** `cargo xtask golden` scores every race and series in the
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
races are loaded, a race's results are 4, and each request adds a few session
statements that the count includes (`years` is a single `SELECT` and still shows 6).

What was done, by recommendation:

- **R1.** `race_results::results_boat_bracket_by_race_id(race_id, race_schedule)` takes the schedule
  the reporter already has. The results query keeps the exact SQL it had (`raceresults ⋈ race ⋈
  boat`, no `ORDER BY`), because tied cards keep their input order through the stable sorts in
  the reporter and a different row order would reorder them in the golden files.
- **R2.** `race::find_race_schedules(race_ids)` loads any number of schedules in 14 statements
  (races, image names, junctions, brackets, classes, class times, result counts, series, RC
  people, plus one grouped count each for brackets, classes, series and results).
  `find_race_schedule(id)` is the one-element case; `all_races(year)` and `get_standings_report` use
  the bulk form. Classes are now ordered by class sort then id, and brackets by min rating
  then id, instead of by whatever order the junction rows are stored in. That was necessary
  for R6: with a unique index led by `race_id` on the junction table the planner would read it in
  index order, which matches the stored order in only 17 of 118 races.
- **R3.** The derived counts stay in the API (the golden files and the admin pages use them) but
  come from one `GROUP BY` query per hydrated batch: `race_bracket_junction::race_counts_by_bracket`
  / `race_counts_by_class`, `race::race_counts_by_series`, `race_results::counts_by_race` /
  `race_counts_by_boat`. Single-item lookups (`bracket::find_bracket`, `race_class::select_by_id`, upserts) go through the
  same batch loaders with a one-element list. The boats page loads boats with a left join to
  `person` and one query each for certificates and race counts; `boat::find_boat_skippers(ids)`
  does the same for the schedule-results endpoint. The reporter looks up the ORC certificates of
  a race's results in one `IN` query instead of one per card.
- **R4.** `image::get_race_report_image_name` / `report_image_names` select `hash` and `race_id`
  only.
- **R5.** `race_time::all_years` selects only the two timestamp columns rather than whole rows. It
  still converts to Pacific years in the server (`time::pacific_year`), not with `EXTRACT`: the
  column is `timestamp without time zone` and the server-side conversion (`time::from_pg`) is what
  defines the stored value, so an SQL expression would have to replicate that convention for a
  saving of one statement. `boat::find_boat_skipper` is one left join.
- **R6.** Schema version 5 has the indexes in the table above and no `raceresults_end_date` or
  `checkin` table; `migrations/0001_baseline.sql` creates the same schema for fresh databases, and
  `startup::SCHEMA_VERSION` is 5. `race_bracket_junction::set_brackets` de-duplicates its input so the new unique index cannot reject a double-attached bracket.
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
checks the schema version) and the rest of the test suite are green.

## Tasks

- [x] R1: thread `RaceSchedule` through `results_boat_bracket_by_race_id`. Golden test.
- [x] R2: `find_race_schedules(ids)` in a fixed number of statements; used by `all_races(year)` and `get_standings_report`. Golden test.
- [x] R3: grouped counts for brackets, classes, series, boats and results; bulk-loaded certificates and skippers. Golden test.
- [x] R4: the `image` module selects only the name.
- [x] R5: `all_years` narrow select (server-side year conversion kept, see above); `find_boat_skipper` as one left join.
- [x] R6: the indexes in schema version 5 and in `migrations/0001_baseline.sql`, duplicate check on the snapshot (none).
- [x] Re-measured and recorded above; moved to `explore_implemented/`.
