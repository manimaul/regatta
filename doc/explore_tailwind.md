# Exploration: Replacing Bootstrap 5 with Tailwind CSS

## Goal

Replace Bootstrap 5.3.3 (CSS + `bootstrap.bundle.js` + Popper) in the `web` module with
[Tailwind CSS](https://tailwindcss.com) v4 (currently 4.3.x), while:

- staying on **Compose HTML** (DOM rendering);
- **preserving the existing `Rg*` component API** (`RgButton`, `RatingSelections`, `RgInput`, `RgDropdown`,
  `RgModal`, `RgTable`, `RgDiv`, …) so the route screens in `components/routes/` change as little as possible.
  API changes are listed in [Necessary API changes](#necessary-api-changes) and are additive where possible.

## Key differences from Bootstrap

| | Bootstrap | Tailwind |
|---|---|---|
| What ships | Prebuilt CSS + JS behaviors (modal, collapse, dropdown via Popper) | **CSS only**, generated at build time from the class names found in source files |
| Components | `.btn`, `.modal`, `.table`, `.form-control`, … | None — compose utilities, or define our own component classes with `@apply` |
| Base reset | Reboot (styled headings, tables, forms) | Preflight (headings, lists, buttons and inputs fully **unstyled**) |
| Spacing scale | `1`=.25rem, `2`=.5rem, `3`=1rem, `4`=1.5rem, `5`=3rem | linear: `n` × .25rem (`3`=.75rem, `4`=1rem, `6`=1.5rem, `12`=3rem) |
| Build | `require("bootstrap.css")` | PostCSS pass that **scans source files** for class names |

Two consequences shape this plan:

1. **All JS behavior moves into Kotlin.** Modal show/hide, `data-bs-dismiss`, and navbar collapse are currently
   handled by Bootstrap JS. Tailwind has no runtime, so this becomes Compose state (or the native `<dialog>`
   element). This is the only part that forces API changes.
2. **Class names must appear as literal strings in Kotlin source.** Tailwind's scanner reads `.kt` files as text.
   Strings assembled at runtime, like `"table-${color.name}"` in `RgTable` or `"$sp$side-$size"` in
   `Customizer.set`, will **not** produce CSS. Every generated class has to come from a literal lookup table
   (`when` / enum property holding the full class string), or be safelisted with `@source inline("…")`.

## Current Bootstrap footprint (summary)

- Loaded in `Main.js.kt` (`require("bootstrap/dist/css/bootstrap.css")`, `require("bootstrap/dist/js/bootstrap.bundle.js")`);
  npm deps `bootstrap` and `@popperjs/core` in `web/build.gradle.kts`.
- **Encapsulated in `components/`** (swap internals, keep API): `RgButton`, `RgForm` (`RgInput`, `RgNumberInput`,
  `RgInputWithButton`, `RgTime`, `RgCheck`), `RgSwitch`, `RgDropdown`/`RgIdDropdown`/`RgDropdownNone`,
  `Modal` (`RgModal`, `RgModalButton`, `RgModalBody`), `RgTable`, `RgGrid` (`RgGrid`, `RgDiv`, `Customizer`),
  `NavBar`, `Alerts`, `RgSortable`.
- **Bootstrap JS interop:** `utils/Bootstrap.kt` (`Bootstrap.Modal`, `Bootstrap.Collapse`), used by
  `AlertsViewModel` (confirmation modal, `hidden.bs.modal`/`shown.bs.modal`) and `NavBar`. Plus 18
  `data-bs-toggle`/`data-bs-target`/`data-bs-dismiss` attributes.
- **Leaked into routes:** 18 raw `Button`s styled with `classes(*RgButtonStyle.X.classes)` +
  `attr("data-bs-dismiss", "modal")` in modal footers (`People`, `Rc`, `Series`, `Classes`, `RaceResults`,
  `AddEditBoatModal`, `RgForm.RgTimeButton`). Utility classes such as `flex-fill d-flex justify-content-between`, `flex-wrap`,
  `flex-grow-1`, `text-danger`, `text-decoration-line-through`, `border-top border-2 border`,
  `link-secondary link-underline-opacity-0`, `container`, `text-center`, `float-start`, `form-control` (`Admin`),
  `form-select` (`RgYearSelect`), `nav nav-pills nav-link` (`Rc`).
- **Unused:** `RgButtonGroup`, `RgRow`, `RgCol`, `RgBp`, `RgColType`.

`RatingSelections` contains no Bootstrap classes directly. It composes `RgDropdown`, `RgNumberInput`,
`RgInputWithButton`, `RgButton`, and `RgSpinner`, so once those are migrated it should compile and work
**unchanged**. It is the regression check for "API preserved".

## Build integration

Kotlin/JS already runs webpack with `cssSupport` (css-loader + style-loader). Tailwind v4 plugs in through PostCSS.

1. **npm dev dependencies** in `web/build.gradle.kts`:
   ```kotlin
   implementation(devNpm("tailwindcss", "4.3.3"))
   implementation(devNpm("@tailwindcss/postcss", "4.3.3"))
   implementation(devNpm("@tailwindcss/forms", "0.5.11"))   // optional, see Forms
   implementation(devNpm("postcss", "8.5.30"))
   implementation(devNpm("postcss-loader", "8.2.1"))
   ```
   Then run `./gradlew kotlinUpgradeYarnLock` (`kotlin-js-store/yarn.lock` is gitignored in this repo, so there's
   nothing to commit).
2. **`web/webpack.config.d/tailwind.js`.** Files in `webpack.config.d` are concatenated into
   `build/js/packages/regatta-web/webpack.config.js`, so `__dirname` resolves the same way it does in
   `dev_server_config.js`. The spike **excludes `app.css` from the generated `/\.css$/` rule and adds a separate rule**
   for it with `style-loader` → `css-loader` → `postcss-loader`. That way Tailwind only processes `app.css`, and
   `maplibre-gl.css` keeps the plain Kotlin-generated rule:
   ```js
   const appCss = /app\.css$/;
   const cssRule = config.module.rules.find(r => String(r.test) === String(/\.css$/));
   cssRule.exclude = appCss;
   config.module.rules.push({ test: appCss, use: [style-loader, css-loader, {
     loader: 'postcss-loader',
     options: { postcssOptions: { plugins: [
       require('@tailwindcss/postcss')({ base: path.resolve(__dirname, '../../../../web/src/jsMain/kotlin') }),
     ] } },
   }] });
   ```
   Loaders run right to left, so `postcss-loader` goes last. This file must **not** be excluded from production
   builds or tests. The test bundle is built in `build/js/packages/regatta-web-test`, which is at the same depth, so
   `base` still resolves.
3. **CSS entry point**, `web/src/jsMain/resources/app.css`, required from `main()` as `require("./app.css")` in place
   of both Bootstrap `require`s. While the migration is in progress, it also imports Bootstrap into a cascade layer
   (see [Coexistence](#coexistence-via-cascade-layers-spike-finding)):
   ```css
   @layer theme, base, bootstrap, components, utilities;
   @import "bootstrap/dist/css/bootstrap.css" layer(bootstrap);   /* removed in Phase 5 */
   @import "tailwindcss";        /* source root comes from the `base` option above */
   @plugin "@tailwindcss/forms";
   @theme { /* CYCT colors, Bootstrap breakpoints, z-index */ }
   @layer base { /* Reboot replacement */ }
   @layer components { /* .rg-btn, .rg-table, … */ }
   ```
   Don't add a relative `@source "../kotlin"`. Resources are copied to `build/js/packages/regatta-web/kotlin/`
   before webpack runs, so a path relative to `app.css` would point into the build directory, not at the
   Kotlin sources. Let the `base` option in `tailwind.js` set the scan root.
4. **Verified in the spike:**
   - `jsBrowserProductionWebpack` builds, and the server serves the bundle.
   - `jsBrowserDevelopmentRun --continuous` picks up a new Tailwind class about 4 seconds after a `.kt` edit.
   - `jsBrowserTest` could **not** be checked. `RouteMatcherTest.kt` references `Route.BoatEdit`, which commit
     08953f5 removed, so test compilation already fails on `main`. Fix that test before relying on Karma.
   - `jsBrowserDevelopmentWebpack` (no dev server) fails in `dev_server_config.js` because `config.devServer` is
     undefined. That's also an existing problem: only the production task renames that file out of the way.
5. **Fallback** (not needed): run the standalone Tailwind CLI from a Gradle `Exec` task.

## Styling strategy: component classes and utilities

Two ways to express Bootstrap-like components in Tailwind:

- **A. Utilities inline in Kotlin**, e.g. `RgButtonStyle.Primary("px-3 py-1.5 rounded bg-blue-600 text-white hover:bg-blue-700 …")`.
  This is the most "Tailwind-native" option, but class strings get long and are repeated in raw `Button` call sites.
- **B. Semantic component classes defined in `app.css` with `@apply`** (`.rg-btn`, `.rg-btn-primary`, `.rg-input`,
  `.rg-table`, `.rg-modal`, …), with utilities used directly only for one-off layout.

**Decision: B.** It keeps `Rg*` internals short, centralizes the design in one CSS file, and avoids the
dynamic-class-name problem for component styles. Classes declared in `@layer components` are always emitted, so they
don't depend on scanning. Use Tailwind utilities directly in Kotlin for layout, and keep those as literals.

A tempting shortcut is a **Bootstrap-name shim**: define `.btn`, `.btn-primary`, `.form-control`, … in `app.css`
with `@apply`, so nothing in Kotlin changes. It works as a temporary bridge during migration (Phase 1), but it
preserves Bootstrap naming forever. Rename to `rg-*` as each component is migrated.

## Theming & base styles

- **`@theme` tokens, derived from the CYCT burgee.** Semantic color scales keep `RgButtonStyle` and `TableColor`
  semantics (`Primary`, `Success`, `Danger`, `warning`, `info`, …) tied to brand colors instead of raw `green-600`:

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
  hamburger between 992 and 1023 px, where it's expanded today, and `container` max-widths would change. Override
  them in `@theme`:
  ```css
  @theme {
    --breakpoint-sm: 576px; --breakpoint-md: 768px; --breakpoint-lg: 992px;
    --breakpoint-xl: 1200px; --breakpoint-2xl: initial; --breakpoint-xxl: 1400px;
  }
  ```
  This keeps `RgBp`, `RgContainerType` and the sticky entries of `RgPosition` (`sticky_sm_top`, …) meaning the same
  widths. Their literal tables map to `sm:sticky sm:top-0`, `xxl:…` and so on.
- **Preflight replaces Reboot.** Preflight is more aggressive: `h1`–`h6` lose their sizes, `ul` loses bullets,
  `button` loses its styling, `img` becomes `display:block`, and tables lose spacing. Screens rely on raw
  `H3`/`H5`/`H6`, `P`, `B`, `Br`, `Ul`, and `Caption`, so add an `@layer base` with heading sizes and margins,
  paragraph spacing, and link color. Otherwise every route shifts visually. Specifics that are easy to miss:
  - **Heading sizes shrink on phones in Bootstrap.** Reboot sizes `h1`–`h4` responsively (e.g. `h1` is
    `calc(1.375rem + 1.5vw)`, capped at 2.5rem from 1200px up). Reproduce that with `clamp()` or `xl:` sizes, so
    page titles don't get huge on phones or tiny on desktop.
  - **Pointer cursor.** Tailwind v4 Preflight gives `button` `cursor: default`. Bootstrap's `.btn`, `.nav-link`
    and `.dropdown-item` show a pointer. Add `button:not(:disabled), [role=button] { cursor: pointer }` to the
    base layer.
  - **SVG is block-level.** Preflight makes `svg`/`img` `display:block`, which would push the inline
    `RgSpinner` and the footer badges in `Router.kt` onto their own lines. Give them `inline-block` explicitly.
- **Forms:** `@tailwindcss/forms` gives native inputs, selects, and checkboxes a sane baseline that is easy to
  restyle. It covers `RgInput`, `RgTime`, `RgCheck`, `RgYearSelect`, and the raw `Input`s in `Admin.kt`.
  Keep input text at **16px or larger below `sm`**. iOS Safari zooms the page when a focused input's font size is
  under 16px. Compact `RgTime` fields can use `text-base sm:text-sm`. Bootstrap's `input-group-sm` (14px) in
  `RgTime` and `Rc.kt` triggers that zoom today, so this is a chance to fix it.
- **Dark mode:** not required. `NavBar` currently forces `data-bs-theme="light"`. Tailwind's `dark:` variant can be
  added later.

## Component mapping (internals change, API stays)

| Component | Bootstrap today | Tailwind replacement | API impact |
|---|---|---|---|
| `RgButton` | `btn btn-*` | `.rg-btn` + `.rg-btn-{primary,secondary,danger,success,light,dark,link,outline-*}` in `@layer components` | Signature unchanged. `RgButtonStyle.classes` **stays** (now `rg-*` names), so raw `Button`s that use it keep working. Add optional `closesModal: Boolean = false`. |
| `RgButtonGroup` | `btn-group` | `inline-flex` + rounded-edge rules. Unused, so consider deleting. | None |
| `RgSpinner` | custom SVG | Keep as is (no Bootstrap). Optionally `animate-spin`. | None |
| `RgInput`, `RgNumberInput` | `label` + `form-control` | `.rg-input` (`block w-full rounded border-gray-300 focus:ring-primary-500 …`) | None; behavior (controlled value, `locked` logic) untouched since these stay native `Input`s |
| `RgInputWithButton` | `input-group mb-3` + `btn-outline-primary` | `flex` row, input `rounded-r-none`, button `rounded-l-none -ml-px` | None |
| `RgTime` | `input-group input-group-sm` + `input-group-text` | `.rg-input-group` / `.rg-input-addon` component classes (`flex`, shared borders, compact padding) | None |
| `RgCheck` | `form-check`, `form-check-input/label` | `flex items-center gap-2` + `.rg-checkbox` | None. The `customClasses` default (`form-check`) becomes `rg-check`; the one caller passing custom classes is unaffected. |
| `RgSwitch`, `RgSwitches` | `form-switch` | Checkbox styled as a switch with `appearance-none` + `peer`/`checked:` utilities (`.rg-switch`) | None |
| `RgDropdown`, `RgIdDropdown`, `RgDropdownNone` (+ `RgSeriesDropdown`, `RgClassDropdown`, `RgSkipperDropdown`) | Already **hand-rolled** in Kotlin (`toggle` state) with `dropdown-toggle`/`dropdown-menu show`/`dropdown-item` classes | Swap classes for `.rg-dropdown-toggle`, `.rg-dropdown-menu` (`absolute z-50 mt-1 rounded shadow-lg bg-white max-h-80 overflow-auto`), `.rg-dropdown-item`. Replace the inline `z-index`/`position` style with classes. The filter `RgInput`'s `customClasses = listOf("w-auto","mx-3","my-2")` becomes Tailwind equivalents (`w-auto mx-4 my-2`). **Add close on outside click and Esc** (decision 5): a document `pointerdown`/`keydown` listener registered in a `DisposableEffect` while the menu is open, ignoring events inside the dropdown's root element. Use `pointerdown` rather than `mousedown` so taps close it on touch screens. When a menu is open inside a modal, Esc must `preventDefault()`/`stopPropagation()` so it closes only the menu and not the dialog too. Cap the menu width at `max-w-[calc(100vw-2rem)]` so long boat or skipper names don't run off a phone screen. | None |
| `FinishCodeDrop` | wraps `RgDropdown` | free | None |
| `RgYearSelect` | `form-select` | `.rg-select` (forms plugin) | None |
| `RgModalButton` / `RgModalBody` / `RgModal` | `data-bs-toggle`/`data-bs-target` + `.modal.fade` markup, Bootstrap JS | **Native `<dialog>`**. `RgModalButton(id)` calls `(document.getElementById(id) as HTMLDialogElement).showModal()`, so the **id-based API is preserved**. You get focus trapping, Esc to close, and a backdrop (`backdrop:bg-black/50`) without JS libraries. The close (×) button calls `close()`. The dialog is a full-viewport scroll container around a centered panel, mirroring Bootstrap's `.modal` > `.modal-dialog`. See [Modals](#modals-on-small-screens). | Signatures unchanged. Footer buttons stop using `data-bs-dismiss` (see below). |
| `Alerts` (confirmation) | `RgModalBody` + `Bootstrap.Modal` in `AlertsViewModel` | Same `<dialog>`. `AlertsViewModel.confirmationModal` becomes `HTMLDialogElement?`. Listen to the dialog's `close` event instead of `hidden.bs.modal`. | Internal type change only |
| `Alerts` (success message) | `alert alert-success d-flex align-items-center fixed-top` | `fixed top-20 inset-x-0 mx-auto max-w-md flex items-center gap-2 rounded bg-success-100 text-success-800 p-3 shadow` | None |
| `NavBar` | `navbar navbar-expand-lg …` + `Bootstrap.Collapse` | Same behavior: a horizontal link row at ≥ `lg` (992px), a hamburger below it that toggles a vertical menu. Compose state replaces `Bootstrap.Collapse`. See [Navbar](#navbar-hamburger-collapse). | None |
| Rc tabs (`nav nav-pills` in `Rc.kt`) | Bootstrap pills | `flex gap-2` + `.rg-pill` / `.rg-pill-active` | Route-level edit |
| `RgTable` + `RgTr`/`RgTd`/`RgTdColor`/`RgTrColor` | `table table-hover table-sm table-striped[-columns] table-bordered caption-top table-${color}` | `.rg-table` component classes written as plain CSS in `@layer components`, reproducing Bootstrap's layering so hover, striping and cell colors combine correctly. Wrapper `overflow-x-auto`. **`TableColor` gets a literal class per entry** (`rg-table-light`, `rg-table-warning`, …) instead of `"table-${it.name}"`. See [Tables](#tables-hover-and-striping). | `TableColor` entries kept; internal property added |
| `RgGrid` / `RgDiv` / `Customizer` (`RgSpace`/`RgSide`/`RgSz`/`RgPosition`/`RgContainerType`) | generates Bootstrap utility names (`m-2`, `mb-3`, `my-4`, `container-fluid`, `fixed-top`, `min-vh-100`, `d-flex`, `flex-column`) | Tailwind uses almost the same names (`m-2`, `mb-3`, `ms-*`/`me-*`, `fixed top-0`, `min-h-screen`, `flex flex-col`), **but the scale differs** and the names are **built dynamically**. Rewrite `Customizer.set` to look up a literal table: `RgSz.s3` → `"…-4"`, `s4` → `"…-6"`, `s5` → `"…-12"` to keep Bootstrap's visual sizes. That's 2 spaces × 7 sides × 7 sizes = 98 literals; generate them once, or `@source inline("{m,p}{,t,b,s,e,x,y}-{0,1,2,4,6,12,auto}")`. `RgContainerType.container_fluid` → `w-full px-4`; `container` → `container mx-auto px-4`; `RgPosition` entries get literal Tailwind classes. | None |
| `RgSortable` | `list-group` / `list-group-item` | `divide-y rounded border` + `px-4 py-2 cursor-move`. SortableJS is unaffected. | None |
| `AppStyle` | Compose `StyleSheet` margins | Unchanged. It works alongside Tailwind. Optionally migrate callers to Tailwind spacing later. | None |

MapLibre (`utils/MapLibre.kt`, which requires its own CSS) and charts are out of scope. Check that Preflight's
`img { display:block; max-width:100% }` and `canvas` rules don't affect the map or chart canvases. If they do,
scope an override.

## Responsive behavior (mobile & desktop)

The site has to work on phones as well as on desktop. That includes the CYCT site's iframe embed, where media
queries measure the **iframe's** width, just as they do with Bootstrap today. Bootstrap gave us a lot of this for
free; these are the places where a naive Tailwind port would regress.

### Navbar (hamburger collapse)

Today, `navbar-expand-lg` shows the links in a row at ≥ 992px. Below that it shows the burgee and a hamburger
button, and the button toggles a vertical link list (`Bootstrap.Collapse`). The port keeps that exactly:

- **Breakpoint:** `lg` must be 992px (see the [Theming](#theming--base-styles) breakpoint override). Otherwise the
  hamburger would also appear between 992 and 1023px.
- **Structure:** a `Nav` (`sticky top-0 border-b bg-…`) containing a `flex flex-wrap items-center justify-between`
  row: brand `Img` (60px tall), a hamburger `Button`, and the link `Ul`.
- **Hamburger:** `lg:hidden`, with `aria-expanded`/`aria-controls`. Bootstrap's `navbar-toggler-icon` is a CSS
  background SVG that disappears with Bootstrap, so draw the three-bar icon as an inline SVG (like `Alerts` does).
- **Link list:** `val open by remember { mutableStateOf(false) }` drives the class choice, written as two full
  literals so the scanner sees both:
  `if (open) "flex basis-full flex-col lg:basis-auto lg:flex-row" else "hidden lg:flex lg:flex-row"`.
  At ≥ `lg` the list always shows in a row, whatever the state. That matches Bootstrap: if the menu is opened on a
  phone and the window then widens, the links appear inline. If it narrows again, the menu is still open.
- **After choosing a route:** today the menu **stays open**, because `NavBar` only calls `pushRoute` and Bootstrap
  collapse doesn't auto-close. Parity keeps that. Setting `open = false` in the click handler is a one-line
  improvement worth considering.
- **Animation:** Bootstrap animates the menu height for 0.35s. That's optional. If wanted, use the
  `grid grid-rows-[0fr]` → `grid-rows-[1fr]` transition trick, or simply show and hide.
- **Layering:** keep Bootstrap's z-index order: dropdown menus (1000) < sticky navbar (1020) < fixed success
  banner (1030). Dialogs sit in the top layer above all of them. Put these values in `@theme` (`--z-dropdown`, …)
  rather than mixing `z-10`/`z-50` ad hoc.
- **`nonav` embed mode** is unchanged.

### Tables (hover and striping)

`RgTable` is used in 10 places, and **both** striping modes are in use:

| Call sites | Settings | Result today |
|---|---|---|
| `Classes`, `People`, `RaceResults` (×2), `Rc` (×2), `Races`, `Series` | defaults: `TableColor.secondary`, row striping | grey table, alternate rows striped, hovered row highlighted |
| `SeriesStandings`, `RaceResultsView` | `stripeColumn = true`, `TableColor.light` | light table, alternate **columns** striped, hovered row highlighted, plus `RgTdColor(TableColor.warning)` cells |

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
  so it never fires on phones. Bootstrap's `:hover` highlights the tapped row on touch screens today. Plain CSS
  keeps that.
- **Keep the font size.** `table-sm` only tightens padding, so don't add `text-sm`. The earlier draft of this plan
  had it.
- **Narrow screens:** keep the `overflow-x-auto` wrapper (`table-responsive`). Wide tables like the 15-column race
  results scroll sideways on phones, as they do today. Don't add `whitespace-nowrap` or sticky columns; that would
  be a behavior change.
- **Variable colors:** derive the `--rg-tbl-*` values for each `TableColor` from the theme scales
  (e.g. `secondary` → `slate-200` background, `warning` → `warning-100`), keeping Bootstrap's contrast between
  background, stripe and hover.

### Modals on small screens

Bootstrap's modal is a full-viewport, scrollable `.modal` containing a centered `.modal-dialog`. The dialog spans
the width minus 0.5rem margins on phones and is 500px wide, centered, from `sm` up. When the content is tall, the
**whole modal scrolls**, and because `.modal-content` doesn't clip, absolutely positioned dropdown menus inside
it aren't cut off. `AddEditBoatModal` relies on both: it holds `RgSkipperDropdown` and `RatingSelections` with
ORC certificate details, which get tall on a phone.

A native `<dialog>` with default styles is a centered box limited to the viewport, with internal scrolling. That
clips the dropdown menus. Setting `overflow: visible` instead, as an earlier draft of this plan suggested, would
make tall modals impossible to scroll on a phone. **Mirror Bootstrap's structure instead:**

- `<dialog class="rg-modal">`: reset the UA styles to `fixed inset-0 m-0 p-0 w-full h-full max-w-none max-h-none
  overflow-y-auto bg-transparent border-0`, with `backdrop:bg-black/50`. The dialog element is the scroll container.
- Inside it, `.rg-modal-panel`: `relative mx-2 my-2 sm:mx-auto sm:my-7 sm:max-w-[500px] rounded-lg bg-white shadow`,
  with header, body and footer. The panel has **no overflow clipping**, so dropdown menus extend past it and the
  dialog scrolls to reveal them.
- **Backdrop click closes the modal**, as in Bootstrap. The native dialog doesn't do this by default
  (`closedby="any"` isn't supported in every browser we target), so close it in a `click` handler when
  `event.target` is the `<dialog>` itself, i.e. outside the panel.
- **Scroll lock:** add `html:has(dialog[open]) { overflow: hidden }` (Bootstrap adds `.modal-open` to `body`). This
  stops the page behind from scrolling, especially on iOS.
- **Confirmation modal:** Esc and backdrop click dismiss it without calling the handler, the same as Bootstrap
  today. Clear `AlertsViewModel`'s `confirmation` state on the dialog's `close` event so it doesn't go stale.
- Footer rows (`RgModalFooterRow`) should `flex-wrap` so two long buttons stack on very narrow screens.

### Other mobile checks

- **Touch-only hover:** Tailwind `hover:` styles on buttons and links no longer "stick" after a tap on phones.
  That's an improvement over Bootstrap; only the table hover needs to stay plain `:hover` (above).
- **iOS input zoom:** keep inputs at 16px or larger below `sm` (see [Theming](#theming--base-styles)).
- **Success banner:** `fixed inset-x-2 sm:inset-x-0 sm:mx-auto sm:max-w-md` so it has margins on phones and is
  centered on desktop. Its z-index sits above the navbar.
- **Viewport meta tag** is already present in `common/src/commonMain/resources/static/index.html` (`width=device-width,
  initial-scale=1`). Tailwind's responsive variants depend on it, just as Bootstrap's do.
- **Test widths:** 360px (small Android), 390px (iPhone), 768px (`md`), **991/992px** (the navbar switch point) and
  1280px+. Test both standalone and inside the CYCT iframe, in Chrome, Firefox and iOS Safari.

## Necessary API changes

1. **`RgButton(closesModal: Boolean = false)`, additive with a default.** It replaces `attr("data-bs-dismiss", "modal")`.
   `RgModalBody` provides a `CompositionLocal` close function for its `<dialog>`, and `RgButton` calls it after `click()`.
   Existing callers are unaffected.
2. **Migrate the 18 raw footer `Button`s** (`classes(*RgButtonStyle.X.classes)` + `data-bs-dismiss`) to
   `RgButton(..., closesModal = true)`. **`RgButtonStyle.classes` can stay**, because Tailwind
   is still class-based and the property simply holds `rg-*` names. These call sites still have to change, though,
   because `data-bs-dismiss` no longer does anything.
3. **Optional `RgModalFooterRow { }` helper** for the repeated `flex-fill d-flex justify-content-between` footer
   wrapper (5 sites), so routes stop hard-coding layout class names.
4. **`AlertsViewModel.confirmationModal`**: `Bootstrap.Modal?` → `HTMLDialogElement?` (internal).
5. **Delete `utils/Bootstrap.kt`.**

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
| `form-control` (raw `Input`s in `Admin.kt`) | switch to `RgInput`, or `.rg-input` |

## Risks / things to verify

- **Dynamic class names**: grep for string templates inside `classes(...)` and the `Customizer` before calling
  any component done. A missing class fails silently, with no build error. A dev-time check helps: a
  `jsBrowserTest` that renders each `RgSz`/`TableColor`/`RgButtonStyle` and asserts `getComputedStyle` isn't the
  default.
- **Preflight fallout**: unstyled headings, lists, and buttons across every route.
- **Webpack/PostCSS wiring** inside the Kotlin Gradle plugin's generated config, in dev (`--continuous`), production
  (`jsBrowserProductionWebpack`, which also feeds the server's `static/`), and Karma tests.
- **Dropdowns inside modals**: `AddEditBoatModal` hosts `RgSkipperDropdown` and `RatingSelections`. The
  full-viewport dialog with an unclipped panel ([Modals](#modals-on-small-screens)) handles this. Verify it on a
  phone with a boat that has several ORC certificates.
- **Bootstrap/Tailwind class-name collisions** while both are loaded. Cascade layers handle this (see
  [Coexistence](#coexistence-via-cascade-layers-spike-finding)): colliding names take Tailwind's meaning. For
  example, Tailwind's `collapse` is `visibility: collapse`, which is why `NavBar` had to be migrated before it
  became safe.
- **iframe embed height**: `Main.js.kt` posts `regatta_scroll_height` to the parent. A `<dialog>` in the top layer
  doesn't add height, the same as Bootstrap's fixed modal today, but confirm on the CYCT site.
- **Visual parity**: Bootstrap's look comes for free. With Tailwind every component's look is designed by us,
  which is more up-front design work than adopting a component library.

## Size & maintenance comparison

- Bootstrap today: ~230 KB CSS + ~80 KB JS (minified sizes of `bootstrap.min.css` / `bootstrap.bundle.min.js`),
  fully shipped.
- Tailwind: only the used classes are emitted. Expect roughly 15–30 KB of CSS, no JS, and Popper is removed.
- Tailwind is actively maintained. The cost moves to us: component styles live in our `app.css`, and behaviors
  live in our Kotlin. Most of that behavior already does (`RgDropdown*` is hand-rolled).

## Phased plan

Each phase leaves the app shippable.

**Phase 0: [x] Spike.** Wire PostCSS/Tailwind into webpack (dev, prod, test). Add `app.css` with `@theme` and a
base layer, and migrate `RgButton` + `Modal` (native `<dialog>`) on the Boats screen, including `AddEditBoatModal`
with its skipper dropdown and `RatingSelections`. Also prototype the `NavBar` collapse and `.rg-table` layering,
because they are the riskiest responsive pieces. Confirm `--continuous` hot reload, the iframe height, and the
[test widths](#other-mobile-checks) in Chrome, Firefox and iOS Safari.

### Phase 0 results (2026-10-13)

**Built on the `tailwind` branch** (beyond the plan's spike scope where it was cheaper to finish than to stub):

- Build wiring (`web/build.gradle.kts`, `web/webpack.config.d/tailwind.js`) and `web/src/jsMain/resources/app.css`,
  which has the burgee theme, Bootstrap breakpoints, z-index scale, base fixes, and `.rg-btn*`, `.rg-modal*`,
  `.rg-navbar*`/`.rg-nav-link` and `.rg-table*` components.
- `RgButton`: `rg-*` classes and `closesModal`. `RgButtonStyle.classes` is kept.
- `Modal.kt`: `<dialog>` with the full-viewport/panel structure, backdrop click, `LocalModalClose`, and a
  **temporary `data-bs-dismiss` shim** so footers on routes that haven't been migrated still close their modal
  (remove in Phase 3).
- `AlertsViewModel` on `HTMLDialogElement`, clearing the stale confirmation on `close`. `utils/Bootstrap.kt` is
  deleted, and `bootstrap.bundle.js` is no longer loaded.
- `NavBar`: Compose-state collapse at 992px, inline-SVG hamburger, `aria-current`.
- `RgTable`/`TableColor`: layered `.rg-table` CSS with literal color classes.
- `AddEditBoatModal` footer moved to `RgButton(closesModal = true)`.

**Verified** against the production bundle served by the app server, with real race data, in Chrome. Phone and
tablet widths were tested in same-origin iframes, which is also how the CYCT embed sees the app:

- **Navbar:** at 390px and 991px the hamburger shows and opens a vertical menu. At exactly 992px the links are
  inline and the hamburger is hidden.
- **Tables:** the default style (grey, striped rows) and the results style (light, striped columns, amber
  `warning` cells) both render. Hovering highlights the whole row across striped and unstriped columns. At 390px
  the 15-column results table scrolls sideways inside its wrapper.
- **Modal at 390px:**
  - The panel fills the width with margins, and the dialog scrolls while the page behind is locked.
  - An absolutely positioned menu near the panel's bottom edge extends past the panel without being clipped.
  - A press that starts in the panel and ends on the backdrop does not close the modal; a real backdrop click
    does, and so does a real Esc keypress.
  - Focus goes to the close button on open.
- **Hot reload:** a new class used in a `.kt` file appeared in the dev bundle's CSS about 4 seconds later.

**Findings that change the plan:**

1. **Coexistence works with cascade layers.** See [below](#coexistence-via-cascade-layers-spike-finding). This
   replaces the "don't run side by side" advice and the `prefix(tw)` fallback.
2. **Preflight's `img { height: auto }` overrides HTML `height=""` attributes.** The burgee grew from 60px to
   about 104px. Size images with classes (`h-[60px] w-auto`). The base layer now restores `img, svg` to inline,
   which fixes the stacked footer badges and inline `RgSpinner`.
3. **Tailwind gets its own webpack rule.** Pushing `postcss-loader` into the shared CSS rule would also run Tailwind
   over `maplibre-gl.css`; see [Build integration](#build-integration).
4. **Existing problems found along the way, unrelated to Tailwind:**
   - `RouteMatcherTest` doesn't compile (it uses `Route.BoatEdit`), which blocks `jsBrowserTest`.
   - `jsBrowserDevelopmentWebpack` fails in `dev_server_config.js`.
   - `RgGrid` emits the enum name `container_fluid` with an underscore. That isn't a Bootstrap class, so it's
     a no-op today; keep that in mind for Phase 4 parity.
   - The first load of results pages is slow (several seconds of spinner).

**Not verified, carried forward:**

- **Firefox and iOS Safari:** only Chrome was available. Karma's Firefox run is blocked by the test compile error
  above. Check these in Phase 5.
- **The real `AddEditBoatModal`:** the boat API needs an admin login, so the modal was tested with the confirmation
  dialog plus a synthetic tall body and absolutely positioned menu. Re-test with `RgSkipperDropdown` and
  `RatingSelections` when logged in (Phase 3).
- **iframe height reporting on the CYCT site** (Phase 5).

### Coexistence via cascade layers (spike finding)

`app.css` declares `@layer theme, base, bootstrap, components, utilities;` and imports Bootstrap's CSS with
`layer(bootstrap)`. Layer order beats specificity, so:

- **Reboot (bootstrap) beats Preflight (base).** Screens that haven't been migrated look as they do today.
- **`.rg-*` components and Tailwind utilities beat Bootstrap.** Migrated components aren't disturbed by Reboot's
  element rules.
- **Name collisions resolve to Tailwind's meaning**, because utilities are layered above Bootstrap. In practice that
  only matters for Bootstrap names still in the source that are also Tailwind utilities. `collapse` is already gone
  with the `NavBar` migration. The remaining collisions, such as `container`, `border` and `mb-3` in `RgForm`, are
  small visual differences, and Phase 4/5 replaces each of them anyway.

So each phase can be merged on its own, as the "each phase leaves the app shippable" goal intended, and Bootstrap's
import is deleted in Phase 5.

**Phase 1: [x] Foundation.** Mostly done by the spike: build wiring, `@theme` (colors, breakpoints, z-index),
the forms plugin, and coexistence layering. Remaining work:

- the full base layer: responsive `h1`–`h4` sizes with `clamp()`, paragraph and list spacing, link color. It can
  only be checked once Reboot stops winning, so check it against a page with the Bootstrap import commented out;
- contrast checks on the burgee color scales (WCAG AA);
- fixing `RouteMatcherTest` so `jsBrowserTest` runs in Chrome Headless and Firefox.

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
- **Tests:**
  - `RouteMatcherTest` now tests path arguments with `Route.RaceEdit` (`/race/:id`), since `BoatEdit` was removed.
  - `jsBrowserTest` also failed because `dev_server_config.js` assumed `config.devServer` exists, and the rename
    hooks in `web/build.gradle.kts` use paths relative to the Gradle daemon's working directory, so they never
    applied. `dev_server_config.js` is now wrapped in `if (config.devServer)`. That also fixes
    `jsBrowserDevelopmentWebpack`, and makes the rename hooks redundant but harmless.
  - In Chrome Headless, `RouteMatcherTest` (2/2) and `AuthTest` (2/2) pass.
  - **Firefox is not installed on this machine**, so the Firefox launcher fails ("No binary for Firefox"). Chrome
    was run with `useFirefox()` temporarily disabled, then restored. Running `jsBrowserTest` as configured needs
    Firefox installed (or `FIREFOX_BIN` set).

**Phase 2: [x] Leaf components (API unchanged).** `RgButton` is done (spike). `RgInput`,
`RgNumberInput`, `RgInputWithButton`, `RgTime`, `RgCheck`, `RgSwitch`, `RgDropdown*` (including outside-click/Esc
close), `RgYearSelect`, `RgSpinner`.
`RatingSelections`, `FinishCodeDrop`, and `TimeRow` should need no edits.

### Phase 2 results (2026-10-13)

- **New `app.css` components:**
  - `.rg-input`/`.rg-select` (Bootstrap `.form-control`/`.form-select` metrics, always 16px);
  - `.rg-input-group`, `.rg-input-addon` and `.rg-input-group-sm`, which uses 16px text below `sm` so iOS doesn't
    zoom, and 14px from 576px up;
  - `.rg-check`, `.rg-checkbox`, and `.rg-switch` (a CSS-only knob);
  - `.rg-dropdown*` (toggle caret, menu at `--z-dropdown` capped to `100vw - 2rem`, items, divider).
- **Kotlin, with signatures unchanged:**
  - `RgInput`, `RgNumberInput`, `RgInputWithButton` (its button uses `RgButtonStyle.PrimaryOutline`; `mb-3` became
    `mb-4` to keep Bootstrap's 1rem), `RgTime`, `RgCheck`, `RgSwitch`;
  - `RgDropdown`/`RgIdDropdown`/`RgDropdownNone`, whose filter input's `mx-3` became `mx-4`;
  - `RgYearSelect`.
  - `RatingSelections`, `FinishCodeDrop` and `TimeRow` needed no edits. `RgSpinner` was already Bootstrap-free and
    is unchanged.
- **Dropdown dismiss** (decision 5):
  - While a menu is open, capture-phase `pointerdown`/`keydown` listeners on `document` close it on a press
    outside its root, or on Escape.
  - Escape is consumed (`preventDefault` + `stopPropagation`), so a dropdown inside a modal closes only itself.
  - The listeners are added and removed with the open state, via `DisposableEffect`.
- **Tests:** added `web/src/jsTest/kotlin/LeafComponentsTest.kt`, 10 tests using
  `org.jetbrains.compose.html:html-test-utils`, a new test dependency. They cover:
  - dropdown open, select and close; outside press vs inside press; Escape consumed;
  - a dropdown with one item is disabled;
  - `RgDropdownNone` filter, None, and filter reset;
  - `RgButton(closesModal)`;
  - `RgNumberInput` number validation; `RgInputWithButton`; `RgCheck`/`RgSwitch` events;
  - the `RgTime` markup has no Bootstrap classes.

  All 14 web tests pass in Chrome Headless (Firefox isn't installed here, see Phase 1). A mutation check (dismiss
  logic disabled) failed exactly the two dismiss tests.
- **Visual check** (Chrome, dev server, Bootstrap still layered underneath):
  - the real `RgYearSelect` on `/races/results`;
  - for the admin-only components, their exact markup and classes rendered with the real stylesheet: inputs, the
    input group with button, compact time fields, checkboxes, switches on and off, both dropdowns open, and a long
    skipper name;
  - compact time fields compute to 16px at 392px wide and 14px at 854px.
- **Still to see in the real app:** the logged-in forms (People, Boats, Classes, Races, RC). Check them in the
  Phase 3 logged-in pass.

**Phase 3: [ ] Dialogs.** `RgModalButton`/`RgModalBody`/`RgModal` on `<dialog>` and `AlertsViewModel` are done
(spike). Migrate the remaining 16 footer buttons (`AddEditBoatModal`'s two are done) to `RgButton(closesModal = true)`,
then delete the `data-bs-dismiss` shim in `RgModalBody` and the `data-bs-*` attributes. Re-test `AddEditBoatModal`
logged in at phone width.

**Phase 4: [ ] Layout, chrome & data.** `NavBar` and `RgTable` are done (spike), except the `nonav` link, which
still uses `nav-link`. Remaining: `Customizer` literal tables + spacing-scale mapping, `RgGrid` (note the
`container_fluid` no-op). Also `Router`'s `#route_content` gets Bootstrap's `d-flex`/`flex-column`/`flex-grow-1`
through `RgDiv`'s `addCustom`. The Phase 1 check showed the content stops being a flex column without Bootstrap,
so map these to `flex`/`flex-col`/`grow`. Then: `Rc.kt` pills, the `Alerts` banner, `RgSortable`.

**Phase 5: [ ] Remove Bootstrap.** Route-level class sweep (`grep -rn 'classes(' web/src/jsMain`) (any Bootstrap name left behind loses its styling once the import is gone),
remove the Bootstrap `@import` and `bootstrap` layer from `app.css`, remove the `bootstrap`/`@popperjs/core` npm deps
(`utils/Bootstrap.kt` and the `require`s are already gone), delete unused
`RgRow`/`RgCol`/`RgButtonGroup`. Do a visual pass of every route at phone and desktop widths and inside the
CYCT iframe embed, in Chrome, Firefox and iOS Safari.

## Decisions

Resolved 2026-10-13; these were the plan's open questions.

1. **Styling: component classes.** Component looks are defined once in `app.css` as `@layer components` classes
   built with `@apply` (`.rg-btn*`, `.rg-input`, `.rg-table`, `.rg-modal*`, `.rg-dropdown*`, …). Tailwind
   utilities appear in Kotlin only for one-off layout, and always as literal strings. See
   [Styling strategy](#styling-strategy-component-classes-and-utilities).
2. **Spacing: keep Bootstrap's visual sizes.** `Customizer`/`RgSz` map onto the Tailwind scale (`s0`→`0`, `s1`→`1`,
   `s2`→`2`, `s3`→`4`, `s4`→`6`, `s5`→`12`, `auto`→`auto`) through a literal lookup table, so the layout doesn't
   shift and `RgDiv` callers are unchanged.
3. **Build: webpack PostCSS.** Use `postcss-loader` via `web/webpack.config.d/tailwind.js`, as described in
   [Build integration](#build-integration). The Tailwind CLI Gradle task is only the contingency if Phase 0 shows
   the webpack wiring is brittle.
4. **Palette: derived from the CYCT burgee** (`common/src/commonMain/resources/static/cyct_burgee.svg`). See
   [Theming](#theming--base-styles) for the token mapping.
5. **Dropdown UX: outside-click and Esc to close are in scope.** This is added inside `RgDropdown`/`RgIdDropdown`/
   `RgDropdownNone` with no API change. Full keyboard navigation (arrow keys, ARIA listbox roles) is out of
   scope and can be a follow-up.
