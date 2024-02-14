# Local Dev
* url = `jdbc:postgresql://localhost:5432/s57server`
* username = `admin`
* password = `mysecretpassword`


Rather than installing PostGIS on your development machine just use `docker-compose` to bring up the database running
in a container with port `5432` exposed to your host network.
```shell
docker-compose up
```

```shell 
kubectl -n regatta get pods
kubectl -n regatta port-forward regatta-postgres-865bc46b86-r52m9 5432:5432
```

```
```

Backup
```shell
docker ps
docker exec -t d6f22ce2de86 pg_dump -U admin regatta > dev_dump_`date +%Y-%m-%d"_"%H_%M_%S`.sql

kubectl -n regatta get pods
kubectl -n regatta exec regatta-postgres-865bc46b86-r52m9 -- pg_dump -U regatta_admin regatta > prod_dump`date +%Y-%m-%d"_"%H_%M_%S`.sql 
```

# Restore 
```shell
cat prod_dump2024-02-12_22_16_01.sql | docker exec -t df989685a4de pg_restore -c -U admin -d regatta 
```