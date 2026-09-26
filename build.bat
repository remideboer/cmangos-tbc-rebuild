@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if not defined JAVA_HOME set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JAVA_HOME is not a JDK: %JAVA_HOME%
  exit /b 1
)

set "MVN="
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MVN=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MVN if exist "%USERPROFILE%\apache-maven-3.9.11\bin\mvn.cmd" set "MVN=%USERPROFILE%\apache-maven-3.9.11\bin\mvn.cmd"
if not defined MVN (
  where mvn.cmd >nul 2>&1
  if not errorlevel 1 set "MVN=mvn.cmd"
)
if not defined MVN (
  echo Maven 3.9+ not found. Set MAVEN_HOME or add mvn.cmd to PATH.
  exit /b 1
)

echo JAVA_HOME=%JAVA_HOME%
echo MVN=%MVN%
call "%MVN%" -f "%~dp0pom.xml" package %*
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

if not exist "tbc-auth\target\tbc-auth-0.1.0-SNAPSHOT.jar" goto :missing
if not exist "tbc-world\target\tbc-world-0.1.0-SNAPSHOT.jar" goto :missing
if not exist "tbc-admin\target\tbc-admin-0.1.0-SNAPSHOT.jar" goto :missing
if not exist "tbc-editor\target\tbc-editor-0.1.0-SNAPSHOT.jar" goto :missing
if not exist "tbc-launcher\target\tbc-launcher-0.1.0-SNAPSHOT.jar" goto :missing
if not exist "TbcLauncher.exe" goto :missing

echo.
echo Shaded jars (launcher starts these paths under this folder):
for %%J in (
  "tbc-auth\target\tbc-auth-0.1.0-SNAPSHOT.jar"
  "tbc-world\target\tbc-world-0.1.0-SNAPSHOT.jar"
  "tbc-admin\target\tbc-admin-0.1.0-SNAPSHOT.jar"
  "tbc-editor\target\tbc-editor-0.1.0-SNAPSHOT.jar"
  "tbc-launcher\target\tbc-launcher-0.1.0-SNAPSHOT.jar"
) do (
  echo   %%~J
  echo     %%~tJ  %%~zJ bytes
)
echo.
echo Jars ready. Run TbcLauncher.exe or start.bat from this directory ^(cwd must be tbc-server^).
echo Note: mvn test alone does NOT refresh these jars — always use build.bat / package.
exit /b 0

:missing
echo Package succeeded but a shaded jar is missing under target\.
exit /b 1
