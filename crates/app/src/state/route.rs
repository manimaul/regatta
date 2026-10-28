//! Every screen's path pattern and nav label.
//!
//! The Leptos router owns navigation and history; this enum keeps the nav bar's list, its labels and the
//! active-link test ([`crate::utils::route_matcher`]).

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Route {
    Home,
    Series,
    Course,
    People,
    Admin,
    AdminCreate,
    Races,
    Boats,
    Classes,
    RaceResult,
    RaceResultView,
    RaceResultEdit,
    SeriesStandingsView,
    Rc,
    NotFound,
}

impl Route {
    /// Every route in declaration order, the order the matcher tries them in.
    pub const ENTRIES: [Route; 15] = [
        Route::Home,
        Route::Series,
        Route::Course,
        Route::People,
        Route::Admin,
        Route::AdminCreate,
        Route::Races,
        Route::Boats,
        Route::Classes,
        Route::RaceResult,
        Route::RaceResultView,
        Route::RaceResultEdit,
        Route::SeriesStandingsView,
        Route::Rc,
        Route::NotFound,
    ];

    pub const fn path_pattern(self) -> &'static str {
        match self {
            Route::Home => "/",
            Route::Series => "/series",
            Route::Course => "/course",
            Route::People => "/people",
            Route::Admin => "/admin",
            Route::AdminCreate => "/admin/create",
            Route::Races => "/races",
            Route::Boats => "/boats",
            Route::Classes => "/class",
            Route::RaceResult => "/races/results",
            Route::RaceResultView => "/races/results/view/:id",
            Route::RaceResultEdit => "/races/results/:id",
            Route::SeriesStandingsView => "/series/standings/view/:id/:year",
            Route::Rc => "/rc",
            Route::NotFound => "/404",
        }
    }

    pub const fn label(self) -> &'static str {
        match self {
            Route::Home => "Home",
            Route::Series => "Series",
            Route::Course => "Course",
            Route::People => "Skippers",
            Route::Admin => "Admin",
            Route::AdminCreate => "Create Admin",
            Route::Races => "Races",
            Route::Boats => "Boats",
            Route::Classes => "Classes",
            Route::RaceResult => "Race Results",
            Route::RaceResultView => "Race Result",
            Route::RaceResultEdit => "Edit Race Results",
            Route::SeriesStandingsView => "Series Standings",
            Route::Rc => "RC",
            Route::NotFound => "NotFound",
        }
    }
}
