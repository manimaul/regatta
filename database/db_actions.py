#!/usr/bin/env python3
"""Back up and restore the regatta database.

    python3 -I database/db_actions.py backup                    # prod -> database/backup/prod_dump_<stamp>.sql
    python3 -I database/db_actions.py restore-prod --yes        # prod <- database/backup/prod_current.sql
    python3 -I database/db_actions.py restore-dev               # dev  <- database/backup/prod_current.sql
    python3 -I database/db_actions.py restore-dev --dump database/backup/prod_dump_....sql

Production is the `regatta` database on the managed PostgreSQL cluster njord uses, reached directly
over TLS as the role that owns it. Dev is the podman `postgres:16` container from
`database/docker-compose.yaml`, published on localhost:5432.

Credentials, read from the environment:
  NJORD_DB_HOST, NJORD_DB_PORT      the managed cluster's endpoint (njord's variables)
  REGATTA_PG_USER, REGATTA_PG_PASS  the role that owns the database (the same variables
                                    `cargo xtask` reads for the k8s manifests)
Dev uses the compose file's defaults (regatta_admin / mysecretpassword).

Passwords go to psql and pg_dump through PGPASSWORD, never on a command line or inside a URI, so
they show up in neither `ps` nor the shell history.

A restore drops and recreates the `public` schema and loads the dump in one transaction: the
database is never seen half-restored, and a failure leaves it as it was. The role can do that
without admin rights because it owns the database and, since PostgreSQL 15, `public` belongs to
`pg_database_owner`. `restore-prod` refuses without --yes and takes a fresh backup first (unless
--no-backup), so the state being replaced is always on disk.
"""

from __future__ import annotations

import argparse
import datetime as dt
import os
import re
import subprocess
import sys
from pathlib import Path

DB_NAME = "regatta"
BACKUP_DIR = Path(__file__).resolve().parent / "backup"
CURRENT_LINK = "prod_current.sql"
COMPLETION_MARKER = "PostgreSQL database dump complete"

DEV_HOST = "localhost"
DEV_PORT = "5432"
DEV_USER = "regatta_admin"
DEV_PASS = "mysecretpassword"


class Fail(Exception):
    pass


def log(msg: str) -> None:
    print(msg, flush=True)


