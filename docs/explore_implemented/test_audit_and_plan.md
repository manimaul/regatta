# Scoring & Placement Test Audit

Plan for unit test coverage of race scoring, placement, elapsed/corrected time and result labels,
measured against the CYCT General Sailing Instructions (GSI). Also records results in the dev
database (2024–2026 seasons) that look like they do not follow the rules.

## Sources

- **CYCT General Sailing Instructions 2026**, from the "General Sailing Instructions" link on
  `cyct.com/Racing` (`docs.ashx?id=1592986`). The signed S3 link to the 2025 PDF had expired. The
  scoring sections (1.4.1.2, 14.1–14.6) match the 2025 text word for word (GSI 14.5 is quoted in
  `crates/scoring/src/reporter.rs`). The sections this plan uses:
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
    planned separately in [`season_trophies.md`](../explore_unimplemented/season_trophies.md).
- A copy of the PDF is kept next to this doc: `2026-CYCT-General-Sailing-Instructions.pdf`.
- RRS 2025–2028 Appendix A and rule 44.3(c), where the GSI does not override them. These cover
  tied places in a race, and a scoring penalty not being worse than DNF and not changing other
  boats' scores.

## Current coverage

| Test | What it covers |
|---|---|
| `crates/scoring/tests/race_result_reporter_kt_test.rs` `test_hoc` | `place()` ordering for one mix of finish, HOC, DNF and DNS cards |
| `crates/common/tests/phrf_test.rs` | `PhrfBFactor::from_b_factor` ranges |
| `crates/common/tests/orc_test.rs` | ORC JSON decode, `find_preferred` cert selection |

There are no tests for elapsed or corrected time, TCF math, penalties, NSC, RET, DNS_RC, series
totals, throw-outs, tiebreaks, class vs. bracket placing, ORC placing, or any of the result labels.

## Where the scoring logic lives

| Concern | Location | Pure? |
|---|---|---|
| Elapsed time, PHRF TCF `650/(B+rating)`, ORC TCF selection, corrected time | `reduce_to_card` (`crates/service/src/reporter.rs`), `card`, `phrf_tcf`, `orc_tcf`, `boat_corrected_time` (`crates/scoring/src/reporter.rs`) | `card` is; the wrapper needs a DB for the certificate lookup |
| Race ordering | `compare`, `card_compare_phrf`, `card_compare_orc` (`crates/scoring/src/place.rs`) | yes |
| Race places, penalties, finish-code scores | `place()` (`crates/scoring/src/place.rs`) | yes |
| Class/bracket placing, ORC placing | `class_card` (`crates/scoring/src/reporter.rs`) | yes |
| PHRF-only before 2026, orphan bucket | `effective_race_class`, `report`; `get_report` fetches | pure, DB at the top |
| Series standings, DNS, throw-out, RC average, tiebreak | `get_standings_report` (fetch), `standings`, `get_standings_boat_bracket`, `group_scores`, `tie_breaker` | DB at the top, pure below |
| Rating helpers | `regatta-common`: `PhrfBFactor`, `Orc3Band::from_phrf_b_factor` / `Orc5Band::from_phrf_b_factor`, `OrcScoringOption::orc_band_label`, `OrcCertificate::virtual_phrf`, `Bracket::label`, `RatingType::rated_label`, `display` (durations, `crates/common/src/utils.rs`) | yes |
| Result labels | `crates/app/src/utils/race_report.rs` (`card_finish_text`, `elapsed_text`, `cor_time_*_text`, `*_place_lines`), `crates/app/src/utils/standings_report.rs` (standings cells), used by `routes/race_results_view.rs` and `routes/series_standings.rs` | yes / component |

## Potentially non-compliant results found in the dev DB

