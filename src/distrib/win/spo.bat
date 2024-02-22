set profile="default"

if not "%1"=="" (
    set profile=%1
)

.\java-runtime\bin\java ^
  -Xms2g ^
  -XX:+UseZGC ^
  --enable-preview ^
  -Xverify:none ^
  -XX:TieredStopAtLevel=1 ^
  -Dspring.profiles.active=%profile% ^
  -Dspring.jmx.enabled=false ^
  -Dspring.config.location=classpath:/application.yml ^
  -jar spo-full.jar
