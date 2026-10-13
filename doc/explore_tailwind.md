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
   Then run `./gradlew kotlinUpgradeYarnLock` to refresh `kotlin-js-store/yarn.lock`.
2. **`web/webpack.config.d/tailwind.js`.** It inserts `postcss-loader` after `css-loader` in the existing CSS rule.
   Files in `webpack.config.d` are concatenated into `build/js/packages/regatta-web/webpack.config.js`, so `__dirname`
   resolves the same way it does in `dev_server_config.js`:
   ```js
   const path = require('path');
   const cssRule = config.module.rules.find(r => String(r.test).includes('css'));
   cssRule.use.push({
     loader: 'postcss-loader',
     options: { postcssOptions: { plugins: [
       require('@tailwindcss/postcss')({ base: path.resolve(__dirname, '../../../../web/src/jsMain/kotlin') }),
     ] } },
   });
   ```
   The exact rule lookup must be checked against the generated config. Unlike `dev_server_config.js`, this file
   must **not** be excluded from the production webpack build or `jsBrowserTest`.
3. **CSS entry point**, `web/src/jsMain/resources/app.css`, required from `main()` in place of the Bootstrap lines:
   ```css
   @import "tailwindcss";
   @source "../kotlin";          /* or rely on the `base` option above */
   @plugin "@tailwindcss/forms";
   @theme { /* CYCT colors, fonts, see Theming */ }
   @layer base { /* Reboot replacement */ }
   @layer components { /* .rg-btn, .rg-table, … */ }
   ```
4. **Verify** that `jsBrowserDevelopmentRun --continuous` rebuilds the CSS when a `.kt` file changes. The Tailwind
   PostCSS plugin registers its scanned files as webpack dependencies, but the Kotlin compile output may be what
   triggers the rebuild. Also verify that `jsBrowserTest` (Karma) still bundles: tests don't import `Main.js.kt`,
   but they share the webpack config.
5. **Fallback** if the webpack wiring proves brittle: run the standalone Tailwind CLI from a Gradle `Exec` task
   (`tailwindcss -i app.css -o build/…/app.css`, plus `--watch` for dev). Then wire its output into
   `jsProcessResources` the same way the root build wires webpack output into the server's `static/`.

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
- **Preflight replaces Reboot.** Preflight is more aggressive: `h1`–`h6` lose their sizes, `ul` loses bullets,
  `button` loses its styling, `img` becomes `display:block`, and tables lose spacing. Screens rely on raw
  `H3`/`H5`/`H6`, `P`, `B`, `Br`, `Ul`, and `Caption`, so add an `@layer base` with heading sizes and margins,
  paragraph spacing, and link color. Otherwise every route shifts visually.
- **Forms:** `@tailwindcss/forms` gives native inputs, selects, and checkboxes a sane baseline that is easy to
  restyle. It covers `RgInput`, `RgTime`, `RgCheck`, `RgYearSelect`, and the raw `Input`s in `Admin.kt`.
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
| `RgDropdown`, `RgIdDropdown`, `RgDropdownNone` (+ `RgSeriesDropdown`, `RgClassDropdown`, `RgSkipperDropdown`) | Already **hand-rolled** in Kotlin (`toggle` state) with `dropdown-toggle`/`dropdown-menu show`/`dropdown-item` classes | Swap classes for `.rg-dropdown-toggle`, `.rg-dropdown-menu` (`absolute z-50 mt-1 rounded shadow-lg bg-white max-h-80 overflow-auto`), `.rg-dropdown-item`. Replace the inline `z-index`/`position` style with classes. The filter `RgInput`'s `customClasses = listOf("w-auto","mx-3","my-2")` becomes Tailwind equivalents (`w-auto mx-4 my-2`). **Add close on outside click and Esc** (decision 5): a document `mousedown`/`keydown` listener registered in a `DisposableEffect` while the menu is open, ignoring clicks inside the dropdown's root element. | None |
| `FinishCodeDrop` | wraps `RgDropdown` | free | None |
| `RgYearSelect` | `form-select` | `.rg-select` (forms plugin) | None |
| `RgModalButton` / `RgModalBody` / `RgModal` | `data-bs-toggle`/`data-bs-target` + `.modal.fade` markup, Bootstrap JS | **Native `<dialog>`**. `RgModalButton(id)` calls `(document.getElementById(id) as HTMLDialogElement).showModal()`, so the **id-based API is preserved**. You get focus trapping, Esc to close, and a backdrop (`backdrop:bg-black/50`) without JS libraries. The close (×) button calls `close()`. Style with `.rg-modal`, `.rg-modal-header/body/footer`. | Signatures unchanged. Footer buttons stop using `data-bs-dismiss` (see below). |
| `Alerts` (confirmation) | `RgModalBody` + `Bootstrap.Modal` in `AlertsViewModel` | Same `<dialog>`. `AlertsViewModel.confirmationModal` becomes `HTMLDialogElement?`. Listen to the dialog's `close` event instead of `hidden.bs.modal`. | Internal type change only |
| `Alerts` (success message) | `alert alert-success d-flex align-items-center fixed-top` | `fixed top-20 inset-x-0 mx-auto max-w-md flex items-center gap-2 rounded bg-success-100 text-success-800 p-3 shadow` | None |
| `NavBar` | `navbar navbar-expand-lg …` + `Bootstrap.Collapse` | `sticky top-0 z-40 border-b bg-gray-50` bar. Links `hidden lg:flex`. Hamburger `lg:hidden` toggles a `remember { mutableStateOf(false) }` **Compose state** instead of `Bootstrap.Collapse`. Active link uses `aria-current` + `aria-[current=page]:` styles. | None |
| Rc tabs (`nav nav-pills` in `Rc.kt`) | Bootstrap pills | `flex gap-2` + `.rg-pill` / `.rg-pill-active` | Route-level edit |
| `RgTable` + `RgTr`/`RgTd`/`RgTdColor`/`RgTrColor` | `table table-hover table-sm table-striped[-columns] table-bordered caption-top table-${color}` | `.rg-table` component class (`w-full border-collapse text-sm`, `[&_td]:border`, `hover:` rows, `even:` row striping, `[&_td:nth-child(even)]` column striping, `caption-top`). Wrapper `overflow-x-auto`. **`TableColor` gets a literal class per entry** (`light("bg-gray-50")`, `warning("bg-warning-100")`, …) instead of `"table-${it.name}"`. | `TableColor` entries kept; internal property added |
| `RgGrid` / `RgDiv` / `Customizer` (`RgSpace`/`RgSide`/`RgSz`/`RgPosition`/`RgContainerType`) | generates Bootstrap utility names (`m-2`, `mb-3`, `my-4`, `container-fluid`, `fixed-top`, `min-vh-100`, `d-flex`, `flex-column`) | Tailwind uses almost the same names (`m-2`, `mb-3`, `ms-*`/`me-*`, `fixed top-0`, `min-h-screen`, `flex flex-col`), **but the scale differs** and the names are **built dynamically**. Rewrite `Customizer.set` to look up a literal table: `RgSz.s3` → `"…-4"`, `s4` → `"…-6"`, `s5` → `"…-12"` to keep Bootstrap's visual sizes. That's 2 spaces × 7 sides × 7 sizes = 98 literals; generate them once, or `@source inline("{m,p}{,t,b,s,e,x,y}-{0,1,2,4,6,12,auto}")`. `RgContainerType.container_fluid` → `w-full px-4`; `container` → `container mx-auto px-4`; `RgPosition` entries get literal Tailwind classes. | None |
| `RgSortable` | `list-group` / `list-group-item` | `divide-y rounded border` + `px-4 py-2 cursor-move`. SortableJS is unaffected. | None |
| `AppStyle` | Compose `StyleSheet` margins | Unchanged. It works alongside Tailwind. Optionally migrate callers to Tailwind spacing later. | None |

