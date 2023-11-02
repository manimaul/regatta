#!/usr/bin/env bash

curl -d '{"name":"Harbor Series"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Spring Single/Double"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Spring Series"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Late Spring Series"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Three Hour Tour"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Summer Series"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Late Summer Series"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Vashon Challenge"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Windseekers Awards Race"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Point Series"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -d '{"name":"Memorial Single/Double"}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/series
curl -H "Content-Type: application/json" 'http://localhost:8888/v1/api/allSeries' | jq

#curl -H "Content-Type: application/json" 'http://localhost:8888/v1/api/series?id=1'
#
#curl -H "Content-Type: application/json" 'http://localhost:8888/v1/api/find/series?name=Summer'
###########################
#curl -d '{"first":"William", "last": "Kamp", "clubMember": true}' -H "Content-Type: application/json" -X POST http://localhost:8888/v1/api/person
#curl -H "Content-Type: application/json" 'http://localhost:8888/v1/api/people' | jq
