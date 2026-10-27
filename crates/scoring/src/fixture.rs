//! Test fixture DSL for race and series scoring (= `Fixture.kt`, test_audit_and_plan.md §2).
//! Builds the same `RaceSchedule` / `RaceResultBoatBracket` records the database would, then
//! scores them through the pure seams [`card`], [`report`] and [`standings`]. No database.
//!
//! ```
//! use regatta_common::FinishCode;
//! use regatta_scoring::fixture::*;
//!
//! let r = race_on("2026-06-17", "18:00", |r| {
//!     r.phrf_class("Fast", |c| {
//!         c.bracket("I", |b| {
//!             b.boat("A", 48).finishes("20:51:26");
//!             b.boat("B", 72).code(FinishCode::Dnf);
//!         })
//!     })
//! });
//! r.report(&Default::default()).assert_bracket_scores(&[("A", 1), ("B", 2)]);
//! ```
//!
//! Times are Pacific local times. A boat named in a race with no outcome is RET, which is also the
//! `RaceResult` default. A boat not named in a race has no record, which standings score as DNS.
//!
//! Kotlin's named default arguments become `*_with(opts, ..)` variants taking a [`RaceOpts`] /
//! [`SeriesOpts`] / [`BoatOpts`] struct (`..Default::default()` for the rest), and the infix
//! outcome helpers are methods on the [`BoatEntry`] the `boat(..)` call returns.

use crate::{card, report, standings};
use jiff::civil::Date;
use jiff::{SignedDuration, Span, Timestamp};
use regatta_common::{
    Boat, BoatSkipper, Bracket, ClassSchedule, FinishCode, OrcCertificate, OrcScoringOption,
    Person, Race, RaceClass, RaceReport, RaceReportCard, RaceResult, RaceResultBoatBracket,
    RaceSchedule, RatingType, Series, StandingsBoatSkipper, StandingsSeries,
    CORRECTION_FACTOR_DEFAULT,
};
use std::collections::HashMap;

pub const PACIFIC_ZONE: &str = "America/Los_Angeles";

/// "2026-06-17" and "18:00[:ss[.SSS]]" in America/Los_Angeles. A time with its own date,
/// "2026-06-18 10:00:00", overrides `date`, for finishes on a later day.
pub fn pacific(date: &str, time: &str) -> Timestamp {
    let trimmed = time.trim();
    let (day, clock) = match trimmed.split_once(' ') {
        Some((day, clock)) => (day, clock),
        None => (date, trimmed),
    };
    let parts: Vec<&str> = clock.split(':').collect();
    let seconds = parts.get(2).copied().unwrap_or("0");
    let (whole_seconds, fraction) = seconds.split_once('.').unwrap_or((seconds, ""));
    let nanos: i32 = format!("{fraction:0<9}")[..9].parse().expect("nanos");
    let hour: i8 = parts[0].parse().expect("hour");
    let minute: i8 = parts
        .get(1)
        .copied()
        .unwrap_or("0")
        .parse()
        .expect("minute");
    let second: i8 = whole_seconds.parse().expect("second");
    let date: Date = day.parse().expect("date");
    date.at(hour, minute, second, nanos)
        .in_tz(PACIFIC_ZONE)
        .expect("Pacific local time")
        .timestamp()
}

/// Ids and shared entities, so the same boat, class and bracket keep their ids across the races of a series.
#[derive(Debug)]
pub struct FixtureRegistry {
    next_id: i64,
    boats: HashMap<String, Boat>,
    people: HashMap<String, Person>,
    classes: HashMap<String, RaceClass>,
    brackets: HashMap<(i64, String), Bracket>,
}

impl Default for FixtureRegistry {
    fn default() -> Self {
        FixtureRegistry {
            next_id: 1,
            boats: HashMap::new(),
            people: HashMap::new(),
            classes: HashMap::new(),
            brackets: HashMap::new(),
        }
    }
}

impl FixtureRegistry {
    pub fn id(&mut self) -> i64 {
        let id = self.next_id;
        self.next_id += 1;
        id
    }

    pub fn race_class(&mut self, name: &str, rating_type: RatingType) -> RaceClass {
        if let Some(existing) = self.classes.get(name) {
            return existing.clone();
        }
        let sort = self.classes.len() as i32;
        let race_class = RaceClass {
            id: self.id(),
            name: name.to_string(),
            sort,
            rating_type,
            ..RaceClass::default()
        };
        self.classes.insert(name.to_string(), race_class.clone());
        race_class
    }

