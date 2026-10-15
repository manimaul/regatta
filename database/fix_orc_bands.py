#!/usr/bin/env python3
"""
Make every race's ORC wind bands follow its PHRF B factor (R4b in
docs/explore_unimplemented/orc_series_standings.md), through the REST API.

For each race in every year the server knows:
  orcScoringOption -> FiveBandWindwardLeeward
  orc5Band         -> Orc5Band.fromPhrfBFactor(phrfBFactor)
  orc3Band         -> Orc3Band.fromPhrfBFactor(phrfBFactor)
The B factor itself is never changed. Races already in that state are left alone.

Dry run by default: prints what would change. Pass --apply to write. Back up the database first
(database/backup). Needs an admin login; the password is prompted for when not given.

  python3 -I database/fix_orc_bands.py --user admin                       # dry run, local server
  python3 -I database/fix_orc_bands.py --user admin --apply
  python3 -I database/fix_orc_bands.py --user admin --base-url https://regatta.example.org --apply

Only the standard library is used. The POST /race route is an upsert of the race row alone
(name, series, RC, B factor, ORC option and bands); class schedules and results are untouched.
"""

import argparse
import base64
import getpass
import hashlib
import json
import sys
import time
import urllib.error
import urllib.request

API = "/v1/api"
OPTION = "FiveBandWindwardLeeward"


# ---- band mapping: a copy of Orc3Band.fromPhrfBFactor / Orc5Band.fromPhrfBFactor in common

def orc3_band(cf: int) -> str:
    if cf >= 600:
        return "Low"
    if 550 <= cf <= 600:
        return "Medium"
    return "High"


def orc5_band(cf: int) -> str:
    if cf >= 650:
        return "Low"
    if 600 <= cf <= 650:
        return "LowMedium"
    if 550 <= cf <= 600:
        return "Medium"
    if 510 <= cf <= 550:
        return "MediumHigh"
    return "High"


# ---- auth: the same scheme as web/utils/Auth.kt and server/auth/Token.kt

def fold_hash(*parts: str) -> str:
    """hash(a, b, c) = sha512(c + sha512(b + sha512(a))) as base64, like Token.hash."""
    result = ""
    for part in parts:
        digest = hashlib.sha512((part + result).encode("utf-8")).digest()
        result = base64.b64encode(digest).decode("ascii")
    return result


def login(base_url: str, user: str, password: str) -> str:
    """POST /login and return the bearer token: base64 of the LoginResponse JSON."""
    password_hash = fold_hash(password)
    now = int(time.time())
    salt = base64.b64encode(hashlib.sha256(f"{now}{user}".encode()).digest()).decode("ascii")
    body = {
        "userName": user,
        "hashOfHash": fold_hash(salt, str(now), password_hash),
        "salt": salt,
        "time": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(now)),
    }
    response = request(base_url, "POST", "/login", body)
    return base64.b64encode(json.dumps(response).encode("utf-8")).decode("ascii")


# ---- http

def request(base_url: str, method: str, path: str, body=None, token: str | None = None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(base_url + API + path, data=data, method=method)
    req.add_header("Accept", "application/json")
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            text = resp.read().decode("utf-8")
            return json.loads(text) if text else None
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", "replace")
        sys.exit(f"{method} {path} -> {e.code} {e.reason}: {text}")


# ---- main

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--base-url", default="http://localhost:8888", help="server root (default: %(default)s)")
    ap.add_argument("--user", required=True, help="admin user name")
    ap.add_argument("--password", help="admin password (prompted when omitted)")
    ap.add_argument("--apply", action="store_true", help="write the changes; without it, only report")
    args = ap.parse_args()

    years = request(args.base_url, "GET", "/years")
    races = []
    for year in sorted(int(y) for y in years):
        for schedule in request(args.base_url, "GET", f"/allRaces?year={year}") or []:
            races.append((year, schedule["race"]))

    changes = []
    for year, race in races:
        cf = race["phrfBFactor"]
        want = {"orcScoringOption": OPTION, "orc3Band": orc3_band(cf), "orc5Band": orc5_band(cf)}
        have = {k: race[k] for k in want}
        if have != want:
            changes.append((year, race, have, want))

    print(f"{len(races)} races in {len(years)} years, {len(changes)} to change")
    for year, race, have, want in changes:
        diff = ", ".join(f"{k}: {have[k]} -> {want[k]}" for k in want if have[k] != want[k])
        print(f"  {year} #{race['id']:>4} B={race['phrfBFactor']:<4} {race['name'][:40]:<40} {diff}")

    if not changes:
        return 0
    if not args.apply:
        print("dry run; pass --apply to write")
        return 0

    password = args.password or getpass.getpass(f"password for {args.user}: ")
    token = login(args.base_url, args.user, password)

    failed = 0
    for year, race, have, want in changes:
        updated = dict(race, **want)
        saved = request(args.base_url, "POST", "/race", updated, token)
        got = {k: (saved or {}).get(k) for k in want}
        ok = got == want
        failed += 0 if ok else 1
        print(f"  #{race['id']:>4} {'ok' if ok else 'MISMATCH ' + json.dumps(got)}")
    print(f"{len(changes) - failed} updated, {failed} failed")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
