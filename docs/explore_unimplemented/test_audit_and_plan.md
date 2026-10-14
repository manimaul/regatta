# Scoring & Placement Test Audit

Plan for unit test coverage of race scoring, placement, elapsed/corrected time and result labels,
measured against the CYCT General Sailing Instructions (GSI). Also records results in the dev
database (2024–2026 seasons) that look like they do not follow the rules.

## Sources

- **CYCT General Sailing Instructions 2026**, from the "General Sailing Instructions" link on
  `cyct.com/Racing` (`docs.ashx?id=1592986`). The signed S3 link to the 2025 PDF had expired. The
  scoring sections (1.4.1.2, 14.1–14.6) match the 2025 text quoted in `RaceResultReporter.kt`, word
  for word. The sections this plan uses:
  - **1.2** Late registration: a boat that does not complete its entry before its 2nd race is DNS in its 1st.
  - **1.4.1.2** An RC volunteer who cannot race gets the average of their series score for that
    race. It does not change other boats' scores, can be used once per series, and cannot be used
    as a tiebreaker.
  - **6.4** A boat that leaves without notifying the RC is scored DSQ, not DNF. DSQ is also
    allowed by 4.3, 4.4, 4.5, 6.5, 8.9 and 12.3.4.
  - **7.1** A rating change takes effect on the day it is recorded, and the boat stays in its fleet for that series.
  - **8.6** A boat that has not started within 5 minutes may be scored DNS.
  - **9.2 / 14.2** Windseekers OCS: 9.2 says *"(2) two points will be added to their finishing
    place"*; 14.2 says *"scored their class finish position +2 places"* (or restart). The two
    wordings differ, see D4. HOC-n is numbered by position on the course.
  - **14.1** Low Point. DNF = finishers + 1. DNS = starters + 1. RET = finishers + 1.
    NSC = starters + 1. DSQ = starters + 2.
  - **14.3** PHRF Time-on-Time.
  - **14.4** Series score is the sum of race scores, with one score excluded *if the series has 5 or more **started** races*.
  - **14.5** A series tie goes to the better finish in the last race.
  - **14.6** One completed race is a series.
  - Season trophies (pages 5–6): a different scoring method, with top-N races, discards for
    cancelled races and a count-back tiebreak. These are not implemented in the app, and are
    planned separately in [`season_trophies.md`](../explore_ideas/season_trophies.md).
- A copy of the PDF is kept next to this doc: `2026-CYCT-General-Sailing-Instructions.pdf`.
- RRS 2025–2028 Appendix A and rule 44.3(c), where the GSI does not override them. These cover
  tied places in a race, and a scoring penalty not being worse than DNF and not changing other
  boats' scores.

## Current coverage

| Test | What it covers |
|---|---|
| `server/.../RaceResultReporterKtTest.testHoc` | `place()` ordering for one mix of finish, HOC, DNF and DNS cards |
| `common/.../PhrfTest` | `PhrfBFactor.fromBFactor` ranges |
| `common/.../OrcTest` | ORC JSON decode, `findPreferred` cert selection |

There are no tests for elapsed or corrected time, TCF math, penalties, NSC, RET, DNS_RC, series
totals, throw-outs, tiebreaks, class vs. bracket placing, ORC placing, or any of the result labels.

## Where the scoring logic lives

| Concern | Location | Pure? |
|---|---|---|
| Elapsed time, PHRF TCF `650/(B+rating)`, ORC TCF selection, corrected time | `RaceResultReporter.reduceToCard`, `phrfTcf`, `orcTcf`, `boatCorrectedTime` (private) | Needs a DB for the certificate lookup |
| Race ordering | `compare`, `cardComparePhrf`, `cardCompareOrc` (top-level, public) | yes |
| Race places, penalties, finish-code scores | `Iterable<RaceReportCard>.place()` (public) | yes |
| Class/bracket placing, ORC placing | `classCard` (public) | yes |
| PHRF-only before 2026, orphan bucket | `getReport` | DB |
| Series standings, DNS, throw-out, RC average, tiebreak | `getStandingsReport`, `getStandingsBoatBracket`, `throwOutWorst`, `nonStarterPlace`, `tieBreaker` (private) | DB at the top, pure below |
| Rating helpers | `common`: `PhrfBFactor`, `Orc3Band/Orc5Band.fromPhrfBFactor`, `OrcScoringOption.orcBandLabel`, `OrcCertificate.virtualPhrf`, `Bracket.label`, `RatingType.ratedLabel`, `Duration.display` | yes |
| Result labels | `web/.../utils/RaceReport.kt` (`finishText`, `elapsedText`, `corTime*Text`), `RaceResultsView.kt` place cells, `SeriesStandings.kt` cells | yes / composable |

## Potentially non-compliant results found in the dev DB

