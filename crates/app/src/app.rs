//! = `Router.kt` + `Main.js.kt`: the page frame (nav bar, alerts, the routed screen, footer) and the
//! 15 routes.

use crate::chart::resize_observer::observe_scroll_height;
use crate::components::alerts::Alerts;
use crate::components::nav_bar::NavBar;
use crate::components::rg_grid::{Customizer, RgContainerType, RgDiv, RgGrid, RgSpace, RgSz};
use crate::components::ElementIds;
use crate::routes::*;
use crate::state::alerts::provide_alerts;
use crate::state::login::provide_login_session;
use crate::state::theme::{provide_theme, Theme};
use leptos::prelude::*;
use leptos_meta::{provide_meta_context, Title};
use leptos_router::components::{Route, Router, Routes};
use leptos_router::hooks::use_params_map;
use leptos_router::path;
use leptos_router::SsrMode;
use regatta_common::VersionInfo;

pub const NOT_FOUND_TEXT: &str = "womp womp, something's missing";

#[component]
pub fn App() -> impl IntoView {
    provide_meta_context();
    provide_context(ElementIds::default());
    provide_theme(crate::initial_theme());
    provide_alerts();
    // the session from the auth cookie (a blocking resource: the page waits for it)
    provide_login_session();
    crate::components::rg_race_year_selector::provide_race_year_loaded();
    let root = NodeRef::<leptos::html::Div>::new();
    observe_scroll_height(root);
    view! {
        <Title text="Regatta"/>
        <div node_ref=root>
            <Router>
                <PageFrame/>
            </Router>
        </div>
    }
}

/// = `Router()`: the grid with the nav bar, the alerts, `#route_content` and the footer.
///
/// Every route renders in `SsrMode::Async`: the server awaits the screen's resources and sends one
/// complete document with the data in the markup (T7), and a `<Redirect>` rendered while logged
/// out (Home) can still set the 302. `<Routes>` must not sit under a `<Suspense>`: the route list
/// is generated with a synchronous render in which a Suspense shows only its fallback, so routes
/// hidden that way never register, every page falls to the file handler (out-of-order streaming,
/// the frame shipped as a template chunk after the body, `ssr=` ignored, no redirect status).
/// The session resource is therefore read under two smaller boundaries: the nav bar's and each
/// screen's ([`Screen`]).
#[component]
fn PageFrame() -> impl IntoView {
    view! {
        <RgGrid r#type=RgContainerType::ContainerFluid>
            <Suspense fallback=|| ()>
                <NavBar/>
            </Suspense>
            <Alerts/>
            <RgDiv id="route_content" customizer=Customizer::new().set(Some(RgSpace::M), None, Some(RgSz::S2)).add_custom("grow").add_custom("flex").add_custom("flex-col")>
                <Routes fallback=|| NOT_FOUND_TEXT>
                    <Route path=path!("/") view=|| view! { <Screen><Home/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/series") view=|| view! { <Screen><Series/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/course") view=|| view! { <Screen><Course/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/people") view=|| view! { <Screen><People/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/admin") view=|| view! { <Screen><Admin create=false/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/admin/create") view=|| view! { <Screen><Admin create=true/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/races") view=|| view! { <Screen><Races/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/boats") view=|| view! { <Screen><Boats/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/class") view=|| view! { <Screen><Classes/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/races/results") view=|| view! { <Screen><RaceResults/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/races/results/view/:id") view=|| view! { <Screen><RaceResultsViewRoute/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/races/results/:id") view=|| view! { <Screen><RaceResultsEditRoute/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/series/standings/view/:id/:year") view=|| view! { <Screen><SeriesStandingsRoute/></Screen> } ssr=SsrMode::Async/>
                    <Route path=path!("/rc") view=|| view! { <Screen><Rc/></Screen> } ssr=SsrMode::Async/>
                </Routes>
            </RgDiv>
            <Footer/>
        </RgGrid>
    }
}

/// A routed screen under its own `<Suspense>`: the screens read the session resource (admin
/// gates, Home's redirect, the Action column), and a blocking resource is awaited by the server
/// only when read inside a Suspense. The fallback is empty: the session is resolved before the
/// first paint and on every client-side navigation.
#[component]
fn Screen(children: ChildrenFn) -> impl IntoView {
    view! { <Suspense fallback=|| ()>{children()}</Suspense> }
}

fn param_i64(key: &str) -> Option<i64> {
    use_params_map().with_untracked(|p| p.get(key).and_then(|v| v.parse().ok()))
}

fn param_i32(key: &str) -> Option<i32> {
    use_params_map().with_untracked(|p| p.get(key).and_then(|v| v.parse().ok()))
}

#[component]
fn RaceResultsViewRoute() -> impl IntoView {
    view! { <RaceResultsView race_id=param_i64("id")/> }
}

#[component]
fn RaceResultsEditRoute() -> impl IntoView {
    view! { <RaceResultsEdit race_id=param_i64("id")/> }
}

#[component]
fn SeriesStandingsRoute() -> impl IntoView {
    view! { <SeriesStandings id=param_i64("id") year=param_i32("year")/> }
}

#[component]
pub fn Footer() -> impl IntoView {
    let version = VersionInfo::CURRENT;
    view! {
        <div class="rg-container">
            <footer>
                <p class="text-center">
                    {format!("Build {} ", version.build_date)}
                    <a
                        class="text-muted no-underline hover:text-fg"
                        target="_blank"
                        rel="noopener noreferrer"
                        href=format!("https://github.com/manimaul/regatta/commit/{}", version.git_hash)
                    >
                        {version.git_hash}
                    </a>
                    <br/>
                    <a href="https://github.com/manimaul/regatta" class="text-muted no-underline hover:text-fg" target="_blank" rel="noopener noreferrer">
                        <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg"/>
                        " "
                        <img src="https://img.shields.io/badge/github-repo-blue?logo=github"/>
                    </a>
                </p>
            </footer>
        </div>
    }
}

/// Exposed for the theme state: the shell and `App` agree on the initial theme.
pub fn theme_or_default(theme: Option<Theme>) -> Theme {
    theme.unwrap_or_default()
}