I dumped `/report` for every race and `/series/standings` for every series and year from the dev
server (2024: 34 races, 2025: 34, 2026: 50). I re-scored them against the GSI and list every
difference below. Nothing has been changed. The decision for each finding is under
[Decisions](#decisions-answered-2026-10-14) (F1→D1 … F6→D6–D8), and the tests pin those decisions down.

### F1. RC volunteer average is shown but not counted (GSI 1.4.1.2)

`get_standings_boat_bracket` set `placeInBracketCorrected` / `placeInClassCorrected` (the average),
and the standings page shows the raw score struck through with the average next to it. But
`totalScoreBracket` / `totalScoreClass` add up the **raw** `placeInBracket` (DNF score). The
throw-out is also chosen from the raw score, before the average is applied. 19 boat-series
entries use DNS_RC. 13 of them have totals that differ from what the screen implies, for example:

| Series | Boat | RC raw → avg | Total shown | Total with avg |
|---|---|---|---|---|
| 2025 Harbor Series III/IV | Slick | 7 → 4 | 15 | 12 |
| 2026 Summer Series WS, Half Fast IV | Boginka | 1 → 6 | 17 | 22 |
| 2025 Late Summer WS, Half Fast IV | Slick | 5 → 3 | 9 | 7 |

The "total with avg" column uses a rounded average over all other races, as the code shows it.
The exact D1 values (fractional, throw-out excluded) are in `golden/deviations.json`; e.g.
Boginka's 2026 Summer Series bracket total becomes 21.33, not 22.

**Final places change** in these series when the average is counted (with the last-race tiebreak):
- 2025 Early Spring WS, Half Fast IV: Slick 2→3, Work Release 3→2
- 2025 Three Hour Tour, Half Fast IV: Slick 3→4, Solitaire 4→3
- 2025 Late Summer WS, Half Fast IV: Slick 3→1, Strider 1→2, Zephyr 2→3
- 2026 Summer Series WS, Fast II: Goes to 11 3→2, Flicka 2→3
- 2026 Late Summer WS, Fast II: Goes to 11 5→4, Bucket List 4→5

Related gaps:
- "Once per series" is not enforced. No data case uses it more than once.
- The average includes DNS and thrown-out races, and is rounded to a whole number. The GSI says
  neither.
- The tiebreak reads the raw DNS_RC place, but the GSI says the RC score cannot be used as a tiebreaker.
- An RC score can be *better* than the raw score (Boginka above: raw 1, because nobody in bracket IV
  finished race 106).

### F2. Unsailed or cancelled races are scored and count toward the throw-out (GSI 14.4, 14.6)

`series_races` returns every race that has a schedule, including races with no results (cancelled,
or still in the future). The no-record (DNS) score then gives every boat 0 + 1 = **1 point** for that
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
- **One throw-out for two totals.** The throw-out selection picks the race with the worst *bracket* score
  and flags it as the throw-out for the class total as well. When the boat's worst class score is
  in a different race, the class total keeps a worse score than it should. The audit script found
  22 boat-series class totals like this, e.g. 2024 Early Spring WS Slick: class total 30, but 29
  with the worst class score dropped. D2 decides the throw-out per scoring group.

### F3. NSC and the starter count (GSI 14.1.4)

`place()` gives NSC the count of all cards, including the NSC boat itself and any RC volunteer.
Read as "the other starters + 1" this is right whenever no RC volunteer is in the group, so the
10 NSC scores in the data are correct on their own (2025 Early Spring WS #3 Obsession 7, 2025
Awards Race Keet class 8, 2026 Late Spring WS #2 Athena 6).

*Revised 2026-10-14.* The first version of this finding read "starters + 1" as including the NSC
boat, which would have made every NSC score one worse than DNS: Athena would score 7 for starting
and not sailing the course but 6 for staying home. D3 now counts the other starters, see there.

The real defect: both `place()` and the no-record (DNS) score count the DNS_RC card as a starter. Under the
D3 definition it is not one, so the DNS and NSC scores of every *other* boat drop by one in each
race with an RC volunteer. The dump has 19 DNS_RC rows across 18 races (race 78 has two). These
shifts belong in `deviations.json` under D3, or the per-decision isolation check in §5 fails on D3.

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

`tie_breaker` always takes the last-race branch, because every boat has an entry for every race.
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
  row mapper (`finish_code` in `crates/service/src/db/race_results.rs`) turns a NULL code into TIME when a finish time exists and RET
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
- Year boundaries differ: `series_races` uses UTC, while the ORC/PHRF switch uses `America/Los_Angeles`.
- ORC places (2026, `ORC_PHRF`) are shown per race but never used in series standings, and the GSI says nothing about ORC.
  *(D9, 2026-10-15: they now score a series alongside the PHRF places; see `orc_series_standings.md`.)*
- The 2025→2026 switch only relabels the class as PHRF, which hides the ORC columns in the web
  view. `class_card` still computes ORC places from each result's own rating type, so
  `hasOrcResults` and the `*Orc` places are set on 2025 reports too. Harmless, since nothing
  reads them for 2025, and pinned by `season_switch_relabels_the_class_but_still_computes_orc_places` in `crates/scoring/tests/corrected_time_test.rs`.
- **Label bug:** the race view (`RaceResultsView`) shows `placeInBracketOrc` in the "Place In Class" column for
  `ORC_PHRF` cards. It should be `placeInClassOrc`.
- **Label ambiguity:** the duration `display()` truncates to whole seconds, so boats with different
  places can show the same corrected time. There are 5 cases, e.g. 2026 Late Spring WS #5:
  Ripley and Amidala are 13th and 14th, both shown as `0h:54m:12s`.

## Test strategy

### 1. Make the scoring testable without a database

Scoring lives in `regatta-scoring` (`crates/scoring`), and all scoring rules are tested as plain
functions there (`cargo test -p regatta-scoring`). Label logic lives in `regatta-app` and is
tested with `cargo test -p regatta-app`. The shared helpers (duration `display`, rating enums,
`Bracket::label`) are tested in `regatta-common`. The refactor only adds seams and does not change
behavior:

- Split `reduce_to_card` into a pure `card(record, class_start, race, orc_cert)` plus a thin DB
  wrapper (`crates/service/src/reporter.rs`) that looks up the certificate.
- Split `get_standings_report` into a pure `standings(series, year, reports: &[RaceReport], now: Timestamp)`
  plus the DB fetch. `now` is unused until D2. It is there so tests can later tell a cancelled
  race from one not yet sailed. Make `tie_breaker` and `phrf_tcf` / `orc_tcf` /
  `boat_corrected_time` public to the tests.
- Keep the "which races count" decision inside `standings`, working from the reports. That is
  where D2's per-bracket and per-class sailed check will go, so it can be tested without a DB.
- Extract the 2025→2026 PHRF-only switch in `get_report` (the `America/Los_Angeles` year check and
  the `RatingType::Phrf` copy) into a pure `effective_race_class(class_schedule) -> RaceClass`, so the
  time-zone test in layer 2 runs without a container.
- Split `get_report` into a pure `report(race_schedule, cards) -> RaceReport` plus the DB fetch, so the
  fixture DSL can build `RaceReport`s for `standings` without a database. `get_report` builds a
  card for every result up front instead of per class; the only visible difference is that a
  result with an `orcRef` whose certificate is missing fails even when no class would have scored
  it, which the golden test will surface if the data has such a row.
- Add a pure `validate_result(result: &RaceResult) -> Option<String>` that returns an error
  message or `None`. The `POST results` route answers 400 when it returns a message. It accepts everything
  until D8.

### 2. A small fixture DSL

Building `RaceReportCard` / `RaceResultBoatBracket` by hand is noisy, and the one existing test
already works around it. Add a test-only builder (`crates/scoring/src/fixture.rs`):

```rust
race_with(RaceOpts { start: "18:00".into(), b_factor: 600, ..Default::default() }, |r| {
    r.phrf_class("Fast", |c| {
        c.bracket("I", |b| {
            b.boat("A", 48).finishes("20:51:26");
            b.boat("B", 72).code(Dnf);
            b.boat("C", None).hoc(1);
            b.boat("D", None).finishes("20:55:00").penalty(2);
        })
    })
});
series(|s| { s.race(|r| { /* ... */ }); s.race(|r| { /* ... */ }); s.rc_volunteer("A", 3); })
```

Builders read times as local Pacific times. Tests then assert the **score**, not the internal
`place` field, using helpers such as `assert_bracket_scores(&[("A", 1), ("B", 4)])`. The builder also needs a
`legacy()` form that produces a result with no `finishCode` set, to cover the NULL-code rows in F6.

### 3. Layers and what each must cover

Each layer is checked against the rule text. The test names should cite the section, e.g.
`gsi_14_1_4_nsc_scores_starters_plus_one`, so a failing test points at the rule it enforces.

| Layer | Module | Behavior to cover |
|---|---|---|
| **Rating math** | `regatta-common` | All `PhrfBFactor` / `Orc3Band` / `Orc5Band` band edges. `virtual_phrf`. `orc_band_label` for every `OrcScoringOption`. `Bracket::label`, `RatingType::rated_label`. (`orc_tcf` lives in `regatta-scoring`; see the next row.) |
| **Elapsed & corrected time** | `regatta-scoring` | `orc_tcf` picks the right certificate field for each option × band (table-driven over all 13 options). Elapsed = finish − *class* start, with separate starts per class. ToT = elapsed × 650/(B + rating) for each B-factor. Cruising TCF = 1. Null rating on a legacy row (TCF 1.0, see D8). Finish before start. Over 24 h. Millisecond precision kept. ORC corrected time alongside PHRF for `ORC_PHRF`. The 2025→2026 switch to PHRF-only, tested in the Pacific time zone. |
| **Race placing (`place`, `class_card`)** | `regatta-scoring` | Ordering: corrected time → HOC position → finish-code weight. Each finish code's score formula (14.1.1–14.1.5) with "finishers" and "starters" defined explicitly, including HOC as finishers, and DNS_RC and DNS as not starters. HOC numbering (14.2). Penalty semantics (9.2/14.2, 44.3(c)): others unchanged, capped at DNF, several penalties, penalty on a non-finisher. Tied corrected times. A bracket with no finishers. Separate class and bracket placing. PHRF and ORC placing each among their own rated entries (R1 / D9); an ORC-only boat has no PHRF place and is not a PHRF starter. Orphan results in "Incorrectly classed". |
| **Series standings** | `regatta-scoring` | DNS = starters + 1 per bracket and class. The same rules again for the ORC bracket and class groups of an ORC class, for ORC-rated rows only (D9: ORC DNS = ORC starters + 1, separate throw-out, RC average, tiebreak and places; PHRF and ORC fields zero where a row is not rated for them; nothing before 2026). Throw-out only when 5 or more races were **started** (14.4), and which race is dropped when scores are equal. Unsailed and future races (14.6, F2). RC volunteer (1.4.1.2, D1): average used in the total, throw-out chosen from the other races first, once per series, no other races scored as DNS, skipped in the tiebreak, other boats unchanged. Tiebreak (14.5) including an unbroken last race (F5) and three-way ties. Class totals vs. bracket totals. Boats that change bracket or skipper. A single-race series. Result validation (`validate_result`, D8). |
| **Labels** | `regatta-app` (pure functions), plus `regatta-common` for `display()` | `card_finish_text` for each code, HOC n, `P+n`, no start time. `elapsed_text` / `cor_time_*_text` formatting including TCF to 4 decimals and `n/a`. Duration `display` rounding (D7). Standings cell decoration (throw-out red, RC strike-through, the `DNS` / `DSQ` / `DNS RC Volunteer` / `HOC n` suffixes). Fractional scores (D1). "Cancelled" / "Not yet sailed" cells (D2). Race view place columns for PHRF / ORC / `ORC_PHRF` × single/multi bracket (catches the F6 column bug). Prefer pulling the cell-text logic into pure functions over component tests. |

### 4. Invariant (property-style) tests

Besides the hand-written cases, generate random fleets with the fixture DSL and check that these always hold:
- Shuffling the input order never changes any score. This catches F5 and any other dependence on sort stability.
- Timed finishers get places 1..k, with gaps only for ties. No score is below 1. DNF/RET equals finishers + 1. NSC/DNS are at least DNF.
- A penalty never changes another boat's score.
- A series total equals the sum of counted race scores, and at most one score is excluded.
- An RC volunteer entry never changes another boat's score or place.

proptest strategies generate the fleets (and shrink a failing one); a small seeded PRNG shuffles the entries, so a failing shuffle can be replayed.

### 5. Golden regression from a production DB snapshot

The 2024–2026 data shows the scoring the club has accepted in practice. The golden tests load a
real production dump into a throwaway Postgres container, using the **testcontainers** crate, and run the
real server code against it. This covers everything the pure-function tests can't reach:
- the raw SQL in `series_races`;
- the sqlx row mapping in `crates/service/src/db/`, e.g. the NULL `finish_code` fallback;
- the bracket and class joins;
- `startup::prepare`, the schema version check, against a real production schema.

**The fixture: a cleaned dump**
- The source is `database/backup/prod_current.sql`. The snapshot came from
  `prod_dump2026-10-13_19_10_11.sql`: about 208 KB of plain-text `pg_dump`, with
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
- The output is committed as `crates/scoring/tests/golden/regatta-2026-10-13-v5.sql`, at schema
  version 5, the only version the server's startup check accepts.
  The date in the file name is the dump date, and the tests use it as `now` (see below).
- **`database/.gitignore` ignores `*.py`, `*.json` and `*.sql`.** Without a change, both scripts
  in this section would be silently untracked. Add `!scrub_dump.py` and `!audit_scoring.py` to it
  (the `backup/` and `*.sql` rules stay).

**The harness: `GoldenDb`** (`crates/server/tests/support/mod.rs`)
- A dev dependency on the `testcontainers` crate, with the version in the workspace `Cargo.toml`.
  testcontainers is used rather than shelling out to `podman run` because it handles port
  allocation, the readiness wait, copying the dump in, `exec` and cleanup; a plain `podman run`
  would need all of that by hand.
- `support::start()` starts a fresh `GenericImage::new("postgres", "16-bookworm")`, the same image as
  `database/docker-compose.yaml`, with `POSTGRES_USER=regatta_admin` and `POSTGRES_DB=regatta` so
  the dump's `OWNER TO regatta_admin` statements work. The official image trusts local connections,
  so `psql -U regatta_admin` inside the container needs no password.
- Load the dump with `psql` **inside the container**. Copy the file in with
  `with_copy_to("/dump.sql", ...)`, then run `exec(ExecCommand::new(["psql", "-v", "ON_ERROR_STOP=1",
  "-U", "regatta_admin", "-d", "regatta", "-f", "/dump.sql"]))`. sqlx can't run it, because the dump
  uses `COPY … FROM stdin`. A non-zero exit code fails the test with psql's stderr.
- Connect with `startup::connect` to the container's URL, then call `startup::prepare`. That is
  the same startup path as production. The test checks that `startup::schema_version` equals
  `startup::SCHEMA_VERSION` afterwards. It checks only the version: the dump also carries an
  orphan `raceclasscategory_id_seq` and other historical object names, and they load without harm.
- Each `support::start()` call gets its own container, so tests never share a database.
- **The clock is pinned.** `get_standings_report` takes a `now: Timestamp` parameter. Routes pass
  the server's clock (`Config::now`, the wall clock unless `REGATTA_FIXED_NOW` is set). The race
  report doesn't need one. The golden test passes the dump instant (`DUMP_INSTANT`), `2026-10-14T02:10:11Z` (the `2026-10-13_19_10_11` Pacific stamp in the
  file name), so races in the future at dump time, 2026 Point Series #3 (race 119, Oct 17) and #4
  (race 121, Oct 31), always read as "Not yet sailed".

**Approved outputs**
- For every race id in the dump, the test serializes `get_report(id)` as pretty-printed JSON with
  stable key order. It also serializes `get_standings_report(series_id, year)` for every series ×
  {2024, 2025, 2026}. That is about 120 race files and about 25 standings files.
- Each output is compared to `golden/approved/race-<id>.json` /
  `golden/approved/standings-<seriesId>-<year>.json`. This is a small approval helper, with no new
  library:
  - On a mismatch, it writes `<name>.received.json` next to the approved file, which is
    git-ignored, and fails with a list of the files that differ.
  - There is no re-approve mode: an approved file changes only by deliberately copying its
    `.received.json` over it.
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
- The golden test needs a container runtime. It is `crates/server/tests/golden.rs`, run with
  `cargo xtask golden` (`cargo test -p regatta-server --test golden`); the scoring, common and app
  crates' tests need no podman.
- With podman on macOS, testcontainers needs:
  - `DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')`;
  - `TESTCONTAINERS_RYUK_DISABLED=true`.

  `support::start()` sets both if they are not already set (and skips `DOCKER_HOST` when
  `/var/run/docker.sock` exists), finding the socket with `podman machine inspect` and failing with
  a "start the podman machine" message when that fails. There is no `docker` CLI and no `~/.testcontainers.properties`
  on the dev machine, so both settings are required. Verified on podman 5.8.2 with the
  `podman-machine-default` applehv machine: the socket is
  `$TMPDIR/podman/podman-machine-default-api.sock`. The repo has no CI workflow; if one is added
  on Linux with Docker, neither setting is needed.
- **Refreshing the snapshot** after a new production backup:
  1. Run `scrub_dump.py` on the new dump into `crates/scoring/tests/golden/`.
  2. Update `DUMP` and `DUMP_INSTANT` in `crates/server/tests/support/mod.rs` (and the
     `RACE_FILES` / `STANDINGS_FILES` counts in `crates/server/tests/golden.rs`).
  3. Run `cargo xtask golden` and write the approved files for the new races from their
     `.received.json` output.
  4. Review the diff. New races should show up as new files, and existing files must not change.

### 6. How the rule tests treat known violations

Rule tests (layer 3) assert the **decided rules** below, not what the code does now. Each test for a
finding that the code doesn't meet yet starts out `#[ignore = "D3: see docs/explore_implemented/test_audit_and_plan.md"]`.
It is un-ignored in the same change that fixes the code. Ignored tests still have to compile:
- Score assertions go through `assert_scores`, which compares as `f64`, so they compile before
  and after D1.
- Tests that need API that doesn't exist yet are written as part of that fix task instead:
  - `FinishCode::Dns` / `Dsq` (D6);
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
- The average is **rounded to the nearest tenth of a point, 0.05 upward** (revised 2026-10-14),
  e.g. 2.67 becomes 2.7 and 2.75 becomes 2.8. GSI 1.4.1.2 says nothing about rounding and amends
  only 14.4, 14.5 and the Appendix A sections they cite; 14.1 applies RRS Appendix A, so the
  rule in force for scoring a boat by an average is A10(a), which rounds this way. Whole-number
  rounding was considered and rejected: it is in no rule, and nearest-whole or round-up would
  manufacture ties against the volunteer in the data (2025 Early Spring WS Slick / Work Release,
  2025 Late Spring WS Keet / Scoundrel). `round_to_tenth` in `crates/scoring/src/reporter.rs`. The race and
  series score fields (`StandingsRace.placeInBracket/Class`, `StandingsBoatSkipper.totalScoreBracket/Class`)
  are `f64` in `regatta-common`. Labels show whole numbers without a decimal (`4`), and others with
  one decimal (`3.5`, `2.7`).
- The bracket score and the class score are each averaged separately, from the bracket and
  class race scores.
- Order of operations:
  1. If the series has 5 or more started races (D2), choose the throw-out from the boat's other
     scored races. Otherwise there is no throw-out.
  2. Average the remaining races, leaving out the throw-out.

  The RC race is never the throw-out: the average of the remaining races cannot be worse than
  the worst of them, so an earlier "if the average is worse, throw out the RC race instead" step
  could never apply and was dropped (Phase 4, `d1_rc_race_is_never_the_throw_out`).
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
  own cell state in `StandingsRace`, e.g. `sailed: bool` plus the race-level status.
- The throw-out applies when 5 or more races were **started** in that scoring group.
- The throw-out is chosen **per scoring group**: the worst bracket score for the bracket total and
  the worst class score for the class total. They can be different races. *(interpretation of
  14.4 "one score will be excluded", applied to each standing separately)*
- Starters but no finishers: if a group had starters but nobody finished (2026 race 106, bracket
  IV, all DNF or RET), the rule is applied literally: DNF/RET = 0 + 1 = **1**. This is accepted
  as GSI 14.1.1, and the race counts as sailed.

**D3. NSC (F3, GSI 14.1.4)**
- NSC = starters + 1, the same as DNS.
- **Starters** means every entry except RC volunteers and DNS: finishers, HOC, DNF, RET, NSC and
  DSQ. DNS_RC entries, DNS entries (D6) and boats with no record are not starters. The same
  definition is used for DNS, NSC and DSQ.
- **A boat's own score counts the other starters** (revised 2026-10-14). A DNF boat is not among
  the finishers it is scored against, and a DNS boat is not among the starters; the same reading
  applies to NSC and DSQ, so an NSC or DSQ boat is a starter for everyone else but not for itself.
  NSC therefore equals DNS from the boat's own point of view, and DSQ is one point worse than DNS.
  With `n` other starters: DNS = NSC = n + 1, DSQ = n + 2. Counting the boat itself would make NSC
  one worse than staying home and DSQ two worse than DNS, which the GSI ladder does not suggest.
- **Finishers** are TIME plus HOC entries, so DNF = RET = finishers + 1, and HOC boats rank after
  all timed finishers.
- Data effect: the 10 NSC rows keep their scores where no RC volunteer is in the group; the change
  lowers the DNS and NSC scores of other boats in the 18 races that have an RC volunteer (see F3).
  `audit_scoring.py` lists them under D3.

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
- `FinishCodeDrop` offers both codes. `set_finish` clears the penalty for them, as it does for NSC.
- Labels are `DNS` and `DSQ` in the race view (`card_finish_text`) and in the standings suffix.

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
- `series_races` selects races by year in `America/Los_Angeles`, not UTC, to match the ORC/PHRF switch in `get_report`.
- A PHRF or `ORC_PHRF` result without a PHRF rating is **rejected on save**:
  - the API, through `validate_result`, answers 400 with the message in the body;
  - the UI disables saving, and the result editor shows the message when a 400 does come back,
    so a rejected save never looks like a silent success.

  Old rows with no rating still score with TCF 1.0, so existing reports keep loading. The audit
  found none in the data. A test pins down that legacy behavior.
- ~~ORC places stay per-race information. Series standings use PHRF only.~~ Replaced by D9.

**D9. ORC series scoring and the rating boundaries (2026-10-15)** *(interpretation: the GSI says
nothing about ORC)*. The full design, data cases and tests are in
`docs/explore_implemented/orc_series_standings.md`; the decisions are:
- **R1.** In a rated class each rating is placed among its own entries: PHRF places among the
  PHRF-rated entries, ORC places among the ORC-rated ones. An ORC-only boat has no PHRF place
  and is not a PHRF starter or finisher for anyone else's score; a PHRF-only boat has no ORC
  place. Cruising classes place every entry. A bracket report lists the PHRF-placed boats by
  place, then the ORC-only boats by ORC place.
- **R2.** A result rated ORC or ORC/PHRF is rejected on save without a certificate, as D8
  rejects a PHRF result without a rating. Old rows keep the TCF 1.0 fallback.
- **R3.** The class report carries the race's ORC scoring option and bands, so the race view's
  heading describes the scoring that was applied.
- **R4.** The PHRF B factor is the authority on the wind: the ORC band the option uses must be
  the one the B factor maps to (`validate_race`, 400 on both race save routes), the web's config
  edits derive the bands from the B factor, and the results editor has a Config modal. The
  stored rows were brought into line with the five-band windward/leeward option by
  `database/fix_orc_bands.py` on 2026-10-15, which changed the 2026 ORC corrected times of the
  races that had been scored with stale bands.
- **Series scoring.** In an ORC-rated class (from 2026, after `effective_race_class`) each row is
  scored under up to two ratings. The ORC bracket and class groups follow every rule above for
  the PHRF ones, per group: sailed when the group had an ORC starter (D2), ORC DNS = ORC
  starters + 1 (D3), throw-out with 5 or more ORC-sailed races chosen among the ORC scores
  (14.4, which can be a different race from the PHRF throw-out), RC average over the ORC scores
  (D1), tiebreak on the ORC scores (D5), and places among the ORC-rated rows only. A row is
  scored under a rating when any of its results in the series carries it, so a boat that got
  its certificate mid-season scores ORC DNS for its earlier PHRF-only races. The PHRF side of
  an ORC class is scored the same way among the PHRF-rated rows and entries (R1), which changes
  nothing in the data since no ORC-only boat has raced. Rows sort by PHRF bracket place, then
  the ORC-only rows by ORC bracket place.
- **Display.** The series standings table shows the ORC side only in an ORC-rated class, the
  race view's gate: a `(PHRF)` and an `(ORC)` line in each race cell with their own throw-out
  decoration, labelled totals, and places in the race view's notation (`2(PHRF),` over
  `1(ORC)`). The ORC line says `DNS` when the boat had no ORC entry that day. The Rating
  column shows the row's rating type, derived from its results.

