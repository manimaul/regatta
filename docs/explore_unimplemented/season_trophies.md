# Season Trophies (Racer of the Year)

Plan for having the app compute the CYCT overall season trophies. They are described on pages
5–6 of the General Sailing Instructions (GSI):
[`../explore_implemented/2026-CYCT-General-Sailing-Instructions.pdf`](../explore_implemented/2026-CYCT-General-Sailing-Instructions.pdf).
This was split out of [`test_audit_and_plan.md`](../explore_implemented/test_audit_and_plan.md) and builds on that
plan's race and series scoring rules (D1–D8): starters and finishers (D3), the RC volunteer
average (D1), unsailed and cancelled races (D2), penalties (D4) and fractional scores.

Status: the rules below were checked against the PDF on 2026-10-14, the decisions are made and
the task checklist is ready. Nothing is implemented yet.

## What the GSI says (verified 2026-10-14)

**Page 5, "CYCT Members Overall Season Trophies".** The trophies are for yachts "whose owners
are a member of Corinthian Yacht Club as of April 1st of each year", awarded at the Christmas
party in December. The table of contents calls them "PHRF, Challenger, and Windseekers".

**Racer of the Year Perpetual Trophy for PHRF Class (page 5).**
- "Because there may be changes to a boat's class during the year, the CYCT will use a boat's
  **OVERALL** finish position in each race as their points ... for both handicapped classes
  (PHRF Fast and PHRF Half-Fast Classes)." Taken literally, overall means the finish across both
  PHRF classes together. The app cannot score it that way; see T3.
- 1: the boat must be a CYCT member and "must participate in at least one (1) race in each series".
- 2: races scored: top 6 of the 7 combined Point and Harbor Series races, top 16 of the 20
  Wednesday Windseekers races, top 2 of the 3 Three Hour Tour races.
- 3: "If a scheduled race in a series is cancelled, abandoned, or invalidated that race shall
  count as a discard ... The top race scores counted ... shall be reduced by the number of races
  that were cancelled, abandoned, or invalidated in that series/combined series."
- 4: the lowest combined total of the 24 races wins.
- 5: ties: (a) "If two or more boats have the same lowest individual overall score, then the
  tiebreaker shall be determined by the total number of said lowest individual scores", the
  higher count wins; (b) "If a tie still exists after (a) and (b) above, then the next lowest
  individual score shall be used ... and so on". The text refers to an "(a) and (b) above" that
  it does not contain: the Windseekers version of the same rule has a first step, "the yacht
  with the lowest individual overall score ... shall be the winner", which was dropped here in
  editing. See T7.

**Windseekers and Challengers Overall Trophies (pages 5–6).** Names one trophy, the
"Windseeker Racer of the Year Perpetual Trophy".
- 1: "Individual class finishes (not overall) for each race shall be utilized ... HOC ...
  finishes will be counted based on position on the course at the time they are horned."
- 2: members only. There is no one-race-per-series requirement.
- 3–5: the same races, discards and total as PHRF.
- 6: ties: (a) the lowest single score "from any race scored" wins, (b) then the count of that
  score, (c) then the next lowest score, and so on.

**GSI 1.4.1.2, RC volunteer (page 8).** "The boat's overall score for racer of the year
calculations will be average of their overall finishes in that series ... This can only be
utilized once per series and cannot be used as a tie breaker."

**Page 4, class selection.** A yacht is scored in the class it registered for a series and must
not change class mid-series; changes happen between series. That is the GSI's reason for
scoring the PHRF trophy on the overall finish.

**Calendar (pages 3–4).** Harbor Series (3, January–February), Point Series (4,
September–October), four Wednesday Windseekers series of 5 (Early Spring, Late Spring, Summer,
Late Summer), Three Hour Tour (3). The single/double-handed races, Ladies Take the Lead, Vashon
Challenge and the Awards Race are in no trophy. The production snapshot matches: 7 + 20 + 3
races per year, except 2025 where the Point Series has 3 races, so N must come from the races
in the database, not from the GSI numbers.

### Corrections to the earlier draft

