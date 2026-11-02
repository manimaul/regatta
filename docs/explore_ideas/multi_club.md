# Multi-club support

A high-level exploration of what has to change for Regatta to host more than one yacht club, each with its own
isolated series, races, classes, course marks and results, while boats and people stay independent records that
can be associated with one or more clubs. It also covers skippers as first-class users (sign up, register for a
race, manage their boats) and optional payment for race registration.

Nothing here is implemented. It lists the areas that change and the decisions each one forces; it is not a design.

## 1. Where we are today

Everything is single-tenant and built around CYCT:

- **Schema** (`migrations/0001_baseline.sql`, version 5): `series`, `race`, `raceclass`, `bracket`,
  `racebracketjunction`, `racetime`, `raceresults`, `image`, `person`, `boat`, `orc`, `auth`, `metadata`. No table
  names a club. `person.club_member` is a single boolean that means "member of CYCT".
- **Identity**: `auth` holds admin user names and password hashes only. There are no accounts for skippers.
  `crates/service/src/auth.rs` issues a stateless 30-day bearer token (`base64(json(LoginResponse))`); the UI keeps
  it in the `regatta_token` cookie. One role exists: admin. While `auth` is empty every request is admin (bootstrap).
- **Authorization**: every mutating server function and REST route calls `require_admin` /
  `validate_admin_token`. Every read is public. Queries in `crates/service/src/db/` have no scoping predicate.
- **Routes**: flat paths (`/races`, `/series`, `/class`, `/rc`, `/admin`) and a flat REST API under `/v1/api`
  (`crates/common/src/api_paths.rs`), pinned by the contract cassettes.
- **Course marks**: a static list compiled into the UI (`marks()` in `crates/app/src/chart/chart_view_model.rs`).
  There is no table and no editor (see `course_creator.md`).
- **RC check-in**: kept in the browser's `localStorage` per race (`checkin_<race_id>` in `routes/rc.rs`), not in
  the database.
- **Tests**: the golden snapshot is a scrubbed CYCT production dump; the contract cassettes replay the flat API.

## 2. Tenancy model

The decisions below shape everything else, so they come first.

- **A club is the tenant.** Series, races, classes, brackets, race times, results, report images, course marks and
  club settings belong to exactly one club and are never visible across clubs.
- **People and boats are global.** A person or a boat is one record platform-wide, and a `club_membership`
  association links it to any number of clubs (with a per-club member flag, replacing `person.club_member`). The
  same boat racing at two clubs keeps one identity, one ORC certificate list, one history. Each club sees the
  members and the boats associated with it; a club admin can look up and associate an existing record rather than
  creating a duplicate.
- **Results stay with the club** that ran the race. A boat's cross-club history is a read-only view that joins
  results across clubs; it is a nice later feature, not a tenancy requirement.
- **ORC certificates** are global data keyed by `orc_ref` and attached to a boat, so nothing changes there.
- **Accounts are separate from people.** A `person` is a roster entry a club can create without the person ever
  logging in; an `account` is a login. An account may be linked to one person. This keeps the existing roster
  workflow and lets a skipper later claim the roster entry a club already made for them.
- **One deployment, one database**, with a `club_id` column on every tenant-owned table and a scoping predicate in
  every query. Schema-per-club or database-per-club are heavier than this project needs. Postgres row-level
  security can be a second line of defense later; the primary one is the data layer.

## 3. What has to change

### 3.1 Database schema

A new migration, an upgrade step in `crates/service/src/startup.rs` and a `SCHEMA_VERSION` bump (the current
server refuses any version it does not know, so the upgrade path has to be explicit).

New tables:

- `club`: id, slug (URL name), name, short name, timezone, locale, branding (burgee image, colors), defaults
  (PHRF B factor, ORC scoring options, default rating types), contact and website, created, active.
- `account`: id, email (unique, verified flag), password hash (argon2, not the current SHA-512 chain), display
  name, optional `person_id`, created, last login, disabled.
- `account_role`: account_id, club_id (nullable for platform roles), role. Roles: `platform_admin`, `club_admin`,
  `race_committee` (scoring and check-in, no club settings), `skipper` (own profile, boats, registrations).
