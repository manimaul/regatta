#!/usr/bin/env bash

# This is a fake token, you can login and copy from http://localhost:8080 home page after logging in
token="eyJpZCI6MSwiaGFzaE9mSGFzaCI6Im9FNHNMc3duZ0QzQzBaN0szZnFlOFZ5ZDRGQmRHK0Z6dTM3OGhUTHRKS2NVMFZxQ3VBZU54NWcyekdJc3hnV2FXNEhWV2duTnYrWjJSbHBKeWNVZWJRPT0iLCJzYWx0IjoiK3NzQUcwdU5CR2JvUEZlWFYyZDk1Zz09IiwiZXhwaXJlcyI6IjIwMjMtMTEtMDdUMDQ6Mzg6MjkuODI1ODM5MzE5WiJ9"
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

curl -H "Content-Type: application/json" 'http://localhost:8888/v1/api/allSeries' | jq
#curl -d '{"first":"William", "last": "Kamp", "clubMember": true}' \
#     -H "Content-Type: application/json" \
#     -H "Authorization: Bearer $token" \
#     -X POST http://localhost:8888/v1/api/person