MapLibre (`utils/MapLibre.kt`, which requires its own CSS) and charts are out of scope. Check that Preflight's
`img { display:block; max-width:100% }` and `canvas` rules don't affect the map or chart canvases. If they do,
scope an override.

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
- **Dropdowns inside modals**: `AddEditBoatModal` hosts `RgSkipperDropdown` and `RatingSelections`. A native
  `<dialog>` lives in the top layer and doesn't clip `absolute` children unless the dialog has `overflow` set,
  so keep `overflow-visible` on the dialog, or verify that scrolling long modals still works.
- **iframe embed height**: `Main.js.kt` posts `regatta_scroll_height` to the parent. A `<dialog>` in the top layer
  doesn't add height, the same as Bootstrap's fixed modal today, but confirm on the CYCT site.
- **Visual parity**: Bootstrap's look comes for free. With Tailwind every component's look is designed by us,
  which is more up-front design work than adopting a component library.

## Size & maintenance comparison

- Bootstrap today: ~230 KB CSS + ~80 KB JS (unminified), fully shipped.
- Tailwind: only the used classes are emitted. Expect roughly 15–30 KB of CSS, no JS, and Popper is removed.
- Tailwind is actively maintained. The cost moves to us: component styles live in our `app.css`, and behaviors
  live in our Kotlin. Most of that behavior already does (`RgDropdown*` is hand-rolled).

## Phased plan

Each phase leaves the app shippable.

**Phase 0: Spike.** Wire PostCSS/Tailwind into webpack (dev, prod, test). Add `app.css` with `@theme` and a
base layer, and migrate `RgButton` + `Modal` (native `<dialog>`) on the Boats screen, including `AddEditBoatModal`
with its skipper dropdown and `RatingSelections`. Confirm `--continuous` hot reload, the iframe height, and Firefox.

**Phase 1: Foundation + coexistence.** Land the build wiring, `@theme`, the base layer, and the forms plugin. During
coexistence Preflight and Reboot both apply, so either load Tailwind **without Preflight**
(`@import "tailwindcss/theme"; @import "tailwindcss/utilities";`) until Bootstrap is gone, or move straight
through Phases 2–4 on one branch.

**Phase 2: Leaf components (API unchanged).** `RgButton` (`rg-*` classes, `closesModal`), `RgInput`,
`RgNumberInput`, `RgInputWithButton`, `RgTime`, `RgCheck`, `RgSwitch`, `RgDropdown*` (including outside-click/Esc
close), `RgYearSelect`, `RgSpinner`.
`RatingSelections`, `FinishCodeDrop`, and `TimeRow` should need no edits.

**Phase 3: Dialogs.** `RgModalButton`/`RgModalBody`/`RgModal` on `<dialog>`. Migrate the 18 footer buttons and
move `AlertsViewModel` to `HTMLDialogElement`.

**Phase 4: Layout, chrome & data.** `Customizer` literal tables + spacing-scale mapping, `RgGrid`, `NavBar`
(Compose-state collapse), `Rc.kt` pills, the `Alerts` banner, `RgTable` + `TableColor` literals, `RgSortable`.

**Phase 5: Remove Bootstrap.** Route-level class sweep (`grep -rn 'classes(' web/src/jsMain`), enable Preflight,
delete `utils/Bootstrap.kt`, remove the `bootstrap`/`@popperjs/core` deps and `require` lines, delete unused
`RgRow`/`RgCol`/`RgButtonGroup`. Do a visual pass of every route at phone and desktop widths and inside the
CYCT iframe embed.

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
