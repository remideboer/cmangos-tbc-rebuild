@echo off
cd /d "%~dp0"
if not defined JAVA_HOME set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

start "TBC Auth" cmd /k java -jar tbc-auth\target\tbc-auth-0.1.0-SNAPSHOT.jar conf\local-realmd.conf
start "TBC World" cmd /k java -jar tbc-world\target\tbc-world-0.1.0-SNAPSHOT.jar conf\local-mangosd.conf
start "TBC Admin" cmd /k java -jar tbc-admin\target\tbc-admin-0.1.0-SNAPSHOT.jar conf\local-realmd.conf
start "TBC Editor" cmd /k java -jar tbc-editor\target\tbc-editor-0.1.0-SNAPSHOT.jar conf\local-mangosd.conf
