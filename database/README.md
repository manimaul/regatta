# Local Dev
* url = `jdbc:postgresql://localhost:5432/s57server`
* username = `admin`
* password = `mysecretpassword`


Rather than installing PostGIS on your development machine just use `docker-compose` to bring up the database running
in a container with port `5432` exposed to your host network.
```shell
docker-compose up
```

## Connect Prod db to local dev
```shell 
kubectl -n regatta get pods
kubectl -n regatta port-forward regatta-postgres-865bc46b86-r52m9 5432:5432
```

## Backup Dev
```shell
docker ps
docker exec -t d6f22ce2de86 pg_dump -U admin regatta > dev_dump_`date +%Y-%m-%d"_"%H_%M_%S`.sql
```

## Backup Prod
```shell
kubectl -n regatta get pods
kubectl -n regatta exec regatta-postgres-865bc46b86-r52m9 -- pg_dump -U regatta_admin regatta > prod_dump`date +%Y-%m-%d"_"%H_%M_%S`.sql 
```

# Restore Dev
```shell
c='df989685a4de'
df='prod_dump2024-02-14_10_30_06.sql' 
docker cp "$df" "$c:/dump.sql"
docker exec -i "$c" dropdb -U admin -f regatta
docker exec -i "$c" createdb -U admin regatta
docker exec -t "$c" psql -U admin regatta -f /dump.sql
```