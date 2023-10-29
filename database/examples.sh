#!/usr/bin/env bash

curl -d '{"name":"Harbor Series"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -H "Content-Type: application/json"  'http://localhost:8888/v1/api/series?id=1'
