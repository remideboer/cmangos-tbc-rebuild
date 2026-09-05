@echo off
cd /d "%~dp0"
if not defined JAVA_HOME set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JAVA_HOME is not a JDK: %JAVA_HOME%
  exit /b 1
)

set "JAR=tbc-editor\target\tbc-editor-0.1.0-SNAPSHOT.jar"
if not exist "%JAR%" (
  echo Missing %JAR%. Run build.bat first.
  exit /b 1
)

java -jar "%JAR%" conf\local-mangosd.conf
