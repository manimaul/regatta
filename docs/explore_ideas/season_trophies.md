# Season Trophies (Racer of the Year)

Idea for having the app compute the CYCT overall season trophies. These are described on pages 5–6
of the General Sailing Instructions (GSI):
[`../explore_implemented/2026-CYCT-General-Sailing-Instructions.pdf`](../explore_implemented/2026-CYCT-General-Sailing-Instructions.pdf).
This was split out of [`test_audit_and_plan.md`](../explore_implemented/test_audit_and_plan.md). It builds on that
plan's race and series scoring rules (D1–D8), especially the definition of starters (D3), the RC
volunteer average (D1), unsailed races (D2) and fractional scores.

## What the GSI says

- **PHRF Racer of the Year Perpetual Trophy.** Uses each boat's finish position in each race. The
  boat must be a CYCT member and must sail at least one race in each series. The tiebreak is
  count-back: the boat with more of the tied lowest score wins, then the next lowest score is
  compared, and so on.
- **Windseekers Racer of the Year Perpetual Trophy.** Uses individual class finishes, with HOC
  counted by position on the course. Members only. The tiebreak is the lowest single score first,
  then count-back.
- **Races scored, for both trophies:**
  - Top 6 of the 7 combined Point and Harbor Series races.
  - Top 16 of the 20 Wednesday Windseekers races.
  - Top 2 of the 3 Three Hour Tour races.
- A cancelled, abandoned or invalidated race counts as a discard. The number of top races counted
  is reduced by one for each such race.
- The total is the lowest combined points.
- RC volunteer (GSI 1.4.1.2): the score for racer of the year is the average of the boat's overall
  finishes in that series.

## Decisions (answered 2026-10-14)

- **Trophy groups** come from a new nullable `trophyGroup` field on `Series`: `PointHarbor`,
  `WindseekersWednesday`, `ThreeHourTour` or `null`.
  - The field is an enum in `common`, a column in `SeriesTable` (Exposed adds the nullable column
    on startup, so no versioned migration is needed), and a select on the Series admin screen.
  - A series with `null` doesn't count toward any trophy.
- **PHRF Racer of the Year** is **one trophy** across all PHRF-rated classes (`PHRF`, `ORC_PHRF`).
  - It uses the boat's **class place** (`placeInClass`) in each race.
  - Eligibility requires at least one race in each series that has a trophy group.
  - The tiebreak is count-back. If still tied, the boats share the place.
- **Windseekers Racer of the Year** is **one trophy per class**: Windseekers Flying Sails and
  Windseekers Non-Flying Sails.
  - It uses the boat's class finish in each race, with HOC as positioned.
  - The tiebreak is the lowest single score first, then count-back. If still tied, the boats share
    the place.
- **Top-N per group:** N = scheduled races − fixed drops − cancelled races. The fixed drops are 1
  for Point + Harbor (7 races), 4 for Wednesday Windseekers (20 races) and 1 for Three Hour Tour
  (3 races). "Scheduled" means the races in the DB for that series and year. "Cancelled" means a
  race with no results whose end time has passed (see D2 in `test_audit_and_plan.md`).
- A race the boat didn't sail scores **DNS = starters + 1**, using the class starters. The top-N
  rule then drops the worst scores.
- An RC volunteer race scores the average of the boat's class finishes in that series. The rule
  is fractional, used once per series, and a boat with no other race scores DNS (see D1).
- Every boat is shown, and boats whose skipper has `Person.clubMember` are flagged as eligible.
  The "member as of April 1" check stays manual.
- Results are served at a new public endpoint, `ApiPaths.seasonStandings`
  (`/season/standings?year=`), and shown on a new web route next to Series standings.

## Open questions

- A boat that changes class during the year (e.g. FS ↔ NFS, or Windseekers ↔ PHRF): do its races
  in the old class count toward the new class's trophy, or does it appear in both?
- Is the "Challenger" fleet mentioned on page 5 still a separate trophy, or is it the same as
  Windseekers NFS?
- Should future (not yet sailed) races reduce N during the season, so the standings show
  "current pace"? Or should they count only once they're sailed or cancelled?
- How should the "member as of April 1" rule be handled if the manual check isn't enough? A
  membership date on `Person` is one option.

## Implementation and test strategy

- A pure function in `server`, `seasonStandings(year, series: List<Series>, reports: List<RaceReport>, now)`,
  is built on the same race reports and seams as series standings (`test_audit_and_plan.md` §1). It uses
  the same fixture DSL.
- Rule tests in `:server:jvmTest`, named after the GSI page 5–6 items, cover:
  - Group membership and top-N.
  - Cancelled-race drops.
  - DNS scoring.
  - PHRF one-race-per-series eligibility.
  - Member flagging.
  - The RC-volunteer average.
  - Both tiebreak chains, including a tie that stays shared.
  - Per-trophy place source: class place for PHRF, class finish per Windseekers class.
- Invariants: shuffling the input order never changes any result, and a boat's total equals the
  sum of its best N scores.
- Golden check: run against the 2024–2026 snapshot from `test_audit_and_plan.md` §5 once the series have
  their `trophyGroup` set. The club's actual trophy results for past years, if available, make a
  good acceptance check.

## Suggested order

1. Finish the series scoring fixes in `test_audit_and_plan.md`, D1–D3 at least.
2. The `trophyGroup` field and the admin UI.
3. The pure function and its rule tests.
4. The endpoint and the web route.
