#!/usr/bin/env bash
set -e
source /etc/regatta.env
JAVA_OPTS="-Djdbcurl=${POSTGRES_URL}:${POSTGRES_PORT}/${POSTGRES_DB} -Duser=${POSTGRES_USER} -Dpassword=${POSTGRES_PASSWORD}" /opt/regatta/bin/regatta
