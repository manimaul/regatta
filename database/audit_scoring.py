#!/usr/bin/env python3
"""Re-score the golden approved outputs under the decided rules and list every deviation.

    python3 database/audit_scoring.py [approved_dir] [out.json]

Defaults: server/src/jvmTest/resources/golden/approved and
server/src/jvmTest/resources/golden/deviations.json, relative to the repo root.

The approved files (see docs/explore_unimplemented/test_audit_and_plan.md §5) hold both the inputs
(each card's resultRecord, the race schedule) and the outputs the code produced. This script
re-scores them with decisions D1-D5 and the D8 checks and writes one entry per difference, keyed
by race or series id, boat id and decision. When a decision is implemented, re-approving the
golden files and re-running this script must remove only that decision's entries.

Rules implemented (test_audit_and_plan.md, "Decisions"):
  D3  starters = all entries except DNS_RC / DNS; finishers = TIME + HOC;
      DNF = RET = finishers + 1; NSC = DNS = starters + 1; DSQ = starters + 2.
  D4  penalty = place + n, capped at DNF, only on TIME / HOC; no other boat changes.
  D7  equal corrected times share the place and the next place skips (no data case).
  D2  a race is sailed for a group (bracket or class) when the group has a starter; unsailed
      races score nothing and count toward neither the throw-out threshold nor the tiebreak;
      the throw-out is chosen per group (worst bracket score / worst class score).
  D1  RC volunteer: average of the other sailed races (DNS included, throw-out excluded),
      fractional; once per series.
  D5  tiebreak: last sailed race, then earlier races; RC races skipped; full ties share a place.
  D8  standings rows grouped by boat id; race years by the Pacific calendar.

No dependencies beyond the standard library.
"""
import functools
import glob
import json
import os
import re
import sys
from datetime import datetime, timezone
from decimal import Decimal
from zoneinfo import ZoneInfo

PACIFIC = ZoneInfo("America/Los_Angeles")
RC = "DNS_RC"
NOT_STARTERS = {"DNS_RC", "DNS"}
FINISH_CODES = {"TIME", "HOC"}

_DURATION = re.compile(r"^(-)?PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$")


def duration_ms(iso):
    """Whole milliseconds of a kotlin.time.Duration ISO string, truncated toward zero like inWholeMilliseconds."""
    if iso is None:
        return None
    m = _DURATION.match(iso)
    if not m:
        raise ValueError(f"unparsed duration {iso!r}")
    sign, h, mi, s = m.groups()
    total = Decimal(h or 0) * 3600 + Decimal(mi or 0) * 60 + Decimal(s or 0)
    ms = int(total * 1000)  # Decimal -> int truncates toward zero
    return -ms if sign else ms


def code_of(card):
    return card["resultRecord"]["result"]["finishCode"]


def boat_id_of(card):
    return card["resultRecord"]["result"]["boatId"]


def is_starter(card):
    return code_of(card) not in NOT_STARTERS


# ---------------------------------------------------------------- race scoring (D3, D4, D7)

def score_group(cards, warnings):
    """Expected score per card index for one scoring group (a bracket or a class). RC cards get None."""
    starters = sum(1 for c in cards if is_starter(c))
    finishers = sum(1 for c in cards if code_of(c) in FINISH_CODES)
    dnf = finishers + 1

    timed = [i for i, c in enumerate(cards) if code_of(c) == "TIME"]
    for i in timed:
        if cards[i]["correctedPhrfTime"] is None:
            warnings.add(f"TIME card without a corrected time: result {cards[i]['resultRecord']['result']['id']}")
    timed.sort(key=lambda i: (duration_ms(cards[i]["correctedPhrfTime"]) is None, duration_ms(cards[i]["correctedPhrfTime"]) or 0))
    hoc = sorted((i for i, c in enumerate(cards) if code_of(c) == "HOC"), key=lambda i: cards[i]["hocPosition"] or 0)

    place = {}
    tied = set()
    n = 0
    prev_ms = object()
    prev_i = None
    pos = 0
    for i in timed:
        n += 1
        ms = duration_ms(cards[i]["correctedPhrfTime"])
        if ms != prev_ms:
            pos = n  # D7: equal times share the place, the next place skips
        else:
            tied.update((i, prev_i))
        place[i] = pos
        prev_ms = ms
        prev_i = i
    for i in hoc:
        n += 1
        place[i] = n

    scores = {}
    for i, c in enumerate(cards):
        code = code_of(c)
        if code in FINISH_CODES:
            penalty = c["penalty"] or 0
            scores[i] = min(place[i] + penalty, dnf)  # D4
        elif code in ("DNF", "RET"):
            scores[i] = dnf
        elif code in ("NSC", "DNS"):
            scores[i] = starters + 1  # D3
        elif code == "DSQ":
            scores[i] = starters + 2
        elif code == RC:
            scores[i] = None
        else:
            raise ValueError(f"unknown finish code {code}")
    return scores, starters, tied