## Suggested order

1. Refactor seams and the fixture DSL (no behavior change; the existing `test_hoc` still passes).
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

Season trophy scoring is planned separately in `docs/explore_unimplemented/season_trophies.md`.

## Task checklist

Tick each box as it lands. When every box is ticked, the plan is complete. Each fix task in
phase 6 includes:
- un-ignoring its rule tests, or writing the ones that need the new API (see §6);
- if `cargo xtask golden` fails, approving the changed outputs from their `.received.json` files;
- re-running `database/audit_scoring.py`;
- checking that the diffs touch only that decision's entries in `golden/deviations.json` and
  only those races and series in `golden/approved/`.

D6 and D7 should leave the golden outputs unchanged. D1, D2 and D8 re-approve every standings
file; for them the `deviations.json` diff is the only isolation check (see §5).

### Phase 1: Seams and fixtures (no behavior change) — done 2026-10-14

- [x] Extract a pure `card(record, class_start, race, orc_cert)` from `reduce_to_card`, keeping a thin DB wrapper for the certificate lookup.
- [x] Extract a pure `standings(series, year, reports, now)` from `get_standings_report`, keeping a thin DB wrapper. `get_standings_report` takes `now: Timestamp`, and the route passes the server's clock.
- [x] Extract the pure `effective_race_class(class_schedule)` (the 2025→2026 PHRF-only switch) from `get_report`.
- [x] Extract the pure `report(race_schedule, cards)` from `get_report` (see §1).
- [x] Make `tie_breaker`, `phrf_tcf`, `orc_tcf` and `boat_corrected_time` reachable from the tests.
- [x] Add the pure `validate_result(result) -> Option<String>` (`crates/scoring/src/validation.rs`), which accepts everything for now, and wire it into the `POST results` route so a message returns 400 with the message as the body.
- [x] Move the cell-text logic for the race view place columns (`routes/race_results_view.rs`) and the standings cells (`routes/series_standings.rs`) into pure functions in `crates/app/src/utils/`: the `*_place_lines` functions in `race_report.rs` and `standings_report.rs`. The D7 column bug is preserved there, with a comment.
- [x] Add the fixture DSL in `crates/scoring/src/fixture.rs`:
  - race, class, bracket and boat builders, with times as Pacific local times;
  - a `series(|s| ..)` builder;
  - `rc_volunteer`;
  - a `legacy()` result form with no finish code, for the NULL-code rows;
  - the `assert_scores` helper, which compares scores as `f64`, plus `assert_bracket_scores` / `assert_class_scores` on a `RaceReport` and `assert_bracket_totals` / `assert_class_totals` on a `StandingsSeries`.
