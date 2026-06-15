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
kubectl -n regatta exec "$pod" -- pg_dump -U regatta_admin regatta > "./backup/$stamp.sql"
rm ./backup/prod_current.sql
ln -s "$stamp.sql" ./backup/prod_current.sql
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
df=$(readlink ./backup/prod_current.sql)
podman cp "./backup/$df" "$id:/dump.sql"
podman cp "./restore_dev.sh" "$id:/restore_dev.sh"
podman exec "$id" /restore_dev.sh
```