def race_decision(card, cards, tied):
    """Which decision explains a race-level difference for this card."""
    code = code_of(card)
    if code == "NSC":
        return "D3"
    if any(c["penalty"] for c in cards):
        return "D4"
    if any(cards[i] is card for i in tied):
        return "D7"
    return "?"


# ---------------------------------------------------------------- series scoring (D1, D2, D5, D8)

def group_series(race_ids, race_reports, class_id, bracket_id, warnings):
    """Per race: the group's cards, whether it was sailed, its starters and expected scores by boat id."""
    per_race = {}
    for rid in race_ids:
        report = race_reports.get(rid)
        cards = []
        if report is not None:
            for cr in report["classReports"]:
                if cr["raceClass"]["id"] != class_id:
                    continue
                for br in cr["bracketReport"]:
                    if bracket_id is None or br["bracket"]["id"] == bracket_id:
                        cards.extend(br["cards"])
        scores, starters, tied = score_group(cards, warnings) if cards else ({}, 0, set())
        sailed = starters > 0  # D2
        by_boat = {}
        for i, c in enumerate(cards):
            b = boat_id_of(c)
            if b in by_boat:
                warnings.add(f"boat {b} has two results in race {rid}")
            by_boat[b] = {"code": code_of(c), "score": scores[i], "penalty": c["penalty"], "card": c}
        per_race[rid] = {"cards": cards, "sailed": sailed, "starters": starters, "by_boat": by_boat, "tied": tied}
    return per_race


def boat_series(race_ids, per_race, boat):
    """Expected per-race scores, throw-out and total for one boat in one group, under D1 and D2.

    Returns (cells, total) where cells[rid] = {"score", "counted", "rc", "decision"} or None when unsailed.
    """
    cells = {}
    rc_races = []
    for rid in race_ids:
        pr = per_race[rid]
        if not pr["sailed"]:
            cells[rid] = None
            continue
        entry = pr["by_boat"].get(boat)
        if entry is None:
            cells[rid] = {"score": pr["starters"] + 1, "rc": False, "code": None}
        elif entry["code"] == RC:
            cells[rid] = {"score": None, "rc": True, "code": RC}
            rc_races.append(rid)
        else:
            cells[rid] = {"score": entry["score"], "rc": False, "code": entry["code"]}

    sailed = [rid for rid in race_ids if cells[rid] is not None]
    scored = [rid for rid in sailed if not cells[rid]["rc"]]
    throw_out = None
    if len(sailed) >= 5 and scored:  # D2 threshold, D1 step 1
        throw_out = max(scored, key=lambda rid: (cells[rid]["score"], race_ids.index(rid)))

    for n, rid in enumerate(rc_races):
        if n > 0:  # D1: once per series, later RC races score DNS
            cells[rid] = {"score": per_race[rid]["starters"] + 1, "rc": True, "code": RC}
            continue
        others = [r for r in scored if r != throw_out]
        if not others:
            cells[rid]["score"] = per_race[rid]["starters"] + 1
            continue
        # the average of the remaining races is never worse than the thrown-out score, so the RC race is never the throw-out
        cells[rid]["score"] = sum(cells[r]["score"] for r in others) / len(others)

    total = 0
    for rid in sailed:
        cells[rid]["counted"] = rid != throw_out
        if rid != throw_out:
            total += cells[rid]["score"]
    return cells, throw_out, total