    pub fn bracket(&mut self, race_class: &RaceClass, name: &str) -> Bracket {
        let key = (race_class.id, name.to_string());
        if let Some(existing) = self.brackets.get(&key) {
            return existing.clone();
        }
        let bracket = Bracket {
            id: self.id(),
            name: name.to_string(),
            class_id: race_class.id,
            ..Bracket::default()
        };
        self.brackets.insert(key, bracket.clone());
        bracket
    }

    pub fn person(&mut self, name: &str) -> Person {
        if let Some(existing) = self.people.get(name) {
            return existing.clone();
        }
        let (first, last) = name.split_once(' ').unwrap_or((name, ""));
        let person = Person {
            id: self.id(),
            first: first.to_string(),
            last: last.to_string(),
            ..Person::default()
        };
        self.people.insert(name.to_string(), person.clone());
        person
    }

    pub fn boat(
        &mut self,
        name: &str,
        rating_type: RatingType,
        rating: Option<i32>,
        skipper: Option<&str>,
    ) -> Boat {
        let person =
            self.person(&skipper.map_or_else(|| format!("{name} Skipper"), str::to_string));
        if let Some(existing) = self.boats.get(name) {
            return existing.clone();
        }
        let boat = Boat {
            id: self.id(),
            name: name.to_string(),
            sail_number: String::new(),
            phrf_rating: rating,
            skipper_id: Some(person.id),
            rating_type,
            ..Boat::default()
        };
        self.boats.insert(name.to_string(), boat.clone());
        boat
    }
}

/// One boat's entry in one race. Set its outcome with the methods below.
#[derive(Debug, Clone)]
pub struct BoatEntry {
    pub name: String,
    pub race_class: RaceClass,
    pub bracket: Bracket,
    pub boat: Boat,
    pub skipper: Person,
    pub rating_type: RatingType,
    pub rating: Option<i32>,
    finish: Option<String>,
    hoc_position: Option<i32>,
    finish_code: Option<FinishCode>,
    penalty_points: Option<i32>,
    orc_ref: Option<String>,
    legacy: bool,
    orphan: bool,
}

impl BoatEntry {
    fn new(
        name: &str,
        race_class: RaceClass,
        bracket: Bracket,
        boat: Boat,
        skipper: Person,
        rating_type: RatingType,
        rating: Option<i32>,
    ) -> Self {
        BoatEntry {
            name: name.to_string(),
            race_class,
            bracket,
            boat,
            skipper,
            rating_type,
            rating,
            finish: None,
            hoc_position: None,
            finish_code: None,
            penalty_points: None,
            orc_ref: None,
            legacy: false,
            orphan: false,
        }
    }

    /// Timed finish at a Pacific local time on the race date, e.g. "20:51:26", "20:51:26.250", or "2026-06-18 10:00:00" for a later day.
    pub fn finishes(&mut self, time: &str) -> &mut Self {
        self.finish = Some(time.to_string());
        self.hoc_position = None;
        self
    }

    /// Horned on course at the given position.
    pub fn hoc(&mut self, position: i32) -> &mut Self {
        self.hoc_position = Some(position);
        self.finish = None;
        self
    }

    /// A finish code other than TIME or HOC: DNF, RET, NSC, DNS_RC.
    pub fn code(&mut self, code: FinishCode) -> &mut Self {
        self.finish_code = Some(code);
        self
    }

    /// Scoring penalty in places (GSI 9.2 / 14.2).
    pub fn penalty(&mut self, places: i32) -> &mut Self {
        self.penalty_points = Some(places);
        self
    }

    /// ORC certificate reference, looked up in the map passed to [`RaceFixture::report`].
    pub fn orc(&mut self, reference: &str) -> &mut Self {
        self.orc_ref = Some(reference.to_string());
        self
    }

    /// A legacy row with no finish code stored: the result keeps the RaceResult default (TIME with a finish, else RET).
    pub fn legacy(&mut self) -> &mut Self {
        self.legacy = true;
        self
    }

    /// A result whose bracket no longer matches the race (bracket id 0), which the report lists under "Incorrectly classed".
    pub fn orphan(&mut self) -> &mut Self {
        self.orphan = true;
        self
    }

