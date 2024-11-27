#!/usr/bin/env bash

dropdb -U regatta_admin -f regatta || echo "skipped dropdb"
createdb -U regatta_admin regatta || echo "skipped create"
psql -U regatta_admin regatta -f /dump.sql
echo "restored dev"
rm /dump.sql