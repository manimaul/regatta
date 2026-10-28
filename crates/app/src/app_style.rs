//! Shared 8px margin classes on Tailwind's spacing scale (8px = step 2).
//!
//! Every value is also listed in the `@source inline(...)` safelist of `crates/app/style/app.css`; a new
//! constant needs an entry there.

pub const MARGIN_ALL: &str = "m-2";
pub const MARGIN_START: &str = "ms-2";
pub const MARGIN_TOP: &str = "mt-2";
pub const MARGIN_BOT: &str = "mb-2";
pub const MARGIN_END: &str = "me-2";
pub const MARGIN_VERT: &str = "my-2";