- `club_membership`: club_id, person_id, member flag, member number, dates. Replaces `person.club_member`.
- `club_boat`: club_id, boat_id, club-local fields (fleet, club sail number override if ever needed, active in
  this club). The boat's rating fields stay on `boat`.
- `invitation`: token, club_id, role, email, invited by, expires, accepted. For onboarding admins and RC.
- `mark_set` and `mark`: a club's named sets of course marks (name, lat, lng, type, color, description). Replaces
  the static list. Races can reference a mark set so the Course screen shows the right marks.
- `race_entry` (registration): race_id, boat_id, skipper account or person, requested class, declared rating or
  cert at registration, status (pending, confirmed, withdrawn, waitlisted), crew count, waiver accepted, created.
- `race_fee`, `payment`: fee configuration per race or series, and the payment record per entry (provider, provider
  id, amount, currency, status, receipt URL). See 3.8.
- `check_in`: race_id, boat_id, checked in by, time. Moves RC check-in from `localStorage` to the database
  (already on the brainstorm list) and lets it be seeded from registrations.
- `session` or `refresh_token` (optional): only if we move away from fully stateless tokens so that logout and
  "disable this account" take effect immediately.

Changed tables:

- `club_id NOT NULL` with a foreign key on `series`, `race`, `raceclass`, `bracket`, `image`, `mark_set`,
  `race_entry`, `check_in`. `racebracketjunction`, `racetime`, `raceresults` inherit the club through their race
  and need no column, but an index on `(club_id, ...)` at the top of each hierarchy keeps queries cheap.
- Unique constraints become per club: a series name, a class name and its sort order, a bracket name are unique
  within a club, not globally.
- `person`: drop `club_member` after the migration moves it into `club_membership`. Add optional email and phone
  so a club can invite its roster.
- `boat.skipper_id` stays, meaning "the person who owns or skippers this boat". Ownership by an account comes
  through that person's linked account.
- `auth`: migrated into `account` + `account_role` (`club_admin` on the migrated club) and dropped.

Data migration: create one club row for CYCT, assign every existing tenant-owned row to it, copy
`person.club_member` into `club_membership`, associate every boat with CYCT, and turn every `auth` row into an
account with the `club_admin` role. The golden snapshot must be refreshed after this, with the approved report and
standings files unchanged in content (the report JSON should not need a club field; if it gains one, the golden
round trip test and the approved files change together).

### 3.2 Identity and authentication

- **Accounts with email.** Sign up with email and password, email verification, password reset. The current
  challenge-style login (`Login { salt, time, hash_of_hash }`) was designed for a single admin typing a shared
  secret; replace it with an ordinary password login over HTTPS and argon2id hashes. Optionally add magic links or
  OAuth (Google, Apple) later; both need the same account table.
- **Email delivery** becomes a dependency (verification, reset, invitations, registration receipts): an SMTP or API
  provider, a `REGATTA_SMTP_*` or provider key in `Config` and the Kubernetes secret, and templates.
- **Token contents.** The token must carry the account id and its roles per club (or a session id that resolves to
  them). Stateless tokens remain workable if they embed roles and we accept up to 30 days of staleness after a
  role change, or carry a short expiry with a refresh. A `session` table is simpler to reason about once there are
  many users and a "disable account" button.
- **Cookie and CORS** rules stay as they are (`HttpOnly`, `SameSite=Lax`, `Secure` behind HTTPS). The REST API keeps
  `Authorization: Bearer`.
- **Bootstrap** changes meaning: an empty `account` table lets the first platform admin be created, after which
  clubs are created through onboarding, not through bootstrap.
- **The `database/*.py` scripts** that use "Copy auth token" keep working if the token stays a bearer token; they
  gain a club parameter.

### 3.3 Authorization

- Replace the single `require_admin` with a small policy layer: `require_platform_admin()`,
  `require_club_role(club_id, Role)`, `require_boat_owner(boat_id)`, `require_self(person_id)`. Server functions
  and REST handlers resolve the club from the URL, never from the body, and pass it to the data layer.
- **Every query in `crates/service/src/db/` gains a club predicate** or takes a club-scoped id whose ownership was
  checked one level up. This is the single largest mechanical change and the one isolation depends on. A
  `ClubScope` newtype passed through `Db` methods makes it hard to forget.
