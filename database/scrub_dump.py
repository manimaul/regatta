#!/usr/bin/env python3
"""Scrub a plain-text pg_dump of the regatta database for use as the golden test snapshot.

    python3 database/scrub_dump.py database/backup/prod_current.sql \
        server/src/jvmTest/resources/golden/regatta-YYYY-MM-DD.sql

- Empties public.auth: every row between its COPY line and the "\\." terminator is dropped, so no
  password hashes are committed.
- Removes the "\\restrict" / "\\unrestrict" lines newer pg_dump versions emit, so older psql
  clients can load the file.
- Fails if public.image has rows, so binary data never lands in the repo by accident.
- Leaves everything else, including person names, untouched.

No dependencies beyond the standard library. See docs/explore_unimplemented/test_audit_and_plan.md §5.
"""
import sys


def scrub(lines):
    out = []
    table = None          # name of the table whose COPY block we are inside
    auth_rows = 0
    image_rows = 0
    for line in lines:
        if line.startswith("\\restrict ") or line.startswith("\\unrestrict "):
            continue
        if table is None:
            if line.startswith("COPY public.auth ("):
                table = "auth"
            elif line.startswith("COPY public.image ("):
                table = "image"
            out.append(line)
            continue
        # inside a COPY block we care about
        if line.rstrip("\n") == "\\.":
            out.append(line)
            table = None
            continue
        if table == "auth":
            auth_rows += 1
            continue  # drop the row
        if table == "image":
            image_rows += 1
            out.append(line)
    if table is not None:
        raise SystemExit(f"unterminated COPY block for {table}")
    if image_rows:
        raise SystemExit(f"refusing to scrub: public.image has {image_rows} row(s) of binary data")
    return out, auth_rows


def main(argv):
    if len(argv) != 3:
        raise SystemExit(__doc__)
    src, dst = argv[1], argv[2]
    with open(src, encoding="utf-8") as f:
        lines = f.readlines()
    out, auth_rows = scrub(lines)
    with open(dst, "w", encoding="utf-8") as f:
        f.writelines(out)
    print(f"wrote {dst}: {len(out)} lines, dropped {auth_rows} auth row(s)")


if __name__ == "__main__":
    main(sys.argv)
