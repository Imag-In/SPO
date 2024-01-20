#!/bin/bash

mkdir -p target
INFO_FILE="./target/build.env"
rm -rf ${INFO_FILE}

SPO_FINAL_NAME=$(./mvnw help:evaluate -Dexpression=project.build.finalName -q -DforceStdout)
SPO_ARTIFACT_ID=$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
SPO_GROUP_ID=$(./mvnw help:evaluate -Dexpression=project.groupId -q -DforceStdout)

echo "SPO_VERSION=\"$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)\"" >> ${INFO_FILE}
echo "JAVA_VERSION=\"$(./mvnw help:evaluate -Dexpression=java.version -q -DforceStdout | cut -d'.' -f1)\"" >> ${INFO_FILE}
echo "MAIN_JAR=\"${SPO_FINAL_NAME}.jar\"" >> ${INFO_FILE}
echo "SPO_ARTIFACT_ID=\"${SPO_ARTIFACT_ID}\"" >> ${INFO_FILE}
echo "SPO_GROUP_ID=\"${SPO_GROUP_ID}\"" >> ${INFO_FILE}
echo "APP_NAME=\"$(./mvnw help:evaluate -Dexpression=spo.appName -q -DforceStdout)\"" >> ${INFO_FILE}
echo "APP_MAIN_CLASS=\"$(./mvnw help:evaluate -Dexpression=mainClass -q -DforceStdout)\"" >> ${INFO_FILE}
echo "APP_DESC=\"$(./mvnw help:evaluate -Dexpression=project.description -q -DforceStdout)\"" >> ${INFO_FILE}
echo "APP_VENDOR=\"$(./mvnw help:evaluate -Dexpression=spo.vendor -q -DforceStdout)\"" >> ${INFO_FILE}