def place_rows(rows, race_ids):
    """D5: assign series places to rows [{boat, total, cells}] -> {boat: place}. Shared places skip."""

    def tiebreak(a, b):
        for rid in reversed(race_ids):
            ca, cb = a["cells"][rid], b["cells"][rid]
            if ca is None or cb is None:  # unsailed for the group
                continue
            if ca["rc"] or cb["rc"]:  # GSI 1.4.1.2: not a tiebreaker
                continue
            if ca["score"] != cb["score"]:
                return -1 if ca["score"] < cb["score"] else 1
        return 0

    def compare(a, b):
        if a["total"] != b["total"]:
            return -1 if a["total"] < b["total"] else 1
        return tiebreak(a, b)

    ordered = sorted(rows, key=functools.cmp_to_key(compare))
    places = {}
    place = 0
    for i, row in enumerate(ordered):
        if i == 0 or compare(ordered[i - 1], row) != 0:
            place = i + 1
        places[row["boat"]] = place
    return places


def fnum(x):
    """Numbers as JSON-friendly values: ints stay ints, fractions are rounded for readability."""
    if x is None:
        return None
    if isinstance(x, float):
        return int(x) if x.is_integer() else round(x, 4)
    return x


def audit_series(standings, race_reports, entries, warnings):
    series = standings["series"]
    sid, year = series["id"], standings["year"]
    race_ids = [r["id"] for r in standings["races"]]
    base = {"kind": "series", "seriesId": sid, "year": year, "series": series["name"]}

    for sc in standings["standings"]:
        class_id = sc["raceClass"]["id"]
        class_name = sc["raceClass"]["name"]
        class_races = group_series(race_ids, race_reports, class_id, None, warnings)
        class_rows = []  # for place in class across brackets
        class_current_rows = []
        class_total_decisions = {}

        for sb in sc["standings"]:
            bracket_id = sb["bracket"]["id"]
            loc = dict(base, **{"class": class_name, "bracket": sb["bracket"]["name"]})
            bracket_races = group_series(race_ids, race_reports, class_id, bracket_id, warnings)
            unsailed = [rid for rid in race_ids if not bracket_races[rid]["sailed"]]

            # D8: rows grouped by boat id
            by_boat = {}
            for row in sb["standings"]:
                b = row["boatSkipper"]["boat"]["id"]
                by_boat.setdefault(b, []).append(row)
            for b, rows in by_boat.items():
                if len(rows) > 1:
                    entries.append(dict(loc, decision="D8", boatId=b, boat=rows[0]["boatSkipper"]["boat"]["name"],
                                        field="rows", current=len(rows), expected=1,
                                        note="one row per boat id; the boat changed skipper mid-series"))

            expected_rows = []
            total_decisions = {}  # boat -> decisions behind a changed bracket total
            ctotal_decisions = {}  # boat -> decisions behind a changed class total
            for b, rows in by_boat.items():
                name = rows[0]["boatSkipper"]["boat"]["name"]
                bloc = dict(loc, boatId=b, boat=name)
                cells, throw_out, total = boat_series(race_ids, bracket_races, b)
                ccells, cthrow, ctotal = boat_series(race_ids, class_races, b)
                expected_rows.append({"boat": b, "total": total, "cells": cells})
                class_rows.append({"boat": b, "total": ctotal, "cells": ccells})

                # current cells, merged across split rows (D8): the row that holds a record wins
                current = []
                for i, rid in enumerate(race_ids):
                    recorded = [r["raceStandings"][i] for r in rows if not r["raceStandings"][i]["nonStarter"]]
                    current.append(recorded[0] if recorded else rows[0]["raceStandings"][i])
                row = min(rows, key=lambda r: r["totalScoreBracket"])
                class_current_rows.append((b, row))
                split = len(rows) > 1
                decisions = {"D8"} if split else set()
                cdecisions = {"D8"} if split else set()
                if any(c is not None and c["rc"] for c in cells.values()):
                    decisions.add("D1")
                    cdecisions.add("D1")
                if unsailed:
                    decisions.add("D2")
                if any(not class_races[rid]["sailed"] for rid in race_ids):
                    cdecisions.add("D2")

                def check_cells(expected, races, cur_field, cor_field, label, found):
                    for rid, cur in zip(race_ids, current):
                        cell = expected[rid]
                        if cell is None:
                            if cur[cur_field] != 0 or not cur["nonStarter"] or cur["throwOut"]:
                                entries.append(dict(bloc, decision="D2", field=f"race {rid}{label}", raceId=rid,
                                                    current=cur[cur_field], expected=None, note="not sailed for this group"))
                            continue
                        if cell["rc"]:
                            shown = cur[cor_field]
                            if fnum(shown) != fnum(cell["score"]) or cur[cur_field] != cur[cor_field]:
                                entries.append(dict(bloc, decision="D1", field=f"race {rid}{label}", raceId=rid,
                                                    current=cur[cur_field], currentShown=shown, expected=fnum(cell["score"]),
                                                    note="RC volunteer average"))
                            continue
                        if cur[cur_field] != cell["score"]:
                            pr = races[rid]
                            entry = pr["by_boat"].get(b)
                            if entry is None:
                                if split and cur["nonStarter"]:
                                    d, note = "D8", "the record is on the boat's other row"
                                elif any(e["code"] == RC for e in pr["by_boat"].values()):
                                    d, note = "D3", "DNS: starters exclude the RC volunteer"
                                else:
                                    d, note = "?", "DNS score differs"
                            else:
                                d = race_decision(entry["card"], pr["cards"], pr["tied"])
                                note = {"D3": "NSC = starters + 1", "D4": "penalty", "D7": "tie", "?": "unexplained"}[d]
                            found.add(d)
                            entries.append(dict(bloc, decision=d, field=f"race {rid}{label}", raceId=rid,
                                                current=cur[cur_field], expected=cell["score"], note=note))

                check_cells(cells, bracket_races, "placeInBracket", "placeInBracketCorrected", "", decisions)
                check_cells(ccells, class_races, "placeInClass", "placeInClassCorrected", " (class)", cdecisions)

                if fnum(row["totalScoreBracket"]) != fnum(total):
                    total_decisions[b] = decisions
                    entries.append(dict(bloc, decision="+".join(sorted(decisions)) or "?", field="totalScoreBracket",
                                        current=row["totalScoreBracket"], expected=fnum(total)))
                if fnum(row["totalScoreClass"]) != fnum(ctotal):
                    note = None
                    if not cdecisions:
                        # the code chooses one throw-out by bracket score and applies it to the class total too;
                        # D2 scores each group on its own, so the class total drops the worst class score
                        thrown = [rid for rid, cur in zip(race_ids, current) if cur["throwOut"]]
                        if cthrow is not None and thrown != [cthrow]:
                            cdecisions.add("D2")
                            note = f"class throw-out: race {cthrow} (worst class score), not race {thrown} (worst bracket score)"
                    ctotal_decisions[b] = cdecisions
                    entry = dict(bloc, decision="+".join(sorted(cdecisions)) or "?", field="totalScoreClass",
                                 current=row["totalScoreClass"], expected=fnum(ctotal))
                    if note:
                        entry["note"] = note
                    entries.append(entry)
            class_total_decisions.update(ctotal_decisions)

            # places in bracket: a tiebreak difference (D5) or a consequence of changed totals
            places = place_rows(expected_rows, race_ids)
            group = set().union(*total_decisions.values()) if total_decisions else set()
            for r in expected_rows:
                cur = min(by_boat[r["boat"]], key=lambda x: x["placeInBracket"])["placeInBracket"]
                if cur != places[r["boat"]]:
                    d = "+".join(sorted(group)) if group else "D5"
                    entries.append(dict(loc, decision=d, boatId=r["boat"], boat=by_boat[r["boat"]][0]["boatSkipper"]["boat"]["name"],
                                        field="placeInBracket", current=cur, expected=places[r["boat"]],
                                        note="totals change in this bracket" if group else "tiebreak"))

        # places in class
        cplaces = place_rows(class_rows, race_ids)
        group = set().union(*class_total_decisions.values()) if class_total_decisions else set()
        for b, row in class_current_rows:
            if row["placeInClass"] != cplaces[b]:
                d = "+".join(sorted(group)) if group else "D5"
                entries.append(dict(base, **{"class": class_name}, decision=d, boatId=b, boat=row["boatSkipper"]["boat"]["name"],
                                    field="placeInClass", current=row["placeInClass"], expected=cplaces[b],
                                    note="totals change in this class" if group else "tiebreak"))


