# The regatta image: the server binary and the Leptos site (hydration bundle, Tailwind CSS,
# assets), built for linux/amd64 only. Build it with `cargo xtask make-img`, which also passes the
# git build arguments (the build context has no .git).

# ---- build -------------------------------------------------------------------------------------
# The build stage runs on the build host's own architecture and cross-compiles the server for
# x86_64 Linux (rustc under qemu emulation crashes). The wasm bundle is target-independent.
FROM --platform=$BUILDPLATFORM docker.io/library/rust:1.99.1-bookworm AS build

# An x86_64 C toolchain for the crates with C sources (ring, aws-lc-sys). On an amd64 host the
# image's own gcc already answers to x86_64-linux-gnu-gcc.
RUN if [ "$(dpkg --print-architecture)" != "amd64" ]; then \
      apt-get update \
      && apt-get install -y --no-install-recommends gcc-x86-64-linux-gnu libc6-dev-amd64-cross \
      && rm -rf /var/lib/apt/lists/*; \
    fi

# cargo-leptos drives both builds (server + wasm) and downloads Tailwind and wasm-opt itself.
# The release binary, checksum-verified: compiling it here needs more memory than a podman VM has.
ARG CARGO_LEPTOS_VERSION=0.3.11
RUN set -eu; \
    triple="$(uname -m)-unknown-linux-gnu"; \
    base="https://github.com/leptos-rs/cargo-leptos/releases/download/v${CARGO_LEPTOS_VERSION}"; \
    cd /tmp; \
    curl -fsSLO "${base}/cargo-leptos-${triple}.tar.gz"; \
    curl -fsSLO "${base}/cargo-leptos-${triple}.tar.gz.sha256"; \
    echo "$(cut -d' ' -f1 cargo-leptos-${triple}.tar.gz.sha256)  cargo-leptos-${triple}.tar.gz" | sha256sum -c -; \
    mkdir cargo-leptos && tar -xzf "cargo-leptos-${triple}.tar.gz" -C cargo-leptos; \
    find cargo-leptos -type f -name cargo-leptos -exec install -m 0755 {} /usr/local/cargo/bin/cargo-leptos \; ; \
    rm -rf /tmp/cargo-leptos*; \
    cargo leptos --version

WORKDIR /build
# The toolchain file adds the wasm32-unknown-unknown target and components.
COPY rust-toolchain.toml ./
RUN rustup show active-toolchain || rustup toolchain install
RUN rustup target add x86_64-unknown-linux-gnu

# With a target triple set cargo-leptos looks for the package name; the binary is `regatta`.
ENV LEPTOS_BIN_TARGET_TRIPLE=x86_64-unknown-linux-gnu \
    LEPTOS_BIN_EXE_NAME=regatta \
    CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER=x86_64-linux-gnu-gcc \
    CC_x86_64_unknown_linux_gnu=x86_64-linux-gnu-gcc \
    AR_x86_64_unknown_linux_gnu=x86_64-linux-gnu-ar

COPY . .

# Version info for crates/common/build.rs (git is not available in the build context).
ARG REGATTA_GIT_HASH=unknown
ARG REGATTA_GIT_BRANCH=unknown
ARG REGATTA_GIT_DIRTY=false
ENV REGATTA_GIT_HASH=${REGATTA_GIT_HASH} \
    REGATTA_GIT_BRANCH=${REGATTA_GIT_BRANCH} \
    REGATTA_GIT_DIRTY=${REGATTA_GIT_DIRTY}

# The registry and target/ are cache mounts, so the artifacts are copied out in the same step.
RUN --mount=type=cache,target=/usr/local/cargo/registry \
    --mount=type=cache,target=/build/target \
    cargo leptos build --release --precompress \
 && mkdir -p /out \
 && cp target/x86_64-unknown-linux-gnu/release/regatta /out/regatta \
 && cp -r target/site /out/site

# ---- runtime -----------------------------------------------------------------------------------
FROM --platform=linux/amd64 gcr.io/distroless/cc-debian12

WORKDIR /opt/regatta
COPY --from=build /out/regatta /opt/regatta/regatta
COPY --from=build /out/site /opt/regatta/site

# Leptos runtime options (the site the server serves) and the server's own configuration
# (crates/service/src/config.rs). REGATTA_DB_URL is overridden by the deployment.
ENV LEPTOS_OUTPUT_NAME=regatta \
    LEPTOS_SITE_ROOT=site \
    LEPTOS_SITE_PKG_DIR=pkg \
    LEPTOS_SITE_ADDR=0.0.0.0:8888 \
    LEPTOS_ENV=PROD \
    REGATTA_DB_URL=postgres://regatta_admin:mysecretpassword@localhost:5432/regatta \
    RUST_LOG=info

EXPOSE 8888

ENTRYPOINT ["/opt/regatta/regatta"]
