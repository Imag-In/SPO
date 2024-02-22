#!/bin/bash

if [ -z "$1" ]
then
    PROFILE=default
else
    PROFILE="$1"
fi

./java-runtime/bin/java \
  -XX:+UseZGC \
  -Xms2g \
  --enable-preview \
  -Xverify:none \
  -XX:TieredStopAtLevel=1 \
  -Dspring.profiles.active=${PROFILE} \
  -Dspring.jmx.enabled=false \
  -Dspring.config.location=classpath:/application.yml \
  -jar ./spo-full.jar

#!/bin/bash