    fn result(
        &self,
        registry: &mut FixtureRegistry,
        race_id: i64,
        date: &str,
    ) -> RaceResultBoatBracket {
        let finish_instant = self.finish.as_deref().map(|time| pacific(date, time));
        let finish_code = if self.legacy {
            // left at the data class default, as the row mapper does for a NULL finish_code
            if finish_instant.is_some() {
                FinishCode::Time
            } else {
                FinishCode::Ret
            }
        } else if finish_instant.is_some() {
            FinishCode::Time
        } else if self.hoc_position.is_some() {
            FinishCode::Hoc
        } else {
            self.finish_code.unwrap_or(FinishCode::Ret)
        };
        let result = RaceResult {
            id: registry.id(),
            race_id,
            boat_id: self.boat.id,
            finish: finish_instant,
            phrf_rating: self.rating,
            orc_ref: self.orc_ref.clone(),
            hoc_position: self.hoc_position,
            bracket_id: if self.orphan { 0 } else { self.bracket.id },
            race_class_id: if self.orphan { 0 } else { self.race_class.id },
            penalty: self.penalty_points,
            rating_type: self.rating_type,
            finish_code,
        };
        RaceResultBoatBracket {
            result,
            boat_skipper: BoatSkipper {
                skipper: Some(self.skipper.clone()),
                boat: Some(self.boat.clone()),
            },
            bracket: if self.orphan {
                Bracket::default()
            } else {
                self.bracket.clone()
            },
        }
    }
}

/// The optional arguments of `boat(..)`: the skipper (a person named after the boat by default)
/// and the rating type (the class's by default).
#[derive(Debug, Clone, Default)]
pub struct BoatOpts<'a> {
    pub skipper: Option<&'a str>,
    pub rating_type: Option<RatingType>,
}

pub struct BracketBuilder<'a> {
    registry: &'a mut FixtureRegistry,
    race_class: RaceClass,
    bracket: Bracket,
    entries: &'a mut Vec<BoatEntry>,
}

impl BracketBuilder<'_> {
    /// Enters a boat. `rating` is the PHRF rating used for this result (`None` for an unrated
    /// entry). The rating type defaults to the class's, the skipper to a person named after the boat.
    pub fn boat(&mut self, name: &str, rating: impl Into<Option<i32>>) -> &mut BoatEntry {
        self.boat_with(name, rating, BoatOpts::default())
    }

    pub fn boat_with(
        &mut self,
        name: &str,
        rating: impl Into<Option<i32>>,
        opts: BoatOpts<'_>,
    ) -> &mut BoatEntry {
        let rating = rating.into();
        let rating_type = opts.rating_type.unwrap_or(self.race_class.rating_type);
        let boat = self.registry.boat(name, rating_type, rating, opts.skipper);
        let person = self.registry.person(
            &opts
                .skipper
                .map_or_else(|| format!("{name} Skipper"), str::to_string),
        );
        self.entries.push(BoatEntry::new(
            name,
            self.race_class.clone(),
            self.bracket.clone(),
            boat,
            person,
            rating_type,
            rating,
        ));
        self.entries.last_mut().expect("just pushed")
    }
}

pub struct ClassBuilder<'a> {
    registry: &'a mut FixtureRegistry,
    race_class: RaceClass,
    entries: &'a mut Vec<BoatEntry>,
    brackets: Vec<Bracket>,
}

impl ClassBuilder<'_> {
    pub fn bracket(&mut self, name: &str, block: impl FnOnce(&mut BracketBuilder<'_>)) {
        let bracket = self.registry.bracket(&self.race_class, name);
        self.brackets.push(bracket.clone());
        let mut builder = BracketBuilder {
            registry: self.registry,
            race_class: self.race_class.clone(),
            bracket,
            entries: self.entries,
        };
        block(&mut builder);
    }
}

/// The arguments of `race(..)`. `name` defaults to "Race" (standalone) or "Race N" (in a
/// series); `date` to "2026-06-17" (standalone) or a week after the previous race (in a series).
#[derive(Debug, Clone)]
pub struct RaceOpts {
    pub name: Option<String>,
    pub date: Option<String>,
    pub start: String,
    pub b_factor: i32,
    pub orc_scoring_option: OrcScoringOption,
}

