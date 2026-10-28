//! Per-feature UI state that holds no server data: alerts, login, route and theme.
//!
//! Server data is not kept here: screens load it as resources over the server functions
//! ([`crate::utils::async_state::Loaded`]) and mutate through [`crate::utils::async_state::call`].

pub mod alerts;
pub mod login;
pub mod route;
pub mod theme;
