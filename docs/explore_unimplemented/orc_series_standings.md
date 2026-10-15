# ORC scores and places in series standings

Status: ready to implement (reviewed 2026-10-15), not started. Supersedes the last line of D8 in
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

**R3. `classCard` passes the race's ORC scoring option and bands into `ClassReportCards`**. Today
only the orphan branch sets `orcScoringOption` / `orc3Band` / `orc5Band`, so every real class
report says the default `FiveBandWindwardLeeward` and the race view's `orcBandLabel()` heading
is wrong for any other option. Scores are unaffected: `card()` reads the option from the race.
Data case: race 101 (2026 Summer Series Windseekers #1) is scored `TripleNumberAllPurpose`, and
its four class reports say five-band. Its golden file changes in exactly those four fields.

## Implementation plan

### Phase 0: Related fixes (server), each its own commit
- [ ] R3: `classCard` passes `raceSchedule.race.orcScoringOption` / `orc3Band` / `orc5Band`.
      Test in `RacePlacingTest`: `class_report_carries_the_races_orc_scoring_option` (a
      `race(orcScoringOption = TripleNumberAllPurpose)` fixture, every class report carries it).
      Golden re-approve: only `race-101.json`, only the four `orcScoringOption` fields.
- [ ] R2: `validateResult` rejects an ORC-rated result without `orcRef`. `ResultValidationTest`:
      `accepts_cruising_and_orc_results_without_a_phrf_rating` gives its ORC result a ref;
      add `d9_rejects_an_orc_result_without_a_certificate` for `ORC` and `ORC_PHRF`, and
      `accepts_an_orc_result_with_a_certificate_and_no_phrf_rating`.
- [ ] R1: `place(rating)` filters by `isPHRF` / `isORC` / all, and returns the unplaced cards after
      the placed ones; `classCard` orders a bracket's cards by PHRF place then ORC place.
      `RacePlacingTest`: rename the pinned test to
      `orc_places_are_among_orc_entries_and_phrf_places_among_phrf_entries`, add an `ORC`-only
      boat (`ratingType = RatingType.ORC, rating = null`, with a cert) to its fixture, and assert
      its PHRF place is `0`, the PHRF places of the others are unchanged, and a PHRF boat's DNF
      scores PHRF finishers + 1 with the ORC-only finisher left out. `InvariantsTest`
      `places_are_contiguous_and_follow_corrected_time_then_hoc` restricts PHRF checks to PHRF
      cards once its random races include ORC-only boats (they are added in phase 1; until then
      it is unchanged). Golden re-approve shows no change.

### Phase 1: Scoring (server + common)
- [ ] Add the `StandingsRace` and `StandingsBoatSkipper` fields above.
- [ ] `getStandingsBoatBracket`: ORC starters per race per group, `bracketSailedOrc` /
      `classSailedOrc`, the ORC race scores (DNS = ORC starters + 1), two more `groupScores`
      calls, ORC throw-out flags, ORC totals, `ratingType` on the row. In an ORC class the PHRF
      starters, sailed flags and DNS count PHRF-rated cards only (Symmetry above); elsewhere the
      existing all-cards counting stays, which is the same thing since every card is PHRF or
      cruising there.
- [ ] `assignPlaces` for `placeInBracketOrc` (in `getStandingsBoatBracket`) and
      `placeInClassOrc` (in `standings`) over ORC-eligible rows only; the PHRF pair over
      PHRF-eligible rows only. Row sort: PHRF bracket place, then rows with none by ORC bracket
      place.
- [ ] Fixture: `StandingsSeries.bracketTotalsOrc()` / `classTotalsOrc()` /
      `assertBracketTotalsOrc` next to `bracketTotals()` / `assertBracketTotals`, reading the
      `*Orc` fields; `orc "ref"` entries and `standings(orcCerts = ...)` already exist.
- [ ] `SeriesStandingsTest` (name after the rule, as the others are):
  - `orc_series_total_is_the_sum_of_orc_race_scores` (an `orcPhrfClass` series where ORC and
    PHRF orders differ, both totals asserted, PHRF totals equal to a PHRF-only run of the same
    fixture).
  - `orc_dns_scores_orc_starters_plus_one` (a PHRF-only boat in the class does not raise it).
  - `orc_race_without_orc_starters_is_not_sailed_for_orc` (scores nothing, not in the count).
  - `orc_throw_out_is_chosen_separately_from_phrf` (worst ORC race ≠ worst PHRF race).
  - `orc_rc_volunteer_averages_orc_scores`.
  - `orc_tiebreak_uses_orc_race_scores`.
  - `orc_places_are_among_orc_rows_only` (PHRF-only row has `placeInClassOrc == 0`).
  - `phrf_only_class_has_no_orc_fields_set` (all zero / false).
  - `orc_only_row_has_no_phrf_fields_set_and_sorts_after_the_phrf_rows`.
  - `orc_only_boat_does_not_count_as_a_phrf_starter` (a PHRF boat's series DNS score in an ORC
    class ignores the ORC-only entry).
  - `boat_that_switches_to_orc_phrf_mid_series_scores_dns_in_orc_before_the_switch`.
- [ ] `InvariantsTest`: `randomSeries` gets an `orcPhrfClass` variant where each boat draws a
      random certificate (a single `fiveBandWlLowMedTot` per boat, as `RacePlacingTest` does)
      and some boats are PHRF-only. Then: for ORC-eligible rows, ORC total = sum of counted ORC
      scores with at most one excluded; ORC places follow ORC totals; shuffling never changes
      the ORC fields; ORC fields are zero for every row outside an ORC class.
- [ ] Golden: `./gradlew :server:jvmGoldenTest -Dgolden.approve=true`, then review the diff.
      Only the new fields may appear (in every standings file, defaults encoded), and only
      `standings-*-2026.json` for ORC classes may carry non-zero ORC values; no PHRF number
      changes anywhere.
- [ ] `database/audit_scoring.py`: `group_series` / `boat_series` get an `orc` flag that reads
      the race cards' `placeInBracketOrc` / `placeInClassOrc`, counts starters among ORC-rated
      cards only, and treats a non-ORC card as DNS; `audit_series` runs the ORC pass for ORC
      classes and compares `totalScoreBracketOrc` / `placeInBracketOrc` and the class pair under
      the new decision id D9. Run `python3 -I database/audit_scoring.py`; `deviations.json` must
      stay empty.

### Phase 2: Table cells (web)
- [ ] `StandingsReport.kt`: `totalLines`, `bracketPlaceLines`, `classPlaceLines` as above;
      remove `totalBracketText` / `totalClassText`.
- [ ] Move `CellLines` to `components/CellLines.kt`.
- [ ] `SeriesStandings.kt`: compute `showOrc`, render the three cells with `CellLines`, and the
      Rating column from the row's `ratingType`.
- [ ] `StandingsLabelsTest`: the three functions × `PHRF` / `ORC` / `ORC_PHRF` × single / multi
      bracket × `showOrc`; `ORC_PHRF` with `placeOrc == 0` falls back to PHRF only.
- [ ] Column header "Total Score (bracket, class)" is unchanged; the rating label is in the cell.
- [ ] Race cells: `StandingsRace.bracketScoreTextOrc()` / `classScoreTextOrc()` and
      `bracketScoreClassesOrc()` / `classScoreClassesOrc()` next to the PHRF ones, and
      `scoreLines(showOrc, multiBracket, rating)` returning `ScoreLine`s as above. The view
      replaces its inline `ScoreText` / suffix sequence with a loop over the lines.
- [ ] `StandingsLabelsTest` for `scoreLines`: `PHRF` / `ORC` / `ORC_PHRF` rows × single / multi
      bracket × `showOrc`; PHRF and ORC throw-outs on different races decorate different lines;
      `nonStarterOrc` with a timed PHRF finish puts ` DNS` on the ORC line only; `-` on the ORC
      line when `bracketSailedOrc` is false; status text still wins over both lines; with
      `showOrc == false` the output equals today's single unlabeled line.

### Phase 3: Docs
- [ ] Add D9 to `test_audit_and_plan.md` with the decisions above and R1 to R3, marked
      *(interpretation)*, strike the "Series standings use PHRF only" line of D8, and update the
      "Race placing" row of the coverage table ("ORC placing only among ORC entries" becomes
      "PHRF and ORC placing each among their own entries").
- [ ] Move this file from `docs/explore_unimplemented/` to `docs/explore_implemented/`.