def audit_race(report, entries, warnings):
    race = report["raceSchedule"]["race"]
    base = {"kind": "race", "raceId": race["id"], "race": race["name"]}
    for cr in report["classReports"]:
        if cr["raceClass"]["id"] == 0:
            continue  # orphan "Incorrectly classed" bucket
        class_cards = [c for br in cr["bracketReport"] for c in br["cards"]]
        cscores, _, ctied = score_group(class_cards, warnings)
        for i, c in enumerate(class_cards):
            if cscores[i] is not None and c["placeInClass"] != cscores[i]:
                entries.append(dict(base, **{"class": cr["raceClass"]["name"]}, decision=race_decision(c, class_cards, ctied),
                                    boatId=boat_id_of(c), boat=c["boatName"], field="placeInClass",
                                    current=c["placeInClass"], expected=cscores[i]))
        for br in cr["bracketReport"]:
            bscores, _, btied = score_group(br["cards"], warnings)
            for i, c in enumerate(br["cards"]):
                if bscores[i] is not None and c["placeInBracket"] != bscores[i]:
                    entries.append(dict(base, **{"class": cr["raceClass"]["name"], "bracket": br["bracket"]["name"]},
                                        decision=race_decision(c, br["cards"], btied), boatId=boat_id_of(c), boat=c["boatName"],
                                        field="placeInBracket", current=c["placeInBracket"], expected=bscores[i]))
    # D8: the race year by the Pacific calendar vs UTC
    for cs in report["raceSchedule"]["schedule"]:
        start = datetime.fromisoformat(cs["startDate"].replace("Z", "+00:00"))
        if start.year != start.astimezone(PACIFIC).year:
            entries.append(dict(base, decision="D8", field="year", classId=cs["raceClass"]["id"],
                                current=start.year, expected=start.astimezone(PACIFIC).year,
                                note="seriesRaces selects by UTC year"))