I dumped `/report` for every race and `/series/standings` for every series and year from the dev
server (2024: 34 races, 2025: 34, 2026: 50). I re-scored them against the GSI and list every
difference below. Nothing has been changed. The decision for each finding is under
[Decisions](#decisions-answered-2026-10-14) (F1→D1 … F6→D6–D8), and the tests pin those decisions down.

### F1. RC volunteer average is shown but not counted (GSI 1.4.1.2)

`getStandingsBoatBracket` sets `placeInBracketCorrected` / `placeInClassCorrected` (the average),
and the standings page shows the raw score struck through with the average next to it. But
`totalScoreBracket` / `totalScoreClass` add up the **raw** `placeInBracket` (DNF score). The
throw-out is also chosen from the raw score, before the average is applied. 19 boat-series
entries use DNS_RC. 13 of them have totals that differ from what the screen implies, for example:

| Series | Boat | RC raw → avg | Total shown | Total with avg |
|---|---|---|---|---|
| 2025 Harbor Series III/IV | Slick | 7 → 4 | 15 | 12 |
| 2026 Summer Series WS, Half Fast IV | Boginka | 1 → 6 | 17 | 22 |
| 2025 Late Summer WS, Half Fast IV | Slick | 5 → 3 | 9 | 7 |

**Final places change** in these series when the average is counted (with the last-race tiebreak):
- 2025 Early Spring WS, Half Fast IV: Slick 2→3, Work Release 3→2
- 2025 Three Hour Tour, Half Fast IV: Slick 3→4, Solitaire 4→3
- 2025 Late Summer WS, Half Fast IV: Slick 3→1, Strider 1→2, Zephyr 2→3
- 2026 Summer Series WS, Fast II: Goes to 11 3→2, Flicka 2→3
- 2026 Late Summer WS, Fast II: Goes to 11 5→4, Bucket List 4→5

Related gaps:
- "Once per series" is not enforced. No data case uses it more than once.
- The average includes DNS and thrown-out races, and is rounded with `roundToInt`. The GSI says
  neither.
- The tiebreak reads the raw DNS_RC place, but the GSI says the RC score cannot be used as a tiebreaker.
- An RC score can be *better* than the raw score (Boginka above: raw 1, because nobody in bracket IV
  finished race 106).

### F2. Unsailed or cancelled races are scored and count toward the throw-out (GSI 14.4, 14.6)

`seriesRaces` returns every race that has a schedule, including races with no results (cancelled,
or still in the future). `nonStarterPlace` then gives every boat 0 + 1 = **1 point** for that
race, and the race counts toward the `size >= 5` throw-out rule.
- **2026 Early Spring Windseekers**: race #2 has no results, so only 4 races were started. The GSI
  says no throw-out, but one was applied. Places change: Half Fast III Bucket List 5→6,
  Amidala 6→5; Half Fast IV Work Release 2→3, Zephyr 3→2.
- 2026 Point Series #3/#4 (still in the future as of 2026-10-14) already show 1 point for every
  boat and are used as the "last race" in the tiebreak.
- Lido 14 series 2026 #2/#4 have no results either. That series has 13 sailed races, so the throw-out is legitimate there.
- 21 class-race cells score the whole class as DNS = 1 because nobody *in that class* entered,
  even though other classes raced (e.g. 2024 Harbor #1 Windseekers FS). This follows "starters + 1"
  literally, but the committee should confirm whether the race counts for that class at all.

### F3. NSC scored as starters, not starters + 1 (GSI 14.1.4)

`place()` gives NSC `starters` (the count of all cards, including the NSC boat itself). The GSI
says starters + 1, which is the same as DNS. This applies to all 10 NSC scores in the data, for
example 2025 Early Spring WS #3 Obsession: 7, but the rule gives 8. 2025 Awards Race Keet: class
8, but the rule gives 9. Right now an NSC scores one point *better* than not showing up.

Related: both `place()` and `nonStarterPlace` count the DNS_RC card as a starter. Under the D3
definition it is not one, so the DNS and NSC scores of every *other* boat drop by one in each race
with an RC volunteer. The dump has 19 DNS_RC rows across 18 races (race 78 has two). These shifts
belong in `deviations.json` under D3, or the per-decision isolation check in §5 fails on D3.

### F4. Penalty moves other boats up (GSI 9.2/14.2, RRS 44.3(c))

`place()` moves the penalised card down *n* places and moves every boat it passes **up** one place.
That is a literal reading of 14.2, "class finish position +2 places". But 9.2 says "2 points will
be added to their finishing place", and RRS 44.3(c) uses "places" the same way: "made worse by the
number of places" while "the scores of other boats shall not be changed", so tied scores are allowed.
- 2024 Early Spring WS #1, Windseekers FS: Klatawa P+2. Current scores are Blue Jay 2, Waymaker 3,
  Klatawa 4. The GSI gives Blue Jay 3, Waymaker 4, Klatawa 4.
- 2024 Late Spring WS #4 Kahuna **RET P+1** scored 6, and 2024 Summer Series WS #4 Gardyloo
  **RET P+1** scored 6. Both are one point worse than the DNF/RET score of 5. RRS 44.3(c) caps a
  penalty at DNF, and a penalty on a non-finisher has no clear meaning. The UI allows it. Both rows
  (ids 387 and 501) are legacy rows with a NULL `finish_code` and no finish time; they are RET only
  because of the row-mapping fallback described in F6.
- Code risk with no data case yet: when there is more than one penalty, the original sorted
  indexes are reused after the list has been changed. The penalty is also not limited to the
  Windseekers classes.

### F5. Series ties that the last race does not break get different places (GSI 14.5)

`tieBreaker` always takes the last-race branch, because every boat has an entry for every race.
That makes the count-back branch, the only one that records `tiedWith`, dead code. When the
last-race places are equal, the comparator returns 0, and the boats get **consecutive distinct
places** based on input order. 12 ties in the data are like this, for example:
- 2024 Harbor Series III/IV: Blue Max and Work Release have the same scores [7, 8, 3], but are placed 7th and 8th.
- 2024 Three Hour Tour I/II: Flash [1,3,2] and Flicka [2,2,2] are placed 2nd and 3rd.
- 2025 Late Summer WS Fast I: Flash and Zulu Tango both have 10 points and 3 in the last race, but are placed 3rd and 4th.
- 2026 Three Hour Tour I: Wicked Sister [2,2,2] and Zulu Tango [1,3,2] are placed 2nd and 3rd.

Under the GSI these should be tied, or broken by a rule the committee chooses, such as RRS A8.2
next-to-last race. The other 53 ties were broken by the last race in the right direction.

### F6. Other code-level rule gaps (no data case found)

- **No DSQ finish code.** GSI 14.1.5 (starters + 2), 4.3, 4.4, 4.5, 6.4, 6.5, 8.9 and 12.3.4 can't be recorded.
- **Legacy rows with no finish code.** 733 of the 2,093 result rows have a NULL `finish_code`. The
  row mapper in `RaceResultsTable.kt` turns a NULL code into TIME when a finish time exists and RET
  otherwise; 75 rows take the RET path. No test covers that fallback.
- **No DNS race code.** DNS only exists as "no record" in the series view, so the late-entry DNS in 1.2 and the 5-minute DNS in 8.6 can't be entered.
- **Equal corrected times in a race:** `place()` gives the tied boats the same place, but the next
  boat gets place+1 instead of skipping (1, 1, 2 instead of 1, 1, 3). RRS A7 averages the points
  for tied places. Comparison is done in whole milliseconds.
- **PHRF OCS** has no representation (RRS A5 applies, since 9.2 is Windseekers only).
- **Rating change mid-series (7.1):** the rating is stored on each result, which is correct, but
  boats are grouped in standings by `BoatSkipper`. A skipper change splits a boat into two rows,
  and a bracket change puts the boat in two brackets with DNS in each.
- A PHRF result with a `null` rating silently gets TCF 1.0.
- Year boundaries differ: `seriesRaces` uses UTC, while the ORC/PHRF switch uses `America/Los_Angeles`.
- ORC places (2026, `ORC_PHRF`) are shown per race but never used in series standings, and the GSI says nothing about ORC.
- **Label bug:** `RaceResultsView` shows `placeInBracketOrc` in the "Place In Class" column for
  `ORC_PHRF` cards. It should be `placeInClassOrc`.
- **Label ambiguity:** `Duration.display()` truncates to whole seconds, so boats with different
  places can show the same corrected time. There are 5 cases, e.g. 2026 Late Spring WS #5:
  Ripley and Amidala are 13th and 14th, both shown as `0h:54m:12s`.

## Test strategy

### 1. Make the scoring testable without a database

Scoring stays in `server`, and all scoring rules are tested as plain functions in
`:server:jvmTest`. Label logic stays in `web` and is tested in `:web:jsBrowserTest`. The shared
helpers (`Duration.display`, rating enums, `Bracket.label`) are tested in `:common:jvmTest`. The
refactor only adds seams and does not change behavior:

- Split `reduceToCard` into a pure `card(record, classStart, race, orcCert?)` plus a thin DB wrapper
  that looks up the certificate.
- Split `getStandingsReport` into a pure `standings(series, year, reports: List<RaceReport>, now: Instant)`
  plus the DB fetch. `now` is unused until D2. It is there so tests can later tell a cancelled
  race from one not yet sailed. Make `throwOutWorst`, `nonStarterPlace`, `tieBreaker` and
  `phrfTcf` / `orcTcf` / `boatCorrectedTime` `internal`.
- Keep the "which races count" decision inside `standings`, working from the reports. That is
  where D2's per-bracket and per-class sailed check will go, so it can be tested without a DB.
- Extract the 2025→2026 PHRF-only switch in `getReport` (the `America/Los_Angeles` year check and
  the `RatingType.PHRF` copy) into a pure `effectiveRaceClass(classSchedule): RaceClass`, so the
  time-zone test in layer 2 runs without a container.
- Split `getReport` into a pure `report(raceSchedule, cards): RaceReport` plus the DB fetch, so the
  fixture DSL can build `RaceReport`s for `standings` without a database. `getReport` now builds a
  card for every result up front instead of per class; the only visible difference is that a
  result with an `orcRef` whose certificate is missing fails even when no class would have scored
  it, which the golden test will surface if the data has such a row.
- Add a pure `validateResult(result: RaceResult): String?` that returns an error message or
  `null`. The `POST results` route answers 400 when it returns a message. It accepts everything
  until D8.

### 2. A small fixture DSL

Building `RaceReportCard` / `RaceResultBoatBracket` by hand is noisy, and the one existing test
already works around it. Add a test-only builder:

```kotlin
race(start = "18:00", bFactor = 600) {
    phrfClass("Fast") { bracket("I") {
        boat("A", rating = 48) finishes "20:51:26"
        boat("B", rating = 72) code DNF
        boat("C") hoc 1
        boat("D") finishes "20:55:00" penalty 2
    } }
}
series { race(...); race(...); rcVolunteer("A", race = 3) }
```

Builders read times as local Pacific times. Tests then assert the **score**, not the internal
`place` field, using helpers such as `assertScores("A" to 1, "B" to 4)`. The builder also needs a
`legacy()` form that produces a result with no `finishCode` set, to cover the NULL-code rows in F6.

### 3. Layers and what each must cover

Each layer is checked against the rule text. The test names should cite the section, e.g.
`gsi_14_1_4_nsc_scores_starters_plus_one`, so a failing test points at the rule it enforces.

| Layer | Module | Behavior to cover |
|---|---|---|
| **Rating math** | `common` | All `PhrfBFactor` / `Orc3Band` / `Orc5Band` band edges. `virtualPhrf`. `orcBandLabel` for every `OrcScoringOption`. `Bracket.label`, `RatingType.ratedLabel`. (`orcTcf` lives in `server`; see the next row.) |
| **Elapsed & corrected time** | server | `orcTcf` picks the right certificate field for each option × band (table-driven over all 13 options). Elapsed = finish − *class* start, with separate starts per class. ToT = elapsed × 650/(B + rating) for each B-factor. Cruising TCF = 1. Null rating on a legacy row (TCF 1.0, see D8). Finish before start. Over 24 h. Millisecond precision kept. ORC corrected time alongside PHRF for `ORC_PHRF`. The 2025→2026 switch to PHRF-only, tested in the Pacific time zone. |
| **Race placing (`place`, `classCard`)** | server | Ordering: corrected time → HOC position → finish-code weight. Each finish code's score formula (14.1.1–14.1.5) with "finishers" and "starters" defined explicitly, including HOC as finishers, and DNS_RC and DNS as not starters. HOC numbering (14.2). Penalty semantics (9.2/14.2, 44.3(c)): others unchanged, capped at DNF, several penalties, penalty on a non-finisher. Tied corrected times. A bracket with no finishers. Separate class and bracket placing. ORC placing only among ORC entries. Orphan results in "Incorrectly classed". |
| **Series standings** | server | DNS = starters + 1 per bracket and class. Throw-out only when 5 or more races were **started** (14.4), and which race is dropped when scores are equal. Unsailed and future races (14.6, F2). RC volunteer (1.4.1.2, D1): average used in the total, throw-out chosen from the other races first, once per series, no other races scored as DNS, skipped in the tiebreak, other boats unchanged. Tiebreak (14.5) including an unbroken last race (F5) and three-way ties. Class totals vs. bracket totals. Boats that change bracket or skipper. A single-race series. Result validation (`validateResult`, D8). |
| **Labels** | `web` jsTest (pure functions), plus `common` for `display()` | `finishText` for each code, HOC n, `P+n`, no start time. `elapsedText` / `corTime*Text` formatting including TCF to 4 decimals and `n/a`. `Duration.display` rounding (D7). Standings cell decoration (throw-out red, RC strike-through, the `DNS` / `DSQ` / `DNS RC Volunteer` / `HOC n` suffixes). Fractional scores (D1). "Cancelled" / "Not yet sailed" cells (D2). Race view place columns for PHRF / ORC / `ORC_PHRF` × single/multi bracket (catches the F6 column bug). Prefer pulling the cell-text logic into pure functions over composition tests, as in the CLAUDE.md note on `waitForRecompositionComplete`. |

### 4. Invariant (property-style) tests

Besides the hand-written cases, generate random fleets with the fixture DSL and check that these always hold:
- Shuffling the input order never changes any score. This catches F5 and any other dependence on sort stability.
- Timed finishers get places 1..k, with gaps only for ties. No score is below 1. DNF/RET equals finishers + 1. NSC/DNS are at least DNF.
- A penalty never changes another boat's score.
- A series total equals the sum of counted race scores, and at most one score is excluded.
- An RC volunteer entry never changes another boat's score or place.

Plain `kotlin.test` loops with a seeded `Random` are enough, so no new dependency is needed.

### 5. Golden regression from a production DB snapshot

The 2024–2026 data shows the scoring the club has accepted in practice. The golden tests load a
real production dump into a throwaway Postgres container, using **Testcontainers**, and run the
real server code against it. This covers everything the pure-function tests can't reach:
- the raw SQL in `seriesRaces`;
- the Exposed row mapping, e.g. `finish` is only kept for TIME;
- the bracket and class joins;
- `Migration.runMigration` plus `SchemaUtils` against a real older schema.

**The fixture: a cleaned dump**
- The source is `database/backup/prod_current.sql`. It is currently
  `prod_dump2026-10-13_19_10_11.sql`: about 208 KB of plain-text `pg_dump`, with schema version 4,
  no `image` rows and 3 `auth` rows. `database/backup/` is git-ignored, so the cleaned copy is
  what gets committed.
- `database/scrub_dump.py <in.sql> <out.sql>` is a small, dependency-free script. It:
  - **empties `public.auth`**, dropping every row between its `COPY` line and the `\.` terminator,
    so no password hashes are committed;
  - **removes the `\restrict` / `\unrestrict` lines** that new `pg_dump` versions emit. The
    `postgres:16` image's psql (16.10 or later) understands them, so this is only for older
    host-side clients, but it keeps the file loadable anywhere;
  - fails if the `image` table has rows, so binary data never lands in the repo by accident;
  - leaves everything else untouched. **Person names stay.**
- The output is committed as `server/src/jvmTest/resources/golden/regatta-2026-10-13.sql`.
  The date in the file name is the dump date, and the tests use it as `now` (see below).
- **`database/.gitignore` ignores `*.py`, `*.json` and `*.sql`.** Without a change, both scripts
  in this section would be silently untracked. Add `!scrub_dump.py` and `!audit_scoring.py` to it
  (the `backup/` and `*.sql` rules stay).

**The harness: `GoldenDb`**
- Add test dependencies on `org.testcontainers:testcontainers` and `org.testcontainers:postgresql`,
  with the version in `buildSrc/.../Versions.kt`. Use 1.21.x or newer, which runs on JDK 25.
  Testcontainers is used rather than a `podman run` from Gradle (as `makeImg` does) because it
  handles port allocation, the readiness wait, copying the dump in, `exec` and cleanup; the Exec
  route would need all of that by hand.
- Start one `PostgreSQLContainer("postgres:16-bookworm")` per test class, the same image as
  `database/docker-compose.yaml`, with `POSTGRES_USER=regatta_admin` and `POSTGRES_DB=regatta` so
  the dump's `OWNER TO regatta_admin` statements work. The official image trusts local connections,
  so `psql -U regatta_admin` inside the container needs no password.
- Load the dump with `psql` **inside the container**. Copy the file in with
  `copyFileToContainer`, then run `execInContainer("psql", "-v", "ON_ERROR_STOP=1", "-U",
  "regatta_admin", "-d", "regatta", "-f", "/dump.sql")`. JDBC can't run it, because the dump uses
  `COPY … FROM stdin`. A non-zero exit code fails the test with psql's stderr.
- Set the `jdbcurl` / `user` / `password` system properties from the container, then call
  `RegattaDatabase.init()`. That is the same startup path as production: migrations, then
  `SchemaUtils`. A test checks that the `metadata` version equals `currentDbVersion` afterwards.
  It checks only the version: the dump also carries a `checkin` table and an orphan
  `raceclasscategory_id_seq` that Exposed does not know about, and they load without harm.
- `RegattaDatabase` is a process-wide singleton, so the golden tests live in one class and don't run in parallel.
- **The clock is pinned.** `getStandingsReport` takes a `now: Instant` parameter that defaults to
  `Clock.System.now()`. Routes keep the default. The race report doesn't need one. The golden test
  passes the dump instant, `2026-10-14T02:10:11Z` (the `2026-10-13_19_10_11` Pacific stamp in the
  file name), so races in the future at dump time, 2026 Point Series #3 (race 119, Oct 17) and #4
  (race 121, Oct 31), always read as "Not yet sailed".

**Approved outputs**
- For every race id in the dump, the test serializes `getReport(id)` as pretty-printed JSON with
  stable key order. It also serializes `getStandingsReport(seriesId, year)` for every series ×
  {2024, 2025, 2026}. That is about 120 race files and about 25 standings files.
- Each output is compared to `golden/approved/race-<id>.json` /
  `golden/approved/standings-<seriesId>-<year>.json`. This is a small approval helper, with no new
  library:
  - On a mismatch, it writes `<name>.received.json` next to the approved file, which is
    git-ignored, and fails with a list of the files that differ.
  - Running with `-Dgolden.approve=true` overwrites the approved files with the received output,
    to bring them up to date on purpose.
- The approved files are first generated from **today's** code. They are a characterization of
  current behavior, deviations included.

**Known deviations**
- `database/audit_scoring.py` reads the approved JSON files, which contain both the inputs (each
  card's `resultRecord` and the race schedule) and the outputs. It re-scores them with the decided
  rules and writes `golden/deviations.json`. It covers:
  - the series and race scoring rules, D1–D5;
  - the D8 data changes: one row per boat id, and the Pacific year boundary.

  D6 adds codes that no stored result uses. The D7 changes either have no case in the data (exact
  ties) or live in web labels, which the golden JSON doesn't contain. Each entry is keyed by race or series id,
  boat id and decision, with the current value and the expected value. It is the same check that
  produced the findings above.
- When a fix lands:
  1. The golden test fails.
  2. Re-approve the outputs.
  3. Re-run the audit.
  4. The `deviations.json` diff must remove **only** that decision's entries. That semantic diff is
     the isolation check. The raw approved-file diff is a secondary signal: for D3, D4 and D5 it
     should touch only the affected races and series, but D1 (every score goes from `3` to `3.0`),
     D2 (new fields on every `StandingsRace`) and D8 (new grouping key) rewrite every standings
     file, so for those three only the `deviations.json` diff is checked.
  5. Expected cross-over: D2 removes some F5 entries early, because the unsailed 2026 Point Series
     #3/#4 are currently the "last race" in that series' tiebreak. The audit script keys an F5
     entry only on ties that remain unbroken after D2's exclusion, so this shows up as D2 work.

  This is what keeps a fix from silently changing unrelated results.

**Running it**
- The golden test needs a container runtime, so it gets its own Gradle task,
  `:server:jvmGoldenTest`. It is a `Test` task over the jvmTest classpath, filtered to
  `*GoldenTest`. `:server:jvmTest` excludes that class, so the everyday test run needs no podman.
- With podman on macOS, Testcontainers needs:
  - `DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')`;
  - `TESTCONTAINERS_RYUK_DISABLED=true`.

  The task sets both if they are not already set, finding the socket with `providers.exec` (safe
  for the configuration cache) and failing with a "start the podman machine" message when
  `podman machine inspect` fails. There is no `docker` CLI and no `~/.testcontainers.properties`
  on the dev machine, so both settings are required. Verified on podman 5.8.2 with the
  `podman-machine-default` applehv machine: the socket is
  `$TMPDIR/podman/podman-machine-default-api.sock`. The repo has no CI workflow; if one is added
  on Linux with Docker, neither setting is needed.
- **Refreshing the snapshot** after a new production backup:
  1. Run `scrub_dump.py` on the new dump.
  2. Replace the committed `.sql` and update the pinned `now`.
  3. Run with `-Dgolden.approve=true`.
  4. Review the diff. New races should show up as new files, and existing files should not change.

### 6. How the rule tests treat known violations

Rule tests (layer 3) assert the **decided rules** below, not what the code does now. Each test for a
finding that the code doesn't meet yet starts out `@Ignore("D3: see docs/explore_unimplemented/test_audit_and_plan.md")`.
It is un-ignored in the same change that fixes the code. Ignored tests still have to compile:
- Score assertions go through `assertScores`, which compares as `Double`, so they compile before
  and after D1.
- Tests that need API that doesn't exist yet are written as part of that fix task instead:
  - `FinishCode.DNS` / `DSQ` (D6);
  - the `StandingsRace` sailed/status fields (D2);
  - web label tests for those.

The golden test (5) records current behavior, and the rule tests (3) record the intended behavior.

## Decisions (answered 2026-10-14)

These replace the open questions. Where a decision interprets or goes further than the GSI text,
it is marked *(interpretation)*, so the committee can confirm the GSI wording later.

**D1. RC volunteer (F1, GSI 1.4.1.2)**
- The total uses the average in place of the raw DNF score.
- The average is taken over every *other* race in the series, excluding cancelled or unsailed
  races and the throw-out. DNS scores count in the average.
- The average is **fractional and not rounded**, e.g. 3.5. The race and series score fields
  (`StandingsRace.placeInBracket/Class(Corrected)`, `StandingsBoatSkipper.totalScoreBracket/Class`)
  become `Double` in `common`. Labels show whole numbers without a decimal (`4`), and others with
  one or two decimals (`3.5`, `3.33`).
- The bracket score and the class score are each averaged separately, from the bracket and
  class race scores.
- Order of operations:
  1. If the series has 5 or more started races (D2), choose the throw-out from the boat's other
     scored races. Otherwise there is no throw-out.
  2. Average the remaining races, leaving out the throw-out.
  3. If the RC average is worse than the chosen throw-out, the RC race becomes the throw-out instead.
     The average does not change. *(interpretation)*
- If the boat has no other scored race to average, the RC race scores **DNS** (starters + 1).
  Standings are computed on every request, so the average appears once the boat races again.
- Use is limited to once per series. The earliest DNS_RC race (by class start) gets the average,
  and any later ones score as DNS.
- "Sign up before the start of the series" stays a manual RC check.
- Other boats' scores never change.

**D2. Unsailed races (F2, GSI 14.4/14.6)**
- "Sailed" is decided **per scoring group**: the bracket for bracket scores, the class for class
  scores. A race with no starters (D3) in a group is **not sailed for that group**
  *(interpretation)*. It scores no points in that group, does not count toward that group's
  5-race throw-out threshold, and is not used in its tiebreak. So the same race can count for a
  boat's class score but not its bracket score.
- A race with no results at all stays in the standings as a column, but scores nothing and is
  excluded from the throw-out count and the tiebreak. It is labeled **"Not yet sailed"** if the
  race's latest class end time is after `now`, and **"Cancelled"** otherwise. The label gets its
  own cell state in `StandingsRace`, e.g. `sailed: Boolean` plus the race-level status.
- The throw-out applies when 5 or more races were **started** in that scoring group.
- Starters but no finishers: if a group had starters but nobody finished (2026 race 106, bracket
  IV, all DNF or RET), the rule is applied literally: DNF/RET = 0 + 1 = **1**. This is accepted
  as GSI 14.1.1, and the race counts as sailed.

**D3. NSC (F3, GSI 14.1.4)**
- NSC = starters + 1, the same as DNS.
- **Starters** means every entry except RC volunteers and DNS: finishers, HOC, DNF, RET, NSC and
  DSQ. DNS_RC entries, DNS entries (D6) and boats with no record are not starters. The same
  definition is used for DNS, NSC and DSQ.
- **Finishers** are TIME plus HOC entries, so DNF = RET = finishers + 1, and HOC boats rank after
  all timed finishers.
- Data effect: besides the 10 NSC rows, this lowers the DNS and NSC scores of other boats in the
  18 races that have an RC volunteer (see F3). `audit_scoring.py` lists them under D3.

**D4. Penalties (F4, GSI 9.2/14.2, RRS 44.3(c))**
- Score = finishing place + n, and **no other boat's score changes**, so ties are allowed. This
  follows the 9.2 wording ("points") and RRS 44.3(c) over the 14.2 wording ("places"), which the
  current code implements by moving other boats up. *(interpretation of the conflicting text)*
- The score is capped at the DNF score (finishers + 1). The cap comes from RRS 44.3(c), which
  governs a boat taking a scoring penalty, not OCS; the GSI itself has no cap. *(interpretation)*
- A penalty is allowed only on timed finishers and HOC. The UI blocks it on RET, DNF, NSC, DSQ
  and DNS, and the scorer ignores any that already exist. This fixes the 2024 Kahuna and Gardyloo
  RET P+1 scores, which become 5.
- Penalties are allowed in all classes.
- Several penalties in one race are applied independently, each against the boat's own unpenalised place.

**D5. Series tiebreak (F5, GSI 14.5)**
- Compare the last race. If it is equal, compare the next-to-last race, and so on back to the
  first race. If every race is equal, the boats **share the place** and the next place skips.
  GSI 14.5 names only the last race and explicitly replaces RRS A8.1 and A8.2; the count-back is
  A8.2's own rule applied anyway. *(interpretation)*
- The comparison uses raw race scores, including thrown-out races.
- A race where either tied boat has an RC volunteer score is skipped (GSI 1.4.1.2).
- Races that were not sailed (D2) are skipped.
- The same rule applies to bracket standings (bracket scores) and class standings (class scores).
- The result must not depend on input order.

**D6. New finish codes (F6)**
- Add `DSQ` (starters + 2, GSI 14.1.5) and `DNS` (starters + 1, GSI 1.2 and 8.6) as finish codes.
  `DNS` scores the same as having no record. Neither code allows a penalty.
- `finish_code` is stored as the enum **name** in a `varchar(128)`, so **no SQL migration** is
  needed, only the enum values.
- Weights set the display order among non-finishers: DNF/RET/HOC 1, NSC 2, DNS 3, DSQ 4,
  DNS_RC 5. Update the existing DNS_RC weight to match.
- `FinishCodeDrop` offers both codes. `setFinish` clears the penalty for them, as it does for NSC.
- Labels are `DNS` and `DSQ` in the race view (`finishText`) and in the standings suffix.

**D7. Race ties and time labels (F6)**
- Equal corrected times (to the millisecond) **share the place, and the next boat skips**: 1, 1, 3.
  *(interpretation)* GSI 14.1 adopts RRS Appendix A and does not override A7, which averages the
  tied places (1.5, 1.5, 3). Once D1 makes scores fractional, A7 costs nothing to follow; the
  committee should confirm which it wants. No data case has an exact tie.
- Elapsed and corrected time labels **round to the nearest second** instead of truncating. Places
  still use millisecond precision.
- Fix the "Place In Class" column bug, so it shows `placeInClassOrc` for `ORC_PHRF`.

**D8. Data and model edge cases (F6)**
- Series standings are grouped **by boat id**, not by boat + skipper. The row shows the skipper
  from the boat's most recent race in the series.
- A boat that changes bracket mid-series keeps one row per bracket. That is current behavior, and
  GSI 7.1 says the boat stays in its fleet, so this is an RC data-entry matter.
- `seriesRaces` selects races by year in `America/Los_Angeles`, not UTC, to match the ORC/PHRF switch in `getReport`.
- A PHRF or `ORC_PHRF` result without a PHRF rating is **rejected on save**:
  - the API, through `validateResult`, answers 400 with the message in the body;
  - the UI disables saving, and the result editor shows the message when a 400 does come back,
    so a rejected save never looks like a silent success.

  Old rows with no rating still score with TCF 1.0, so existing reports keep loading. The audit
  found none in the data. A test pins down that legacy behavior.
- ORC places stay per-race information. Series standings use PHRF only.

## Suggested order

1. Refactor seams and the fixture DSL (no behavior change; the existing `testHoc` still passes).
2. Rating math, elapsed/corrected time and labels.
3. Golden regression snapshot with the known-deviation list.
4. Race placing and series rule tests for D1–D8. Tests the code fails start out ignored.
5. Invariant tests.
6. Fix each decision in turn: un-ignore or write its tests, re-approve the golden outputs and
   re-run the audit, which removes that decision's known-deviation entries. Suggested fix order:
   1. D3 and D7, which are local to `place()`.
   2. D4.
   3. D6, which needs enum values and UI only, with no SQL migration.
   4. D2.
   5. D1 and D5, which need fractional scores.
   6. D8.

   The order follows the dependencies: D4's cap and D6's DNS/DSQ scores use D3's starter and
   finisher definitions; D1's average leaves out the races D2 marks unsailed; D5 skips the RC race
   from D1 and the unsailed races from D2.

Season trophy scoring is planned separately in `docs/explore_ideas/season_trophies.md`.

## Task checklist

Tick each box as it lands. When every box is ticked, the plan is complete. Each fix task in
phase 6 includes:
- un-ignoring its rule tests, or writing the ones that need the new API (see §6);
- if `:server:jvmGoldenTest` fails, re-approving with `-Dgolden.approve=true`;
- re-running `database/audit_scoring.py`;
- checking that the diffs touch only that decision's entries in `golden/deviations.json` and
  only those races and series in `golden/approved/`.

D6 and D7 should leave the golden outputs unchanged. D1, D2 and D8 re-approve every standings
file; for them the `deviations.json` diff is the only isolation check (see §5).

### Phase 1: Seams and fixtures (no behavior change) — done 2026-10-14

- [x] Extract a pure `card(record, classStart, race, orcCert?)` from `reduceToCard`, keeping a thin DB wrapper for the certificate lookup.
- [x] Extract a pure `standings(series, year, reports, now)` from `getStandingsReport`, keeping a thin DB wrapper. `getStandingsReport` gains `now: Instant = Clock.System.now()`, and the route keeps the default.
- [x] Extract the pure `effectiveRaceClass(classSchedule)` (the 2025→2026 PHRF-only switch) from `getReport`.
- [x] Extract the pure `report(raceSchedule, cards)` from `getReport` (see §1).
- [x] Make `throwOutWorst`, `nonStarterPlace`, `tieBreaker`, `phrfTcf`, `orcTcf` and `boatCorrectedTime` `internal`.
- [x] Add the pure `validateResult(result): String?` (`results/ResultValidation.kt`), which accepts everything for now, and wire it into the `POST results` route so a message returns 400 with the message as the body.
- [x] Move the cell-text logic for the race view place columns (`RaceResultsView.kt`) and the standings cells (`SeriesStandings.kt`) into pure functions in `web/.../utils/`: the `*PlaceLines` functions in `RaceReport.kt` and `StandingsReport.kt`. The D7 column bug is preserved there, with a comment.
- [x] Add the fixture DSL in `server/src/jvmTest/.../results/Fixture.kt`:
  - race, class, bracket and boat builders, with times as Pacific local times;
  - a `series {}` builder;
  - `rcVolunteer`;
  - a `legacy()` result form with no finish code, for the NULL-code rows;
  - the `assertScores` helper, which compares scores as `Double`, plus `assertBracketScores` / `assertClassScores` on a `RaceReport` and `assertBracketTotals` / `assertClassTotals` on a `StandingsSeries`.
- [x] Port `RaceResultReporterKtTest.testHoc` to the DSL. `:server:jvmTest` passes.
  Note: before Phase 1 `testHoc` was already failing (expected 1, got 3): its hand-built cards never
  set `result.finish`, so `place()` scored every card as RET. The "Current coverage" table above
  overstated it. The DSL port builds real records, and the two DNS cards were dropped because DNS has
  no race record; DNS is covered by the standings test `gsi_14_1_2_dns_scores_starters_plus_one`.

### Phase 2: Rating math, times and labels

- [ ] `:common:jvmTest` covers:
  - `PhrfBFactor`, `Orc3Band` and `Orc5Band` band edges;
  - `virtualPhrf`;
  - `orcBandLabel` for every option;
  - `Bracket.label`, including open-ended ranges;
  - `RatingType.ratedLabel`;
  - `Duration.display`, including over 24 h. The rounding test (D7) starts out ignored.
- [ ] `:server:jvmTest` has a table-driven `orcTcf` test: every `OrcScoringOption` × band maps to the right certificate field.
- [ ] `:server:jvmTest` covers elapsed and corrected time:
  - per-class start;
  - ToT for each B-factor;
  - cruising TCF = 1;
  - null rating on a legacy row (TCF 1.0);
  - finish before start;
  - over 24 h;
  - millisecond precision;
  - ORC alongside PHRF for `ORC_PHRF`;
  - the 2025→2026 switch through `effectiveRaceClass`, in the Pacific time zone;
  - the NULL `finish_code` fallback in `RaceResultsTable.rowToResult`: a finish time gives TIME, none gives RET.
- [ ] `:web:jsBrowserTest` covers:
  - `finishText` for every code, `HOC n`, `P+n` and no start time;
  - `elapsedText` / `corTime*Text`, including TCF to 4 decimals and `n/a`;
  - standings cell decoration (throw-out, RC strike-through, existing code suffixes);
  - race view place columns for PHRF / ORC / `ORC_PHRF` × single/multi bracket. The D7 column test starts out ignored.

### Phase 3: Golden regression (DB snapshot)

- [ ] Add `!scrub_dump.py` and `!audit_scoring.py` to `database/.gitignore`, which ignores `*.py`, `*.json` and `*.sql`.
- [ ] Write `database/scrub_dump.py`. It:
  - empties `public.auth`;
  - strips `\restrict` / `\unrestrict`;
  - fails if `image` has rows;
  - leaves person names as they are.
- [ ] Scrub `prod_dump2026-10-13_19_10_11.sql` and commit it as `server/src/jvmTest/resources/golden/regatta-2026-10-13.sql`.
- [ ] Add the Testcontainers `testcontainers` + `postgresql` test dependencies (1.21.x or newer), with the version in `Versions.kt`.
- [ ] Add the `GoldenDb` harness:
  - `postgres:16-bookworm` with `regatta_admin` / `regatta`;
  - the dump loaded with in-container `psql -v ON_ERROR_STOP=1`;
  - system properties set from the container;
  - `RegattaDatabase.init()`.
  - A test checks that the `metadata` version equals `currentDbVersion` after startup (version only, not table parity).
- [ ] Add the approval helper:
  - pretty-printed JSON with stable key order;
  - a `.received.json` file written on mismatch;
  - `-Dgolden.approve=true` to approve;
  - `*.received.json` git-ignored.
- [ ] Add `GoldenTest`. It covers every race from `allRaces(year)` and every series × 2024–2026, with `getStandingsReport`'s `now` pinned to `2026-10-14T02:10:11Z`.
- [ ] Generate and commit `golden/approved/` from today's code.
- [ ] Add the `:server:jvmGoldenTest` task:
  - filtered to `*GoldenTest`, and excluded from `:server:jvmTest`;
  - sets `DOCKER_HOST` from `podman machine inspect` (via `providers.exec`) and `TESTCONTAINERS_RYUK_DISABLED=true` when they are unset, and fails with a clear message when the machine is not running.
- [ ] Write `database/audit_scoring.py`. It reads `golden/approved/`, re-scores with the D1–D5 rules plus the D8 grouping and year-boundary checks, and writes `golden/deviations.json`.
- [ ] Commit `deviations.json`. Its entries match findings F1–F5 (including the D3 DNS/NSC shifts in the 18 RC-volunteer races) and any D8 cases in the data.
- [ ] `:server:jvmGoldenTest` passes locally on podman.

### Phase 4: Rule tests (tests the code fails start out `@Ignore("Dn: …")`)

These cover everything that compiles against today's API. Tests for DNS/DSQ codes, D2 status
fields and their labels are written in the D6 and D2 fix tasks (§6).

- [ ] Race placing:
  - ordering (corrected time → HOC → code weight);
  - the 14.1.1–14.1.4 formulas, with finishers and starters as in D3;
  - HOC numbering;
  - penalties (D4): others unchanged, DNF cap, several penalties, non-finishers;
  - equal times (D7);
  - starters but no finishers;
  - class vs. bracket placing;
  - ORC placing among ORC entries only;
  - the orphan bucket.
- [ ] Series standings:
  - DNS = starters + 1;
  - scores and throw-out for races that weren't sailed per bracket or class, and throw-out at 5 or more started races (D2), checked through scores and totals;
  - RC volunteer (D1): average, order of operations, no other races, once per series, fractional;
  - tiebreak (D5): back through earlier races, a shared place, RC and unsailed races skipped, three-way ties, class and bracket;
  - class vs. bracket totals;
  - grouping by boat (D8);
  - a single-race series.
- [ ] `validateResult` rejects a PHRF / `ORC_PHRF` result without a rating (D8).
- [ ] Web label rule tests for fractional scores (D1). These compile against `Double` once D1 lands, so they are written in the D1 fix task. Note it here so it isn't forgotten.

### Phase 5: Invariants

- [ ] Seeded random-fleet tests check that:
  - shuffling the input never changes a score;
  - places are contiguous apart from ties;
  - DNF/RET = finishers + 1;
  - NSC/DNS are at least the DNF score;
  - a penalty never changes another boat's score;
  - a series total equals the counted scores, with at most one excluded;
  - an RC volunteer never changes another boat's score or place.

### Phase 6: Fixes (in this order)

- [ ] **D3:** NSC = starters + 1, using the D3 definitions of starters and finishers in `place()` and `nonStarterPlace`.
- [ ] **D7:**
  - equal corrected times score 1, 1, 3;
  - `Duration.display` rounds to the nearest second;
  - the `ORC_PHRF` "Place In Class" column shows `placeInClassOrc`.
- [ ] **D4:**
  - the penalty adds points, others unchanged, capped at DNF;
  - penalties apply only to TIME and HOC, and the scorer ignores the rest;
  - the UI blocks the penalty for other codes.
- [ ] **D6:**
  - add `FinishCode.DNS` and `DSQ`, with the new weights;
  - add the scoring, `FinishCodeDrop` options, `setFinish` penalty clearing, and the race and standings labels;
  - write the DSQ = starters + 2 and DNS race-scoring tests and the `DNS` / `DSQ` label tests.
- [ ] **D2:**
  - sailed is decided per bracket and class;
  - unsailed races show "Cancelled" / "Not yet sailed", score nothing, and are excluded from the throw-out count and tiebreak;
  - add the `StandingsRace` sailed/status fields, using `standings`'s `now`;
  - write the status tests and the web "Cancelled" / "Not yet sailed" cell tests.
- [ ] **D1:**
  - score fields become `Double` in `common`, and the web labels format decimals (`4`, `3.5`, `3.33`);
  - the RC average follows the D1 rules, and totals use it;
  - write the fractional-label web tests.
- [ ] **D5:**
  - the tiebreak goes back race by race, and fully tied boats share the place;
  - RC and unsailed races are skipped;
  - the result does not depend on input order.
- [ ] **D8:**
  - group standings by boat id, showing the latest skipper;
  - `validateResult` rejects a PHRF / `ORC_PHRF` result without a rating, the UI disables save, and the result editor shows the 400 message;
  - `seriesRaces` selects by year in `America/Los_Angeles`.

### Done

- [ ] No `@Ignore` referencing this doc remains.
- [ ] Re-running `database/audit_scoring.py` on the approved outputs produces an empty `golden/deviations.json`.
- [ ] `:common:jvmTest`, `:common:jsTest`, `:server:jvmTest`, `:server:jvmGoldenTest` and `:web:jsBrowserTest` all pass.
- [ ] Update the Tests section of `CLAUDE.md`:
  - `:server:jvmGoldenTest` and its podman setup;
  - approving outputs with `-Dgolden.approve=true`;
  - `scrub_dump.py`, `audit_scoring.py` and the snapshot-refresh steps;
  - the fixture DSL.
- [ ] Move this doc to `docs/explore_implemented/`.