impl Default for RaceOpts {
    fn default() -> Self {
        RaceOpts {
            name: None,
            date: None,
            start: "18:00".to_string(),
            b_factor: CORRECTION_FACTOR_DEFAULT,
            orc_scoring_option: OrcScoringOption::FiveBandWindwardLeeward,
        }
    }
}

pub struct RaceBuilder<'a> {
    registry: &'a mut FixtureRegistry,
    name: String,
    date: String,
    start: String,
    b_factor: i32,
    orc_scoring_option: OrcScoringOption,
    series_id: Option<i64>,
    race_id: i64,
    schedules: Vec<ClassSchedule>,
    entries: Vec<BoatEntry>,
}

/// A race defined by its builder, before its result records get their ids.
#[derive(Debug, Clone)]
pub struct RaceData {
    name: String,
    date: String,
    b_factor: i32,
    orc_scoring_option: OrcScoringOption,
    series_id: Option<i64>,
    race_id: i64,
    schedules: Vec<ClassSchedule>,
    entries: Vec<BoatEntry>,
}

impl<'a> RaceBuilder<'a> {
    fn new(
        registry: &'a mut FixtureRegistry,
        name: String,
        date: String,
        opts: &RaceOpts,
        series_id: Option<i64>,
    ) -> Self {
        let race_id = registry.id();
        RaceBuilder {
            registry,
            name,
            date,
            start: opts.start.clone(),
            b_factor: opts.b_factor,
            orc_scoring_option: opts.orc_scoring_option,
            series_id,
            race_id,
            schedules: Vec::new(),
            entries: Vec::new(),
        }
    }

