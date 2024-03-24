#!/bin/bash

APP_VERSION="${SPO_VERSION}"
#APP_NAME=${SPO_ARTIFACT_ID}

echo "java home: ${JAVA_HOME}"
echo "App groupId: ${SPO_GROUP_ID}"
echo "App artifactId: ${SPO_ARTIFACT_ID}"
echo "App version: ${APP_VERSION}"
echo "main JAR file: ${MAIN_JAR}"
echo "App name: ${APP_NAME}"
echo "App classname: ${APP_MAIN_CLASS}"
echo "App description: ${APP_DESC}"
echo "App vendor: ${APP_VENDOR}"

# ------ SETUP DIRECTORIES AND FILES ------------------------------------------
# Remove previously generated java runtime and installers. Copy all required
# jar files into the input/libs folder.

BUILD_DIR=./build
rm -rfd ${BUILD_DIR}/java-runtime/
rm -rfd ${BUILD_DIR}/installer/

mkdir -p ${BUILD_DIR}/installer/input/libs/

cp dependencies/BOOT-INF/lib/* ${BUILD_DIR}/installer/input/libs/
cp "${MAIN_JAR}" ${BUILD_DIR}/installer/input/libs/

# ------ REQUIRED MODULES -----------------------------------------------------
# Use jlink to detect all modules that are required to run the application.
# Starting point for the jdep analysis is the set of jars being used by the
# application.

echo "detecting required modules"
detected_modules=$("${JAVA_HOME}"/bin/jdeps \
  --multi-release "${JAVA_VERSION}" \
  --ignore-missing-deps \
  --print-module-deps \
  --class-path "${BUILD_DIR}/installer/input/libs/*" \
  "${MAIN_JAR}")
echo "detected modules: ${detected_modules}"


# ------ MANUAL MODULES -------------------------------------------------------
# jdk.crypto.ec has to be added manually bound via --bind-services or
# otherwise HTTPS does not work.
#
# See: https://bugs.openjdk.java.net/browse/JDK-8221674
#
# In addition we need jdk.localedata if the application is localized.
# This can be reduced to the actually needed locales via a jlink paramter,
# e.g., --include-locales=en,de.

manual_modules=jdk.crypto.ec,jdk.localedata,java.naming,java.management
echo "manual modules: ${manual_modules}"

# ------ RUNTIME IMAGE --------------------------------------------------------
# Use the jlink tool to create a runtime image for our application. We are
# doing this is a separate step instead of letting jlink do the work as part
# of the jpackage tool. This approach allows for finer configuration and also
# works with dependencies that are not fully modularized, yet.

echo "creating java runtime image"
"$JAVA_HOME"/bin/jlink \
  --no-header-files \
  --no-man-pages  \
  --compress=zip-6  \
  --strip-debug \
  --add-modules "${detected_modules},${manual_modules}" \
  --include-locales=en,fr \
  --output ${BUILD_DIR}/java-runtime

# ------ Portable app -------------
echo "creating portable version"
mkdir -p ${BUILD_DIR}/spo
cp src/distrib/linux/spo.sh ${BUILD_DIR}/spo
cp spo-full.jar ${BUILD_DIR}/spo/spo-full.jar
cp -rf ${BUILD_DIR}/java-runtime ${BUILD_DIR}/spo/
chmod a+x ${BUILD_DIR}/spo/spo.sh

cd build

tar czf "${SPO_ARTIFACT_ID}-linux-portable.tar.gz" spo
mv "${SPO_ARTIFACT_ID}-linux-portable.tar.gz" "${SPO_ARTIFACT_ID}_${APP_VERSION}-linux-portable.tar.gz"
sha256sum "${SPO_ARTIFACT_ID}_${APP_VERSION}-linux-portable.tar.gz" > "${SPO_ARTIFACT_ID}_${APP_VERSION}-linux-portable.tar.gz.sha256"
cd ..

# ------ PACKAGING ------------------------------------------------------------
# A loop iterates over the various packaging types supported by jpackage. In
# the end we will find all packages inside the ${BUILD_DIR}/installer directory.

for type in "deb" "rpm"
do
  echo "Creating installer of type ... $type"

  "$JAVA_HOME"/bin/jpackage \
  --type $type \
  --dest ${BUILD_DIR}/installer \
  --input ${BUILD_DIR}/installer/input/libs \
  --name "${APP_NAME}" \
  --main-class "${APP_MAIN_CLASS}" \
  --main-jar "${MAIN_JAR}" \
  --java-options -XX:+UseZGC \
  --java-options -Xms2g \
  --java-options -Xverify:none \
  --java-options '--enable-preview' \
  --java-options '-Djdk.gtk.verbose=true' \
  --java-options '-Djdk.gtk.version=3' \
  --java-options "-Dspring.jmx.enabled=false" \
  --java-options "--add-opens javafx.controls/javafx.scene.control=ALL-UNNAMED" \
  --java-options "--add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED" \
  --java-options -Dspring.profiles.active=default \
  --java-options -Dspring.config.location=classpath:/application.yml \
  --runtime-image ${BUILD_DIR}/java-runtime \
  --icon src/main/resources/images/spo-256x256.png \
  --linux-shortcut \
  --linux-menu-group "${APP_VENDOR}" \
  --app-version "${APP_VERSION}" \
  --vendor "${APP_VENDOR}" \
  --copyright "Copyright © 2023 ${APP_VENDOR}" \
  --license-file LICENSE.txt \
  --description "$APP_DESC" \

done


# ------ CHECKSUM FILE --------------------------------------------------------
arch_name="$(uname -m)"
APP_NAME=$(echo "${APP_NAME}" | tr ' ' '-' | tr '[:upper:]' '[:lower:]')

ls -la ${BUILD_DIR}/installer/

if [ "${arch_name}" = "aarch64" ]; then
    mv "${BUILD_DIR}/installer/${APP_NAME}_${APP_VERSION}_arm64.deb" "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}_${APP_VERSION}_arm64.deb"
    sha256sum "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}_${APP_VERSION}_arm64.deb" > "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}_${APP_VERSION}-1_arm64.deb.sha256"
    mv "${BUILD_DIR}/installer/${APP_NAME}-${APP_VERSION}-1.aarch64.rpm"  "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}-1.aarch64.rpm"
    sha256sum "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}-1.aarch64.rpm" > "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}-1.aarch64.rpm.sha256"
else
    mv "${BUILD_DIR}/installer/${APP_NAME}_${APP_VERSION}_amd64.deb" "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}_${APP_VERSION}_amd64.deb"
    sha256sum "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}_${APP_VERSION}_amd64.deb" > "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}_${APP_VERSION}_amd64.deb.sha256"
    mv "${BUILD_DIR}/installer/${APP_NAME}-${APP_VERSION}-1.x86_64.rpm" "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}-1.x86_64.rpm"
    sha256sum "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}-1.x86_64.rpm" > "${BUILD_DIR}/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}-1.x86_64.rpm.sha256"
fi


