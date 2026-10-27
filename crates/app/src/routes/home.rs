//! = `routes/Home.kt`.

use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::server_fns;
use crate::state::login::use_login;
use crate::state::route::Route;
use crate::utils::async_state::call;
use crate::utils::browser;
use leptos::either::EitherOf3;
use leptos::prelude::*;
use leptos_router::components::Redirect;

#[component]
pub fn Home() -> impl IntoView {
    let login = use_login();
    // nothing until the cookie session is known: the redirect must not fire on the Suspense's
    // registering render, where the resource is still None
    view! {
        {move || match login.resolved() {
            None => EitherOf3::A(()),
            Some(true) => EitherOf3::B(view! { <HomeLoggedIn/> }),
            Some(false) => EitherOf3::C(view! { <Redirect path=Route::RaceResult.path_pattern()/> }),
        }}
    }
}

#[component]
pub fn HomeLoggedIn() -> impl IntoView {
    let login = use_login();
    view! {
        <h4>{move || format!("Logged in as {}", login.user_name())}</h4>
        <p>{move || format!(" Login expires in {}", login.expires_display.get())}</p>
        <div class="flex-wrap">
            <RgButton
                label="Copy auth token"
                style=RgButtonStyle::PrimaryOutline
                click=move |()| {
                    // the bearer token (base64 of the LoginResponse JSON) is the cookie's value, read back from the server
                    call(server_fns::auth_token(), |token| {
                        let token = token.ok().flatten().unwrap_or_else(|| "none".to_string());
                        browser::clipboard_write(&token);
                        browser::alert(&format!("Auth token copied to clipboard\n\n{token}"));
                    });
                }
            />
        </div>
    }
}
