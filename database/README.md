# Local Dev
* url = `jdbc:postgresql://localhost:5432/s57server`
* username = `admin`
* password = `mysecretpassword`


Rather than installing PostGIS on your development machine just use `docker-compose` to bring up the database running
in a container with port `5432` exposed to your host network.
```shell
docker-compose up
```

``` 
kubectl -n regatta get pods
kubectl -n regatta port-forward regatta-postgres-865bc46b86-r52m9 5432:5432
```
