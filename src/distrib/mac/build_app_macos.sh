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

rm -rfd ./build/java-runtime/
rm -rfd build/installer/

mkdir -p build/installer/input/libs/

cp dependencies/BOOT-INF/lib/* build/installer/input/libs/
cp "${MAIN_JAR}" build/installer/input/libs/

"${JAVA_HOME}"/bin/java --version

# ------ REQUIRED MODULES -----------------------------------------------------
# Use jlink to detect all modules that are required to run the application.
# Starting point for the jdep analysis is the set of jars being used by the
# application.

echo "detecting required modules"
detected_modules=$("$JAVA_HOME"/bin/jdeps \
  --multi-release "${JAVA_VERSION}" \
  --ignore-missing-deps \
  --print-module-deps \
  --class-path "build/installer/input/libs/*" \
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

manual_modules=jdk.crypto.ec,jdk.localedata
echo "manual modules: ${manual_modules}"

# ------ RUNTIME IMAGE --------------------------------------------------------
# Use the jlink tool to create a runtime image for our application. We are
# doing this is a separate step instead of letting jlink do the work as part
# of the jpackage tool. This approach allows for finer configuration and also
# works with dependencies that are not fully modularized, yet.

echo "creating java runtime image"
"${JAVA_HOME}"/bin/jlink \
  --no-header-files \
  --no-man-pages  \
  --compress=zip-6  \
  --strip-debug \
  --add-modules "${detected_modules},${manual_modules}" \
  --include-locales=en,fr \
  --output build/java-runtime

# ------ PACKAGING ------------------------------------------------------------
# A loop iterates over the various packaging types supported by jpackage. In
# the end we will find all packages inside the build/installer directory.

# Somehow before signing there needs to be another step: xattr -cr build/installer/JDKMon.app
uname -p
arch_name="$(uname -m)"
echo "Arch: ${arch_name}"

if [ "${arch_name}" = "x86_64" ]; then
    if [ "$(sysctl -in sysctl.proc_translated)" = "1" ]; then
        echo "Running on Rosetta 2"
    else
        echo "Running on native Intel"
    fi
elif [ "${arch_name}" = "arm64" ]; then
    echo "Running on ARM"
else
    echo "Unknown architecture: ${arch_name}"
fi

#for type in "app-image" "dmg" "pkg"
for type in "dmg" "pkg"
do
  echo "Creating installer of type ... $type"

  ${JAVA_HOME}/bin/jpackage \
  --type $type \
  --dest build/installer \
  --input build/installer/input/libs \
  --name "${APP_NAME}" \
  --main-class "${APP_MAIN_CLASS}" \
  --main-jar "${MAIN_JAR}" \
  --java-options -XX:+UseZGC \
  --java-options -Xms2g \
  --java-options -Xverify:none \
  --java-options '--enable-preview' \
  --java-options "-Dspring.profiles.active=default" \
  --java-options "-Dspring.config.location=classpath:/application.yml" \
  --java-options "-Dspring.jmx.enabled=false" \
  --java-options "--add-opens javafx.controls/javafx.scene.control=ALL-UNNAMED" \
  --java-options "--add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED" \
  --runtime-image build/java-runtime \
  --icon src/distrib/mac/spo.icns \
  --app-version "${APP_VERSION}" \
  --vendor "${APP_VENDOR}" \
  --copyright "Copyright © 2023 ${APP_VENDOR}" \
  --license-file LICENSE.txt \
  --description "${APP_DESC}" \
  --mac-package-name "${SPO_ARTIFACT_ID}"

    mv "build/installer/${APP_NAME}-${APP_VERSION}.${type}" "build/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}.${type}"
    if [ "${arch_name}" = "arm64" ]; then
      mv "build/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}.${type}" "build/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}-aarch64.${type}"
      shasum -a 256 "build/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}-aarch64.${type}" > "build/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}-aarch64.${type}.sha256"
    else
      shasum -a 256 "build/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}.${type}" > "build/installer/${SPO_ARTIFACT_ID}-${APP_VERSION}.${type}.sha256"
    fi
done
