#!/bin/sh
cd /root/brif-nix
ant run -Darg0=$1 -Darg1=setup:true -Darg2=$2 -Darg3=$3 >/var/log/$1.boostrapper.log 2>&1

APPLICATION_ID=$(curl http://metadata/computeMetadata/v1/instance/attributes/application_id -H "X-Google-Metadata-Request: True")
REST_API_KEY=$(curl http://metadata/computeMetadata/v1/instance/attributes/rest_api_key -H "X-Google-Metadata-Request: True")
NIXER_ID=$(curl http://metadata/computeMetadata/v1/instance/attributes/nixer_id -H "X-Google-Metadata-Request: True")

curl -X PUT \
  -H "X-Parse-Application-Id: $APPLICATION_ID" \
  -H "X-Parse-REST-API-Key: $REST_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"free_spots":{"__op":"Increment","amount":1}}' \
  https://api.parse.com/1/classes/Nixers/$NIXER_ID

gsutil cp /var/log/$1.boostrapper.log gs://setup-user-logs/$1.boostrapper.log  