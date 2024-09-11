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
kubectl -n regatta get pods
kubectl -n regatta port-forward regatta-postgres-865bc46b86-r52m9 5432:5432
```

## Backup Dev
```shell
docker ps
docker exec -t d6f22ce2de86 pg_dump -U regatta_admin regatta > dev_dump_`date +%Y-%m-%d"_"%H_%M_%S`.sql
docker exec -t "$c" psql -U regatta_admin regatta -f /dump.sql
```

## Backup Prod
```shell
kubectl -n regatta get pods
kubectl -n regatta exec regatta-postgres-9f4cbdd48-k7zvb -- pg_dump -U regatta_admin regatta > prod_dump`date +%Y-%m-%d"_"%H_%M_%S`.sql 
kubectl -n regatta exec regatta-postgres-865bc46b86-r52m9 -- bash -c "echo 'SHOW timezone;' | psql -U regatta_admin regatta"
```

## Prod Shell
```shell
kubectl -n regatta exec --stdin --tty regatta-postgres-865bc46b86-r52m9 -- /bin/bash
```

# Restore Dev
```shell
c='53e7bb79c828'
df='prod_dump2024-04-25_07_53_27.sql' 
docker cp "$df" "$c:/dump.sql"
echo "SHOW timezone;" | docker exec -i "$c" psql -U regatta_admin regatta
docker exec -i "$c" dropdb -U regatta_admin -f regatta
docker exec -i "$c" createdb -U regatta_admin regatta
echo "SHOW timezone;" | docker exec -i "$c" psql -U regatta_admin regatta
docker exec -t "$c" psql -U regatta_admin regatta -f /dump.sql

docker exec -it "$c" sh -c "bash"
```