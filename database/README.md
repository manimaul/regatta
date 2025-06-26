# Local Dev
* url = `jdbc:postgresql://localhost:5432/s57server`
* username = `regatta_admin`
* password = `mysecretpassword`


Rather than installing PostGIS on your development machine just use `docker-compose` to bring up the database running
in a container with port `5432` exposed to your host network.
```shell
docker-compose up
```

## Connect Prod db to local dev
```shell 
app=regatta-postgres-service
pod=$(kubectl get pods -n regatta -l app="$app" -o jsonpath='{.items[*].metadata.name}')
kubectl -n regatta port-forward "$pod" 5432:5432
```

## Backup Dev
```shell
id=$(docker ps --filter name=database_postgres_1 --format json | jq -r '.ID')
stamp=dev_dump`date +%Y-%m-%d"_"%H_%M_%S`
docker exec -t "$id" pg_dump -U regatta_admin regatta > "./backup/$stamp.sql"
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
id=$(docker ps --filter name=database_postgres_1 --format json | jq -r '.ID')
df=$(readlink ./backup/prod_current.sql)
docker cp "./backup/$df" "$id:/dump.sql"
docker cp "./restore_dev.sh" "$id:/restore_dev.sh"
docker exec "$id" /restore_dev.sh
```