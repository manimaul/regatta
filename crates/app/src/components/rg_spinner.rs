//! The loading spinner: an SVG arc spinning on a gradient stroke.

use leptos::prelude::*;

/// A spinner `size` px square.
#[component]
pub fn RgSpinner(
    #[prop(default = 50.0)] size: f64,
    #[prop(default = "black")] color: &'static str,
) -> impl IntoView {
    let center = size / 2.0;
    let stroke = size / 10.0;
    let radius = center - stroke;
    let amt = (2.0 * std::f64::consts::PI * radius) * 0.8;
    let rotate_from = format!("0, {center}, {center}");
    let rotate_to = format!("360, {center}, {center}");
    view! {
        <svg width=size.to_string() height=size.to_string()>
            <defs>
                <linearGradient id="grad">
                    <stop offset="0%" stop-color=color></stop>
                    <stop offset="100%" stop-color=color stop-opacity="0"></stop>
                </linearGradient>
            </defs>
            <circle
                cx=center.to_string()
                cy=center.to_string()
                r=radius.to_string()
                fill="none"
                stroke="url(#grad)"
                stroke-width=stroke.to_string()
                stroke-dasharray=amt.to_string()
                stroke-linecap="round"
            >
                <animateTransform
                    attributeName="transform"
                    attributeType="XML"
                    type="rotate"
                    repeatCount="indefinite"
                    from=rotate_from
                    to=rotate_to
                    dur="2s"
                ></animateTransform>
            </circle>
        </svg>
    }
}