- The PHRF trophy used the boat's **class place**, which the GSI text contradicts: it says
  **overall**. The class place stays (T3). Fast and Half Fast are separate `RaceClass` rows
  (ids 1/2 up to mid-2026, 37/38 after the rating split moved to 93/94) with their own start
  times and often their own course or number of laps, so corrected times across the two are
  not comparable and an overall finish is not meaningful. The same holds for Windseekers FS
  and NFS. The departure is now stated, and is an open question for the committee.
- The two tiebreak chains were different. They are the same rule, with the first step missing
  from the PHRF text; T7 uses one chain for both.
- The tiebreak compares **counted** races ("from any race scored"), not the discards. Series
  standings (D5) compare raw scores including the throw-out; the trophies do not.
- Top-N, future races and the "each series" eligibility are pinned down in T4 and T6.

## Decisions (answered 2026-10-14)

Where a decision interprets the GSI or goes further than its text, it is marked
*(interpretation)* so the committee can confirm it.

**T1. Trophy groups on `Series`.**
- `Series` gets an optional `trophy_group: Option<TrophyGroup>`. The enum lives in
  `crates/common` with a label and a fixed number of drops: `PointHarbor` (1),
  `WindseekersWednesday` (4), `ThreeHourTour` (1). The field is not written while `None`, as
  `RaceReportCard.orc_dxt_id`, so the golden files and the contract cassettes do not change.
- The `series` table stores the enum name in a nullable `varchar` column. That is a schema
  change: a new SQL migration, an upgrade step in `crates/service/src/startup.rs` and a bump of
  `SCHEMA_VERSION`, as `CLAUDE.md` lists under "Schema".
- The Series admin modal gets a "Season trophy group" select, and the Series table a column.
- A series with no group counts toward nothing (the Lido 14 series, for instance).
- After deploying, set the groups on the existing series by hand: Harbor and Point Series →
  Point + Harbor; the four Windseekers series → Wednesday Windseekers; Three Hour Tour → Three
  Hour Tour. The web page says so when no series in the year has a group.

**T2. Three trophies, one fleet each, keyed by rating type** *(interpretation of "PHRF,
Challenger, and Windseekers")*.
- `PHRF`: results whose rating type `is_phrf()` (`PHRF`, `ORC_PHRF`). ORC-only results are left
  out; no such class exists in the data.
- `WindseekersFS`: `CruisingFlyingSails`. `WindseekersNFS`: `CruisingNonFlyingSails`.
- The fleet decides which trophy a boat is listed under (T5); scores are class places (T3).
- The fleet is read from the **result record's rating type**, not the class id, because class
  ids change (the PHRF classes were replaced mid-2026) and the single/double-handed classes are
  separate rows. In every trophy race in the snapshot each rating type has exactly one class, so
  "fleet" and "class" coincide for Windseekers; the PHRF fleet is two classes.
- "Challenger" is taken to be the historical name of the non-flying-sails fleet, so the three
  trophies are PHRF, Windseekers FS and Windseekers NFS. Open question for the committee.

**T3. Race score = the boat's class place from the race report** *(departs from the GSI's
"overall" wording for PHRF)*.
- Fast and Half Fast, like Windseekers FS and NFS, have separate start times and often different
  courses or lap counts, so finishes are only comparable within a class. The score is the
  `place_in_class` that `class_card` already computes for the race report; no new placing is
  needed. For PHRF that is the finish within Fast or within Half Fast, never across both, which
  is the one point where the app cannot follow the GSI text. Series standings use the same
  class place.
- `place_in_class` already orders HOC boats by position on the course (GSI 14.2), applies D3 for
  DNF, RET, NSC, DSQ and D4 for penalties, shares equal places (D7), and is the PHRF place for
  `ORC_PHRF` classes. ORC places are not used; the trophies are PHRF only, like series standings.
- Orphan cards (bracket id 0) are left out.
- A boat with no record in a race scores **DNS = class starters + 1** (GSI 14.1.2 with D3
  starters), against the class it is registered in for that series: the class of its nearest
  sailed race in the same series, else of its nearest trophy race in the year (page 4: a yacht
  is scored in the class it registered for the series). *(interpretation)* That includes a race
  it sailed in another fleet (T5).
