//! The Admin screen: the login and create-admin forms.

use crate::components::rg_button::{RgButton, RgButtonStyle};
use crate::components::rg_grid::{Customizer, RgDiv, RgSide, RgSpace, RgSz};
use crate::state::login::{use_login, LoginStatus};
use leptos::either::EitherOf5;
use leptos::prelude::*;
use leptos_router::hooks::use_navigate;
use leptos_router::NavigateOptions;

/// The Admin screen; `create` shows the create-admin form (also while logged in) instead of the login form.
#[component]
pub fn Admin(create: bool) -> impl IntoView {
    let login = use_login();
    view! {
        <div>
            {move || match login.status() {
                LoginStatus::Loading => EitherOf5::A(view! { <progress></progress> }),
                LoginStatus::Ready => EitherOf5::B(view! { <Login create=create/> }),
                LoginStatus::Complete => EitherOf5::C(view! { <div>"Done"</div> }),
                LoginStatus::LoggedIn => EitherOf5::D(view! {
                    <h4>{format!("Logged in as {}", login.user_name())}</h4>
                    {create.then(|| view! { <Login create=create/> })}
                }),
                LoginStatus::Failed => {
                    login.reload();
                    EitherOf5::E(view! {
                        <progress></progress>
                        <p>{login.state.with(|s| s.error_message.clone()).unwrap_or_default()}</p>
                    })
                }
            }}
        </div>
    }
}

/// The user name and password form; `create` makes it create or replace an admin instead of logging in.
#[component]
pub fn Login(create: bool) -> impl IntoView {
    let login = use_login();
    let navigate = use_navigate();
    let user_name = move || login.state.with(|s| s.auth.user_name.clone());
    let pass = move || login.state.with(|s| s.pass.clone());
    let label = if create { "Add" } else { "Login" };
    view! {
        <h2>{if create { "Add Login" } else { "Login" }}</h2>
        <form on:submit=|ev| ev.prevent_default()>
            <RgDiv customizer=Customizer::new().set(Some(RgSpace::M), Some(RgSide::B), Some(RgSz::S3))>
                <label for="username">"Username"</label>
                <input type="text" id="username" class="rg-input" on:input=move |ev| login.set_user_name(event_target_value(&ev)) value=user_name prop:value=user_name/>
            </RgDiv>
            <RgDiv customizer=Customizer::new().set(Some(RgSpace::M), Some(RgSide::B), Some(RgSz::S3))>
                <label for="password">"Password"</label>
                <input type="password" id="password" class="rg-input" on:input=move |ev| login.set_password(event_target_value(&ev)) value=pass prop:value=pass/>
            </RgDiv>
            <RgButton
                label=label
                style=RgButtonStyle::Primary
                disabled=Signal::derive(move || login.state.with(|s| s.pass.len() < 4))
                click=move |()| if create {
                    login.submit_new()
                } else {
                    // a successful login goes Home
                    let navigate = navigate.clone();
                    login.login(move || navigate("/", NavigateOptions::default()))
                }
            />
        </form>
    }
}
