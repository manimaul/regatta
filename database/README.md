# Local Dev
* `REGATTA_DB_URL=postgres://regatta_admin:mysecretpassword@localhost:5432/regatta` (the server's default)
* username = `regatta_admin`
* password = `mysecretpassword`


Rather than installing PostgreSQL on your development machine just use `podman-compose` to bring up the database running
in a container with port `5432` exposed to your host network.
```shell
podman-compose up
```

## Production database
Production is the `regatta` database on the managed PostgreSQL cluster  
`REGATTA_PG_USER` / `REGATTA_PG_PASS` come from the environment (the same variables `cargo xtask` uses for the k8s manifests).
Keep the password in `PGPASSWORD`, never in the URI.

```shell
export PGPASSWORD="$REGATTA_PG_PASS"
url="postgresql://$REGATTA_PG_USER@$NJORD_DB_HOST:$NJORD_DB_PORT/regatta?sslmode=require"
psql "$url"                                                     # prod shell
```

## Backup and restore
`db_actions.py` does all three. It reads `NJORD_DB_HOST` / `NJORD_DB_PORT` from the environment and
`REGATTA_PG_USER` / `REGATTA_PG_PASS` from the environment; dev
uses `docker-compose.yaml`'s defaults. Dumps land in `database/backup/` (gitignored) as
`prod_dump_<stamp>.sql`, and `prod_current.sql` always points at the newest.
```shell
python3 -I database/db_actions.py backup                 # prod -> database/backup/prod_dump_<stamp>.sql
python3 -I database/db_actions.py restore-prod --yes     # prod <- prod_current.sql (takes a fresh backup first)
python3 -I database/db_actions.py restore-dev            # dev  <- prod_current.sql
python3 -I database/db_actions.py restore-dev --dump database/backup/prod_dump_<stamp>.sql
```
A restore drops and recreates the `public` schema and loads the dump in one transaction, so the
database is never seen half-restored and a failure leaves it as it was. `restore-prod` refuses
without `--yes`; `--no-backup` skips the backup it otherwise takes before writing. The dev container
is PostgreSQL 16 and the managed cluster 17, so the restore strips the `SET transaction_timeout`
a newer `pg_dump` emits.

## Fix ORC wind bands
Makes every race's ORC wind bands follow its PHRF B factor with the 5-Band Windward/Leeward option,
through the REST API (R4b in `docs/explore_unimplemented/orc_series_standings.md`). Dry run by
default; back up first, then `--apply` with the admin bearer token from "Copy auth token" on the
web home page (`--token` or `REGATTA_TOKEN`, prompted otherwise).
```shell
python3 -I database/fix_orc_bands.py                                # report
python3 -I database/fix_orc_bands.py --apply --token <pasted token> # write
REGATTA_TOKEN=<pasted token> python3 -I database/fix_orc_bands.py --base-url https://host --apply
```

# Scripts
* `fix_orc_bands.py` — make every race's ORC wind bands follow its PHRF B factor (REST API, dry run by default).
* `orc_backfill.py` — resolve the ORC file id (certificate link) of stored certificates still on ORC's active list (REST API, dry run by default): `python3 -I database/orc_backfill.py --apply --token <token>`.
* `audit_scoring.py`, `scrub_dump.py` — golden regression helpers for `crates/scoring/tests/golden/`, see `CLAUDE.md`.
* `db_actions.py` — back up production, restore production from a backup, restore the dev database from a backup (above).
