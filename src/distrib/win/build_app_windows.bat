@ECHO OFF

set APP_VERSION=%SPO_VERSION%
rem set APP_NAME=%SPO_ARTIFACT_ID%

echo "java home: %JAVA_HOME%"
echo "App groupId: %SPO_GROUP_ID%"
echo "App artifactId: %SPO_ARTIFACT_ID%"
echo "App version: %APP_VERSION%"
echo "main JAR file: %MAIN_JAR%"
echo "App name: %APP_NAME%"
echo "App classname: %APP_MAIN_CLASS%"
echo "App description: %APP_DESC%"
echo "App vendor: %APP_VENDOR%"

rem ------ SETUP DIRECTORIES AND FILES ----------------------------------------
rem Remove previously generated java runtime and installers. Copy all required
rem jar files into the input/libs folder.

IF EXIST build\java-runtime rmdir /S /Q  .\build\java-runtime
IF EXIST build\installer rmdir /S /Q target\installer

xcopy /S /Q dependencies\BOOT-INF\lib\* build\installer\input\libs\
copy %MAIN_JAR% build\installer\input\libs\

rem ------ REQUIRED MODULES ---------------------------------------------------
rem Use jlink to detect all modules that are required to run the application.
rem Starting point for the jdep analysis is the set of jars being used by the
rem application.

echo detecting required modules

"%JAVA_HOME%\bin\jdeps" ^
  --multi-release %JAVA_VERSION% ^
  --ignore-missing-deps ^
  --class-path "build\installer\input\libs\*" ^
  --print-module-deps ^
  %MAIN_JAR% > temp.txt

set /p detected_modules=<temp.txt

echo detected modules: %detected_modules%

rem ------ MANUAL MODULES -----------------------------------------------------
rem jdk.crypto.ec has to be added manually bound via --bind-services or
rem otherwise HTTPS does not work.
rem
rem See: https://bugs.openjdk.java.net/browse/JDK-8221674

set manual_modules=jdk.crypto.ec,jdk.localedata
echo manual modules: %manual_modules%

rem ------ RUNTIME IMAGE ------------------------------------------------------
rem Use the jlink tool to create a runtime image for our application. We are
rem doing this is a separate step instead of letting jlink do the work as part
rem of the jpackage tool. This approach allows for finer configuration and also
rem works with dependencies that are not fully modularized, yet.

echo creating java runtime image

call "%JAVA_HOME%\bin\jlink" ^
  --no-header-files ^
  --no-man-pages ^
  --compress=zip-6 ^
  --strip-debug ^
  --add-modules %detected_modules%,%manual_modules% ^
  --include-locales=en,fr ^
  --output build\java-runtime


rem ------ PACKAGING ----------------------------------------------------------
rem A loop iterates over the various packaging types supported by jpackage. In
rem the end we will find all packages inside the target/installer directory.

echo create package using jpackage

for %%s in ("msi" "exe") do call "%JAVA_HOME%\bin\jpackage" ^
  --type %%s ^
  --dest build\installer ^
  --input build\installer\input\libs ^
  --name "%APP_NAME%" ^
  --main-class "%APP_MAIN_CLASS%" ^
  --main-jar "%MAIN_JAR%"" ^
  --java-options -XX:+UseZGC ^
  --java-options -Xms2g ^
  --java-options '--enable-preview' ^
  --runtime-image build\java-runtime ^
  --icon src\distrib\win\spo.ico ^
  --win-shortcut ^
  --win-per-user-install ^
  --win-menu ^
  --win-menu-group "%APP_VENDOR%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "%APP_VENDOR%" ^
  --copyright "Copyright © 2023 %APP_VENDOR%" ^
  --description "%APP_DESC%" ^


rem ------ CHECKSUM FILE ------------------------------------------------------
move "build\installer\%APP_NAME%-%APP_VERSION%.msi" "build\installer\%SPO_ARTIFACT_ID%-%APP_VERSION%.msi"
move "build\installer\%APP_NAME%-%APP_VERSION%.exe" "build\installer\%SPO_ARTIFACT_ID%-%APP_VERSION%.exe"
certutil -hashfile "build\installer\%SPO_ARTIFACT_ID%-%APP_VERSION%.msi" SHA256 > "build\installer\%SPO_ARTIFACT_ID%-%APP_VERSION%.msi.sha256"
certutil -hashfile "build\installer\%SPO_ARTIFACT_ID%-%APP_VERSION%.exe" SHA256 > "build\installer\%SPO_ARTIFACT_ID%-%APP_VERSION%.exe.sha256"
