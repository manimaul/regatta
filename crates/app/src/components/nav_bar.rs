//! The nav bar: logo, theme toggle, and the screen links, collapsed into a hamburger menu below `lg`.
//!
//! Navigation goes through the Leptos router; the active link comes from matching the current path against
//! [`crate::state::route`]. The cookie session decides which links show.

use crate::components::rg_grid::{Customizer, RgDiv, RgSpace, RgSz};
use crate::components::theme_toggle::ThemeToggle;
use crate::state::login::{use_login, LoginStatus};
use crate::state::route::Route;
use crate::utils::route_matcher::Routing;
use leptos::either::Either;
use leptos::prelude::*;
use leptos_router::hooks::{use_location, use_navigate, use_query_map};
use leptos_router::NavigateOptions;

const LOGGED_IN_ROUTES: [Route; 9] = [
    Route::Home,
    Route::People,
    Route::Boats,
    Route::Series,
    Route::Classes,
    Route::Races,
    Route::Rc,
    Route::RaceResult,
    Route::Course,
];

const PUBLIC_ROUTES: [Route; 2] = [Route::RaceResult, Route::Course];

#[component]
pub fn NavBar() -> impl IntoView {
    let location = use_location();
    let query = use_query_map();
    let navigate = use_navigate();
    let login = use_login(); // the cookie session decides the link list and Logout/Admin
    let menu_open = RwSignal::new(false);
    let current_route = Memo::new(move |_| Routing::from(&location.pathname.get()).route);
    let nonav = move || query.with(|q| q.get("nonav").is_some());
    let full_href = move || {
        let search = location.search.get();
        let href = format!(
            "{}{}",
            location.pathname.get(),
            if search.is_empty() {
                String::new()
            } else {
                format!("?{search}")
            }
        );
        href.replace("nonav", "nav")
    };
    let go = move |route: Route| {
        navigate(route.path_pattern(), NavigateOptions::default());
        menu_open.set(false);
    };
    view! {
        {move || if nonav() {
            Either::Left(view! {
                <RgDiv customizer=Customizer::new().set(Some(RgSpace::M), None, Some(RgSz::S2))>
                    <a href=full_href() target="_blank" class="block px-4 py-2 no-underline">"Full Regatta Results"</a>
                </RgDiv>
            })
        } else {
            let go = go.clone();
            Either::Right(view! {
                <nav class="rg-navbar">
                    <div class="rg-navbar-inner">
                        <a href="#">
                            // a height class, not a height attribute: Preflight's img { height: auto } overrides the attribute
                            <img src="/cyct_burgee.svg" class="rg-navbar-logo inline-block h-[60px] w-auto" alt="logo"/>
                        </a>
                        // top right on every screen size; at lg the links sit between the logo and here
                        <div class="rg-navbar-end">
                            <ThemeToggle/>
                            <button
                                class="rg-navbar-toggler lg:hidden"
                                aria-controls="navbarNavDropdown"
                                aria-expanded=move || menu_open.get().to_string()
                                aria-label="Toggle navigation"
                                on:click=move |_| menu_open.update(|o| *o = !*o)
                            >
                                <HamburgerIcon/>
                            </button>
                        </div>
                        // Below lg the list is a vertical menu that animates open/closed (.rg-navbar-collapse in app.css);
                        // at lg and up it's always a row.
                        <div class="rg-navbar-collapse" data-open=move || menu_open.get().to_string()>
                            <ul id="navbarNavDropdown" class="m-0 p-0 list-none flex flex-col lg:flex-row lg:ms-4">
                                {
                                    let go = go.clone();
                                    move || {
                                        let routes: &[Route] = if login.logged_in() { &LOGGED_IN_ROUTES } else { &PUBLIC_ROUTES };
                                        routes.iter().copied().map(|route| {
                                            let go = go.clone();
                                            view! {
                                                <li>
                                                    <button
                                                        class="rg-nav-link"
                                                        aria-current=move || (current_route.get() == route).then_some("page")
                                                        on:click=move |_| go(route)
                                                    >
                                                        {route.label()}
                                                    </button>
                                                </li>
                                            }
                                        }).collect_view()
                                    }
                                }
                                {
                                    let go = go.clone();
                                    move || if login.status() == LoginStatus::LoggedIn {
                                        let go = go.clone();
                                        Either::Left(view! {
                                            <li>
                                                <button class="rg-nav-link" on:click=move |_| {
                                                    let go = go.clone();
                                                    login.logout(move || go(Route::Home));
                                                    menu_open.set(false);
                                                }>"Logout"</button>
                                            </li>
                                        })
                                    } else {
                                        let go = go.clone();
                                        Either::Right(view! {
                                            <li>
                                                <button class="rg-nav-link" on:click=move |_| go(Route::Admin)>"Admin"</button>
                                            </li>
                                        })
                                    }
                                }
                            </ul>
                        </div>
                    </div>
                </nav>
            })
        }}
    }
}

#[component]
fn HamburgerIcon() -> impl IntoView {
    view! {
        <svg viewBox="0 0 30 30" width="30" height="30" aria-hidden="true">
            <path d="M4 7h22M4 15h22M4 23h22" stroke="currentColor" stroke-width="2" stroke-linecap="round"></path>
        </svg>
    }
}
