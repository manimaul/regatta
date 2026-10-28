# Rust doc comment standards

How we write `///` and `//!` comments in this workspace. It distills
[How to write documentation](https://doc.rust-lang.org/rustdoc/how-to-write-documentation.html) from the rustdoc book
and adapts it to a project that is not a published library: our readers are the maintainers and the AI agents that
work on this code, and both usually arrive at an item cold, from a search hit or a stack trace, without having read
the code around it.

The aim is that a doc comment alone is enough to use an item correctly and to know why it behaves the way it does.

## Core principles

1. **Say what it is and why it exists, not how it works.** The code shows the how. The doc gives the contract
   (inputs, result, side effects, invariants) and the reason behind any rule that is not obvious.
2. **Write for a reader with no surrounding context.** Assume they reached this item from `grep` or a jump to
   definition. Don't write "see above" or "as before", and don't rely on the reader having just read the previous
   function. Name the domain concept outright: "the PHRF corrected time", not "the time".
3. **Cite the source of every rule.** Scoring and validation rules come from the sailing instructions or a recorded
   decision. Name it (`GSI 14.1.4`, `D3`, `R4a`) and the document that holds it
   (`docs/explore_implemented/test_audit_and_plan.md`, `orc_series_standings.md`). The citation is what lets someone
   tell a deliberate rule from a bug.
4. **Document what is true now.** The Kotlin port is finished. Describe the Rust item on its own terms; don't add
   "`= Kotlin fooBar`" mappings, and remove them when you touch a comment that still has one. Use Rust vocabulary:
   `None`, not "null"; "returns `Err`", not "throws".
5. **Don't repeat the signature.** rustdoc already shows and links the types. Spend the words on what the types
   can't say: units, ordering, which `None` means what, who calls this and when.
6. **A stale doc is worse than none.** Change the doc in the same commit as the behavior. If you can't keep a
   statement true, delete it.

## Structure of an item doc

```
[one-line summary]

[details: contract, rationale, invariants]

[# Errors / # Panics / # Safety, when they apply]

[# Examples, when the call shape isn't obvious]
```

### The summary line

Everything before the first blank line becomes the summary in module indexes and search results, and it is often
the only part a reader (or an agent skimming a file) looks at.

- One sentence, ideally under 100 characters.
- Functions start with a verb in the third person: "Ranks the cards...", "Checks a race before it is saved."
- Types and modules start with a noun phrase saying what the thing is: "Which entries [`place`] ranks: ..."
- It must be true and useful on its own, without the paragraphs below it.

### Details

Include what applies, skip what doesn't:

- **Rationale and source.** The GSI section or decision ID, plus a one-line reason when the rule is surprising.
- **Units and formats.** Milliseconds or seconds, seconds per mile, local or UTC, which ORC wind band, a JSON
  format the golden files pin.
- **Ordering and identity.** Sort order and whether it is stable or total, whether indices point into another
  slice, whether input order is preserved.
- **Side effects.** Database writes, cookies set, cache invalidation, anything that mutates through a `&mut` or a
  callback.
- **Non-local invariants.** Constraints that break something far away when violated. The usual ones here: the
  golden JSON is byte for byte (field order, defaults, nulls); server and wasm must render the same markup or
  hydration panics; Tailwind class names must be literal strings.
- **Meaning of the edge cases.** What an empty input, a `None` field or a missing rating does.

### Sections

Use rustdoc's standard headings so readers and tools find them in the same place:

- `# Errors`: for every public function that returns `Result`, the conditions that produce each kind of error.
  One line per case is enough.
- `# Panics`: whenever a panic is reachable from inputs the caller controls. If a panic means "a bug elsewhere"
  (an invariant violated), say which invariant.
- `# Safety`: required on every `unsafe fn` and `unsafe` trait: what the caller must uphold.
- `# Examples`: optional here, unlike in a published library. Add one when the call shape isn't obvious from the
  signature: builders, the scoring fixture DSL, callbacks, anything with a non-obvious setup. Examples are doctests
  and run with `cargo test --workspace`, so they must compile and pass; use `no_run` for anything that needs a
  database or network, and avoid `ignore`, which rots silently.

## Module and crate docs (`//!`)

Every crate's `lib.rs` and every non-trivial module opens with a `//!` block that tells a newcomer where they are:

1. One sentence: what the module is for and where it sits in the workspace (which crate calls it, what it calls).
2. The main entry points, as intra-doc links.
3. Constraints that apply to the whole module: "pure, no I/O", "SSR only", "every query goes through `::text` for
   timestamps".
4. Pointers to the design documents in `docs/` that explain it in depth.

A module doc is a map, not a manual; a short paragraph and a list of links is usually right.

## What to document

- Every `pub` item in `regatta-common`, `regatta-scoring` and `regatta-service`: these are the crates other crates
  build on.
- In `regatta-server`, `regatta-app` and the rest: every module, plus any item whose behavior isn't obvious from
  its name and signature. A component with props needs a doc; a three-line route handler that calls the service
  usually does not.
- Private items when they carry a rule, an invariant or a workaround. Rationale is worth more on a private helper
  than a summary on an obvious getter.
- Struct fields and enum variants whose meaning or unit isn't evident from the name.

Skip docs that only restate the name (`/// The boat name.` on `boat_name`). No doc is better than a doc that adds
nothing.

## Doc comments versus `//` comments

- `///` and `//!` describe the contract: what a caller needs to know.
- `//` inside a body explains a non-obvious step: why this order, why this workaround, a link to the bug it avoids.
  Keep these sparse, matching the surrounding code.
- Don't narrate code (`// loop over the cards`), and don't leave history (`// changed from X`); git keeps history.

## Markdown and links

- Link items with intra-doc links: [`place`], [`RaceReportCard`], [`crate::fixture`]. Rustdoc checks them and the
  link survives a reader jumping around in an editor. Use plain backticks only for things rustdoc can't resolve.
- Code, identifiers, file paths, env variables and literal values go in backticks: `REGATTA_FIXED_NOW`,
  `crates/scoring/src/fixture.rs`.
- Refer to files by repository-relative path so a reader can open them directly.
- Lists and tables are fine when they make a contract easier to scan (a mapping of finish codes, say).
  Avoid headings other than the standard sections.
- Wrap at 120 columns, the same as the code.

## Example

Before (a port artifact: Kotlin vocabulary, a mapping that no longer helps, and the second paragraph describes the
Kotlin version rather than this one):

```rust
/// Ranks the cards that `among` includes and hands each its place through `place_handler`.
/// Returns the ranked cards in place order, followed by the cards `among` left out, untouched
/// and in their input order. The ORC ranking uses the ORC corrected times.
///
/// The Kotlin extension works on shared card objects; here `cards` is the one card store and
/// `subset` the indices into it that stand for the Kotlin receiver list. The returned indices are
/// the Kotlin return list, and `place_handler` mutates the card in place as the Kotlin lambda did.
pub fn place(/* ... */) -> Vec<usize>
```

After (self-contained, with the source of the rule and the index contract stated in Rust terms):

```rust
/// Ranks the cards in `subset` that `among` includes and hands each its place.
///
/// `subset` holds indices into `cards`; the result holds the same indices, the ranked ones in place
/// order followed by the ones `among` left out, in their input order. `place_handler` gets each
/// ranked card with its place: 1-based for finishers, equal corrected times sharing one (D7), and the
/// finish code's score otherwise (GSI 14.1 / D3), plus any penalty capped at the DNF score (D4). The
/// ORC ranking orders by ORC corrected time, the others by PHRF corrected time (see [`compare`]).
/// R1 (`orc_series_standings.md`): each rating is placed among its own entries only.
pub fn place(/* ... */) -> Vec<usize>
```

And a validation function, before and after:

```rust
/// Checks a race before it is saved. Returns an error message for the client, or null when the
/// race is acceptable.
```

```rust
/// Checks a race before it is saved, returning the message to show the user, or `None` if it is valid.
///
/// R4a (`orc_series_standings.md`): the ORC wind band the scoring option uses must be the one the PHRF
/// B factor maps to, so both time corrections agree on the wind. Nothing is corrected here; the user
/// is told what to change.
```

## Checking

```shell
cargo doc --workspace --no-deps          # broken intra-doc links appear as warnings
cargo test --workspace --doc             # runs the examples
```

Treat a rustdoc warning like a clippy warning: fix it before committing.

`regatta-common`, `regatta-scoring` and `regatta-service` enable `clippy::missing_errors_doc` and
`clippy::missing_panics_doc` (`[lints.clippy]` in their `Cargo.toml`), so `cargo clippy ... -D warnings` fails on a
missing `# Errors` or `# Panics` section there.

## Checklist

- [ ] The summary line stands alone and fits on one line.
- [ ] The doc states the contract, not the implementation.
- [ ] Every scoring or validation rule names its GSI section or decision ID.
- [ ] Units, ordering and the meaning of `None` are explicit where they matter.
- [ ] `# Errors`, `# Panics` and `# Safety` are present where they apply.
- [ ] Items are linked with intra-doc links; `cargo doc` is warning-free.
- [ ] No Kotlin mappings, no "null", no history.
- [ ] The doc changed in the same commit as the behavior it describes.
