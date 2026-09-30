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
echo Note: mvn test alone does NOT refresh these jars - always use build.bat / package.

rem --- optional content compile (YAML → DBC + patch MPQ); skipped without base DBC ---
call :content_compile
exit /b 0

:missing
echo Package succeeded but a shaded jar is missing under target\.
exit /b 1

:content_compile
set "CONTENT_JAR=tbc-content\target\tbc-content-0.1.0-SNAPSHOT.jar"
if not exist "%CONTENT_JAR%" (
  echo content: tbc-content jar missing - skip
  goto :eof
)
set "BASE_DBC=%CONTENT_BASE_DBC%"
if not defined BASE_DBC if exist "conf\local-mangosd.conf" (
  for /f "usebackq tokens=1,* delims==" %%A in (`findstr /i /b /c:"DataDir" "conf\local-mangosd.conf"`) do (
    set "DD=%%B"
  )
)
if not defined BASE_DBC if defined DD (
  rem Strip quotes/spaces; normalize / → \ so "if exist" sees DataDir\dbc\Spell.dbc
  set "DD=%DD:"=%"
  for /f "tokens=* delims= " %%Z in ("%DD%") do set "DD=%%Z"
  set "DD=%DD:/=\%"
  if exist "%DD%\dbc\Spell.dbc" set "BASE_DBC=%DD%\dbc"
)
if not defined BASE_DBC (
  echo content: no CONTENT_BASE_DBC / DataDir\dbc - skip content compile
  goto :eof
)
if not exist "%BASE_DBC%\Spell.dbc" (
  echo content: Spell.dbc not found under %BASE_DBC% - skip
  goto :eof
)
echo content: compiling YAML deltas with base %BASE_DBC%
set "CONTENT_OUT=%~dp0content\out"
set "MPQ_NAME=patch-tbc-custom.MPQ"
"%JAVA_HOME%\bin\java.exe" -jar "%CONTENT_JAR%" compile --content "%~dp0content" --base-dbc "%BASE_DBC%" --out "%CONTENT_OUT%" --mpq-name %MPQ_NAME%
if errorlevel 1 (
  echo content compile failed.
  exit /b 1
)
call :install_patch_mpq
goto :eof

:install_patch_mpq
rem After a successful content compile the overlay is patchable — install into the lab client.
rem Opt out: CONTENT_INSTALL_PATCH=0. Soft-skip when no client Data (artifact still in content\out).
set "SRC_MPQ=%CONTENT_OUT%\%MPQ_NAME%"
if not exist "%SRC_MPQ%" (
  echo content: compiled MPQ missing - %SRC_MPQ%
  exit /b 1
)
if /i "%CONTENT_INSTALL_PATCH%"=="0" (
  echo content: skip MPQ install ^(CONTENT_INSTALL_PATCH=0^)
  echo content: artifact ready: %SRC_MPQ%
  goto :eof
)
set "LAB_DATA=%CONTENT_CLIENT_DATA%"
if not defined LAB_DATA if exist "%~dp0..\WoW-2.4.3-client\Data\" set "LAB_DATA=%~dp0..\WoW-2.4.3-client\Data"
if not defined LAB_DATA (
  echo content: no lab client Data - skip install; artifact ready: %SRC_MPQ%
  echo content: set CONTENT_CLIENT_DATA or use in-repo WoW-2.4.3-client
  goto :eof
)
rem Mount as stock-8606 enUS\patch-enUS-3.MPQ + clear Cache/WDB (see content\install-client-patch.bat).
call "%~dp0content\install-client-patch.bat" "%SRC_MPQ%"
if errorlevel 1 exit /b 1
goto :eof