    pub fn phrf_class(&mut self, name: &str, block: impl FnOnce(&mut ClassBuilder<'_>)) {
        self.race_class(name, RatingType::Phrf, None, block)
    }

    pub fn orc_phrf_class(&mut self, name: &str, block: impl FnOnce(&mut ClassBuilder<'_>)) {
        self.race_class(name, RatingType::OrcPhrf, None, block)
    }

    pub fn orc_class(&mut self, name: &str, block: impl FnOnce(&mut ClassBuilder<'_>)) {
        self.race_class(name, RatingType::Orc, None, block)
    }

    /// A non-flying-sails cruising class; see [`RaceBuilder::cruising_class_with`] for flying sails.
    pub fn cruising_class(&mut self, name: &str, block: impl FnOnce(&mut ClassBuilder<'_>)) {
        self.cruising_class_with(name, false, None, block)
    }

    pub fn cruising_class_with(
        &mut self,
        name: &str,
        flying: bool,
        start: Option<&str>,
        block: impl FnOnce(&mut ClassBuilder<'_>),
    ) {
        let rating_type = if flying {
            RatingType::CruisingFlyingSails
        } else {
            RatingType::CruisingNonFlyingSails
        };
        self.race_class(name, rating_type, start, block)
    }

    /// A class with its own start time ("HH:mm\[:ss\]", Pacific), defaulting to the race start.
    pub fn race_class(
        &mut self,
        name: &str,
        rating_type: RatingType,
        start: Option<&str>,
        block: impl FnOnce(&mut ClassBuilder<'_>),
    ) {
        let race_class = self.registry.race_class(name, rating_type);
        let mut class_builder = ClassBuilder {
            registry: self.registry,
            race_class: race_class.clone(),
            entries: &mut self.entries,
            brackets: Vec::new(),
        };
        block(&mut class_builder);
        let brackets = class_builder.brackets;
        let start_instant = pacific(&self.date, start.unwrap_or(&self.start));
        self.schedules.push(ClassSchedule {
            race_class,
            brackets,
            start_date: start_instant,
            end_date: start_instant
                .checked_add(SignedDuration::from_hours(6))
                .expect("end date"),
        });
    }

    fn finish(self) -> RaceData {
        RaceData {
            name: self.name,
            date: self.date,
            b_factor: self.b_factor,
            orc_scoring_option: self.orc_scoring_option,
            series_id: self.series_id,
            race_id: self.race_id,
            schedules: self.schedules,
            entries: self.entries,
        }
    }
}

impl RaceData {
    fn build(self, registry: &mut FixtureRegistry) -> RaceFixture {
        assert!(
            !self.schedules.is_empty(),
            "race '{}' needs at least one class",
            self.name
        );
        let race = Race {
            id: self.race_id,
            name: self.name,
            series_id: self.series_id,
            phrf_b_factor: self.b_factor,
            orc_scoring_option: self.orc_scoring_option,
            ..Race::default()
        };
        let schedule = RaceSchedule {
            race,
            result_count: self.entries.len() as i64,
            schedule: self.schedules,
            ..RaceSchedule::default()
        };
        let results = self
            .entries
            .iter()
            .map(|entry| entry.result(registry, self.race_id, &self.date))
            .collect();
        RaceFixture {
            date: self.date,
            schedule,
            results,
        }
    }
}

/// A built race: its schedule and result records, scored on demand.
#[derive(Debug, Clone)]
pub struct RaceFixture {
    pub date: String,
    pub schedule: RaceSchedule,
    pub results: Vec<RaceResultBoatBracket>,
}

impl RaceFixture {
    /// Scores the race through the pure seams. `orc_certs` resolves `orc` references.
    pub fn report(&self, orc_certs: &HashMap<String, OrcCertificate>) -> RaceReport {
        let starts: HashMap<i64, Timestamp> = self
            .schedule
            .schedule
            .iter()
            .map(|s| (s.race_class.id, s.start_date))
            .collect();
        let cards = self
            .results
            .iter()
            .map(|record| {
                let cert = record.result.orc_ref.as_deref().map(|reference| {
                    orc_certs
                        .get(reference)
                        .unwrap_or_else(|| panic!("no certificate for orc ref {reference}"))
                });
                card(
                    record.clone(),
                    starts.get(&record.bracket.class_id).copied(),
                    &self.schedule.race,
                    cert,
                )
            })
            .collect();
        report(self.schedule.clone(), cards)
    }
}

/// A standalone race with the default name, date, start, B factor and ORC option.
pub fn race(block: impl FnOnce(&mut RaceBuilder<'_>)) -> RaceFixture {
    race_with(RaceOpts::default(), block)
}

/// A standalone race on `date` starting at `start` (Pacific).
pub fn race_on(date: &str, start: &str, block: impl FnOnce(&mut RaceBuilder<'_>)) -> RaceFixture {
    race_with(
        RaceOpts {
            date: Some(date.to_string()),
            start: start.to_string(),
            ..RaceOpts::default()
        },
        block,
    )
}

/// A standalone race.
pub fn race_with(opts: RaceOpts, block: impl FnOnce(&mut RaceBuilder<'_>)) -> RaceFixture {
    let mut registry = FixtureRegistry::default();
    race_in(&mut registry, opts, block)
}

/// A standalone race sharing `registry` with other fixtures.
pub fn race_in(
    registry: &mut FixtureRegistry,
    opts: RaceOpts,
    block: impl FnOnce(&mut RaceBuilder<'_>),
) -> RaceFixture {
    let name = opts.name.clone().unwrap_or_else(|| "Race".to_string());
    let date = opts
        .date
        .clone()
        .unwrap_or_else(|| "2026-06-17".to_string());
    let mut builder = RaceBuilder::new(registry, name, date, &opts, None);
    block(&mut builder);
    builder.finish().build(registry)
}

pub struct SeriesBuilder {
    registry: FixtureRegistry,
    series_id: i64,
    first_date: String,
    builders: Vec<RaceData>,
    volunteers: Vec<(String, usize)>,
}

impl SeriesBuilder {
    /// Adds a race a week after the previous one, starting at the series' first date.
    pub fn race(&mut self, block: impl FnOnce(&mut RaceBuilder<'_>)) {
        self.race_with(RaceOpts::default(), block)
    }

    /// Adds a race. `date` defaults to a week after the previous race, starting at the series'
    /// first date.
    pub fn race_with(&mut self, opts: RaceOpts, block: impl FnOnce(&mut RaceBuilder<'_>)) {
        let name = opts
            .name
            .clone()
            .unwrap_or_else(|| format!("Race {}", self.builders.len() + 1));
        let race_date = opts.date.clone().unwrap_or_else(|| {
            let first: Date = self.first_date.parse().expect("first date");
            first
                .checked_add(Span::new().days(7 * self.builders.len() as i64))
                .expect("race date")
                .to_string()
        });
        let mut builder = RaceBuilder::new(
            &mut self.registry,
            name,
            race_date,
            &opts,
            Some(self.series_id),
        );
        block(&mut builder);
        self.builders.push(builder.finish());
    }

    /// Enters `boat` as an RC volunteer (DNS_RC) in race number `race` (1-based), in the class and
    /// bracket the boat sailed in its nearest other race of the series. The boat must not already
    /// be entered in that race.
    pub fn rc_volunteer(&mut self, boat: &str, race: usize) {
        self.volunteers.push((boat.to_string(), race));
    }

    fn build(mut self, series: Series, year: i32) -> SeriesFixture {
        for (boat_name, race_number) in std::mem::take(&mut self.volunteers) {
            assert!(
                race_number >= 1 && race_number <= self.builders.len(),
                "series has no race {race_number}"
            );
            let target_index = race_number - 1;
            assert!(
                !self.builders[target_index]
                    .entries
                    .iter()
                    .any(|e| e.name == boat_name),
                "{boat_name} is already entered in race {race_number}"
            );
            let mut by_distance: Vec<usize> = (0..self.builders.len()).collect();
            by_distance.sort_by_key(|&i| (i as i64 - target_index as i64).abs());
            let example = by_distance
                .iter()
                .find_map(|&i| {
                    self.builders[i]
                        .entries
                        .iter()
                        .find(|e| e.name == boat_name)
                        .cloned()
                })
                .unwrap_or_else(|| {
                    panic!("{boat_name} has no other entry in the series to take its class and bracket from")
                });
            let mut entry = BoatEntry::new(
                &example.name,
                example.race_class.clone(),
                example.bracket.clone(),
                example.boat.clone(),
                example.skipper.clone(),
                example.rating_type,
                example.rating,
            );
            entry.finish_code = Some(FinishCode::DnsRc);
            entry.orc_ref = example.orc_ref.clone();
            self.builders[target_index].entries.push(entry);
        }
        let registry = &mut self.registry;
        let races = self
            .builders
            .into_iter()
            .map(|data| data.build(registry))
            .collect();
        SeriesFixture {
            series,
            year,
            races,
        }
    }
}

/// A built series: its races, scored on demand.
#[derive(Debug, Clone)]
pub struct SeriesFixture {
    pub series: Series,
    pub year: i32,
    pub races: Vec<RaceFixture>,
}

impl SeriesFixture {
    pub fn reports(&self, orc_certs: &HashMap<String, OrcCertificate>) -> Vec<RaceReport> {
        self.races
            .iter()
            .map(|race| race.report(orc_certs))
            .collect()
    }

    /// The default `now` of [`SeriesFixture::standings`]: a day after the last race.
    pub fn default_now(&self) -> Timestamp {
        self.races
            .iter()
            .filter_map(|race| race.schedule.end_time())
            .max()
            .expect("a series has a race")
            .checked_add(SignedDuration::from_hours(24))
            .expect("now")
    }

    /// Standings through the pure seam, with `now` a day after the last race and no certificates.
    pub fn standings(&self) -> StandingsSeries {
        self.standings_with(None, &HashMap::new())
    }

    /// Standings through the pure seam. `now` defaults to a day after the last race.
    pub fn standings_with(
        &self,
        now: Option<Timestamp>,
        orc_certs: &HashMap<String, OrcCertificate>,
    ) -> StandingsSeries {
        let now = now.unwrap_or_else(|| self.default_now());
        standings(
            self.series.clone(),
            self.year,
            &self.reports(orc_certs),
            now,
        )
    }
}

/// The arguments of `series(..)`. `first_date` defaults to June 17 of `year`.
#[derive(Debug, Clone)]
pub struct SeriesOpts {
    pub name: String,
    pub year: i32,
    pub first_date: Option<String>,
}

impl Default for SeriesOpts {
    fn default() -> Self {
        SeriesOpts {
            name: "Series".to_string(),
            year: 2026,
            first_date: None,
        }
    }
}

/// A series of races that share boats, classes and brackets.
pub fn series(block: impl FnOnce(&mut SeriesBuilder)) -> SeriesFixture {
    series_with(SeriesOpts::default(), block)
}

pub fn series_with(opts: SeriesOpts, block: impl FnOnce(&mut SeriesBuilder)) -> SeriesFixture {
    let mut registry = FixtureRegistry::default();
    let series = Series {
        id: registry.id(),
        name: opts.name,
        ..Series::default()
    };
    let first_date = opts
        .first_date
        .unwrap_or_else(|| format!("{}-06-17", opts.year));
    let mut builder = SeriesBuilder {
        registry,
        series_id: series.id,
        first_date,
        builders: Vec::new(),
        volunteers: Vec::new(),
    };
    block(&mut builder);
    builder.build(series, opts.year)
}

// ---- assertions: scores are compared as f64 so the same tests compile before and after D1

/// A map literal: `scores(&[("A", 1), ("B", 2)])`.
pub fn scores<T: Into<f64> + Copy>(entries: &[(&str, T)]) -> HashMap<String, f64> {
    entries
        .iter()
        .map(|(boat, score)| (boat.to_string(), (*score).into()))
        .collect()
}

/// `places(&[("A", 1)])`: a map literal of series places.
pub fn places(entries: &[(&str, i32)]) -> HashMap<String, i32> {
    entries
        .iter()
        .map(|(boat, place)| (boat.to_string(), *place))
        .collect()
}

/// Asserts each expected boat's score. Boats not listed are not checked.
pub fn assert_scores<T: Into<f64> + Copy>(actual: &HashMap<String, f64>, expected: &[(&str, T)]) {
    for (boat, score) in expected {
        let value = actual
            .get(*boat)
            .unwrap_or_else(|| panic!("no score for {boat} in {actual:?}"));
        let expected: f64 = (*score).into();
        assert_eq!(expected, *value, "score of {boat}");
    }
}

/// The race report helpers of the Kotlin fixture, as an extension trait.
pub trait RaceReportExt {
    fn cards(&self) -> Vec<&RaceReportCard>;
    /// Bracket scores of the race by boat name.
    fn bracket_scores(&self) -> HashMap<String, f64>;
    /// Class scores of the race by boat name.
    fn class_scores(&self) -> HashMap<String, f64>;
    /// ORC bracket scores of the race by boat name (0 when the boat has no ORC place).
    fn bracket_scores_orc(&self) -> HashMap<String, f64>;
    /// ORC class scores of the race by boat name (0 when the boat has no ORC place).
    fn class_scores_orc(&self) -> HashMap<String, f64>;
    fn assert_bracket_scores<T: Into<f64> + Copy>(&self, expected: &[(&str, T)]) {
        assert_scores(&self.bracket_scores(), expected)
    }
    fn assert_class_scores<T: Into<f64> + Copy>(&self, expected: &[(&str, T)]) {
        assert_scores(&self.class_scores(), expected)
    }
    /// The card of one boat (its first card).
    fn card(&self, boat: &str) -> &RaceReportCard {
        self.cards()
            .into_iter()
            .find(|card| card.boat_name == boat)
            .unwrap_or_else(|| panic!("no card for {boat}"))
    }
}

/// `associate`: a later card with the same boat name overrides an earlier one.
fn associate(
    cards: &[&RaceReportCard],
    value: impl Fn(&RaceReportCard) -> f64,
) -> HashMap<String, f64> {
    let mut map = HashMap::new();
    for card in cards {
        map.insert(card.boat_name.clone(), value(card));
    }
    map
}

impl RaceReportExt for RaceReport {
    fn cards(&self) -> Vec<&RaceReportCard> {
        self.class_reports
            .iter()
            .flat_map(|c| c.bracket_report.iter())
            .flat_map(|b| b.cards.iter())
            .collect()
    }

    fn bracket_scores(&self) -> HashMap<String, f64> {
        associate(&self.cards(), |c| f64::from(c.place_in_bracket))
    }

    fn class_scores(&self) -> HashMap<String, f64> {
        associate(&self.cards(), |c| f64::from(c.place_in_class))
    }

    fn bracket_scores_orc(&self) -> HashMap<String, f64> {
        associate(&self.cards(), |c| f64::from(c.place_in_bracket_orc))
    }

    fn class_scores_orc(&self) -> HashMap<String, f64> {
        associate(&self.cards(), |c| f64::from(c.place_in_class_orc))
    }
}

fn boat_name(row: &StandingsBoatSkipper) -> String {
    row.boat_skipper
        .boat
        .as_ref()
        .map_or_else(String::new, |b| b.name.clone())
}

/// `groupBy { boat name }.mapValues { first() }`: the first row of each boat.
fn first_rows<'a>(rows: &[&'a StandingsBoatSkipper]) -> Vec<&'a StandingsBoatSkipper> {
    let mut seen: Vec<String> = Vec::new();
    let mut out = Vec::new();
    for row in rows {
        let name = boat_name(row);
        if !seen.contains(&name) {
            seen.push(name);
            out.push(*row);
        }
    }
    out
}

/// The standings helpers of the Kotlin fixture, as an extension trait. A boat that appears in
/// more than one bracket keeps its first row.
pub trait StandingsExt {
    fn rows(&self) -> Vec<&StandingsBoatSkipper>;
    /// The standings row of one boat (its first row, if it appears in more than one bracket).
    fn row(&self, boat: &str) -> &StandingsBoatSkipper {
        self.rows()
            .into_iter()
            .find(|row| boat_name(row) == boat)
            .unwrap_or_else(|| panic!("no standings row for {boat}"))
    }
    fn row_values<T>(&self, value: impl Fn(&StandingsBoatSkipper) -> T) -> HashMap<String, T> {
        first_rows(&self.rows())
            .into_iter()
            .map(|row| (boat_name(row), value(row)))
            .collect()
    }
    /// Bracket totals by boat name.
    fn bracket_totals(&self) -> HashMap<String, f64> {
        self.row_values(|r| r.total_score_bracket)
    }
    /// Class totals by boat name.
    fn class_totals(&self) -> HashMap<String, f64> {
        self.row_values(|r| r.total_score_class)
    }
    /// Series places in bracket by boat name.
    fn bracket_places(&self) -> HashMap<String, i32> {
        self.row_values(|r| r.place_in_bracket)
    }
    /// Series places in class by boat name.
    fn class_places(&self) -> HashMap<String, i32> {
        self.row_values(|r| r.place_in_class)
    }
    /// The per-race bracket scores of one boat, in race order.
    fn bracket_race_scores(&self, boat: &str) -> Vec<f64> {
        self.row(boat)
            .race_standings
            .iter()
            .map(|r| r.place_in_bracket)
            .collect()
    }
    /// The per-race class scores of one boat, in race order.
    fn class_race_scores(&self, boat: &str) -> Vec<f64> {
        self.row(boat)
            .race_standings
            .iter()
            .map(|r| r.place_in_class)
            .collect()
    }
    fn assert_bracket_totals<T: Into<f64> + Copy>(&self, expected: &[(&str, T)]) {
        assert_scores(&self.bracket_totals(), expected)
    }
    fn assert_class_totals<T: Into<f64> + Copy>(&self, expected: &[(&str, T)]) {
        assert_scores(&self.class_totals(), expected)
    }

    // ---- the ORC side of the same rows (D9); 0 for a row not scored under ORC

    fn bracket_totals_orc(&self) -> HashMap<String, f64> {
        self.row_values(|r| r.total_score_bracket_orc)
    }
    fn class_totals_orc(&self) -> HashMap<String, f64> {
        self.row_values(|r| r.total_score_class_orc)
    }
    fn bracket_places_orc(&self) -> HashMap<String, i32> {
        self.row_values(|r| r.place_in_bracket_orc)
    }
    fn class_places_orc(&self) -> HashMap<String, i32> {
        self.row_values(|r| r.place_in_class_orc)
    }
    fn bracket_race_scores_orc(&self, boat: &str) -> Vec<f64> {
        self.row(boat)
            .race_standings
            .iter()
            .map(|r| r.place_in_bracket_orc)
            .collect()
    }
    fn class_race_scores_orc(&self, boat: &str) -> Vec<f64> {
        self.row(boat)
            .race_standings
            .iter()
            .map(|r| r.place_in_class_orc)
            .collect()
    }
    fn assert_bracket_totals_orc<T: Into<f64> + Copy>(&self, expected: &[(&str, T)]) {
        assert_scores(&self.bracket_totals_orc(), expected)
    }
    fn assert_class_totals_orc<T: Into<f64> + Copy>(&self, expected: &[(&str, T)]) {
        assert_scores(&self.class_totals_orc(), expected)
    }
}

impl StandingsExt for StandingsSeries {
    fn rows(&self) -> Vec<&StandingsBoatSkipper> {
        self.standings
            .iter()
            .flat_map(|c| c.standings.iter())
            .flat_map(|b| b.standings.iter())
            .collect()
    }
}