- A race in which the boat's class had **no starter** is **not sailed by the class** *(D2
  applied per class)*: it scores nothing for that boat and reduces that boat's N for the group
  by one (T4), like a cancellation. *(interpretation)*

**T4. Top-N per group, per boat.**
- N = (races in the database for the group's series in that year) − (the group's drops) −
  (races cancelled, or not sailed by the boat's class). With all 7 / 20 / 3 races entered and
  sailed that is 6 / 16 / 2. N is the same for every boat of a class, so the page shows the
  fleet-level counts once and each row's "n of N" carries its own N.
- Cancelled is D2's `Cancelled` status: no results and the latest class end time has passed.
  Abandoned and invalidated races are entered the same way (results deleted), so they need no
  new status. *(interpretation)*
- A race **not yet sailed** (D2 `NotYetSailed`) neither scores nor reduces N, so during the
  season a boat's total is the sum of its best `min(N, races scored so far)` scores and the
  page shows the current pace. When every race is sailed or cancelled the count is exactly N.
  *(interpretation)*
- The group total is the sum of the counted scores; the discards are flagged on the race cell.
  When equal scores compete for the last counted slot, the earlier race is counted; the choice
  never changes the total or the tiebreak, which only look at the multiset of counted scores.
- The trophy total is the sum of the group totals.

**T5. Which boats appear** *(interpretation)*.
- A boat is listed under a trophy when it has at least one result record in that fleet in a
  trophy-group race of the year (an RC volunteer record counts; a D6 `DNS` record does not).
- Every other trophy race scores DNS for it (T3), including races it sailed in a different
  fleet. The cell says which fleet it sailed in. So a boat that moved between FS and NFS, or
  between Windseekers and PHRF, appears under both trophies with its real results in each.
- Each row shows the skipper of the boat's most recent trophy race (D8).

**T6. Eligibility, places and the winner.**
- Member: the row's skipper has `Person.club_member`. The GSI says "owner" and "as of April 1";
  the app has neither an owner nor a membership date, so the flag stands in for both and the
  date stays a manual RC check. *(interpretation)*
- PHRF only: the boat must have **participated** (a result record in the PHRF fleet that is a
  starter under D3, or an RC volunteer record) in **every series that has a trophy group** in
  that year. *(interpretation of "each series")* The row lists the series it missed.
- Eligible = member and no missed series. **Places are assigned among eligible boats only**;
  the winner is place 1. Ineligible boats are listed after them, ordered by total, with no
  place and the reason shown, so the RC can see what a membership fix would change.
- A boat with no counted score yet is listed with a total of 0 and no place.

**T7. One tiebreak for both trophies** *(interpretation; the PHRF text lost its first step)*.
- Each boat's **counted** scores are sorted ascending, leaving out any RC volunteer average
  (GSI 1.4.1.2: "cannot be used as a tie breaker"). The lists are compared element by element;
  the first lower element wins. This is exactly the GSI chain: the first element is the lowest
  single score (Windseekers 6a); when it is equal, the second element tells whether a boat has
  another of that score (6b / PHRF 5a); when all those are equal the next lowest score decides
  (6c / PHRF 5b), and so on.
- Boats that are still equal share the place and the next place skips, as in D5.
- Discarded races are never compared ("from any race scored").
- The result must not depend on input order.

**T8. RC volunteer** *(GSI 1.4.1.2 applied with D1's conventions)*.
- In each fleet, the boat's first `DNS_RC` race in a series scores the **average of its other
  class scores in that series** (the GSI's "overall finishes" read as in T3): the races its class
  sailed, DNS scores included, rounded to the nearest tenth with 0.05 up (`round_to_tenth`, D1). Nothing is excluded for a throw-out, because
  the trophy has no per-series throw-out; the group discards are chosen afterwards and the RC
  score can be one of them.
- Later `DNS_RC` races in the same series score DNS. With no other class race in that series
  the RC race scores DNS; it becomes the average once the boat races again.
- The RC score counts toward the total and the discards like any other score, and is left out
  of the tiebreak (T7).
- The RC record counts as participation for T6.

**T9. A boat that changes class.** Fast ↔ Half Fast: one row under PHRF, each race scored in
the class it sailed, missed races against the class of its nearest race (T3). FS ↔ NFS, or
Windseekers ↔ PHRF: a row under each fleet it raced in (T5). *(interpretation)*

**T10. The trophy year** is the Pacific calendar year, `year_bounds` in
`crates/service/src/db/time.rs` (D8). It
matches the GSI calendar: Harbor Series in January, Point Series in October, awards in December.

**T11. Output and location.** Results are computed on every request (no stored state) from
the same race reports as series standings, served at `api_paths::TROPHIES` (`/trophies?year=`),
and shown at the web route `/trophies/:year`, reached from a "Season Trophies" button next to
the year select on the Race Results screen.

## Open questions (for the race committee)

- The GSI scores the PHRF trophy on the **overall** finish; the app scores the class finish
  because Fast and Half Fast start separately and often sail different courses (T3). Confirm,
  and consider amending the GSI wording to match.
- Is "Challenger" the Windseekers NFS trophy, so that there are three trophies (T2)? If instead
  there is a single Windseekers trophy across FS and NFS, T2 becomes one fleet with two classes
  and T3's "class finish" would need the class place rather than the fleet place.
- Should a boat that changed fleet appear under both trophies (T5), or only under the fleet of
  its most recent race?
- Confirm the PHRF tiebreak is the same chain as the Windseekers one (T7).
- Confirm that a race the boat's class did not start reduces that boat's N (T3), rather than
  scoring nothing with N unchanged.
- "Member as of April 1" and "owner": is the skipper's membership flag enough, or should
  `Person` get a membership date?

## How it works

### Data model (`crates/common/src/data/race_data.rs`)

```rust
/// The season trophy group a series counts toward (T1), stored and serialized by its variant name.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum TrophyGroup {
    PointHarbor,
    WindseekersWednesday,
    ThreeHourTour,
}

impl TrophyGroup {
    pub const ENTRIES: [TrophyGroup; 3] = [
        TrophyGroup::PointHarbor,
        TrophyGroup::WindseekersWednesday,
        TrophyGroup::ThreeHourTour,
    ];

    pub const fn label(self) -> &'static str {
        match self {
            TrophyGroup::PointHarbor => "Point + Harbor Series",
            TrophyGroup::WindseekersWednesday => "Wednesday Windseekers",
            TrophyGroup::ThreeHourTour => "Three Hour Tour",
        }
    }

    pub const fn drops(self) -> i32 {
        match self {
            TrophyGroup::PointHarbor | TrophyGroup::ThreeHourTour => 1,
            TrophyGroup::WindseekersWednesday => 4,
        }
    }
}

/// The fleet a trophy is scored over (T2).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum TrophyFleet {
    #[serde(rename = "PHRF")]
    Phrf,
    WindseekersFS,
    WindseekersNFS,
}

impl TrophyFleet {
    pub const fn label(self) -> &'static str {
        match self {
            TrophyFleet::Phrf => "PHRF Racer of the Year",
            TrophyFleet::WindseekersFS => "Windseekers Flying Sails Racer of the Year",
            TrophyFleet::WindseekersNFS => "Windseekers Non-Flying Sails Racer of the Year",
        }
    }
}

// Series gains:
//     #[serde(skip_serializing_if = "Option::is_none")]
//     pub trophy_group: Option<TrophyGroup>,

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SeasonTrophies {
    pub year: i32,
    pub groups: Vec<TrophyGroupRaces>,  // every trophy-group race of the year, in start order
    pub trophies: Vec<TrophyStandings>, // one per fleet, PHRF, FS, NFS
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TrophyGroupRaces {
    pub group: TrophyGroup,
    pub series: Vec<Series>,
    pub races: Vec<TrophyRaceInfo>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TrophyRaceInfo {
    pub race: Race,
    pub series_id: i64,
    pub status: RaceStatus,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TrophyStandings {
    pub fleet: TrophyFleet,
    pub counts: Vec<TrophyGroupCount>, // how N was arrived at, per group
    pub rows: Vec<TrophyRow>,          // eligible boats by place, then ineligible by total
}

/// Fleet-level counts; a row's own N is in [`TrophyGroupTotal`].
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TrophyGroupCount {
    pub group: TrophyGroup,
    pub scheduled: i32,
    pub sailed: i32,
    pub cancelled: i32,
    pub not_yet_sailed: i32,
    pub not_sailed_by_fleet: i32,
    pub drops: i32,
    pub counted: i32,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TrophyRow {
    pub boat_skipper: BoatSkipper,
    pub member: bool,
    pub missed_series: Vec<Series>, // PHRF only; empty for Windseekers
    pub eligible: bool,
    #[serde(default)]
    pub place: Option<i32>, // None when ineligible or nothing scored yet
    pub total: f64,
    pub group_totals: Vec<TrophyGroupTotal>,
    pub races: Vec<TrophyRace>, // same order as SeasonTrophies.groups' races
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TrophyGroupTotal {
    pub group: TrophyGroup,
    pub total: f64,
    pub counted: i32, // this row's N (T4)
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TrophyRace {
    pub race_id: i64,
    pub group: TrophyGroup,
    #[serde(default)]
    pub score: Option<f64>, // None when the race scores nothing for this fleet
    #[serde(default)]
    pub counted: bool,
    #[serde(default)]
    pub finish_code: Option<FinishCode>, // None = no record (DNS)
    #[serde(default)]
    pub hoc_position: Option<i32>,
    #[serde(default)]
    pub rc_average: bool, // T8: the score is the RC volunteer average
    pub status: RaceStatus,
    pub class_sailed: bool, // T3: the boat's class had a starter
    #[serde(default)]
    pub race_class: Option<RaceClass>, // the class the score or DNS was taken in
    #[serde(default)]
    pub other_fleet: Option<TrophyFleet>, // T5: the boat sailed this race in another fleet
}
```

Scores are `f64` for the same reason as series standings (D1), and labels use `score_text`.

### Scoring (`crates/scoring/src/trophies.rs`)

A pure function next to `standings` in `crates/scoring/src/reporter.rs`, re-exported from the
crate root, with its database wrapper in `crates/service/src/reporter.rs` next to
`get_standings_report`:

```rust
// crates/scoring/src/trophies.rs
pub fn trophies(year: i32, series: &[Series], reports: &[RaceReport], now: Timestamp) -> SeasonTrophies

// crates/service/src/reporter.rs
pub async fn get_season_trophies(db: &Db, year: i32, now: Timestamp) -> Result<SeasonTrophies>
```

The wrapper takes `db.all_series()` with a `trophy_group`, their `db.series_races(id, year)` and
`get_report_for` over `db.find_race_schedules` for each, as `get_standings_report` does. `now`
is the server's `state.now()` (the `REGATTA_FIXED_NOW` clock in tests). The function:

1. Builds the groups: series by `trophy_group`, races by start time, and each race's
   `RaceStatus` with the same rule as `standings()`.
2. For each fleet, for each race: the fleet's cards with their `place_in_class`, and per class
   the starter count (T3).
3. The fleet's boats (T5), each boat's skipper (D8), and each boat's class per series (T3).
4. Per boat, per race: its class place, DNS = its class's starters + 1, or nothing when its
   class did not start or the race was not sailed at all. Then T8 per series: the first RC race
   becomes the average.
5. Per group and boat: N (T4), the best N scores flagged, the group total.
6. Eligibility (T6), then places with a comparator of total then T7, reusing the shape of
   `assign_places`; ineligible rows follow, by total.

Nothing in `RaceReport` or the race view changes.

### API

`api_paths::TROPHIES = "/trophies"` in `crates/common/src/api_paths.rs`,
`GET /v1/api/trophies?year=2026`, public, no auth, answers `SeasonTrophies`; 400 without a
year. The handler is a thin adapter over `get_season_trophies` in
`crates/server/src/routes/series.rs` (next to `standings`), registered in
`crates/server/src/routes/mod.rs`. The `Series` upsert already carries the new field. The route
gets a scenario and cassette in `crates/contract` (see `crates/contract/README.md`).

### Web

- **Button.** On the Race Results screen (`RaceResults` in `crates/app/src/routes/race_results.rs`),
  next to the `RgRaceYearSelector`: an `RgButton` labelled "Season Trophies" that navigates to
  `format!("/trophies/{}", selected_year().unwrap_or(0))` with `use_navigate`, as the "View
  Standings" button does.
- **Route.** `Route::SeasonTrophies` in `crates/app/src/state/route.rs` (path pattern
  `/trophies/:year`, label "Season Trophies", added to `Route::ENTRIES`), and a
  `<Route path=path!("/trophies/:year") ... ssr=SsrMode::Async/>` in `crates/app/src/app.rs`
  rendering a `SeasonTrophiesRoute` that passes `param_i32("year")` to `SeasonTrophies(year)` in
  `crates/app/src/routes/season_trophies.rs` (declared and re-exported in `routes/mod.rs`). The
  screen loads through `Loaded` over a server function `season_trophies(year)` in
  `crates/app/src/server_fns.rs` (`#[server(name = SeasonTrophiesCall, prefix = "/api",
  endpoint = "season_trophies", input = Json)]`, calling `get_season_trophies` with
  `ssr::now()`, as `series_standings` does) and renders through `async_view`. It works with
  `?nonav` like the standings pages (the nav bar reads it on every route).
- **Page layout** (`season_trophies.rs`), top to bottom:
  1. `<h1>` "Season Trophies {year}".
  2. **Races counted** table, one row per trophy group: series names, scheduled, sailed,
     cancelled, not yet sailed, drops. When no series has a group: "No series is assigned to a
     season trophy group for {year}. Set one on the Series screen."
  3. **One section per trophy** (PHRF, Windseekers FS, Windseekers NFS), each with:
     - `<h2>` trophy name and a winner line: "Winner: Boat – Skipper (total)", "Tied: A, B" when
       place 1 is shared, or "No eligible boat yet".
     - A caption with the counts: "Counting the best 5 of 6 Point + Harbor races (7
       scheduled, 1 drop, 1 cancelled), 16 of 20 Wednesday Windseekers, 2 of 3 Three Hour Tour".
     - The standings table, `RgTable` (it scrolls horizontally), with column groups per trophy
       group like the series standings table:
       `Place | Boat | Skipper | Member | [races of group 1 …] | Group 1 total (n of N) | [group 2 …] | … | Total | Notes`.
       A race cell shows the class place with the same suffixes as series standings (" DNS",
       " HOC3", " DSQ", " RC Volunteer"), in `text-danger-fg` when discarded, muted
       "Cancelled" / "Not yet sailed" / "Not sailed" when the race scores nothing, and
       " (sailed FS)" when the DNS comes from racing in another fleet. The RC average is
       fractional, e.g. `2.7 RC Volunteer`. Notes carry "Not a member" and "No race in Harbor
       Series", and ineligible rows have no place.
     - A footnote with the rule summary and GSI page references, including the tiebreak chain.
- **Labels.** Pure cell and winner-line helpers in `crates/app/src/utils/trophy_report.rs`
  (declared in `utils/mod.rs`), covered by `crates/app/tests/trophy_labels_test.rs`, as
  `standings_report.rs` is by `standings_labels_test.rs`. Class names stay literal strings so
  Tailwind finds them.
- **Admin.** `AddEditSeries` in `crates/app/src/routes/series.rs` gets an `RgDropdownNone` over
  `TrophyGroup::ENTRIES` labelled "Season trophy group" ("None" clears it);
  `SeriesState::edit_trophy_group`; the series table (`AllSeries`) gets a "Season trophy group"
  column. The dropdown sits in a `<div class="mb-4">`, not a `<p>`.

## Test strategy

- **Fixture.** Extend `crates/scoring/src/fixture.rs` with a season builder that shares one
  `FixtureRegistry` across series, so the same boats, classes and brackets keep their ids:
  ```rust
  let s = season(2026, |y| {
      y.series("Harbor Series", TrophyGroup::PointHarbor, Some("2026-01-24"), |s| {
          s.race(|r| {
              r.phrf_class("Fast", |c| c.bracket("I", |b| { b.boat("A", 48).finishes("14:10:00"); }));
              r.phrf_class("Half Fast", |c| c.bracket("III", |b| { b.boat("B", 120).finishes("14:30:00"); }));
          });
      });
      y.series("Early Spring Windseekers", TrophyGroup::WindseekersWednesday, None, |s| { /* ... */ });
  });
  s.trophies().assert_totals(TrophyFleet::Phrf, &[("A", 1), ("B", 2)]);
  ```
  with helpers `row(fleet, boat)`, `race_scores(fleet, boat)`, `places(fleet)`, in the shape
  of the standings assertions (`assert_bracket_totals`).
- **Rule tests** in `crates/scoring/tests/season_trophies_test.rs`, named after the GSI item or
  decision they enforce:
  - `t3_phrf_scores_the_class_finish_not_the_overall_finish` (T3)
  - `t3_dns_uses_the_starters_of_the_boats_class_in_that_series`,
    `t3_dns_class_falls_back_to_the_nearest_trophy_race` (T3)
  - `gsi_p5_2a_point_harbor_counts_the_best_6_of_7`, `gsi_p5_2b_wednesday_counts_the_best_16_of_20`, `gsi_p5_2c_three_hour_tour_counts_the_best_2_of_3` (T4)
  - `gsi_p5_3_cancelled_race_reduces_the_races_counted` (T4)
  - `t4_not_yet_sailed_race_neither_scores_nor_reduces_n`
  - `t4_n_comes_from_the_races_in_the_database` (a 6-race Point + Harbor year counts 5)
  - `t3_race_not_started_by_the_boats_class_reduces_its_n`
  - `gsi_14_1_2_missing_race_scores_class_starters_plus_one` (T3)
  - `gsi_p5_1_phrf_needs_a_race_in_every_trophy_series`, `t6_rc_volunteer_counts_as_participation`
  - `gsi_p5_1_and_p6_2_non_member_is_listed_but_not_placed` (T6)
  - `t6_places_are_assigned_among_eligible_boats_only`
  - `gsi_p6_1_windseekers_scores_the_class_finish_with_hoc_by_position` (T3)
  - `gsi_p6_6a_tie_goes_to_the_lowest_single_counted_score`, `gsi_p6_6b_then_to_the_count_of_it`,
    `gsi_p6_6c_then_to_the_next_lowest`, `t7_phrf_uses_the_same_chain`,
    `t7_discarded_scores_are_not_compared`, `t7_fully_tied_boats_share_the_place` (T7)
  - `gsi_1_4_1_2_rc_scores_the_average_of_the_other_class_finishes_in_that_series`,
    `gsi_1_4_1_2_rc_is_once_per_series`, `gsi_1_4_1_2_rc_score_is_not_used_in_the_tiebreak`,
    `t8_rc_with_no_other_race_scores_dns`, `t8_rc_average_rounds_to_the_tenth` (T8)
  - `t5_boat_that_changed_fleet_appears_under_both_with_dns_in_the_other`
  - `t9_boat_that_moved_between_fast_and_half_fast_keeps_one_phrf_row`
  - `t2_fleet_follows_the_rating_type_not_the_class_id` (two PHRF class pairs in one year)
  - `t2_orc_only_results_are_left_out`, `t3_orphan_results_are_left_out`
  - `t1_series_without_a_group_is_ignored`
  - `d8_row_shows_the_skipper_of_the_most_recent_race`
- **Invariants** (proptest, in `crates/scoring/tests/invariants_test.rs` or a new
  `season_trophies_invariants_test.rs`): shuffling the input series, races and cards changes
  nothing; every total is the sum of the counted scores; the number of counted races per group
  is `min(counted, scored)`; a boat appears at most once per trophy; eligible rows come first
  and carry contiguous shared places.
- **Golden.** `crates/server/tests/golden.rs` adds `trophies-{year}.json` for 2024–2026 to
  `crates/scoring/tests/golden/approved/` by mapping the snapshot's series names to groups in
  the test (no SQL change to the snapshot), computed with `now = DUMP_INSTANT`
  (`crates/server/tests/support/mod.rs`); `crates/scoring/tests/golden_test.rs` re-scores them
  from the approved race reports without a database, as it does the standings files. The 2024
  and 2025 files are a sanity check against the club's actual trophy results if those can be
  found.
- **Web.** `crates/app/tests/route_matcher_test.rs` for `/trophies/2026` (the
  every-pattern test covers `:year`); `trophy_labels_test.rs` for the cell text, suffixes,
  discard decoration and the winner line; an SSR render test (`common::render`, as the other
  component tests in `crates/app/tests/`) of a small `SeasonTrophies` section that checks the
  winner line and an ineligible row's note; and a `/trophies/2024` case in
  `crates/server/tests/ssr.rs`, rendered through the router with its data.

## Task checklist

Tick each box as it lands. Phases 1–3 are server-side and can be reviewed before the page.

### Phase 1: Trophy group on series

- [ ] `TrophyGroup` enum and `Series.trophy_group` in `crates/common/src/data/race_data.rs`
      (T1), skipped while `None`; `crates/common/tests/golden_round_trip.rs` stays green.
- [ ] Schema: a migration adding the nullable `trophy_group varchar` column to `series`, its
      upgrade step in `crates/service/src/startup.rs`, the `SCHEMA_VERSION` bump and the rest of
      what `CLAUDE.md` lists for a schema change; map the column in `to_series` and
      `upsert_series` (`crates/service/src/db/series.rs`). Start the server against the dev DB
      and check the column was added.
- [ ] `cargo xtask golden` and `cargo xtask contract` pass unchanged: the field is not written
      while `None`, so no approved file or cassette moves.
- [ ] Series admin: "Season trophy group" select in `AddEditSeries`, `edit_trophy_group` on
      `SeriesState`, and a column in the series table (`crates/app/src/routes/series.rs`).
- [ ] After deploying: set the groups on the production series (T1).

### Phase 2: Scoring

- [ ] `TrophyFleet`, `SeasonTrophies` and the row/race structs in `crates/common` (T2, model
      above).
- [ ] `season(year, |y| y.series(...))` builder and assertions in `crates/scoring/src/fixture.rs`.
- [ ] `crates/scoring/src/trophies.rs`: `trophies(year, series, reports, now)` (T3–T10).
- [ ] Rule tests in `crates/scoring/tests/season_trophies_test.rs` (list above), all green.
- [ ] Invariant tests.

### Phase 3: Endpoint and golden

- [ ] `api_paths::TROPHIES`, the `get_season_trophies(db, year, now)` wrapper in
      `crates/service/src/reporter.rs`, the `GET` handler in `crates/server/src/routes/series.rs`
      registered in `routes/mod.rs`.
- [ ] Contract scenario and cassette for `GET trophies` in `crates/contract`.
- [ ] The approved `trophies-{year}.json` files, checked by `crates/server/tests/golden.rs` and
      `crates/scoring/tests/golden_test.rs`; review the 2024 and 2025 winners against the club's
      records if available.
- [ ] Optional: extend `database/audit_scoring.py` to re-score the approved trophy files.

### Phase 4: Web

- [ ] `Route::SeasonTrophies` in `state/route.rs`, the `<Route>` in `app.rs`, the
      `route_matcher_test.rs` case.
- [ ] `season_trophies` server function in `server_fns.rs`.
- [ ] `crates/app/src/utils/trophy_report.rs` label helpers and `trophy_labels_test.rs`.
- [ ] `crates/app/src/routes/season_trophies.rs`: races-counted table, one section per trophy
      with winner line, caption, standings table and footnote; empty state when no series has a
      group.
- [ ] "Season Trophies" button next to the year select on the Race Results screen.
- [ ] `?nonav` works on the page.
- [ ] SSR render test for the page section and the `crates/server/tests/ssr.rs` case.

### Done

- [ ] `CLAUDE.md`: add `SeasonTrophies` to the screens in `routes/` and the trophies scoring
      (`trophies.rs`) to the `regatta-scoring` row of the crate table.
- [ ] Move this file to `docs/explore_implemented/` and update the link in
      `test_audit_and_plan.md`.
- [ ] Take the open questions to the race committee and record their answers here.
