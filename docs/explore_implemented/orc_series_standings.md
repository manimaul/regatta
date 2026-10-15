# ORC scores and places in series standings

Status: phases 0 to 2 and R4 done 2026-10-15; phase 3 (docs) not started. Supersedes the last line of D8 in
`docs/explore_implemented/test_audit_and_plan.md` ("ORC places stay per-race information. Series
standings use PHRF only.") once implemented.

## Goal

Score an `ORC` / `ORC_PHRF` class as a series under ORC as well as PHRF, and show both in the
existing standings table without adding columns. The "Total Score", "Place In Bracket" and
"Place In Class" cells carry the rating the same way the race results page does:

| Row rating | Cell |
|---|---|
| `ORC_PHRF` | `2(PHRF),` ⏎ `1(ORC)` |
| `PHRF` (in an ORC class) | `1(PHRF)` |
| `ORC` | `1(ORC)` |
| any boat in a PHRF-only or cruising class | `1` (unchanged) |

The per-race cells get the same treatment: a `(PHRF)` line and an `(ORC)` line in the existing
cell, each with its own throw-out decoration.

The notation and the gate are copied from the race view (`utils/RaceReport.kt`
`bracketPlaceLines` / `classPlaceLines`, rendered with `CellLines`): the suffixes appear only
when the class itself is ORC-rated, so pre-2026 seasons and PHRF-only classes look exactly as
they do today.

## Current state

- Each race report already places every ORC entry among the ORC entries of its bracket and
  class (`RaceReportCard.placeInBracketOrc` / `placeInClassOrc`, set by `place(orc = true)` in
  `RaceResultReporter.classCard`). PHRF places are among all entries of the group, ORC-only
  boats included (phase 0 changes this). Pinned by
  `RacePlacingTest.orc_places_are_among_orc_entries_only_and_phrf_places_among_all`.
- The result editor (`RaceResultAddViewModel.isValid`) already requires a certificate for `ORC`
  and `ORC_PHRF` results and a PHRF rating for `PHRF` and `ORC_PHRF`; the server
  (`validateResult`) checks only the rating (D8). The production snapshot has no `ORC`-only
  result and no `ORC_PHRF` result without a certificate.
- `RaceResultReporter.getStandingsBoatBracket` reads only `placeInBracket` / `placeInClass`
  (the PHRF places) into `StandingsRace`, so series totals, throw-outs, tiebreaks and places
  are PHRF-only, including for a boat that is `ORC`-only.
- `StandingsBoatSkipper` has one `totalScoreBracket` / `totalScoreClass` and one
  `placeInBracket` / `placeInClass`. The web table (`components/routes/SeriesStandings.kt`,
  labels in `utils/StandingsReport.kt`) prints them unlabeled.
- Series scoring is already generic over "a group": `groupScores(cells, sailed, score, tieScore,
  dns)` and `assignPlaces(rows, total, score, sailed, set)` take lambdas. Adding ORC is adding two
  more groups, not new rules.
- The 2025→2026 switch (`effectiveRaceClass`) relabels a rated class as `PHRF` before 2026, and
  `StandingsClass.raceClass` is that effective class, so a 2025 series never sees an ORC class.

## Design

### Scoring (server, `RaceResultReporter`)

Treat ORC as two more scoring groups per boat row, "bracket ORC" and "class ORC", scored with the
same rules as the PHRF groups. Every decision in `test_audit_and_plan.md` applies per group,
exactly as D2 already does for bracket vs. class:

| Rule | ORC reading |
|---|---|
| Race score | `card.placeInBracketOrc` / `card.placeInClassOrc` from the race report. |
| Sailed for the group (D2) | The group had at least one **ORC starter** in that race: a card with `ratingType().isORC` and `isStarter()`. |
| DNS / no record (GSI 14.1.2) | ORC starters in the group + 1. A boat that raced as `PHRF` that day has no ORC entry, so it scores DNS in the ORC groups, the same as a boat that stayed home. |
| Throw-out (GSI 14.4) | Chosen per ORC group when that group has 5 or more sailed races. It may be a different race from the PHRF throw-out. |
| RC volunteer (D1) | Averaged from the boat's other ORC race scores. |
| Series tiebreak (D5) | `tieBreaker` over the ORC race scores. |
| Series place | `assignPlaces` over the **ORC-eligible rows only**; other rows keep `0`, as `RaceReportCard` does. |

**Class gate.** The ORC groups are scored only when the standings class is ORC-rated
(`raceClass.ratingType.isORC` on the effective class passed to `getStandingsBoatBracket`). The
2025→2026 switch relabels only the class: the 2025 result records still say `ORC_PHRF` and
`classCard` still fills their `*Orc` places, so without the gate a 2025 series would carry ORC
totals that the web never shows. With it, every ORC field is `0` / `false` outside an ORC class,
which is what the golden diff and `phrf_only_class_has_no_orc_fields_set` check.

**Eligibility.** Inside an ORC class, a row is ORC-eligible when any of the boat's results in the
series has `ratingType().isORC`; PHRF-eligible when any has `isPHRF`. The row's `ratingType` is
derived from that pair (both → `ORC_PHRF`, one → that one, neither → the boat's own type) so the
web can use the same `when` as the race view; outside an ORC class it is the rating type of the
boat's most recent result, the same record D8 takes the skipper from. Rating type is read from
the result, not the boat, so a boat that changed rating mid-season is scored under both and the
standings do not change when its boat record is edited later.

**Symmetry.** After phase 0 the PHRF groups mirror the ORC groups in an ORC class: a race is
sailed for the PHRF group when it had a PHRF starter, DNS is PHRF starters + 1, and the PHRF
totals and places are computed for PHRF-eligible rows only; an `ORC`-only row keeps `0` in the
PHRF fields, as a PHRF-only row does in the ORC fields. In a PHRF-only or cruising class every
entry is PHRF-eligible (or cruising), so nothing changes there and the golden PHRF numbers stay.

**Per-race ORC cell.** For race `i` of an ORC-eligible row, with `card` the boat's card in that
race (or null):

| Case | `placeInBracketOrc` (class likewise) | `nonStarterOrc` |
|---|---|---|
| ORC group not sailed (`bracketSailedOrc == false`) | `0.0` | `false` |
| `card != null && card.ratingType().isORC` | `card.placeInBracketOrc` | `false` |
| `card != null` but not ORC-rated that day, or `card == null` | ORC bracket starters + 1 | `true` |

`nonStarterOrc` tells the web to print `DNS` on the ORC line when the PHRF line has a finish
(a boat that raced PHRF-only that day). The RC volunteer case needs nothing extra: the cell's
`finishCode` is `DNS_RC` for both lines, and `groupScores` reads it from the shared cell.

Row order stays by PHRF bracket place; the table is one table, and sorting by ORC place would
reorder PHRF rows. Rows with no PHRF place (`ORC`-only boats, place `0`) go after the PHRF rows,
ordered by their ORC bracket place.

`getStandingsBoatBracket` grows from two `groupScores` calls to four and from one
`assignPlaces` to two (plus the class-level pair in `standings`). Factor the per-group inputs
(starters-per-race, sailed-per-race, race score accessor) into a small private helper so the
four calls read as a table.

### Data (`common`, `RaceData.kt`)

Additive fields with defaults, named like `RaceReportCard`'s, so existing JSON and the golden
files still decode:

```kotlin
data class StandingsRace(
    ...
    val placeInBracketOrc: Double = 0.0,
    val placeInClassOrc: Double = 0.0,
    var throwOutOrc: Boolean = false,
    var throwOutClassOrc: Boolean = false,
    val bracketSailedOrc: Boolean = false,
    val classSailedOrc: Boolean = false,
    val nonStarterOrc: Boolean = false,
)

data class StandingsBoatSkipper(
    ...
    val ratingType: RatingType = RatingType.CruisingNonFlyingSails,  // derived, see above
    val totalScoreBracketOrc: Double = 0.0,
    val totalScoreClassOrc: Double = 0.0,
    var placeInBracketOrc: Int = 0,
    var placeInClassOrc: Int = 0,
)
```

A nested `phrf` / `orc` score object per row would be cleaner but changes the existing JSON
shape, every golden file, and every web accessor at once. Flat fields keep the change
additive; a later refactor can fold both into a struct once the shape has settled.

The golden test serializes with `encodeDefaults = true`, so the new fields appear in **every**
approved standings file, with their defaults outside ORC classes. That is the expected shape
of the re-approve diff.

No database change: standings are computed on every request.

### Web (`SeriesStandings.kt`, `utils/StandingsReport.kt`)

- `showOrc = standingsClass.raceClass.ratingType.isORC`, the race view's gate.
- `CellLines` is private to `RaceResultsView.kt`; move it to `components/CellLines.kt` so both
  views share it.
- The "Rating" column shows the row's derived `ratingType.label` instead of the boat's current
  `boat.ratingType`, so the column agrees with the `(PHRF)` / `(ORC)` labels in the cells.
- Replace `totalBracketText` / `totalClassText` with line-list functions mirroring
  `RaceReport.kt`, rendered with `CellLines`:
  - `StandingsBoatSkipper.totalLines(showOrc, multiBracket)`:
    single bracket → `["15(PHRF), ", "11(ORC)"]`;
    multi-bracket → `["12, 15 (PHRF)", "9, 11 (ORC)"]` (the header already says
    "(bracket, class)"; the bracket total stays bold as it is now).
  - `StandingsBoatSkipper.bracketPlaceLines(showOrc)` and `classPlaceLines(showOrc)`:
    `["2(PHRF), ", "1(ORC)"]`, `["1(PHRF)"]`, `["1(ORC)"]`, or `["3"]` when `!showOrc`.
  - The `when` is the race view's: `ORC_PHRF && placeOrc != 0` → both; `isPHRF` → PHRF;
    `ORC` → ORC. Keep them pure so `StandingsLabelsTest` covers them.
- Race cells: when `showOrc`, each race cell gets a second line with the ORC scores in the same
  "bracket, class" shape. The first line is labeled `(PHRF)` and the second `(ORC)`, e.g.
  `3, 5 (PHRF)` ⏎ `2, 2 (ORC)` in a multi-bracket class, `3 (PHRF)` ⏎ `2 (ORC)` in a single
  bracket. The red throw-out decoration is per line (`throwOut` / `throwOutClass` on the PHRF
  line, `throwOutOrc` / `throwOutClassOrc` on the ORC line), since the two throw-outs can be
  different races. The finish-code suffix (`DNF`, `HOC3`, `DNS RC Volunteer`) is printed once,
  after the last line. A PHRF-only row in an ORC class shows one `(PHRF)` line; an ORC-only row
  one `(ORC)` line. The "Not yet sailed" / "Cancelled" / "Not sailed" status text still replaces
  the whole cell; a race sailed for the PHRF group but not the ORC group shows the ORC line as
  `-` (the existing dash for "not sailed in this group"). It is a second line in an existing
  cell, not a column; the data is in `StandingsRace` either way.
- Race cell structure: a list of strings is not enough here, since the bracket score and the
  class score on one line are decorated independently (red only on the thrown-out one). The
  pure helper returns typed lines:

  ```kotlin
  data class ScoreSegment(val text: String, val classes: List<String>)
  data class ScoreLine(val segments: List<ScoreSegment>, val suffix: String)

  fun StandingsRace.scoreLines(showOrc: Boolean, multiBracket: Boolean, rating: RatingType): List<ScoreLine>
  ```

  Each line is the existing `bracketScoreText` / `classScoreText` pair with their decoration
  classes (ORC variants read the `*Orc` fields), followed by the rating label when `showOrc`.
  The suffix is `codeSuffix()` on the last line, except that an ORC line with `nonStarterOrc`
  and a `TIME` / `HOC` finish code carries ` DNS` itself. The view renders each segment with
  the existing `ScoreText` and a `Br` between lines. Today's rendering is the one-line,
  no-label case of the same function, so the PHRF-only path is covered by the current tests.

## Decisions (2026-10-15)

The GSI says nothing about ORC series scoring, so the whole feature is an *(interpretation)*,
to be recorded as D9 in `test_audit_and_plan.md` alongside D2 and D5 for the committee to confirm.
The related fixes R1 to R3 below are part of D9.

1. **ORC scores in the per-race cells.** Yes: each race cell carries a `(PHRF)` line and an
   `(ORC)` line, as described under "Web". Without it the ORC total could not be checked against
   the table and the ORC throw-out could not be shown.
2. **Throw-out threshold.** Per group as written: the ORC bracket and ORC class groups each throw
   out their worst score once they have 5 or more sailed races, counted as ORC-sailed races. This
   is the D2 reading applied to two more groups. A boat can therefore have a throw-out under PHRF
   and none under ORC, or the other way round.
3. **Eligibility and DNS.** A row is ORC-eligible when any of its results in the series is
   ORC-rated; a race it sailed as PHRF-only scores DNS in the ORC groups (ORC starters + 1).
4. **Row order.** By PHRF bracket place, unchanged.

## Related fixes (phase 0, decided 2026-10-15)

Three things found while reading, each small enough to fix first so the standings build on
clean race reports. They become D9's first three bullets.

**R1. PHRF places are among PHRF-rated entries only** *(interpretation)*. An `ORC`-only result
has no PHRF rating (the editor requires none), so `card()` gives it a PHRF TCF of 1.0 and
`place()` ranks it among the PHRF boats on raw elapsed time: it gets a meaningless PHRF place,
and it counts as a PHRF starter and finisher in everyone else's DNF / DNS / DSQ scores. Fix:
`place()` takes which rating it is placing and filters the cards the same way for both,
`isPHRF` for PHRF and `isORC` for ORC; cruising classes and the orphan bucket place every card
as now. `classCard` must still return the ORC-only cards in the bracket report: `place()`
returns the placed cards by place followed by the filtered-out cards, which `classCard` then
orders by ORC place so the race view lists them after the PHRF finishers. `hasOrcResults` is
unchanged. The web race view already prints `n(ORC)` only for an `ORC` row, so nothing shows a
`0` PHRF place. The production snapshot has no `ORC`-only result, so no golden number moves.

**R2. A result rated ORC needs a certificate on save**. Extend `validateResult` (D8): an `ORC` or
`ORC_PHRF` result with no `orcRef` is rejected with `"An ORC result needs an ORC certificate"`
(the rating type's label in the message, as for the rating). The editor already disables save
in that case and shows a 400 body, so the UI needs nothing. Old rows keep the TCF 1.0 fallback
in `card()`, as D8 does for a missing rating; the snapshot has none.

**R3. `classCard` passes the race's ORC scoring option and bands into `ClassReportCards`**. Before,
only the orphan branch set `orcScoringOption` / `orc3Band` / `orc5Band`, so every real class
report said the default `FiveBandWindwardLeeward` with bands derived from the race's B factor,
while `card()` scored the ORC corrected times with the race's *stored* option and bands. The
heading now describes the scoring that was applied. Scores are unaffected.

The golden diff was wider than first predicted: not only race 101's option, but the bands of 63
race files, because the stored bands disagree with the B factor in most races. That is R4.

**R4. The stored race bands do not follow the B factor** (found during R3). `RacesEditViewModel.setCF`
saves a new B factor without touching `orc3Band` / `orc5Band`, and `RaceTable.rowToRace` fills a
NULL band column from the *default* B factor rather than the row's. Only the RC page
(`RcViewModel.selectPhrfBFactor` / `selectOrcOption`) keeps the two in step. In the snapshot 101
of 118 races store `Low` / `LowMedium`, including 2026 ORC races with a B factor of 550 (which
maps to `Medium` / `Medium`) and 480 (`High` / `High`). `card()` → `orcTcf` reads the stored
bands, so the ORC corrected times of those races were computed with the low-wind certificate
numbers while the B factor says medium or high wind. Affected 2026 races with ORC classes: 88,
92, 94, 95, 96, 98, 99, 102, 103, 104, 105, 107, 108, 110, 112, 113, 114, 115.

R4 is split: **R4a** stops new inconsistent rows and gives the results editor a way to set the
config (decided 2026-10-15, below); **R4b** is what to do with the existing rows and the scores
computed from them (open, discussed next).

**R4a. Reject a race whose band is out of step with its B factor, and add a Config modal to the
Results Editor.**

*The rule.* The band in use must be the one the B factor maps to:
`orc3Band == Orc3Band.fromPhrfBFactor(phrfBFactor)` when `orcScoringOption.uses3Band`,
`orc5Band == Orc5Band.fromPhrfBFactor(phrfBFactor)` when `uses5Band`, nothing to check for a
single-number option. Only the band in use is checked because `RcViewModel.selectOrcOption`
sets the B factor from the selected band and leaves the *other* band as it was, so checking
both would reject the RC page's own saves. (`fromPhrfBFactor(band.phrfBFactor) == band` holds
for every band of both enums, so a band chosen on the RC page always passes.) The B factor
itself stays a free integer, as today.

*Server.* A pure `validateRace(race: Race): String?` in `results/RaceValidation.kt`, the shape
of `validateResult`, returning e.g.
`"PHRF B factor 550 is Medium, but the ORC wind band is Low/Medium"`. Both save routes call it
and answer `400` with the message as plain text, as the results route does: `post(race)`
(`upsertRace`, the races editor) and `post(raceSchedule)` (`insertSchedule`, the RC page and the
new modal). The rejected race is not saved and nothing is corrected server-side; the client
is told what to change. `Network` already keeps a 400 body as the request's error.

*Web.* The RC page's config section becomes a shared form, and the schedule edits behind it
become pure functions so both screens and the tests share them:
- `utils/RaceConfig.kt`: `RaceSchedule.withPhrfBFactor(cf)` (sets the B factor and derives
  both bands from it), `RaceSchedule.withOrcOption(option, orc3Band, orc5Band)` (sets the
  option and bands and the B factor from the band in use, as `selectOrcOption` does now), and
  `RaceSchedule.withClassStart(classSchedule, start)`. `RcViewModel` calls them.
- `components/RaceConfigForm.kt`: `RaceConfigForm(schedule, onClassStart, onPhrfBFactor,
  onOrcOption)` extracted from `RcRaceConfig`: one `RgTime` per class, the PHRF B factor input
  with its `PhrfBFactor` label, and `OrcScoringOptionSelect` (which moves there from `Rc.kt`).
  The RC page keeps its status line and auto-save around it.
- `RacesEditViewModel.setCF` uses `withPhrfBFactor`, so the races editor stops producing
  stale bands. (It saves through `post(race)`, which now validates too.)
- Results Editor (`RaceResultsEdit` in `RaceResults.kt`): a **Config** `RgModalButton` next to
  "Viewer" and "Add Result", opening `RgModalBody(id = "race-config")` with `RaceConfigForm`
  and a Cancel / Save footer, the pattern of the result and race modals. State lives in
  `RaceResultAddViewModel`: `configDraft: RaceSchedule?` (a copy of `results.raceSchedule`
  taken when the modal opens), `configError: String?`, and `openConfig()`,
  `configClassStart()`, `configPhrfBFactor()`, `configOrcOption()`, `saveConfig()`.
  `saveConfig` posts the draft with `Api.postSchedule`; on success it closes the modal and
  calls `reload()` so the schedule, class starts and ratings on the page are current; on a
  400 it keeps the modal open and shows the message under the form. Cancel drops the draft.

*Tests.*
- `server` `RaceValidationTest`: every `Orc3Band` and `Orc5Band` with its own `phrfBFactor`
  is accepted under a 3-band / 5-band option; 550 with `LowMedium` under a 5-band option is
  rejected with the message above; 550 with 3-band `Low` under a 3-band option is rejected;
  the unused band may disagree (5-band option, `orc5Band` right, `orc3Band` stale) and is
  accepted; a single-number option is accepted with any bands.
- `web` jsTest `RaceConfigTest`: `withPhrfBFactor(550)` yields `Medium` / `Medium`;
  `withOrcOption(FiveBandWindwardLeeward, _, High)` yields B factor 480 and `orc3Band` `High`
  (both bands derived, which is stricter than the RC page today and makes the "both bands"
  rule possible later); `withClassStart` changes only that class.
- Golden: unaffected, validation runs on save only.

**R4b. The existing rows and the scores computed from them** (decided and done 2026-10-15: the
B factor is the authority, applied by `database/fix_orc_bands.py` in production and dev; the
options below are kept for the record).
Once R4a is in, no new race can be saved out of step, but the 101 stored rows stay as they are
and the race view and ORC scoring keep reading them. Options:
- Make the B factor the single source of truth: `orcTcf` derives the band from
  `race.phrfBFactor`, `upsertRace` stores the derived bands, and a migration backfills the
  columns. The 2026 ORC corrected times and per-race ORC places of the races listed above
  change in the golden files; the PHRF numbers do not.
- Keep the stored bands as the truth and leave the past results as scored. The RC must then
  open each inconsistent race in the Config modal and save it, which R4a's validation will
  refuse until the band is corrected by hand.

The first option is the one the RC page already assumes. It changes published 2026 ORC race
results, so it is the committee's call; until then phase 1 builds on the stored bands, as the
race view does.

## Implementation plan

### Phase 0: Related fixes (server), each its own commit — done 2026-10-15
- [x] R3: `classCard` passes `raceSchedule.race.orcScoringOption` / `orc3Band` / `orc5Band`.
      `RacePlacingTest.class_report_carries_the_races_orc_scoring_option_and_bands`. Golden
      re-approved: the `orcScoringOption` / `orc3Band` / `orc5Band` fields of the class reports in
      63 race files, no score or place (see R4 for why it was not just race 101).
- [x] R2: `validateResult` rejects an ORC-rated result without `orcRef`.
      `ResultValidationTest.r2_rejects_an_orc_result_without_a_certificate` and
      `accepts_an_orc_result_with_a_certificate_and_no_phrf_rating`.
- [x] R1: `place(among: PlaceAmong)` ranks `All` / `Phrf` / `Orc` entries and returns the rest
      after them; `classCard` lists a bracket's PHRF-placed cards by place, then the ORC-only
      cards by ORC place. `RacePlacingTest.orc_places_are_among_orc_entries_and_phrf_places_among_phrf_entries`
      (ORC-only boat has PHRF place 0, a PHRF DNF scores PHRF finishers + 1 without it, bracket
      order). Golden unchanged. `InvariantsTest` is unchanged until phase 1 adds ORC-only boats
      to its random races, when `places_are_contiguous_and_follow_corrected_time_then_hoc` must
      restrict its PHRF checks to PHRF cards.
- [x] R4a (server): `validateRace` in `results/RaceValidation.kt`; `post(race)` and
      `post(raceSchedule)` answer 400 with its message; `RaceValidationTest`. Done 2026-10-15.
- [x] R4a (web): `utils/RaceConfig.kt` schedule helpers used by `RcViewModel` and
      `RacesEditViewModel.setCF` (whose `save` now shows a rejected save as an alert);
      `components/RaceConfigForm.kt` extracted from `RcRaceConfig` (with
      `OrcScoringOptionSelect`); Config button and `race-config` modal on the Results Editor
      backed by `RaceResultAddViewModel` draft state, with the 400 message shown in the modal;
      `RaceConfigTest` in jsTest. Checked in the browser against the dev database. Done 2026-10-15.
- [x] R4b: `database/fix_orc_bands.py` made every race's bands follow its B factor with the
      5-Band Windward/Leeward option, run in production and dev on 2026-10-15. The golden
      snapshot predates it; the next refresh will show the 2026 ORC corrected times and places
      of the races listed under R4 change. `rowToRace`'s NULL-band fallback still derives from
      the default B factor; it no longer matters, since every row has bands and `validateRace`
      keeps them in step from here on.

### Phase 1: Scoring (server + common) — done 2026-10-15
- [x] The `StandingsRace` and `StandingsBoatSkipper` fields above.
- [x] `getStandingsBoatBracket`: starters per race for each of the four groups (`phrfCard` /
      `orcCard` decide which entries count, R1 symmetry in an ORC class), `bracketSailedOrc` /
      `classSailedOrc`, the ORC race scores (DNS = ORC starters + 1, `nonStarterOrc`), four
      `groupScores` calls, the ORC throw-out flags and totals, `ratingType` on the row.
- [x] `assignPlaces` per rating over that rating's rows (`phrfRows` / `orcRows`), in the
      bracket and in `standings` for the class. Row sort: PHRF bracket place, then the rows with
      none by ORC bracket place.
- [x] Fixture: `bracketTotalsOrc()` / `classTotalsOrc()` / `bracketPlacesOrc()` /
      `classPlacesOrc()` / `bracketRaceScoresOrc()` / `classRaceScoresOrc()` /
      `assertBracketTotalsOrc` / `assertClassTotalsOrc`; `rcVolunteer` copies the boat's
      certificate reference.
- [x] `SeriesStandingsTest`, the twelve `d9_*` tests listed above (plus
      `d9_orc_fields_are_not_set_before_2026` for the season switch), on a fixture where the
      ORC order is the reverse of the PHRF order.
- [x] `InvariantsTest`: `randomSeries(withOrc = true)` enters an ORC/PHRF class with each boat
      ORC/PHRF, PHRF-only or ORC-only for the series and a random certificate; four `d9_*`
      invariants (ORC totals and throw-outs, places per rating and row order, shuffling, ORC
      fields zero outside an ORC class). The random *races* stay PHRF, so
      `places_are_contiguous_and_follow_corrected_time_then_hoc` is unchanged.
- [x] Golden re-approved: 22 standings files, each identical to before once the new fields are
      removed; no race file changed; ORC series scores only in the 2026 ORC/PHRF classes (one or
      two ORC boats per class).
- [x] `database/audit_scoring.py`: `score_group` / `group_series` take a rating (`all` /
      `phrf` / `orc`) and place among that rating's entries (R1); `audit_series` skips ORC-only
      rows on the PHRF side; `audit_series_orc` re-scores the ORC side under D9; `audit_race`
      checks the ORC race places. `boat_series` now picks the throw-out among equal worst scores
      the way `groupScores` does (worse score in the other group, then the earliest race), which
      the PHRF pass never needed since it compares totals. `deviations.json` is empty.

### Phase 2: Table cells (web) — done 2026-10-15
- [x] `StandingsReport.kt`: `ScoreSegment` / `ScoreLine`, the four `*Orc` score text and
      decoration helpers, `statusText(rating, showOrc)`, `scoreLines`, `totalLines`,
      `bracketPlaceLines`, `classPlaceLines`. `totalBracketText` / `totalClassText` stay as
      the building blocks they were. The ORC line says `DNS` whenever the boat had no ORC entry
      (`nonStarterOrc`), and the PHRF line then carries the boat's own code, so a boat with no
      record at all says `DNS` on both lines.
- [x] `components/CellLines.kt`: `CellLines` (moved from the race view) and `ScoreLines`.
- [x] `SeriesStandings.kt`: `showOrc` from the class, the Rating column from the row's
      `ratingType`, the race cells through `scoreLines`, totals through `totalLines`, places
      through the place line helpers.
- [x] `StandingsLabelsTest`: nine new tests over the ORC helpers (the single unlabeled line
      without the gate, lines per rating, per-side throw-out decoration, the ORC non-starter,
      the dash for an unsailed ORC group, status text per rating, totals and places per rating).
- [x] Checked in the browser against the dev database: the 2026 Summer Series Windseekers
      standings show the two ORC boats with both lines, and a 2025 series is unchanged.

### Phase 3: Docs
- [ ] Add D9 to `test_audit_and_plan.md` with the decisions above and R1 to R3, marked
      *(interpretation)*, strike the "Series standings use PHRF only" line of D8, and update the
      "Race placing" row of the coverage table ("ORC placing only among ORC entries" becomes
      "PHRF and ORC placing each among their own entries").
- [ ] Move this file from `docs/explore_unimplemented/` to `docs/explore_implemented/`.
