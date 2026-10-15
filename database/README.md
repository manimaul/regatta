# Local Dev
* url = `jdbc:postgresql://localhost:5432/s57server`
* username = `regatta_admin`
* password = `mysecretpassword`


Rather than installing PostGIS on your development machine just use `podman-compose` to bring up the database running
in a container with port `5432` exposed to your host network.
```shell
podman-compose up
```

## Connect Prod db to local dev
```shell 
app=regatta-postgres-service
pod=$(kubectl get pods -n regatta -l app="$app" -o jsonpath='{.items[*].metadata.name}')
kubectl -n regatta port-forward "$pod" 5432:5432
```

## Backup Dev
```shell
id=$(podman ps --filter name=database_postgres_1 --format json | jq -r '.[0].Id')
stamp=dev_dump`date +%Y-%m-%d"_"%H_%M_%S`
podman exec -t "$id" pg_dump -U regatta_admin regatta > "./backup/$stamp.sql"
rm ./backup/dev_current.sql
ln -s "$stamp.sql" ./backup/dev_current.sql
```

## Backup Prod
```shell
app=regatta-postgres-service
pod=$(kubectl get pods -n regatta -l app="$app" -o jsonpath='{.items[*].metadata.name}')
stamp=prod_dump`date +%Y-%m-%d"_"%H_%M_%S`
kubectl -n regatta exec "$pod" -- pg_dump -U regatta_admin regatta > "./database/backup/$stamp.sql"
rm ./database/backup/prod_current.sql
ln -s "$stamp.sql" ./database/backup/prod_current.sql
```

# Restore Prod
```
app=regatta-postgres-service
pod=$(kubectl get pods -n regatta -l app="$app" -o jsonpath='{.items[*].metadata.name}')
cat ./backup/prod_current.sql | kubectl -n regatta exec -i "$pod" -- psql -U regatta_admin -d regatta
```

## Prod Shell
```
app=regatta-postgres-service
pod=$(kubectl get pods -n regatta -l app="$app" -o jsonpath='{.items[*].metadata.name}')
kubectl -n regatta exec --stdin --tty "$pod" -- /bin/bash
```

# Restore Prod Backup to Dev
```shell
id=$(podman ps --filter name=database_postgres_1 --format json | jq -r '.[0].Id')
df=$(readlink ./database/backup/prod_current.sql)
podman cp "./database/backup/$df" "$id:/dump.sql"
podman cp "./database/restore_dev.sh" "$id:/restore_dev.sh"
podman exec "$id" /restore_dev.sh
```

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
* `audit_scoring.py`, `scrub_dump.py` — golden regression helpers, see `CLAUDE.md`.
