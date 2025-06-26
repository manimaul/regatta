# Regatta
Software for scoring sailing regattas utilizing [PHRF](https://www.ussailing.org/competition/offshore/phrf/) scoring.
Developed by William Kamp of SV Waymaker for the [Corinthian Yacht Club of Tacoma](https://cyct.com/).

![Sailing Vessel Waymaker](./sv_waymaker.jpeg "SV Waymaker")

### Build and run 
```shell
cd database
docker-compose up
```
```shell
./gradlew :server:run
```

### Run in browser hot loading
```shell
cd database
docker-compose up
```
```shell
./gradlew :server:run
```

```shell
./gradlew :web:jsRun --continuous
```

### Run in container 
```shell
cd database
docker-compose up
```

```shell
./gradlew makeImg
docker run --network=host ghcr.io/manimaul/regatta:latest
```

### Useful links:
- [Use Kotlin Code from JS](https://kotlinlang.org/docs/js-to-kotlin-interop.html)
- [JavaScript modules](https://kotlinlang.org/docs/js-modules.html)
- [webpack bundling](https://kotlinlang.org/docs/js-project-setup.html#webpack-bundling)

### Install on bare metal / VM 

Install Postgres Local
```shell
sudo apt install postgresql postgresql-client
sudo su - postgres -c "createuser regatta_admin"
sudo su - postgres -c "createdb regatta"
sudo -u postgres psql -c "ALTER USER regatta_admin PASSWORD 'mysecretpassword';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE regatta TO regatta_admin;"
sudo -u postgres psql -c "ALTER DATABASE regatta OWNER TO regatta_admin;"
sudo bash -c "sed -i -e 's/5433/5432/g' /etc/postgresql/15/main/postgresql.conf"
sudo bash -c 'echo "listen_addresses = 'localhost'" >> /etc/postgresql/15/main/postgresql.conf'
sudo systemctl restart postgresql
```

Build & Install Binary
```shell
./gradlew :server:buildDeb
sudo dpkg -i server/build/distributions/regatta<version>.deb
```

Edit `/etc/regatta.env` and restart service
```shell
sudo systemctl restart regatta.service
sudo systemctl status regatta.service
```

Check logs
```shell
sudo journalctl -u regatta -f
```
Uninstall
```shell
sudo dpkg --remove regatta
```

### Nginx Reverse Proxy Info

```shell
sudo apt install nginx openssl
sudo mkdir -p /etc/tls
sudo chmod go+r /etc/tls/key.pem
sudo openssl req -x509 -newkey rsa:4096 -nodes -keyout /etc/tls/key.pem -out /etc/tls/cert.pem -days 365 -subj '/CN=localhost'
sudo cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.bak
sudo vim /etc/nginx/sites-enabled/default
```

/etc/nginx/sites-enabled/default
```
server {
    listen 80;
    server_name regatta;

    location / {
        proxy_pass http://localhost:8888;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    listen [::]:443 ssl ipv6only=on;
    listen 443 ssl;
    server_name regatta;
    ssl_certificate /etc/tls/cert.pem;
    ssl_certificate_key /etc/tls/key.pem;
    add_header Content-Security-Policy upgrade-insecure-requests;
}
```

```shell
sudo systemctl restart nginx
journalctl -fxeu nginx
```