- Public reads stay public but become club-scoped (a club's results and standings are public under that club's
  URL). Cross-club reads (a boat's history) are explicit.
- Writes to global records (boat, person) are allowed to: the linked account (self), a club admin of any club the
  record is associated with, and platform admins. Clubs cannot delete a boat or person; they can only remove the
  association.

### 3.4 Onboarding and sign-up flows

- **Club onboarding**: create account, verify email, create club (name, slug, timezone, burgee), the creator becomes
  its `club_admin`. Whether club creation is self-serve or gated by a platform admin (invite code, approval queue)
  is an open question; start gated.
- **Club admin setup wizard**: defaults (B factor, ORC options), classes and brackets (optionally cloned from a
  template or another season), first series, a mark set, invite RC members.
- **Invitations**: a club admin invites by email with a role; the link creates or attaches an account and grants the
  role.
- **Skipper sign-up**: create account, verify email, create or claim a person (match on email the club entered, or
  a club-approved claim), add boats or claim boats whose `skipper_id` points at that person, join a club
  (self-join with club approval, or by invitation).
- **Claiming** needs care: a club's roster entry and boat record pre-exist, and the wrong person must not be able to
  take them over. Email match plus club confirmation is the safe default.

### 3.5 Routes, URLs and the REST contract

- **UI**: a club segment in the path, `/{slug}/races`, `/{slug}/series/standings/view/:id/:year`, and so on, with
  the club resolved once in the router and put in context for every screen. Account pages (`/account`,
  `/account/boats`) and platform admin pages (`/platform/clubs`) sit outside a club. The home page becomes either a
  club picker or, when the host name maps to a club (`results.cyct.com`), that club's home.
- **REST**: `/v1/api/clubs/{slug}/...` for tenant resources, `/v1/api/boats`, `/v1/api/people`, `/v1/api/account`
  for global ones. The current flat paths are pinned by the contract cassettes; either keep them as aliases to the
  default club for a deprecation period (`REGATTA_DEFAULT_CLUB`) or re-record the cassettes as a breaking v2. A
  `/v2/api` namespace avoids silently changing v1 behavior.
- **Report and standings JSON** should ideally not change shape, so the golden files survive. A club field can be
  added to the surrounding envelope rather than to `RaceReport` and the standings types.

### 3.6 Club self-management

Most of this exists already and only needs scoping plus a club settings screen:

- **Series, races, classes, brackets, RC assignment**: the existing screens, filtered by club and writable by that
  club's admins and RC. Race-level fields that are CYCT constants today (B factor default, ORC option default)
  take their default from the club.
- **Club settings**: name, slug, timezone (race times are stored without a zone today and interpreted as local),
  burgee and theme colors (the semantic `--rg-*` tokens make per-club accents feasible), public results on or off,
  registration and payment settings.
- **Course mark sets**: an editor (the `course_creator.md` idea) backed by the new tables, a mark-set picker on the
  race, and the Course screen reading the club's set instead of the compiled list.
- **Members and boats**: a club view of its associated people and boats, with "associate existing" search across
  the global tables, member flags, and removal of the association.
- **Roles**: add and remove `club_admin` and `race_committee` holders.

### 3.7 Skipper features

- **Profile**: name, email, phone, emergency contact, linked person record, club memberships.
- **My boats**: create and edit boats the account owns, attach ORC certificates (the existing `orcCert` fetch),
  set PHRF rating, mark a boat sold or inactive, transfer a boat to another account.
- **Race registration**: browse a club's upcoming races, register a boat, choose or be assigned a class, declare
  the rating or certificate used, accept the waiver, pay if required, withdraw. Registration windows (open, close,
  late fee) live on the race or the series.
- **Entry list**: public per race; RC sees it on the RC screen and the check-in list is seeded from it.
- **Results and notifications**: a skipper's own results across clubs on one page; optional email when a race is
  scored.
- **RC screen** gains the persisted check-in and the entry list, which also removes the `localStorage` dependency.

### 3.8 Race registration payments (optional per club)

- **Provider**: Stripe is the pragmatic choice. Clubs are separate legal entities, so money must reach each club's
  own account: Stripe Connect (Standard accounts), with the platform optionally taking an application fee. The
  club connects its Stripe account from club settings; without one, registration is free or "pay the club
  directly" with an admin-set paid flag.
- **Flow**: entry created as `pending` → Stripe Checkout Session (hosted page, no card data touches the server) →
  webhook `checkout.session.completed` marks the payment and confirms the entry → receipt email. The webhook needs
  an unauthenticated route with signature verification, idempotent handling, and a secret in `Config`.
- **Fee model**: per race or per series, member and non-member prices, early and late pricing, optional per-crew
  add-ons, refund policy and a manual refund action for admins. Amounts in minor units with a currency on the club.
- **Records**: `race_fee` and `payment` tables (3.1); an admin payments view per race; reconciliation export (CSV)
  for the club treasurer.
- **Compliance**: Checkout keeps the server out of PCI scope. Terms and refund policy text per club. Payments and
  webhooks must be excluded from `REGATTA_FIXED_NOW` and from the replayed contract scenarios, or recorded against
  Stripe's test mode.

### 3.9 Scoring engine

`regatta-scoring` is pure and takes fleets built from already-loaded data, so it does not change. The service
wrappers that load a race or a series for scoring take a club-scoped race or series id. The seeded invariants and
the database-free golden check stay as they are.

### 3.10 Testing

- **Golden**: refresh the snapshot through the new migration (CYCT becomes club 1); approved files must still match
  byte for byte. Add a second, synthetic club to the dump or to a fixture so the golden and contract runs exercise
  isolation (club B must never appear in club A's pages, and vice versa).
- **Contract**: new cassettes for club-scoped routes, accounts, invitations, registration and the Stripe webhook
  (test-mode fixtures); the existing cassettes either keep passing through the alias layer or are retired with v1.
- **SSR tests**: every screen rendered under a club path, plus the account and platform screens.
- **Isolation tests** at the data layer: for each `Db` method, a call scoped to club A with an id owned by club B
  returns not found. This is the test that protects the tenancy promise and should be written alongside the
  `ClubScope` change, not after.
- **Auth tests**: role matrix per endpoint (anonymous, skipper, RC of club A, admin of club A, admin of club B,
  platform admin).

### 3.11 Deployment and operations

- New secrets: email provider, Stripe keys and webhook secret, token signing key if sessions become signed.
- Backups (`database/db_actions.py`) are unchanged in mechanism but now hold several clubs' data; restores must
  never be partial per club without a tool for it. A per-club export (and delete, for a club that leaves) is a
  reasonable requirement.
- Per-club host names need a wildcard or SAN certificate at the ingress and a host-to-club lookup in the server.
- Rate limiting and abuse controls on sign-up, login, password reset and the webhook route.
- Logging and tracing should carry the club slug so a report can be traced to its tenant.

## 4. Open questions

- Is club creation self-serve or approved by a platform admin? (Suggest approved at first.)
- Does the person record keep existing without an account (yes, clubs manage rosters today), and who may edit a
  shared person or boat when two clubs both use it?
- Path-based (`/cyct/...`) or host-based (`cyct.regatta...`) club URLs, or both?
- Keep the v1 REST API working for CYCT's existing scripts, and for how long?
- Should a race be able to belong to two clubs (joint regattas)? If yes, "host club" plus co-host read access is
  simpler than shared ownership.
- Stateless tokens with embedded roles, or a session table?
- How are PHRF ratings handled across clubs that use different PHRF fleets or ORC options? Today the rating lives on
  the boat; it may need to live on the club association or be declared at registration.

## 5. Suggested order

1. **Tenancy foundation**: `club` table, `club_id` on tenant tables, `club_membership` and `club_boat`, data
   migration making CYCT club 1, `ClubScope` through the data layer, club-scoped routes, isolation tests, golden
   refresh. No new user-facing features yet; CYCT works as before under its slug.
2. **Accounts and roles**: `account`, roles, email, new login, invitations, club settings screen, platform admin
   screen for clubs. Migrate `auth` rows.
3. **Course mark sets** and persisted check-in (both small and independently useful).
4. **Skipper accounts**: sign-up, claim person and boats, my boats, my results.
5. **Race registration** without payment: entries, windows, entry list, check-in seeded from entries.
6. **Payments**: Stripe Connect, Checkout, webhook, fees, admin payments view.
7. **Club onboarding polish**: self-serve creation if wanted, templates, per-club host names and branding.
