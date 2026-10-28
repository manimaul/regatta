# Exploration: Replacing Bootstrap 5 with Tailwind CSS

## Goal

Replace Bootstrap 5.3.3 (CSS + `bootstrap.bundle.js` + Popper) in the UI with
[Tailwind CSS](https://tailwindcss.com) v4 (currently 4.3.x), while:

- keeping the UI's own component layer (`crates/app/src/components/`) as the only place that knows how things look;
- **preserving the existing `Rg*` component API** (`RgButton`, `RatingSelections`, `RgInput`, `RgDropdown`,
  `RgModal`, `RgTable`, `RgDiv`, …) so the route screens in `crates/app/src/routes/` change as little as possible.
  API changes are listed in [Necessary API changes](#necessary-api-changes) and are additive where possible.

## Key differences from Bootstrap

| | Bootstrap | Tailwind |
|---|---|---|
| What ships | Prebuilt CSS + JS behaviors (modal, collapse, dropdown via Popper) | **CSS only**, generated at build time from the class names found in source files |
| Components | `.btn`, `.modal`, `.table`, `.form-control`, … | None — compose utilities, or define our own component classes with `@apply` |
| Base reset | Reboot (styled headings, tables, forms) | Preflight (headings, lists, buttons and inputs fully **unstyled**) |
| Spacing scale | `1`=.25rem, `2`=.5rem, `3`=1rem, `4`=1.5rem, `5`=3rem | linear: `n` × .25rem (`3`=.75rem, `4`=1rem, `6`=1.5rem, `12`=3rem) |
| Build | a prebuilt stylesheet | a Tailwind pass that **scans source files** for class names |

Two consequences shape this plan:

1. **All JS behavior moves into the components.** Modal show/hide, `data-bs-dismiss`, and navbar collapse were
   handled by Bootstrap JS. Tailwind has no runtime, so this becomes component state (or the native `<dialog>`
   element). This is the only part that forces API changes.
2. **Class names must appear as literal strings in the Rust source.** Tailwind's scanner reads the `.rs` files as
   text. Strings assembled at runtime, like `format!("rg-table-{color}")` or a spacing name built from its parts,
   will **not** produce CSS. Every generated class has to come from a literal lookup table (a `match` returning the
   full class string), or be safelisted with `@source inline("…")`.

## Bootstrap footprint (summary)

- Bootstrap's CSS and `bootstrap.bundle.js` were loaded globally, with `@popperjs/core` alongside.
- **Encapsulated in `components/`** (swap internals, keep API): `RgButton`, `RgForm` (`RgInput`, `RgNumberInput`,
  `RgInputWithButton`, `RgTime`, `RgCheck`), `RgSwitch`, `RgDropdown`/`RgIdDropdown`/`RgDropdownNone`,
  `Modal` (`RgModal`, `RgModalButton`, `RgModalBody`), `RgTable`, `RgGrid` (`RgGrid`, `RgDiv`, `Customizer`),
  `NavBar`, `Alerts`, `RgSortable`.
- **Bootstrap JS interop:** `Bootstrap.Modal` and `Bootstrap.Collapse`, used by the alerts state (confirmation
  modal, `hidden.bs.modal`/`shown.bs.modal`) and `NavBar`. Plus 18 `data-bs-toggle`/`data-bs-target`/
  `data-bs-dismiss` attributes.
- **Leaked into routes:** 18 raw `<button>`s styled with `RgButtonStyle`'s classes + `data-bs-dismiss="modal"` in
  modal footers (`People`, `Rc`, `Series`, `Classes`, `RaceResults`, `AddEditBoatModal`, `RgForm`'s
  `RgTimeButton`). Utility classes such as `flex-fill d-flex justify-content-between`, `flex-wrap`,
  `flex-grow-1`, `text-danger`, `text-decoration-line-through`, `border-top border-2 border`,
  `link-secondary link-underline-opacity-0`, `container`, `text-center`, `float-start`, `form-control` (`Admin`),
  `form-select` (the race year selector), `nav nav-pills nav-link` (`Rc`).
- **Unused:** `RgButtonGroup`, `RgRow`, `RgCol`, `RgBp`, `RgColType`.

`RatingSelections` contains no Bootstrap classes directly. It composes `RgDropdown`, `RgNumberInput`,
`RgInputWithButton`, `RgButton`, and `RgSpinner`, so once those are migrated it should compile and work
**unchanged**. It is the regression check for "API preserved".

## Build integration

cargo-leptos runs Tailwind v4 itself; no separate CSS toolchain is needed.

1. **Entry point:** the root `Cargo.toml`'s leptos metadata sets only
   `tailwind-input-file = "crates/app/style/app.css"`. Setting `style-file` to the same file replaced the Tailwind
   output with the raw CSS. The result is served as `/pkg/regatta.css`, linked from the shell
   (`crates/app/src/lib.rs`) with a plain `<link>`.
2. **Scan root:** `app.css` turns automatic source detection off and names the Rust sources explicitly, so
   `target/` and the rest of the repository are not scanned:
   ```css
   @import "tailwindcss" source(none);
   @plugin "@tailwindcss/forms";
   @source "../src/**/*.rs";
   @source inline("{m,p}{,t,b,s,e,x,y}-{0,1,2,4,6,12,auto}");   /* RgDiv spacing safelist */
   @theme { /* CYCT colors, Bootstrap breakpoints, z-index */ }
   @layer base { /* Reboot replacement */ }
   @layer components { /* .rg-btn, .rg-table, … */ }
   ```
3. **During the migration** `app.css` also imported Bootstrap into a cascade layer
   (see [Coexistence](#coexistence-via-cascade-layers-spike-finding)):
   `@layer theme, base, bootstrap, components, utilities;` and
   `@import "bootstrap/dist/css/bootstrap.css" layer(bootstrap);`. Both were removed in Phase 5.
4. **Third-party CSS stays outside Tailwind.** `maplibre-gl.css` is loaded from jsDelivr by the shell, not run
   through Tailwind. Because it is unlayered, it beats anything inside Tailwind's native cascade layers, so the
   overrides for it (the chart popup block at the end of `app.css`) sit outside any `@layer`.

## Styling strategy: component classes and utilities

Two ways to express Bootstrap-like components in Tailwind:

- **A. Utilities inline in Rust**, e.g. an `RgButtonStyle::Primary` that returns
  `"px-3 py-1.5 rounded bg-blue-600 text-white hover:bg-blue-700 …"`. This is the most "Tailwind-native" option,
  but class strings get long and are repeated in raw `<button>` call sites.
- **B. Semantic component classes defined in `app.css` with `@apply`** (`.rg-btn`, `.rg-btn-primary`, `.rg-input`,
  `.rg-table`, `.rg-modal`, …), with utilities used directly only for one-off layout.

**Decision: B.** It keeps `Rg*` internals short, centralizes the design in one CSS file, and avoids the
dynamic-class-name problem for component styles. Classes declared in `@layer components` are always emitted, so they
don't depend on scanning. Use Tailwind utilities directly in Rust for layout, and keep those as literals.

A tempting shortcut is a **Bootstrap-name shim**: define `.btn`, `.btn-primary`, `.form-control`, … in `app.css`
with `@apply`, so no component changes. It works as a temporary bridge during migration (Phase 1), but it
preserves Bootstrap naming forever. Rename to `rg-*` as each component is migrated.

## Theming & base styles

- **`@theme` tokens, derived from the CYCT burgee.** Semantic color scales keep `RgButtonStyle` and `TableColor`
  semantics (`Primary`, `Success`, `Danger`, `Warning`, `Info`, …) tied to brand colors instead of raw `green-600`:

  | Token scale | Source | Base value |
  |---|---|---|
  | `--color-primary-*` | burgee navy | `#131340` (darkest step; lighter steps generated for hover/containers/tints) |
  | `--color-info-*` | burgee light blue | `#5eb2db` |
  | `--color-danger-*` | burgee red | `#ef4137` |
  | `--color-success-*` | Tailwind `green` | no burgee equivalent |
  | `--color-warning-*` | Tailwind `amber` | no burgee equivalent |
  | `--color-secondary-*` | Tailwind `slate` | neutral buttons/text |
  | `--color-surface` | burgee off-white | `#fafaff` (page background) |

  Generate the 50–950 steps for each brand color in Phase 1, e.g. with `oklch()` lightness steps, and check that
  text/background pairs meet WCAG AA contrast. Navy is very dark, so `primary` buttons use `primary-900` with white
  text and `PrimaryOutline` uses a navy border/text. Set `--font-sans` to the system UI font stack, matching
  Bootstrap's default, so no web-font download is needed.
- **Breakpoints: use Bootstrap's values.** Tailwind's defaults differ from Bootstrap's (`sm` 640 vs 576, `lg` 1024
  vs 992, `xl` 1280 vs 1200, `2xl` 1536 vs `xxl` 1400). Without an override, the navbar would collapse to the
  hamburger between 992 and 1023 px, where it was expanded under Bootstrap, and container max-widths would change.
  Override them in `@theme`:
  ```css
  @theme {
    --breakpoint-sm: 576px; --breakpoint-md: 768px; --breakpoint-lg: 992px;
    --breakpoint-xl: 1200px; --breakpoint-2xl: initial; --breakpoint-xxl: 1400px;
  }
  ```
  This keeps `RgContainerType` and the sticky entries of `RgPosition` (sticky-sm-top, …) meaning the same widths.
  Their literal tables map to `.rg-sticky-sm-top` and so on.
- **Preflight replaces Reboot.** Preflight is more aggressive: `h1`–`h6` lose their sizes, `ul` loses bullets,
  `button` loses its styling, `img` becomes `display:block`, and tables lose spacing. Screens rely on raw
  `<h3>`/`<h5>`/`<h6>`, `<p>`, `<b>`, `<br>`, `<ul>`, and `<caption>`, so add an `@layer base` with heading sizes
  and margins, paragraph spacing, and link color. Otherwise every route shifts visually. Specifics that are easy
  to miss:
  - **Heading sizes shrink on phones in Bootstrap.** Reboot sizes `h1`–`h4` responsively (e.g. `h1` is
    `calc(1.375rem + 1.5vw)`, capped at 2.5rem from 1200px up). Reproduce that with `clamp()` or `xl:` sizes, so
    page titles don't get huge on phones or tiny on desktop.
  - **Pointer cursor.** Tailwind v4 Preflight gives `button` `cursor: default`. Bootstrap's `.btn`, `.nav-link`
    and `.dropdown-item` show a pointer. Add `button:not(:disabled), [role=button] { cursor: pointer }` to the
    base layer.
  - **SVG is block-level.** Preflight makes `svg`/`img` `display:block`, which would push the inline
    `RgSpinner` and the footer badges (in `crates/app/src/app.rs`) onto their own lines. Give them `inline-block`
    explicitly.
- **Forms:** `@tailwindcss/forms` gives native inputs, selects, and checkboxes a sane baseline that is easy to
  restyle. It covers `RgInput`, `RgTime`, `RgCheck`, `RgRaceYearSelector`, and the raw `<input>`s in
  `routes/admin.rs`. Keep input text at **16px or larger below `sm`**. iOS Safari zooms the page when a focused
  input's font size is under 16px. Compact `RgTime` fields can use `text-base sm:text-sm`. Bootstrap's
  `input-group-sm` (14px) in `RgTime` and the RC screen triggered that zoom, so this was a chance to fix it.
- **Dark mode:** not part of this migration. The light/dark themes came later, through the `--rg-*` semantic
  tokens in `app.css` and `ThemeToggle` (`crates/app/src/state/theme.rs`).

## Component mapping (internals change, API stays)

| Component | Bootstrap | Tailwind replacement | API impact |
|---|---|---|---|
| `RgButton` | `btn btn-*` | `.rg-btn` + `.rg-btn-{primary,secondary,danger,success,light,dark,link,outline-*}` in `@layer components` | Signature unchanged. `RgButtonStyle::classes()` **stays** (now `rg-*` names), so raw `<button>`s that use it keep working. Add optional `closes_modal: bool` (default false). |
| `RgButtonGroup` | `btn-group` | `inline-flex` + rounded-edge rules. Unused, so consider deleting. | None |
| `RgSpinner` | custom SVG | Keep as is (no Bootstrap). Optionally `animate-spin`. | None |
| `RgInput`, `RgNumberInput` | `label` + `form-control` | `.rg-input` (`block w-full rounded border-gray-300 focus:ring-primary-500 …`) | None; behavior (controlled value, `locked` logic) untouched since these stay native `<input>`s |
| `RgInputWithButton` | `input-group mb-3` + `btn-outline-primary` | `flex` row, input `rounded-r-none`, button `rounded-l-none -ml-px` | None |
| `RgTime` | `input-group input-group-sm` + `input-group-text` | `.rg-input-group` / `.rg-input-addon` component classes (`flex`, shared borders, compact padding) | None |
| `RgCheck` | `form-check`, `form-check-input/label` | `flex items-center gap-2` + `.rg-checkbox` | None. The custom-classes default (`form-check`) becomes `rg-check`; the one caller passing custom classes is unaffected. |
| `RgSwitch`, `RgSwitches` | `form-switch` | Checkbox styled as a switch with `appearance-none` + `peer`/`checked:` utilities (`.rg-switch`) | None |
| `RgDropdown`, `RgIdDropdown`, `RgDropdownNone` (+ `RgSeriesDropdown`, `RgClassDropdown`, `RgSkipperDropdown`) | Already **hand-rolled** (open state in the component) with `dropdown-toggle`/`dropdown-menu show`/`dropdown-item` classes | Swap classes for `.rg-dropdown-toggle`, `.rg-dropdown-menu` (`absolute z-50 mt-1 rounded shadow-lg bg-white max-h-80 overflow-auto`), `.rg-dropdown-item`. Replace the inline `z-index`/`position` style with classes. The filter `RgInput`'s custom classes `w-auto mx-3 my-2` become Tailwind equivalents (`w-auto mx-4 my-2`). **Add close on outside click and Esc** (decision 5): document `pointerdown`/`keydown` listeners registered while the menu is open, ignoring events inside the dropdown's root element. Use `pointerdown` rather than `mousedown` so taps close it on touch screens. When a menu is open inside a modal, Esc must `preventDefault()`/`stopPropagation()` so it closes only the menu and not the dialog too. Cap the menu width at `max-w-[calc(100vw-2rem)]` so long boat or skipper names don't run off a phone screen. | None |
| `FinishCodeDrop` | wraps `RgDropdown` | free | None |
| `RgRaceYearSelector` | `form-select` | `.rg-select` (forms plugin) | None |
| `RgModalButton` / `RgModalBody` / `RgModal` | `data-bs-toggle`/`data-bs-target` + `.modal.fade` markup, Bootstrap JS | **Native `<dialog>`**. `RgModalButton(id)` calls `showModal()` on the dialog with that id, so the **id-based API is preserved**. You get focus trapping, Esc to close, and a backdrop (`backdrop:bg-black/50`) without JS libraries. The close (×) button calls `close()`. The dialog is a full-viewport scroll container around a centered panel, mirroring Bootstrap's `.modal` > `.modal-dialog`. See [Modals](#modals-on-small-screens). | Signatures unchanged. Footer buttons stop using `data-bs-dismiss` (see below). |
| `Alerts` (confirmation) | `RgModalBody` + `Bootstrap.Modal` in the alerts state | Same `<dialog>`, with element id `confirmation`. Listen to the dialog's `close` event instead of `hidden.bs.modal`. | Internal change only |
| `Alerts` (success message) | `alert alert-success d-flex align-items-center fixed-top` | `fixed top-20 inset-x-0 mx-auto max-w-md flex items-center gap-2 rounded bg-success-100 text-success-800 p-3 shadow` | None |
| `NavBar` | `navbar navbar-expand-lg …` + `Bootstrap.Collapse` | Same behavior: a horizontal link row at ≥ `lg` (992px), a hamburger below it that toggles a vertical menu. Component state replaces `Bootstrap.Collapse`. See [Navbar](#navbar-hamburger-collapse). | None |
| Rc tabs (`nav nav-pills` in `routes/rc.rs`) | Bootstrap pills | `flex gap-2` + `.rg-pill` / `.rg-pill-active` | Route-level edit |
| `RgTable` + `RgTr`/`RgTd`/`RgTdColor`/`RgTrColor` | `table table-hover table-sm table-striped[-columns] table-bordered caption-top table-<color>` | `.rg-table` component classes written as plain CSS in `@layer components`, reproducing Bootstrap's layering so hover, striping and cell colors combine correctly. Wrapper `overflow-x-auto`. **`TableColor` gets a literal class per entry** (`rg-table-light`, `rg-table-warning`, …, via `TableColor::cls()`) instead of a name built from the variant. See [Tables](#tables-hover-and-striping). | `TableColor` entries kept; internal method added |
| `RgGrid` / `RgDiv` / `Customizer` (`RgSpace`/`RgSide`/`RgSz`/`RgPosition`/`RgContainerType`) | Bootstrap utility names (`m-2`, `mb-3`, `my-4`, `container-fluid`, `fixed-top`, `min-vh-100`, `d-flex`, `flex-column`) | Tailwind uses almost the same names (`m-2`, `mb-3`, `ms-*`/`me-*`, `fixed top-0`, `min-h-screen`, `flex flex-col`), **but the scale differs**. `Customizer::set` looks up a literal table (`spacing_class` in `components/rg_grid.rs`): `RgSz::S3` → `"…-4"`, `S4` → `"…-6"`, `S5` → `"…-12"` to keep Bootstrap's visual sizes. That's 2 spaces × 7 sides × 7 sizes = 98 literals, also covered by `@source inline("{m,p}{,t,b,s,e,x,y}-{0,1,2,4,6,12,auto}")`. `RgContainerType` and `RgPosition` entries get literal `rg-*` classes. | None |
| `RgSortable` | `list-group` / `list-group-item` | `divide-y rounded border` + `px-4 py-2 cursor-move`. SortableJS is unaffected. | None |
| `app_style.rs` | margin constants | Literal Tailwind spacing classes (`MARGIN_START` = `ms-2`, …). | None |

MapLibre (`crates/app/src/chart/`, with its CSS from the CDN) and charts are out of scope. Check that Preflight's
`img { display:block; max-width:100% }` and `canvas` rules don't affect the map or chart canvases. If they do,
scope an override.

## Responsive behavior (mobile & desktop)

The site has to work on phones as well as on desktop. That includes the CYCT site's iframe embed, where media
queries measure the **iframe's** width, just as they did with Bootstrap. Bootstrap gave us a lot of this for
free; these are the places where a naive Tailwind port would regress.

### Navbar (hamburger collapse)

Under Bootstrap, `navbar-expand-lg` showed the links in a row at ≥ 992px. Below that it showed the burgee and a
hamburger button, and the button toggled a vertical link list (`Bootstrap.Collapse`). The port keeps that exactly
(`crates/app/src/components/nav_bar.rs`):

- **Breakpoint:** `lg` must be 992px (see the [Theming](#theming--base-styles) breakpoint override). Otherwise the
  hamburger would also appear between 992 and 1023px.
- **Structure:** a `<nav>` (`sticky top-0 border-b bg-…`) containing a `flex flex-wrap items-center justify-between`
  row: brand `<img>` (60px tall), a hamburger `<button>`, and the link `<ul>`.
- **Hamburger:** `lg:hidden`, with `aria-expanded`/`aria-controls`. Bootstrap's `navbar-toggler-icon` is a CSS
  background SVG that disappears with Bootstrap, so draw the three-bar icon as an inline SVG (like `Alerts` does).
- **Link list:** a `menu_open` signal sets `data-open` on the `.rg-navbar-collapse` wrapper, and `app.css` selects
  on `[data-open="true"]`, so no class name is built at runtime. At ≥ `lg` the list always shows in a row,
  whatever the state. That matches Bootstrap: if the menu is opened on a phone and the window then widens, the
  links appear inline. If it narrows again, the menu is still open.
- **After choosing a route:** the menu **closes** (changed 2026-10-13 at your request). Bootstrap left it
  open. Every link (routes, Logout, Admin) sets `menu_open` to false after navigating.
- **Animation** (added 2026-10-13): the `<ul>` sits in the `.rg-navbar-collapse` wrapper, which animates
  `grid-template-rows: 0fr ↔ 1fr` over Bootstrap's `.35s ease`. That reaches the content's natural height without
  measuring it in JavaScript.
  - `visibility` turns `hidden` only after the close transition, so collapsed links aren't keyboard-focusable or
    read by screen readers.
  - `prefers-reduced-motion` disables the animation.
  - At ≥ `lg` the wrapper is a plain flex row with no transition.
  - `crates/app/tests/nav_bar_test.rs` covers the rendered markup, and the `hamburger_toggles_and_link_click_closes_menu`
    case in `crates/app/tests/wasm.rs` covers toggle and close-on-click.
- **Layering:** keep Bootstrap's z-index order: dropdown menus (1000) < sticky navbar (1020) < fixed success
  banner (1030). Dialogs sit in the top layer above all of them. These values live in `@theme` (`--z-dropdown`,
  `--z-sticky`, `--z-fixed`) rather than mixing `z-10`/`z-50` ad hoc.
- **`nonav` embed mode** is unchanged.

### Tables (hover and striping)

`RgTable` is used in 10 places, and **both** striping modes are in use:

| Call sites | Settings | Result |
|---|---|---|
| `Classes`, `People`, `RaceResults` (×2), `Rc` (×2), `Races`, `Series` | defaults: `TableColor::Secondary`, row striping | grey table, alternate rows striped, hovered row highlighted |
| `SeriesStandings`, `RaceResultsView` | striped columns, `TableColor::Light` | light table, alternate **columns** striped, hovered row highlighted, plus `RgTdColor` `Warning` cells |

**The issue:** in Bootstrap, the three effects stack. Each cell gets its base color from the table color
(`--bs-table-bg`). Striping and hover are painted **over** it with an inset `box-shadow`, and hover takes priority
over striping. A naive Tailwind version, with `even:bg-…` on `tr`, `hover:bg-…` on `tr` and colored `td`s, breaks:
cell backgrounds cover the row's hover color, so hover vanishes on striped or colored cells. Row-level and
column-level striping also fight each other.

**The fix:** write `.rg-table` as plain CSS in `@layer components` with the same layering:

```css
@layer components {
  .rg-table { width: 100%; caption-side: top; border-collapse: collapse;
    --rg-tbl-bg: transparent; --rg-tbl-striped: rgb(0 0 0 / .05); --rg-tbl-hover: rgb(0 0 0 / .075);
    --rg-tbl-border: var(--color-gray-300); }
  .rg-table > :not(caption) > tr > * {
    padding: .25rem;                                   /* table-sm: tighter padding, same font size */
    border: 1px solid var(--rg-tbl-border);           /* table-bordered */
    background-color: var(--rg-tbl-bg);
    box-shadow: inset 0 0 0 9999px var(--rg-cell-state, var(--rg-cell-stripe, transparent)); }
  .rg-table-striped > tbody > tr:nth-of-type(odd) > *              { --rg-cell-stripe: var(--rg-tbl-striped); }
  .rg-table-striped-columns > :not(caption) > tr > :nth-child(even) { --rg-cell-stripe: var(--rg-tbl-striped); }
  .rg-table-hover > tbody > tr:hover > *                            { --rg-cell-state: var(--rg-tbl-hover); }
  /* TableColor variants set the variables. They work on <table>, <tr> or <td>, so RgTrColor/RgTdColor keep working. */
  .rg-table-secondary { --rg-tbl-bg: …; --rg-tbl-striped: …; --rg-tbl-hover: …; --rg-tbl-border: …; color: …; }
  .rg-table-light     { … }
  .rg-table-warning   { … }   /* one block per TableColor entry */
}
```

Notes:

- **Use plain `:hover`, not Tailwind's `hover:` variant.** In v4, `hover:` only applies under `@media (hover: hover)`,
  so it never fires on phones. Bootstrap's `:hover` highlighted the tapped row on touch screens. Plain CSS
  keeps that.
- **Keep the font size.** `table-sm` only tightens padding, so don't add `text-sm`. The earlier draft of this plan
  had it.
- **Narrow screens:** keep the `overflow-x-auto` wrapper (`table-responsive`). Wide tables like the 15-column race
  results scroll sideways on phones, as they did under Bootstrap. Don't add `whitespace-nowrap` or sticky columns;
  that would be a behavior change.
- **Variable colors:** derive the `--rg-tbl-*` values for each `TableColor` from the theme scales
  (e.g. `Secondary` → `slate-200` background, `Warning` → `warning-100`), keeping Bootstrap's contrast between
  background, stripe and hover.

### Modals on small screens

Bootstrap's modal is a full-viewport, scrollable `.modal` containing a centered `.modal-dialog`. The dialog spans
the width minus 0.5rem margins on phones and is 500px wide, centered, from `sm` up. When the content is tall, the
**whole modal scrolls**, and because `.modal-content` doesn't clip, absolutely positioned dropdown menus inside
it aren't cut off. `AddEditBoatModal` relies on both: it holds `RgSkipperDropdown` and `RatingSelections` with
ORC certificate details, which get tall on a phone.

A native `<dialog>` with default styles is a centered box limited to the viewport, with internal scrolling. That
clips the dropdown menus. Setting `overflow: visible` instead, as an earlier draft of this plan suggested, would
make tall modals impossible to scroll on a phone. **Mirror Bootstrap's structure instead**
(`crates/app/src/components/modal.rs`):

- `<dialog class="rg-modal">`: reset the UA styles to `fixed inset-0 m-0 p-0 w-full h-full max-w-none max-h-none
  overflow-y-auto bg-transparent border-0`, with `backdrop:bg-black/50`. The dialog element is the scroll container.
- Inside it, `.rg-modal-panel`: `relative mx-2 my-2 sm:mx-auto sm:my-7 sm:max-w-[500px] rounded-lg bg-white shadow`,
  with header, body and footer. The panel has **no overflow clipping**, so dropdown menus extend past it and the
  dialog scrolls to reveal them.
- **Backdrop click closes the modal**, as in Bootstrap. The native dialog doesn't do this by default
  (`closedby="any"` isn't supported in every browser we target), so close it in a `click` handler when
  `event.target` is the `<dialog>` itself, i.e. outside the panel, and only when the press also started there.
- **Scroll lock:** add `html:has(dialog[open]) { overflow: hidden }` (Bootstrap adds `.modal-open` to `body`). This
  stops the page behind from scrolling, especially on iOS.
- **Confirmation modal:** Esc and backdrop click dismiss it without calling the handler, the same as Bootstrap.
  Clear the alerts state's `confirmation` on the dialog's `close` event so it doesn't go stale.
- Footer rows (`RgModalFooterRow`) should `flex-wrap` so two long buttons stack on very narrow screens.

### Other mobile checks

- **Touch-only hover:** Tailwind `hover:` styles on buttons and links no longer "stick" after a tap on phones.
  That's an improvement over Bootstrap; only the table hover needs to stay plain `:hover` (above).
- **iOS input zoom:** keep inputs at 16px or larger below `sm` (see [Theming](#theming--base-styles)).
- **Success banner:** `fixed inset-x-2 sm:inset-x-0 sm:mx-auto sm:max-w-md` so it has margins on phones and is
  centered on desktop. Its z-index sits above the navbar.
- **Viewport meta tag** is rendered by the shell in `crates/app/src/lib.rs` (`width=device-width,
  initial-scale=1`). Tailwind's responsive variants depend on it, just as Bootstrap's do.
- **Test widths:** 360px (small Android), 390px (iPhone), 768px (`md`), **991/992px** (the navbar switch point) and
  1280px+. Test both standalone and inside the CYCT iframe, in Chrome, Firefox and iOS Safari.

## Necessary API changes

1. **`RgButton(closes_modal: bool)`, additive with a default of false.** It replaces `data-bs-dismiss="modal"`.
   `RgModalBody` provides a `ModalClose` handle for its `<dialog>` in context, and `RgButton` calls it after
   `click`. Existing callers are unaffected.
2. **Migrate the 18 raw footer `<button>`s** (`RgButtonStyle` classes + `data-bs-dismiss`) to
   `RgButton(..., closes_modal = true)`. **`RgButtonStyle::classes()` can stay**, because Tailwind
   is still class-based and the method simply returns `rg-*` names. These call sites still have to change, though,
   because `data-bs-dismiss` no longer does anything.
3. **Optional `RgModalFooterRow` helper** for the repeated `flex-fill d-flex justify-content-between` footer
   wrapper (5 sites), so routes stop hard-coding layout class names.
4. **The confirmation dialog** is driven by the native `<dialog>` instead of `Bootstrap.Modal` (internal).
5. **Delete the Bootstrap JS interop.**

Route-level cleanup (no `Rg*` API change): translate the remaining Bootstrap utilities to Tailwind literals.

| Bootstrap | Tailwind |
|---|---|
| `d-flex` / `flex-fill` / `flex-grow-1` / `flex-wrap` | `flex` / `flex-1` / `grow` / `flex-wrap` |
| `justify-content-between` / `align-items-center` | `justify-between` / `items-center` |
| `text-danger` | `text-danger-600` (theme color) |
| `text-decoration-line-through` | `line-through` |
| `border-top border-2 border` | `border-t-2` |
| `link-secondary link-underline-opacity-0` | `text-gray-600 no-underline hover:underline` |
| `text-center` / `float-start` | `text-center` / `float-left` (or `float-start`) |
| `form-control` (raw `<input>`s in `routes/admin.rs`) | switch to `RgInput`, or `.rg-input` |

## Risks / things to verify

- **Dynamic class names**: grep for `format!` near `class=` and in the `Customizer` before calling any component
  done. A missing class fails silently, with no build error. A test that renders each `RgSz`/`TableColor`/
  `RgButtonStyle` and checks the class against what Tailwind emits catches this.
- **Preflight fallout**: unstyled headings, lists, and buttons across every route.
- **Dropdowns inside modals**: `AddEditBoatModal` hosts `RgSkipperDropdown` and `RatingSelections`. The
  full-viewport dialog with an unclipped panel ([Modals](#modals-on-small-screens)) handles this. Verify it on a
  phone with a boat that has several ORC certificates.
- **Bootstrap/Tailwind class-name collisions** while both are loaded. Cascade layers handle this (see
  [Coexistence](#coexistence-via-cascade-layers-spike-finding)): colliding names take Tailwind's meaning. For
  example, Tailwind's `collapse` is `visibility: collapse`, which is why `NavBar` had to be migrated before it
  became safe.
- **iframe embed height**: the app root posts `regatta_scroll_height` to the parent
  (`observe_scroll_height` in `crates/app/src/chart/resize_observer.rs`, called from `app.rs`). A `<dialog>` in
  the top layer doesn't add height, the same as Bootstrap's fixed modal, but confirm on the CYCT site.
- **Visual parity**: Bootstrap's look comes for free. With Tailwind every component's look is designed by us,
  which is more up-front design work than adopting a component library.

## Size & maintenance comparison

- Bootstrap: ~230 KB CSS + ~80 KB JS (minified sizes of `bootstrap.min.css` / `bootstrap.bundle.min.js`),
  fully shipped.
- Tailwind: only the used classes are emitted. Expect roughly 15–30 KB of CSS, no JS, and Popper is removed.
- Tailwind is actively maintained. The cost moves to us: component styles live in our `app.css`, and behaviors
  live in our components. Most of that behavior already did (`RgDropdown*` is hand-rolled).

## Phased plan

Each phase leaves the app shippable.

**Phase 0: [x] Spike.** Wire Tailwind into the build. Add `app.css` with `@theme` and a base layer, and migrate
`RgButton` + `Modal` (native `<dialog>`) on the Boats screen, including `AddEditBoatModal` with its skipper
dropdown and `RatingSelections`. Also prototype the `NavBar` collapse and `.rg-table` layering, because they are
the riskiest responsive pieces. Confirm the iframe height and the [test widths](#other-mobile-checks) in Chrome,
Firefox and iOS Safari.

### Phase 0 results (2026-10-13)

**Built on the `tailwind` branch** (beyond the plan's spike scope where it was cheaper to finish than to stub):

- Build wiring and `app.css`, which has the burgee theme, Bootstrap breakpoints, z-index scale, base fixes, and
  `.rg-btn*`, `.rg-modal*`, `.rg-navbar*`/`.rg-nav-link` and `.rg-table*` components.
- `RgButton`: `rg-*` classes and `closes_modal`. `RgButtonStyle::classes()` is kept.
- `Modal`: `<dialog>` with the full-viewport/panel structure, backdrop click, the `ModalClose` context, and a
  **temporary `data-bs-dismiss` shim** so footers on routes that hadn't been migrated still closed their modal
  (removed in Phase 3).
- The confirmation dialog on the native `<dialog>`, clearing the stale confirmation on `close`. The Bootstrap JS
  interop is deleted, and `bootstrap.bundle.js` is no longer loaded.
- `NavBar`: state-driven collapse at 992px, inline-SVG hamburger, `aria-current`.
- `RgTable`/`TableColor`: layered `.rg-table` CSS with literal color classes.
- `AddEditBoatModal` footer moved to `RgButton(closes_modal = true)`.

**Verified** against the production build served by the app server, with real race data, in Chrome. Phone and
tablet widths were tested in same-origin iframes, which is also how the CYCT embed sees the app:

- **Navbar:** at 390px and 991px the hamburger shows and opens a vertical menu. At exactly 992px the links are
  inline and the hamburger is hidden.
- **Tables:** the default style (grey, striped rows) and the results style (light, striped columns, amber
  `Warning` cells) both render. Hovering highlights the whole row across striped and unstriped columns. At 390px
  the 15-column results table scrolls sideways inside its wrapper.
- **Modal at 390px:**
  - The panel fills the width with margins, and the dialog scrolls while the page behind is locked.
  - An absolutely positioned menu near the panel's bottom edge extends past the panel without being clipped.
  - A press that starts in the panel and ends on the backdrop does not close the modal; a real backdrop click
    does, and so does a real Esc keypress.
  - Focus goes to the close button on open.

**Findings that change the plan:**

1. **Coexistence works with cascade layers.** See [below](#coexistence-via-cascade-layers-spike-finding). This
   replaces the "don't run side by side" advice and the `prefix(tw)` fallback.
2. **Preflight's `img { height: auto }` overrides HTML `height=""` attributes.** The burgee grew from 60px to
   about 104px. Size images with classes (`h-[60px] w-auto`). The base layer now restores `img, svg` to inline,
   which fixes the stacked footer badges and inline `RgSpinner`.
3. **Third-party CSS must stay out of Tailwind's pass**, so `maplibre-gl.css` is loaded on its own; see
   [Build integration](#build-integration).
4. **Found along the way, unrelated to Tailwind:** the fluid container never matched Bootstrap's
   `container-fluid`, so it was a no-op and the app has always rendered edge to edge; keep that in mind for
   Phase 4 parity.

**Not verified, carried forward:**

- **Firefox and iOS Safari:** only Chrome was available. Check these in Phase 5.
- **The real `AddEditBoatModal`:** the boat API needs an admin login, so the modal was tested with the confirmation
  dialog plus a synthetic tall body and absolutely positioned menu. Re-test with `RgSkipperDropdown` and
  `RatingSelections` when logged in (Phase 3).
- **iframe height reporting on the CYCT site** (Phase 5).

### Coexistence via cascade layers (spike finding)

`app.css` declared `@layer theme, base, bootstrap, components, utilities;` and imported Bootstrap's CSS with
`layer(bootstrap)`. Layer order beats specificity, so:

- **Reboot (bootstrap) beats Preflight (base).** Screens that haven't been migrated look as before.
- **`.rg-*` components and Tailwind utilities beat Bootstrap.** Migrated components aren't disturbed by Reboot's
  element rules.
- **Name collisions mostly resolve to Tailwind's meaning**, because utilities are layered above Bootstrap.
  `collapse` is already gone with the `NavBar` migration.
- **Exception (found in Phase 4): Bootstrap's `!important` spacing utilities win.** For `!important`
  declarations the layer order is reversed, so `.mb-4 { margin-bottom: 1.5rem !important }` in the `bootstrap`
  layer beats Tailwind's `mb-4` (1rem). While Bootstrap is loaded, `mb-4`, `mt-4`, `mx-4` and `px-4` render at
  24px instead of 16px, in `RgInputWithButton`, the `Admin`/`Rc` `RgDiv`s, the `RgDropdownNone` filter and the
  `nonav` link. Same-sized pairs (`m-0`, `m-2`, `my-2`, `p-0`, `py-2`) are unaffected, and Bootstrap has no `-6`
  or `-12` steps. This is 8px of extra spacing in four places until Phase 5 removes the import. If a phase is
  ever shipped before then, use Tailwind's arbitrary values (e.g. `mb-[1rem]`), which can't collide.

So each phase can be merged on its own, as the "each phase leaves the app shippable" goal intended, and Bootstrap's
import is deleted in Phase 5.

**Phase 1: [x] Foundation.** Mostly done by the spike: build wiring, `@theme` (colors, breakpoints, z-index),
the forms plugin, and coexistence layering. Remaining work:

- the full base layer: responsive `h1`–`h4` sizes with `clamp()`, paragraph and list spacing, link color. It can
  only be checked once Reboot stops winning, so check it against a page with the Bootstrap import commented out;
- contrast checks on the burgee color scales (WCAG AA).

### Phase 1 results (2026-10-13)

- **Base layer** (`app.css` `@layer base`) recreates Bootstrap 5.3 Reboot for the elements the screens use:
  - body font, line height and color;
  - `h1`–`h6` weights and margins. `h1`–`h4` use Reboot's responsive sizes as `min(calc(…+…vw), cap)`, which
    reaches the cap exactly at 1200px, as Bootstrap's RFS does;
  - `p`, `ul`/`ol` (spacing and bullets), `b`/`strong`, `small`, `hr`, `label`, `a`;
  - the earlier pointer-cursor and inline `img`/`svg` fixes.

  **Verification:** with the Bootstrap import temporarily commented out, I compared computed styles (font size,
  weight, line height, margins, list padding and style, display, decoration, cursor, vertical align) for 69
  elements on `/races/results`, `/races/results/view/100` and `/admin`, at 1280px and 390px. Every value matched
  Bootstrap, except one difference that isn't part of the base layer (see the Phase 4 note).
- **Link color is a deliberate change:** burgee light blue darkened to `info-700` (`#376d87`, 5.7:1 on white),
  replacing Bootstrap's blue. Body text is `slate-900`.
- **Contrast:** every text/background pair the `.rg-*` classes use passes WCAG AA (4.5:1), with Tailwind's real
  palette values and the `color-mix()` burgee shades computed exactly. The exception was **success**: white on
  `green-600` is only 3.22:1. `--color-success-600`/`-700` now map to `green-700`/`green-800` (4.94:1 and 7.09:1).
  Selected ratios:

  | Pair | Ratio |
  |---|---|
  | white on primary navy `#131340` | 17.6 |
  | white on danger-600 `#ca352d` | 5.2 |
  | white on success (`green-700`) | 4.9 |
  | white on secondary-600 | 7.6 |
  | nav link `slate-600` on surface | 7.3 |
  | table caption `slate-500` on white | 4.8 |
  | `slate-900` on the darkest table state (secondary + hover overlay) | 12.3 |

**Phase 2: [x] Leaf components (API unchanged).** `RgButton` is done (spike). `RgInput`,
`RgNumberInput`, `RgInputWithButton`, `RgTime`, `RgCheck`, `RgSwitch`, `RgDropdown*` (including outside-click/Esc
close), `RgRaceYearSelector`, `RgSpinner`.
`RatingSelections`, `FinishCodeDrop`, and `TimeRow` should need no edits.

### Phase 2 results (2026-10-13)

- **New `app.css` components:**
  - `.rg-input`/`.rg-select` (Bootstrap `.form-control`/`.form-select` metrics, always 16px);
  - `.rg-input-group`, `.rg-input-addon` and `.rg-input-group-sm`, which uses 16px text below `sm` so iOS doesn't
    zoom, and 14px from 576px up;
  - `.rg-check`, `.rg-checkbox`, and `.rg-switch` (a CSS-only knob);
  - `.rg-dropdown*` (toggle caret, menu at `--z-dropdown` capped to `100vw - 2rem`, items, divider).
- **Components, with signatures unchanged:**
  - `RgInput`, `RgNumberInput`, `RgInputWithButton` (its button uses `RgButtonStyle::PrimaryOutline`; `mb-3` became
    `mb-4` to keep Bootstrap's 1rem), `RgTime`, `RgCheck`, `RgSwitch`;
  - `RgDropdown`/`RgIdDropdown`/`RgDropdownNone`, whose filter input's `mx-3` became `mx-4`;
  - `RgRaceYearSelector`.
  - `RatingSelections`, `FinishCodeDrop` and `TimeRow` needed no edits. `RgSpinner` was already Bootstrap-free and
    is unchanged.
- **Dropdown dismiss** (decision 5, `crates/app/src/components/rg_dropdown.rs`):
  - While a menu is open, capture-phase `pointerdown`/`keydown` listeners on `document` close it on a press
    outside its root, or on Escape.
  - Escape is consumed (`preventDefault` + `stopPropagation`), so a dropdown inside a modal closes only itself.
  - The listeners are added and removed with the open state.
- **Tests:** `crates/app/tests/leaf_components_test.rs` checks the rendered markup (dropdown closed with the
  primary toggle, a dropdown with one item disabled, `RgDropdownNone`, button classes, number input, input with
  button, check and switch, `RgTime`'s compact input group with no Bootstrap classes, the inline spinner). The
  browser cases in `crates/app/tests/wasm.rs` cover the behavior:
  - dropdown open, select and close; outside press vs inside press; Escape consumed;
  - `RgDropdownNone` filter, None, and filter reset;
  - `RgNumberInput` number validation; `RgInputWithButton`; `RgCheck`/`RgSwitch` events.
- **Visual check** (Chrome, dev server, Bootstrap still layered underneath):
  - the real `RgRaceYearSelector` on `/races/results`;
  - for the admin-only components, their exact markup and classes rendered with the real stylesheet: inputs, the
    input group with button, compact time fields, checkboxes, switches on and off, both dropdowns open, and a long
    skipper name;
  - compact time fields compute to 16px at 392px wide and 14px at 854px.
- **Still to see in the real app:** the logged-in forms (People, Boats, Classes, Races, RC). Check them in the
  Phase 3 logged-in pass.

**Phase 3: [x] Dialogs.** `RgModalButton`/`RgModalBody`/`RgModal` on `<dialog>` and the confirmation dialog are
done (spike). Migrate the remaining 16 footer buttons (`AddEditBoatModal`'s two are done) to
`RgButton(closes_modal = true)`, then delete the `data-bs-dismiss` shim in `RgModalBody` and the `data-bs-*`
attributes. Re-test `AddEditBoatModal` logged in at phone width.

### Phase 3 results (2026-10-13)

- **Footers:** all 16 remaining raw footer `<button>`s are now `RgButton`, converted mechanically. The converter
  rejected any block whose shape it didn't recognize.
  - Converted: `RgForm`'s `RgTimeButton` 2, `RaceResults` 2, `Rc` 4, `Classes` 1, `People` 5, `Series` 2.
  - Labels, styles, `disabled` conditions and click bodies were kept exactly. Only buttons that had `data-bs-dismiss`
    got `closes_modal = true`. `RgButton` runs `click` before closing, the same order as Bootstrap.
- **Bootstrap leftovers removed:** the `data-bs-dismiss` fallback in `RgModalBody` is deleted, and no `data-bs-*`
  attributes remain in the code.
- **`RgModalFooterRow`** (new component; nothing existing changed) replaces the
  `flex-fill d-flex justify-content-between` wrapper in the 5 modal footers. It spreads its buttons to both ends
  and wraps them on narrow screens. The same Bootstrap classes in the Classes screen's bracket Min/Max row are modal
  *content*, so they're left for the Phase 4/5 sweep.
- **Tests:** `crates/app/tests/modal_test.rs` checks the rendered modal button, body and confirmation dialog. The
  browser cases in `crates/app/tests/wasm.rs` cover:
  - open by id; `closes_modal` vs plain buttons;
  - × button; backdrop click vs a press that starts in the panel;
  - the confirmation → Yes answers and closes, and a dismissal clears the question without answering.
- **Logged-in pass** (you logged in; edits allowed in this test environment), in Chrome at phone width (390px
  iframe) and at 854px. Every throwaway record was deleted afterwards; the database was checked to confirm it:
  - **Boats** (real clicks and typing):
    - Add Boat → name → skipper dropdown. Its menu extends past the panel and the dialog scrolls; the filter works.
    - A real Esc closes only the menu.
    - Rating Type → PHRF (`RatingSelections`), rating 150 → Save. The modal closes and the boat is saved correctly.
    - Edit Boat opens pre-filled (Clear disabled) → sail number → Save.
    - Delete Boat → confirmation: No keeps the boat, Yes deletes it.
  - **People:** add a skipper, then Edit → in-modal Delete → No (back to Delete/Save) → Delete → Yes deletes and
    closes.
  - **Series:** add (Save disabled while the name is empty) → Edit → Delete.
  - **Classes:** the edit modal (brackets, rating fields) renders and scrolls at phone width.
  - **RC:** check in a boat → Finish modal Cancel/Save → delete confirmation Cancel, then "Yes - Delete it!" →
    check the boat out again.
  - **Race results editor:** Edit → Cancel; Add Result renders; a real Esc closes it.
  - **At 854px:** the modal panel is 500px wide and centered, and a real backdrop click closes it.
- **Found along the way (existing, not changed):**
  - ~~`RgTimeButton` has no callers~~: deleted (2026-10-13).
  - ~~The Classes edit modal's title is hard-coded as "Add Race Class"~~: it now shows "Edit Race Class" when the
    class being edited has a non-zero id (2026-10-13).
  - ~~RC check-in labels end with a stray `}`~~: fixed in `drop_label()` (`crates/common/src/data/race_data.rs`),
    which had one `}` too many. The same pass fixed `rated_label()` printing "ORCnull" for an ORC boat without
    certificates (2026-10-13).
- **For Phase 4:** the success banner (`alert alert-success`) and the RC pills are still Bootstrap-styled.

**Phase 4: [x] Layout, chrome & data.** `NavBar` and `RgTable` are done (spike), except the `nonav` link, which
still uses `nav-link`. Remaining: `Customizer` literal tables + spacing-scale mapping, `RgGrid` (note the fluid
container no-op). Also `#route_content` (in `app.rs`) gets Bootstrap's `d-flex`/`flex-column`/`flex-grow-1`
through `RgDiv`'s `add_custom`. The Phase 1 check showed the content stops being a flex column without Bootstrap,
so map these to `flex`/`flex-col`/`grow`. Then: the RC pills, the `Alerts` banner, `RgSortable`.

### Phase 4 results (2026-10-13)

- **`Customizer` / `RgDiv` spacing:** `RgSz` gained `tw()`, the Tailwind step with the same size as Bootstrap's
  (`S3`→`4`, `S4`→`6`, `S5`→`12`). `spacing_class` spells every combination out as a literal, and
  `@source inline("{m,p}{,t,b,s,e,x,y}-{0,1,2,4,6,12,auto}")` in `app.css` makes Tailwind emit every
  combination regardless. A test checks every `RgSpace` × `RgSide` × `RgSz` combination against that pattern, so
  the two can't drift apart. `RgSz::size()` (Bootstrap's step) is kept, so the API is unchanged.
- **`RgGrid`:**
  - `RgContainerType` and `RgPosition` carry one literal `rg-*` class per entry (`cls()`). `.rg-container*` and
    the fixed/sticky positions are reproduced in `app.css` with Bootstrap's max-widths and z-index.
  - `min-vh-100 d-flex flex-column` became `min-h-screen flex flex-col`.
  - `RgContainerType::ContainerFluid` → `.rg-container-fluid` is just `width: 100%`. The app has always rendered
    edge to edge (see Phase 0), so that look is kept rather than adding padding now.
- **`#route_content`:** its `add_custom` classes are now `grow`/`flex`/`flex-col`, so the content is a flex
  column again, as it was under Bootstrap.
- **`nonav` link:** `nav-link` became `block px-4 py-2 no-underline`.
- **RC pills:** `nav nav-pills` became `.rg-pills`/`.rg-pill`. The active tab uses `aria-current="page"` and is
  navy with white text (17.6:1); inactive pills use the link color `info-700` (5.7:1).
- **Success banner:** `.rg-alert-success` replaces `alert alert-success d-flex align-items-center fixed-top` and the
  inline 80px top margin:
  - fixed at 80px from the top, with 8px side margins on phones and centered at `max-width: 28rem` from `sm` up;
  - `z-index` 1030, `green-800` on `green-100` (6.45:1);
  - the unused Bootstrap Icons classes on the check icon are gone.
- **`RgSortable`:** `list-group`/`list-group-item` became `.rg-list-group`/`.rg-list-group-item` (Bootstrap's
  list-group metrics plus `cursor: move`). The SortableJS ghost class `bg-info` became `.rg-sortable-ghost`.
- **Tests:** `crates/app/tests/layout_test.rs`: the safelist guard (`every_spacing_combination_is_safelisted`),
  spacing keeping Bootstrap's sizes, `RgGrid` container/position classes being single `rg-*` names, `RgSortable`
  classes, and the `Alerts` banner.
- **Verified in Chrome** (dev server, your saved login):
  - `/rc`: `#route_content` is a flex column with its 8px margin; the pill row's margin is 24px (Bootstrap
    `my-4`); active and inactive pill colors are right.
  - `/series` → Change Sort Order: list-group styling; a real drag reordered the items; Cancel discarded it.
  - An unchanged Edit Series → Save showed the banner: 80px from the top, 448px wide and centered at 854px, with
    8px side margins at 390px.
  - `?nonav`: no navbar; the "Full Regatta Results" link opens `?nav` in a new tab.
  - The production CSS contains safelisted combinations that no source file uses (`mb-12`, `pe-auto`).
- **Found:** the `!important` collision described in
  [Coexistence](#coexistence-via-cascade-layers-spike-finding). It's resolved by Phase 5.

**Phase 5: [x] Remove Bootstrap.** Route-level class sweep (`grep -rn 'class=' crates/app/src`) (any Bootstrap
name left behind loses its styling once the import is gone), remove the Bootstrap `@import` and `bootstrap` layer
from `app.css` (the Bootstrap JS interop is already gone), delete unused `RgRow`/`RgCol`/`RgButtonGroup`. Do a
visual pass of every route at phone and desktop widths and inside the CYCT iframe embed, in Chrome, Firefox and
iOS Safari.

### Phase 5 results (2026-10-13)

- **Route-level class sweep.** Every remaining Bootstrap name was replaced:

  | Where | Was | Now |
  |---|---|---|
  | `RaceResultsView` class box | `border-top border-2 border` | `border-2 border-slate-200` (Tailwind's default border color is `currentColor`) |
  | results/standings bracket rows | `table-light table-borderless` on `<tr>` | `TableColor::Light.cls()`. `table-borderless` only applies to `<table>`, so it was a no-op on a row. |
  | footer (`app.rs`) | `container`, `link-secondary link-underline-opacity-0` | `rg-container`, `text-slate-600 no-underline hover:text-slate-900` |
  | `Classes` bracket min/max row | `flex-fill d-flex justify-content-between` | `flex-auto flex justify-between` |
  | `Course` map | `flex-grow-1` | `grow` |
  | RC check-in filter | `input-group input-group-sm`, `form-control` | `rg-input-group rg-input-group-sm`, `rg-input` |
  | `Admin` login fields | `form-control` | `rg-input` |
  | `SeriesStandings` throw-outs | `text-danger`, `text-decoration-line-through` | `text-danger-600`, `line-through` |

  `float-start`, `flex-wrap` and `text-center` mean the same in Tailwind and stay. Every raw
  `<input>`/`<select>`/checkbox carries an `rg-*` class, so the forms plugin's defaults never show unstyled.
- **Bootstrap removed:**
  - the `@import` and `bootstrap` layer are gone from `app.css`;
  - no Bootstrap CSS or JS is in the build output;
  - unused `RgRow`, `RgCol`, `RowCustomizer`, `ColCustomizer`, `RgBp`, `RgColType` and `RgButtonGroup` were
    deleted;
  - remaining mentions of Bootstrap are explanatory comments only.
- **Base layer:** added Reboot's `th { text-align: inherit }`, since browsers center `<th>` by default.
- **Bug found and fixed (from Phase 2):** in compact input groups (`RgTime`, the RC filter) the inner corners were
  rounded. `.rg-input-group-sm > …` had the same specificity as the corner-squaring rules and came later in the
  file. It now comes first, so the X button and filter field share a square seam (−1px overlap) and only the
  outer corners are rounded.
- **The `!important` collision is resolved:** `mb-4`/`mt-4`/`mx-4`/`px-4` are Tailwind's 16px now, and links use
  `info-700` instead of Bootstrap blue.
- **Visual pass** (Chrome, logged in): every route at 390px and at a real 1280px (a scaled frame).
  - Routes: Home, Skippers, Boats, Series, Classes, Races, Edit Race, Create Race, Race Results view, Results
    Editor, Series Standings, RC (Race Config and Check-In), Course, Admin.
  - On each one, the script checked for no horizontal page overflow at 390px (wide tables scroll inside their
    wrapper) and no leftover Bootstrap class names.
  - On the nine routes measured while the listener was attached, the `regatta_scroll_height` message the app posts
    to its parent for the CYCT iframe embed matched the document height (within 1px). Course, Create Race and
    Admin were checked after the dev server's live reload dropped my listener, so their heights weren't recorded.
  - Course's map canvas fills the space (`grow`) at both widths.
- **Not verified:**
  - **Firefox and iOS Safari:** neither is available on this machine.
  - **On the CYCT site itself:** the embed was simulated with same-origin iframes and the height messages.
  - **The logged-out Admin login form:** its fields use the same verified `rg-input`, but I didn't log you out to
    look at it.

## Decisions

Resolved 2026-10-13; these were the plan's open questions.

1. **Styling: component classes.** Component looks are defined once in `app.css` as `@layer components` classes
   built with `@apply` (`.rg-btn*`, `.rg-input`, `.rg-table`, `.rg-modal*`, `.rg-dropdown*`, …). Tailwind
   utilities appear in Rust only for one-off layout, and always as literal strings. See
   [Styling strategy](#styling-strategy-component-classes-and-utilities).
2. **Spacing: keep Bootstrap's visual sizes.** `Customizer`/`RgSz` map onto the Tailwind scale (`S0`→`0`, `S1`→`1`,
   `S2`→`2`, `S3`→`4`, `S4`→`6`, `S5`→`12`, `Auto`→`auto`) through a literal lookup table, so the layout doesn't
   shift and `RgDiv` callers are unchanged.
3. **Build: Tailwind run by cargo-leptos** from `tailwind-input-file`, with the scan root set by `@source` in
   `app.css`, as described in [Build integration](#build-integration).
4. **Palette: derived from the CYCT burgee** (`crates/app/public/cyct_burgee.svg`). See
   [Theming](#theming--base-styles) for the token mapping.
5. **Dropdown UX: outside-click and Esc to close are in scope.** This is added inside `RgDropdown`/`RgIdDropdown`/
   `RgDropdownNone` with no API change. Full keyboard navigation (arrow keys, ARIA listbox roles) is out of
   scope and can be a follow-up.
