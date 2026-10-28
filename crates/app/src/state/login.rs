//! Login state: the admin session and its expiry countdown.
//!
//! The session lives in the `regatta_token` cookie (D3, `docs/explore_implemented/rust_port.md`): the server
//! renders the logged-in state from it through [`crate::server_fns::current_login`] (a blocking resource, so the
//! first HTML already carries the right nav bar), and [`crate::server_fns::login`],
//! [`crate::server_fns::logout`] and [`crate::server_fns::create_admin`] set, clear and bootstrap it.

use crate::server_fns::{self, CurrentLogin};
use crate::utils::async_state::call;
use crate::utils::browser;
use crate::utils::date_util::get_clock_value;
use jiff::Timestamp;
use leptos::prelude::*;
use regatta_common::{AuthRecord, Login, LoginResponse};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum LoginStatus {
    Loading,
    #[default]
    Ready,
    Complete,
    LoggedIn,
    Failed,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LoginState {
    pub auth: AuthRecord,
    pub pass: String,
    pub login_status: LoginStatus,
    pub error_message: Option<String>,
    /// What the browser knows since the page loaded: `Some(Some(..))` after a login,
    /// `Some(None)` after a logout or an expiry; `None` defers to the server's session resource.
    pub local: Option<Option<CurrentLogin>>,
}

impl Default for LoginState {
    fn default() -> Self {
        LoginState {
            auth: AuthRecord {
                id: 0,
                hash: String::new(),
                user_name: browser::local_storage_get("username").unwrap_or_default(),
            },
            pass: String::new(),
            login_status: LoginStatus::Ready,
            error_message: None,
            local: None,
        }
    }
}

/// Tells whether `login` has expired at `now`.
pub fn is_expired(login: &LoginResponse, now: Timestamp) -> bool {
    login.expires < now
}

/// Formats the login expiry countdown shown on the home screen.
///
/// The text is `"d days, h hours, m minutes, s seconds"`, `"EXPIRED"`, or `"NOT LOGGED IN"` for `None`.
pub fn login_expires(login: Option<&LoginResponse>, now: Timestamp) -> String {
    let Some(login) = login else {
        return "NOT LOGGED IN".to_string();
    };
    let remaining = login.expires.duration_since(now);
    if remaining.is_negative() {
        return "EXPIRED".to_string();
    }
    let total = remaining.as_secs();
    let days = total / 86_400;
    let hours = total % 86_400 / 3600;
    let minutes = total % 3600 / 60;
    let seconds = total % 60;
    format!("{days} days, {hours} hours, {minutes} minutes, {seconds} seconds")
}

#[derive(Clone, Copy)]
pub struct LoginContext {
    pub state: RwSignal<LoginState>,
    /// The session the request cookie proves (`current_login`); `None` in contexts created outside
    /// the app (component tests), which render logged out.
    pub session: Option<Resource<Option<CurrentLogin>>>,
    /// The current time of day, refreshed every 250 ms in the browser.
    pub clock_display: RwSignal<String>,
    pub expires_display: RwSignal<String>,
}

fn new_context(session: Option<Resource<Option<CurrentLogin>>>) -> LoginContext {
    let ctx = LoginContext {
        state: RwSignal::new(LoginState::default()),
        session,
        clock_display: RwSignal::new(String::new()),
        expires_display: RwSignal::new(String::new()),
    };
    provide_context(ctx);
    ctx
}

/// Provides a login context without a session (logged out), for components rendered outside the app.
pub fn provide_login() -> LoginContext {
    new_context(None)
}

/// Provides the app's login context: the cookie session as a blocking resource, and the expiry clock.
///
/// The page is not sent before the session resolves, so a `<Redirect>` from Home can still become a 302.
pub fn provide_login_session() -> LoginContext {
    let session = Resource::new_blocking(
        || (),
        |()| async move { server_fns::current_login().await.ok().flatten() },
    );
    let ctx = new_context(Some(session));
    ctx.start_clock();
    ctx
}

pub fn use_login() -> LoginContext {
    use_context::<LoginContext>().unwrap_or_else(provide_login)
}

impl LoginContext {
    /// The current session: the browser's own knowledge first, else the server's resource. Read
    /// it inside the app's `<Suspense>`.
    pub fn current(&self) -> Option<CurrentLogin> {
        if let Some(local) = self.state.with(|s| s.local.clone()) {
            return local;
        }
        self.session.and_then(|r| r.get().flatten())
    }

    /// Whether the session is known yet: `None` while the server's resource is still loading (the
    /// first, registering render of a `<Suspense>`), else whether it is a login. Decisions with a
    /// side effect on the response, such as Home's `<Redirect>`, wait for `Some`: a redirect
    /// rendered on the registering pass would set the 302 before the cookie had been read.
    pub fn resolved(&self) -> Option<bool> {
        if let Some(local) = self.state.with(|s| s.local.clone()) {
            return Some(local.is_some());
        }
        match self.session {
            Some(resource) => resource.get().map(|session| session.is_some()),
            None => Some(false),
        }
    }

    pub fn login_response(&self) -> Option<LoginResponse> {
        self.current().map(|c| c.login)
    }

    pub fn logged_in(&self) -> bool {
        self.current().is_some()
    }

    pub fn status(&self) -> LoginStatus {
        match self.state.with(|s| s.login_status) {
            LoginStatus::Ready | LoginStatus::LoggedIn => {
                if self.logged_in() {
                    LoginStatus::LoggedIn
                } else {
                    LoginStatus::Ready
                }
            }
            other => other,
        }
    }

    /// The name shown in "Logged in as": the session's, else the form's.
    pub fn user_name(&self) -> String {
        match self.current() {
            Some(current) => current.user_name,
            None => self.state.with(|s| s.auth.user_name.clone()),
        }
    }

    pub fn set_user_name(&self, value: String) {
        self.state.update(|s| s.auth.user_name = value);
    }

    pub fn set_password(&self, value: String) {
        self.state.update(|s| {
            s.auth.hash = if value.is_empty() {
                String::new()
            } else {
                crate::auth_hash::hash(&[&value])
            };
            s.pass = value;
        });
    }

    /// Creates an admin from the form through [`crate::server_fns::create_admin`], with the hashed password.
    ///
    /// The status is `Complete` or `Failed` for 3 s, then the form shows again.
    pub fn submit_new(&self) {
        let state = self.state;
        state.update(|s| s.login_status = LoginStatus::Loading);
        let record = state.with_untracked(|s| s.auth.clone());
        call(server_fns::create_admin(record), move |result| {
            state.update(|s| match result {
                Ok(saved) => {
                    s.auth = saved;
                    s.login_status = LoginStatus::Complete;
                }
                Err(message) => {
                    s.login_status = LoginStatus::Failed;
                    s.error_message = Some(message);
                }
            });
            browser::set_timeout(
                move || {
                    let local = state.try_with_untracked(|s| s.local.clone()).flatten();
                    state.try_set(LoginState {
                        local,
                        ..LoginState::default()
                    });
                },
                3000,
            );
        });
    }

    /// Logs in with the form's credentials, then runs `on_success` (the caller navigates Home).
    ///
    /// Sends [`crate::server_fns::login`] (which sets the cookie) the chained hash of a fresh salt, the current time in
    /// seconds and the password hash.
    pub fn login(&self, on_success: impl FnOnce() + 'static) {
        let state = self.state;
        state.update(|s| s.login_status = LoginStatus::Loading);
        let auth = state.with_untracked(|s| s.auth.clone());
        let time = Timestamp::now();
        let salt = crate::auth_hash::salt();
        let login = Login {
            user_name: auth.user_name.clone(),
            hash_of_hash: crate::auth_hash::hash(&[
                &salt,
                &time.as_second().to_string(),
                &auth.hash,
            ]),
            salt,
            time,
        };
        call(server_fns::login(login), move |result| match result {
            Ok(response) => {
                browser::local_storage_set("username", &auth.user_name);
                state.update(|s| {
                    s.login_status = LoginStatus::LoggedIn;
                    s.local = Some(Some(CurrentLogin {
                        login: response,
                        user_name: auth.user_name.clone(),
                    }));
                });
                on_success();
            }
            Err(message) => state.update(|s| {
                s.login_status = LoginStatus::Failed;
                s.error_message = Some(message);
            }),
        });
    }

    /// Clears the cookie through [`crate::server_fns::logout`] and forgets the session, then runs `on_done`.
    ///
    /// The caller navigates Home, which sends a logged-out visitor to the results.
    pub fn logout(&self, on_done: impl FnOnce() + 'static) {
        let state = self.state;
        call(server_fns::logout(), move |_| {
            browser::local_storage_set("username", "");
            state.set(LoginState {
                local: Some(None),
                ..LoginState::default()
            });
            on_done();
        });
    }

    /// Resets after a failed login: its message shows for 3 s, then the form again.
    pub fn reload(&self) {
        let state = self.state;
        browser::set_timeout(
            move || {
                state.try_update(|s| s.login_status = LoginStatus::Ready);
            },
            3000,
        );
    }

    /// Starts the 250 ms clock that updates the time and the expiry countdown.
    ///
    /// An expired login logs out after 3 s. A no-op on the server.
    fn start_clock(&self) {
        let ctx = *self;
        let expiring = RwSignal::new(false);
        // The session resource is read in an effect (where hydration allows it) into a plain
        // signal; the interval only formats that signal, so it never touches the resource.
        let login = RwSignal::new(None);
        Effect::new(move || login.set(ctx.login_response()));
        browser::set_interval(
            move || {
                let now = Timestamp::now();
                ctx.clock_display.set(get_clock_value());
                let login = login.get_untracked();
                let display = login_expires(login.as_ref(), now);
                if display == "EXPIRED" && !expiring.get_untracked() {
                    expiring.set(true);
                    browser::set_timeout(move || ctx.logout(|| {}), 3000);
                }
                ctx.expires_display.set(display);
            },
            250,
        );
    }
}