def run(cmd: list[str], *, env: dict[str, str] | None = None, stdin=None, input: str | None = None) -> str:
    full_env = dict(os.environ)
    if env:
        full_env.update(env)
    proc = subprocess.run(cmd, env=full_env, stdin=stdin, input=input, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if proc.returncode != 0:
        raise Fail(f"{cmd[0]} failed ({proc.returncode}):\n{proc.stderr.strip()}")
    if proc.stderr.strip():
        log(proc.stderr.rstrip())
    return proc.stdout


# ---------------------------------------------------------------- credentials

def require_env(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        raise Fail(f"{name} is not set")
    return value


# ---------------------------------------------------------------- connections

class Conn:
    def __init__(self, label: str, host: str, port: str, user: str, password: str, ssl: bool) -> None:
        self.label = label
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.ssl = ssl

    def __str__(self) -> str:
        return f"{self.label} ({self.user}@{self.host}:{self.port}/{DB_NAME})"

    def _env(self) -> dict[str, str]:
        env = {"PGPASSWORD": self.password, "PGCONNECT_TIMEOUT": "20"}
        if self.ssl:
            env["PGSSLMODE"] = "require"
        return env

    def _conn_args(self) -> list[str]:
        return ["-h", self.host, "-p", self.port, "-U", self.user, "-d", DB_NAME]

    def psql(self, args: list[str], *, stdin=None, input: str | None = None) -> str:
        cmd = ["psql", "-X", "-q", "-At", "-v", "ON_ERROR_STOP=1"] + self._conn_args() + args
        return run(cmd, env=self._env(), stdin=stdin, input=input)

    def sql(self, sql: str) -> str:
        # Through stdin, not -c, so several statements run in one session.
        return self.psql(["-f", "-"], input=sql)

    def pg_dump(self, out: Path) -> None:
        cmd = ["pg_dump", "--no-owner", "--no-privileges", "--no-tablespaces",
               "--encoding=UTF8", "--format=plain"] + self._conn_args()
        with out.open("w") as f:
            proc = subprocess.run(cmd, env={**os.environ, **self._env()}, stdout=f,
                                  stderr=subprocess.PIPE, text=True)
        if proc.returncode != 0:
            out.unlink(missing_ok=True)
            raise Fail(f"pg_dump failed ({proc.returncode}):\n{proc.stderr.strip()}")

    def check(self) -> None:
        try:
            version = self.sql("select version();").strip()
        except Fail as e:
            hint = ""
            if self.label == "dev":
                hint = "\nIs the dev database up? `cd database && podman-compose up`"
            raise Fail(f"cannot connect to {self}:\n{e}{hint}")
        log(f"{self.label}: {version.split(',')[0]}")


def prod() -> Conn:
    return Conn("prod", require_env("NJORD_DB_HOST"), require_env("NJORD_DB_PORT"),
                require_env("REGATTA_PG_USER"),
                require_env("REGATTA_PG_PASS"), ssl=True)


def dev(host: str, port: str) -> Conn:
    return Conn("dev", host, port, DEV_USER, DEV_PASS, ssl=False)


# ---------------------------------------------------------------- actions

def backup(conn: Conn) -> Path:
    BACKUP_DIR.mkdir(exist_ok=True)
    stamp = dt.datetime.now().strftime("%Y-%m-%d_%H_%M_%S")
    path = BACKUP_DIR / f"prod_dump_{stamp}.sql"
    log(f"dumping {conn} -> {path}")
    conn.pg_dump(path)
    tail = path.read_text()[-400:]
    if COMPLETION_MARKER not in tail:
        path.unlink(missing_ok=True)
        raise Fail(f"{path.name} does not end with pg_dump's completion marker; the stream was cut")
    link = BACKUP_DIR / CURRENT_LINK
    link.unlink(missing_ok=True)
    link.symlink_to(path.name)
    log(f"  {path.stat().st_size:,} bytes; {CURRENT_LINK} -> {path.name}")
    return path


def resolve_dump(arg: Path | None) -> Path:
    path = arg if arg else BACKUP_DIR / CURRENT_LINK
    if not path.exists():
        raise Fail(f"{path} does not exist" + ("" if arg else "; run `backup` first or pass --dump"))
    text = path.read_text()
    if COMPLETION_MARKER not in text[-400:]:
        raise Fail(f"{path} does not end with pg_dump's completion marker; refusing to restore a cut dump")
    return path.resolve()


# Settings a newer pg_dump emits that an older server rejects (pg_dump 17+ writes
# `SET transaction_timeout`, which PostgreSQL 16, the dev container, does not know). Dropping them
# restores the server's defaults, which is what they are set to anyway.
UNPORTABLE_SET = re.compile(r"^SET transaction_timeout = .*$", re.M)


def restore(conn: Conn, dump_path: Path) -> None:
    text = UNPORTABLE_SET.sub("", dump_path.read_text())
    # pg_dump omits the pinned public schema unless its owner or ACL changed; recreate it ourselves
    # only when the dump does not.
    recreate = "" if re.search(r"^CREATE SCHEMA public;", text, re.M) else "CREATE SCHEMA public;\n"
    script = "SET client_min_messages = warning;\nDROP SCHEMA public CASCADE;\n" + recreate + text
    log(f"restoring {dump_path.name} into {conn} (single transaction)")
    conn.psql(["--single-transaction", "-f", "-"], input=script)
    tables = conn.sql("select count(*) from pg_tables where schemaname = 'public';").strip()
    version = conn.sql("select value from metadata where name = 'version';").strip()
    log(f"  restore complete: {tables} tables, metadata.version = {version}")


# ---------------------------------------------------------------- main

def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="action", required=True)

    sub.add_parser("backup", help="dump production to database/backup/ and repoint prod_current.sql")

    rp = sub.add_parser("restore-prod", help="replace production with a dump (backs up first)")
    rp.add_argument("--dump", type=Path, help=f"dump to restore (default: backup/{CURRENT_LINK})")
    rp.add_argument("--yes", action="store_true", help="really replace the production database")
    rp.add_argument("--no-backup", action="store_true", help="skip the fresh backup taken before restoring")

    rd = sub.add_parser("restore-dev", help="replace the local dev database with a dump")
    rd.add_argument("--dump", type=Path, help=f"dump to restore (default: backup/{CURRENT_LINK})")
    rd.add_argument("--dev-host", default=DEV_HOST)
    rd.add_argument("--dev-port", default=DEV_PORT)

    args = p.parse_args()
    try:
        if args.action == "backup":
            conn = prod()
            conn.check()
            backup(conn)
        elif args.action == "restore-prod":
            dump_path = resolve_dump(args.dump)
            conn = prod()
            if not args.yes:
                raise Fail(f"this replaces {conn} with {dump_path.name}; re-run with --yes")
            conn.check()
            if not args.no_backup:
                backup(conn)
            restore(conn, dump_path)
        elif args.action == "restore-dev":
            dump_path = resolve_dump(args.dump)
            conn = dev(args.dev_host, args.dev_port)
            conn.check()
            restore(conn, dump_path)
        return 0
    except Fail as e:
        print(f"error: {e}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
