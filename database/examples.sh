#!/usr/bin/env bash

# This is a fake token, you can login and copy from http://localhost:8080 home page after logging in
token="eyJpZCI6MSwiaGFzaE9mSGFzaCI6Imk0eDR0T3dLNWU3WkdvSi81a3R1NnhtbEVUU243d00vY0p2YjN0K1dsQ3pPS3ZYM2tXZHhseTBBY1FmZmtod2ZBeGdYYlNGNndQY0k4U0RKZk0yZjFBPT0iLCJzYWx0IjoiSnJ4QW5hd2xLUldOK2JCMTJZbkFpUT09IiwiZXhwaXJlcyI6IjIwMjMtMTEtMDhUMTM6MjQ6MTYuODEwMTU4MjQ5WiJ9"
echo $token | base64 -d | jq

function postSeries() {
curl -d "{\"name\":\"$1\"}" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $token" \
     -X POST http://localhost:8888/v1/api/series | jq
}

function postAllSeries() {
declare -a arr=("Harbor Series" "Spring Single/Double" "Spring Series" "Late Spring Series" "Three Hour Tour" "Summer Series" "Late Summer Series" "Vashon Challenge" "Windseekers Awards Race" "Point Series" "Memorial Single/Double")
for i in "${arr[@]}"
do
   postSeries "$i"
done
}

postAllSeries
curl -H "Content-Type: application/json" 'http://localhost:8888/v1/api/allSeries' | jq
#curl -d '{"first":"William", "last": "Kamp", "clubMember": true}' \
#     -H "Content-Type: application/json" \
#     -H "Authorization: Bearer $token" \
#     -X POST http://localhost:8888/v1/api/person