- [x] Port `test_hoc` (`crates/scoring/tests/race_result_reporter_kt_test.rs`) to the DSL. `cargo test -p regatta-scoring` passes.
  Note: before Phase 1 `test_hoc` was already failing (expected 1, got 3): its hand-built cards never
  set `result.finish`, so `place()` scored every card as RET. The "Current coverage" table above
  overstated it. The DSL port builds real records, and the two DNS cards were dropped because DNS has
  no race record; DNS is covered by the standings test `gsi_14_1_2_dns_scores_starters_plus_one`.

### Phase 2: Rating math, times and labels — done 2026-10-14

- [x] `regatta-common` covers (`crates/common/tests/rating_bands_test.rs`, `labels_test.rs`):
  - `PhrfBFactor`, `Orc3Band` and `Orc5Band` band edges;
  - `virtual_phrf`;
  - `orc_band_label` for every option;
  - `Bracket::label`, including open-ended ranges;
  - `RatingType::rated_label`;
  - duration `display`, including over 24 h. The rounding test (D7) starts out ignored.
- [x] `regatta-scoring` has a table-driven `orc_tcf` test (`crates/scoring/tests/orc_tcf_test.rs`): every `OrcScoringOption` × band maps to the right certificate field, and a banded option without its band fails.
- [x] `regatta-scoring` covers elapsed and corrected time (`crates/scoring/tests/corrected_time_test.rs`):
  - per-class start;
  - ToT for each B-factor;
  - cruising TCF = 1;
  - null rating on a legacy row (TCF 1.0);
  - finish before start;
  - over 24 h (the DSL's `finishes` now accepts "2026-06-18 10:00:00" for a later day);
  - millisecond precision (the DSL's `finishes` accepts fractional seconds);
  - ORC alongside PHRF for `ORC_PHRF`;
  - the 2025→2026 switch through `effective_race_class`, in the Pacific time zone, including the note above that it only relabels the class;
  - the NULL `finish_code` fallback (`null_finish_code_falls_back_to_time_or_ret`, which checks the rule on the model's default and is `#[ignore]`d; the row mapper itself is `finish_code` in `crates/service/src/db/race_results.rs`).
- [x] `regatta-app` covers (`crates/app/tests/race_report_labels_test.rs`, `standings_labels_test.rs`):
  - `card_finish_text` for every code, `HOC n`, `P+n` and no start time;
  - `elapsed_text` / `cor_time_*_text`, including TCF to 4 decimals and `n/a`;
  - standings cell decoration (throw-out, RC strike-through, existing code suffixes);
  - race view place columns for PHRF / ORC / `ORC_PHRF` × single/multi bracket. The D7 column test starts out ignored.

### Phase 3: Golden regression (DB snapshot) — done 2026-10-14

- [x] Add `!scrub_dump.py` and `!audit_scoring.py` to `database/.gitignore`, which ignores `*.py`, `*.json` and `*.sql`.
- [x] Write `database/scrub_dump.py`. It:
  - empties `public.auth`;
  - strips `\restrict` / `\unrestrict`;
  - fails if `image` has rows;
  - leaves person names as they are.
- [x] Scrub `prod_dump2026-10-13_19_10_11.sql` and commit it under `crates/scoring/tests/golden/` (the only diff to the source is the three dropped auth rows and the two restrict lines). The committed file is `regatta-2026-10-13-v5.sql`, the same data at schema version 5.
- [x] Add the `testcontainers` crate as a dev dependency of `regatta-server`, with the version in the workspace `Cargo.toml`.
- [x] Add the `GoldenDb` harness (`crates/server/tests/support/mod.rs`):
  - `postgres:16-bookworm` with `regatta_admin` / `regatta`;
  - the dump loaded with in-container `psql -v ON_ERROR_STOP=1`;
  - a pool connected to the container;
  - `startup::prepare`.
  - A test checks that `startup::schema_version` equals `startup::SCHEMA_VERSION` after startup (version only, not table parity).
- [x] Add the approval helper (`Approvals` in `crates/server/tests/golden.rs`):
  - pretty-printed JSON with stable key order, defaults included, so the files are self-describing for the audit script;
  - a `.received.json` file written on mismatch;
  - `*.received.json` git-ignored.
- [x] Add the golden test (`golden_reports_and_standings_match_approved`). It covers every race from `all_races(year)` for every year in `all_years()` (so a refreshed snapshot with a new season needs no code change) and every series × year that has at least one race, with `get_standings_report`'s `now` pinned to `2026-10-14T02:10:11Z`.
- [x] Generate and commit `golden/approved/` from today's code: 118 race files and 22 standings files, about 11 MB pretty-printed.
- [x] Add `cargo xtask golden` (`cargo test -p regatta-server --test golden`):
  - the harness sets `DOCKER_HOST` from `podman machine inspect` and `TESTCONTAINERS_RYUK_DISABLED=true` when they are unset, skips `DOCKER_HOST` when `/var/run/docker.sock` exists, and fails with a clear message when the machine is not running.
- [x] Write `database/audit_scoring.py`. It reads `golden/approved/`, re-scores with the D1–D5 rules plus the D8 grouping and year-boundary checks, and writes `golden/deviations.json`. Each entry names one decision, or a `+`-joined set when a total or place depends on several (those entries disappear once the last of them lands).
- [x] Commit `deviations.json`: 682 entries (D1 66, D1+D2 4, D1+D2+D3 25, D1+D2+D8 7, D1+D3 27, D1+D8 7, D2 376, D2+D3 8, D3 116, D4 20, D5 23, D8 3). They match findings F1–F5, the D3 DNS/NSC shifts in the RC-volunteer races, the Short Circuit skipper split (D8), and the class throw-out gap added to F2 above. No entry is unattributed, and no D7 or D8 year-boundary case exists in the data.
- [x] `cargo xtask golden` passes locally on podman.

### Phase 4: Rule tests (tests the code fails start out `#[ignore = "Dn: …"]`) — done 2026-10-14

These cover everything that compiles against today's API. Tests for DNS/DSQ codes, D2 status
fields and their labels are written in the D6 and D2 fix tasks (§6).

Every ignored test was run once with its `#[ignore]` stripped and fails on today's code, so
un-ignoring it in a fix task is a real check. 25 tests are ignored: D1 7, D2 3, D3 4, D4 4, D5 4,
D7 1, D8 2. The DSL gained `orphan()` for the "Incorrectly classed" bucket.

- [x] Race placing (`crates/scoring/tests/race_placing_test.rs`):
  - ordering (corrected time → HOC → code weight);
  - the 14.1.1–14.1.4 formulas, with finishers and starters as in D3;
  - HOC numbering;
  - penalties (D4): others unchanged, DNF cap, several penalties, non-finishers, and a penalty on HOC;
  - equal times (D7), and times a millisecond apart not being equal;
  - starters but no finishers;
  - class vs. bracket placing;
  - ORC placing among ORC entries only;
  - the orphan bucket (orphans have no class start, so no elapsed time and no meaningful place; the test pins the bucket, not the places).
- [x] Series standings (`crates/scoring/tests/series_standings_test.rs`):
  - DNS = starters + 1;
  - scores and throw-out for races that weren't sailed per bracket or class, throw-out at 5 or more started races, and the throw-out chosen per scoring group (D2), checked through scores and totals;
  - RC volunteer (D1): average, order of operations, no other races, once per series, fractional, never the throw-out;
  - tiebreak (D5): back through earlier races, a shared place, RC races skipped, three-way ties, class and bracket, input order. Unsailed races are skipped by construction once D2 lands (an unsailed race has no score to compare), so no separate test;
  - class vs. bracket totals;
  - grouping by boat (D8);
  - a single-race series.
- [x] `validate_result` rejects a PHRF / `ORC_PHRF` result without a rating (D8) (`crates/scoring/tests/result_validation_test.rs`).
- [x] Web label rule tests for fractional scores (D1), written in the D1 fix task.

### Phase 5: Invariants — done 2026-10-14

- [x] Random-fleet tests (`crates/scoring/tests/invariants_test.rs`, 300 races or 200 series per property, proptest fleets with seeded entry shuffles) check that:
  - shuffling the input never changes a score (race level passes today; with penalties present it fails, ignored under D4: the penalty pass reuses indexes after the list has changed, so a shuffle can move a boat's score. Seed 245 shows it);
  - places are contiguous apart from ties: timed finishers 1..k by corrected time, HOC k+1..k+h by position, no score below 1. Finish times are random milliseconds, so exact ties do not occur; the tie rule itself is D7's hand-written test;
  - DNF/RET = finishers + 1, in bracket and class;
  - NSC is at least the DNF score;
  - a penalty never changes another boat's score, and adds to the penalised boat's own place capped at DNF (ignored, D4);
  - a series total equals the counted scores, with at most one excluded and none under 5 races;
  - series places follow totals, and every boat has one row;
  - shuffling every race's entries never changes a standings score or place (ignored, D5);
  - an RC volunteer never changes another boat's score or place, compared with the same series where the boat is simply absent (ignored, D3).

  Each ignored invariant was run once with its `#[ignore]` stripped and fails today. The
  generator numbers HOC positions across the whole race, as GSI 14.2 does, not per bracket.

### Phase 6: Fixes (in this order)

- [x] **D3:** NSC = starters + 1, using the D3 definitions of starters and finishers in `place()` and the no-record score (`FinishCode::is_starter` / `is_finisher`). Golden: 5 NSC races and 10 standings files re-approved; `deviations.json` lost exactly the 116 D3 entries plus 14 totals that needed only D3, and the `+D3` labels dropped their D3 part.
  - [x] *2026-10-14 revision (D1):* the RC average is rounded to the nearest tenth, 0.05 up (RRS A10(a)), in `group_scores` and `audit_scoring.py`. 7 standings files re-approved; only the RC cells and their totals moved (e.g. 2.67 to 2.7), no series place changed, `deviations.json` stayed empty.
  - [x] *2026-10-14 revision:* NSC and DSQ score the **other** starters + 1 / + 2 (the boat is not counted against itself). `place()`, `race_placing_test.rs`, `audit_scoring.py` and the 5 NSC races plus 3 standings files re-approved; every NSC score dropped by one and `deviations.json` stayed empty. `test_audit_results.md` regenerated: the 10 NSC race rows now match prod and are gone, Thane/Windswept no longer flip in 2025 Late Spring WS.
- [x] **D7:**
  - equal corrected times score 1, 1, 3;
  - duration `display` rounds to the nearest second;
  - the `ORC_PHRF` "Place In Class" column shows `placeInClassOrc`.
  Golden outputs unchanged, as expected.
- [x] **D4:**
  - the penalty adds points, others unchanged, capped at DNF;
  - penalties apply only to TIME and HOC, and the scorer ignores the rest;
  - the UI blocks the penalty for other codes: `TimeRow` shows the penalty buttons only for TIME and HOC (so HOC can now take one), and `set_finish` clears the penalty when the code changes to anything else.
  Golden: races 18, 26, 31 and their series re-approved; `deviations.json` lost exactly the 20 D4 entries.
- [x] **D6:**
  - add `FinishCode::Dns` and `Dsq`, with the new weights;
  - add the scoring, `FinishCodeDrop` options (it lists every code), `set_finish` penalty clearing (D4's rule covers it), and the race and standings labels;
  - write the DSQ = starters + 2 and DNS race-scoring tests and the `DNS` / `DSQ` label tests.
  Golden outputs unchanged, as expected.
- [x] **D2:**
  - sailed is decided per bracket and class;
  - unsailed races show "Cancelled" / "Not yet sailed", score nothing, and are excluded from the throw-out count and tiebreak; a race sailed by other boats but by nobody in the group shows "Not sailed", or a dash for that group's score when the other group was sailed;
  - add the `StandingsRace` fields `bracketSailed`, `classSailed`, `status` (`RaceStatus`) and `throwOutClass` (the class total has its own throw-out), using `standings`'s `now`; the no-record score is folded into the cell construction;
  - write the status tests and the web "Cancelled" / "Not yet sailed" cell tests.
  Golden: every standings file re-approved (new fields). `deviations.json` lost the 382 D2 entries and the mixed D1+D2 ones, and gained 2 pure D5 ties that the last-sailed-race rule exposed in 2024 Late Spring WS. Two stale labels in the audit script were fixed along the way: it had added D2 to any boat whose group had an unsailed race, and flagged a lone RC card in an unsailed race.
- [x] **D1:**
  - score fields become `f64` in `regatta-common` (`StandingsRace.placeInBracket/Class`, `StandingsBoatSkipper.totalScore*`), and the web labels format decimals through `score_text` in `crates/common/src/utils.rs` (`4`, `3.5`, `3.33`). The `placeIn*Corrected` fields are gone: the RC cell simply holds the average, with its " DNS RC Volunteer" suffix;
  - the RC average follows the D1 rules in `group_scores`, which scores one group of a row (sailed, throw-out, RC average, later RC races as DNS, total), and totals use it;
  - write the fractional-label web tests (`fractional_scores_are_formatted` in `crates/app/tests/standings_labels_test.rs`, `score_text_formats_fractions` in `crates/common/tests/labels_test.rs`).
  Golden: every standings file re-approved (`f64` scores). `deviations.json` lost all 89 D1 entries, the `D1+D8` labels became `D8`, and 2 more pure D5 ties surfaced in 2025 Early Spring WS once the totals agreed.
- [x] **D5:**
  - the tiebreak goes back race by race, and fully tied boats share the place (the next place skips);
  - RC and unsailed races are skipped;
  - the result does not depend on input order (stable sort, comparator reads only scores); `tiedWith` is gone from `StandingsBoatSkipper`.
  Golden: the affected standings files re-approved; `deviations.json` lost exactly the 27 D5 entries.
- [x] **D8:**
  - group standings by boat id, showing the latest skipper;
  - `validate_result` rejects a PHRF / `ORC_PHRF` result without a rating; the UI already disabled save through `is_valid`, and the network layer now keeps a rejected request's plain-text body so the error display shows "400 - A PHRF result needs a PHRF rating";
  - `series_races`, `all_races` and `all_years` select by year in `America/Los_Angeles`.
  Golden: the standings with the Short Circuit skipper split re-approved; `deviations.json` lost the last 13 entries and is empty.

### Done

- [x] No `#[ignore]` referencing this doc remains.
- [x] Re-running `database/audit_scoring.py` on the approved outputs produces an empty `golden/deviations.json`.
- [x] `cargo test -p regatta-common`, `-p regatta-scoring`, `-p regatta-app` and `cargo xtask golden` all pass.
- [x] Update the Tests section of `CLAUDE.md`:
  - `cargo xtask golden` and its podman setup;
  - `scrub_dump.py`, `audit_scoring.py` and the snapshot-refresh steps;
  - the fixture DSL.
- [x] Move this doc to `docs/explore_implemented/` (2026-10-14).
- [x] D9 (ORC series scoring, R1 to R4) implemented 2026-10-15; see `orc_series_standings.md`.
