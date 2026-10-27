# Regatta
Software for scoring sailing regattas utilizing [ORC](https://orc.org/), [PHRF](https://www.ussailing.org/competition/offshore/phrf/), Cruising Class Flying and Cruising class Non-Flying scoring.

ORC Scoring supports the [US scoring options](crates/common/src/data/orc_certificate.rs) 

Developed by William Kamp of SV Waymaker for the [Corinthian Yacht Club of Tacoma](https://cyct.com/).

![Sailing Vessel Waymaker](./sv_waymaker.jpeg "SV Waymaker")

Regatta is a Rust workspace: an [axum](https://github.com/tokio-rs/axum) server rendering a
[Leptos](https://leptos.dev/) UI that hydrates in the browser from WebAssembly, over PostgreSQL 16.

### Prerequisites
- [rustup](https://rustup.rs/): the toolchain and the `wasm32-unknown-unknown` target are pinned in `rust-toolchain.toml`
- [cargo-leptos](https://github.com/leptos-rs/cargo-leptos): `cargo install cargo-leptos --locked --version 0.3.11`
- [podman](https://podman.io/) and podman-compose, for the database, the container-backed tests and the image

### Build and run
```shell
cd database
podman-compose up
```
```shell
cargo leptos watch     # http://127.0.0.1:8888, rebuilds and live-reloads on change
```

`cargo leptos serve` builds once and serves; `cargo xtask run` does the same with a release build. The server reads
`REGATTA_DB_URL` (default `postgres://regatta_admin:mysecretpassword@localhost:5432/regatta`, the compose database),
`REGATTA_BIND` (default `0.0.0.0:8888`) and `RUST_LOG`.

### Test
```shell
cargo test --workspace                                # the golden and contract tests need podman running
cargo clippy --workspace --all-targets -- -D warnings
```

### Run in container
The image is built for linux/amd64: the build stage cross-compiles natively, and on Apple Silicon `podman run` runs the image under emulation. `--pull=never` keeps the run on the local build rather than the published image.
```shell
cd database
podman-compose up
```

```shell
cargo xtask make-img
podman run --pull=never -p 8888:8888 \
  -e REGATTA_DB_URL=postgres://regatta_admin:mysecretpassword@host.containers.internal:5432/regatta \
  ghcr.io/manimaul/regatta:latest
```

### Deploy to Kubernetes
```shell
export REGATTA_PG_USER=... REGATTA_PG_PASS=...
podman login ghcr.io
cargo xtask k8s-apply-secrets   # first deploy and credential changes
cargo xtask deploy-server       # build and push the image, apply k8s_deploy/server.yaml, restart and wait
```

### Install on bare metal / VM (x86_64 Linux)

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
cargo leptos build --release --precompress
sudo mkdir -p /opt/regatta
sudo cp target/release/regatta /opt/regatta/regatta
sudo cp -r target/site /opt/regatta/site
```

`/etc/regatta.env`
```shell
REGATTA_DB_URL=postgres://regatta_admin:mysecretpassword@localhost:5432/regatta
LEPTOS_OUTPUT_NAME=regatta
LEPTOS_SITE_ROOT=/opt/regatta/site
LEPTOS_SITE_PKG_DIR=pkg
LEPTOS_SITE_ADDR=0.0.0.0:8888
LEPTOS_ENV=PROD
RUST_LOG=info
```

`/etc/systemd/system/regatta.service`
```ini
[Unit]
Description=Regatta scoring server
After=network-online.target postgresql.service

[Service]
EnvironmentFile=/etc/regatta.env
WorkingDirectory=/opt/regatta
ExecStart=/opt/regatta/regatta
Restart=on-failure
DynamicUser=yes

[Install]
WantedBy=multi-user.target
```

Enable, restart and check the service
```shell
sudo systemctl daemon-reload
sudo systemctl enable --now regatta.service
sudo systemctl restart regatta.service
sudo systemctl status regatta.service
```

Check logs
```shell
sudo journalctl -u regatta -f
```
Uninstall
```shell
sudo systemctl disable --now regatta.service
sudo rm -rf /opt/regatta /etc/systemd/system/regatta.service /etc/regatta.env
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