def main(argv):
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    approved = argv[1] if len(argv) > 1 else os.path.join(root, "server/src/jvmTest/resources/golden/approved")
    out = argv[2] if len(argv) > 2 else os.path.join(root, "server/src/jvmTest/resources/golden/deviations.json")

    race_reports = {}
    for path in glob.glob(os.path.join(approved, "race-*.json")):
        with open(path, encoding="utf-8") as f:
            report = json.load(f)
        race_reports[report["raceSchedule"]["race"]["id"]] = report
    standings = []
    for path in glob.glob(os.path.join(approved, "standings-*.json")):
        with open(path, encoding="utf-8") as f:
            standings.append(json.load(f))

    entries = []
    warnings = set()
    for rid in sorted(race_reports):
        audit_race(race_reports[rid], entries, warnings)
    for s in sorted(standings, key=lambda s: (s["year"], s["series"]["id"])):
        audit_series(s, race_reports, entries, warnings)

    def key(e):
        return (e["kind"], e.get("year", 0), e.get("seriesId", 0), e.get("raceId", 0), e.get("class", ""),
                e.get("bracket", ""), e.get("boatId", 0), e["field"], e["decision"])

    entries.sort(key=key)
    counts = {}
    for e in entries:
        counts[e["decision"]] = counts.get(e["decision"], 0) + 1
    result = {
        "source": os.path.relpath(approved, root),
        "races": len(race_reports),
        "standings": len(standings),
        "counts": dict(sorted(counts.items())),
        "warnings": sorted(warnings),
        "entries": entries,
    }
    with open(out, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, sort_keys=False)
        f.write("\n")
    print(f"{len(entries)} deviation(s) -> {os.path.relpath(out, root)}")
    for d, n in sorted(counts.items()):
        print(f"  {d}: {n}")
    for w in sorted(warnings):
        print(f"  warning: {w}")


if __name__ == "__main__":
    main(sys.argv)
