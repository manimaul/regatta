//! = `RouteMatcher.kt` and `Routing` of `RouteViewModel.kt`. The Leptos router matches the
//! `<Route>`s; this matcher is what the nav bar uses to tell which [`Route`] a pathname is on
//! (the `aria-current` link) without a router context, so it stays testable natively.
//!
//! A pattern compiles to the Kotlin regex `/seg/seg/(?<key>[^/]*)[/]?` matched in full: static
//! segments must be equal, a `:key` segment captures anything without a slash (the empty string
//! included), and one trailing slash is allowed.

use crate::state::route::Route;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RouteMatcher {
    pub route: Route,
    segments: Vec<Segment>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
enum Segment {
    Static(String),
    Param(String),
}

impl RouteMatcher {
    pub fn build(route: Route) -> RouteMatcher {
        let pattern = route.path_pattern();
        let segments = if pattern == "/" {
            Vec::new()
        } else {
            pattern
                .split('/')
                .filter(|s| !s.is_empty())
                .map(|segment| match segment.strip_prefix(':') {
                    Some(keyword)
                        if !keyword.is_empty()
                            && keyword.chars().all(|c| c.is_alphanumeric() || c == '_') =>
                    {
                        Segment::Param(keyword.to_string())
                    }
                    _ => Segment::Static(segment.to_string()),
                })
                .collect()
        };
        RouteMatcher { route, segments }
    }

    pub fn matches(&self, path: &str) -> bool {
        self.groups(path).is_some()
    }

    /// The captured `:key` values, or `None` when the path does not match.
    pub fn groups(&self, path: &str) -> Option<Vec<(String, String)>> {
        if self.segments.is_empty() {
            // the Kotlin regex is "/[/]?"
            return (path == "/" || path == "//").then(Vec::new);
        }
        let rest = path.strip_prefix('/')?;
        let mut parts: Vec<&str> = rest.split('/').collect();
        if parts.len() == self.segments.len() + 1 && parts.last() == Some(&"") {
            parts.pop();
        }
        if parts.len() != self.segments.len() {
            return None;
        }
        let mut groups = Vec::new();
        for (segment, part) in self.segments.iter().zip(parts) {
            match segment {
                Segment::Static(s) if s == part => {}
                Segment::Static(_) => return None,
                Segment::Param(key) => groups.push((key.clone(), part.to_string())),
            }
        }
        Some(groups)
    }
}

/// = `Routing`: the route a path resolves to, with its captured arguments.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Routing {
    pub route: Route,
    pub path: String,
    pub args: Option<Vec<(String, String)>>,
}

impl Routing {
    pub fn from(path: &str) -> Routing {
        Route::ENTRIES
            .iter()
            .map(|route| RouteMatcher::build(*route))
            .find_map(|matcher| {
                matcher.groups(path).map(|groups| Routing {
                    route: matcher.route,
                    path: path.to_string(),
                    args: Some(groups),
                })
            })
            .unwrap_or_else(|| Routing {
                route: Route::NotFound,
                path: path.to_string(),
                args: None,
            })
    }

    pub fn arg(&self, key: &str) -> Option<&str> {
        self.args
            .as_ref()?
            .iter()
            .find(|(k, _)| k == key)
            .map(|(_, v)| v.as_str())
    }
}
