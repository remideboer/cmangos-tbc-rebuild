@echo off
setlocal EnableExtensions EnableDelayedExpansion
rem Mount compiled overlay as stock-8606 enUS\patch-enUS-3.MPQ (client does not open custom names).
rem Usage: install-client-patch.bat [path\to\compiled.MPQ]

set "SRC_MPQ=%~1"
if not defined SRC_MPQ set "SRC_MPQ=%~dp0out\patch-tbc-custom.MPQ"

if not exist "%SRC_MPQ%" (
  echo content: compiled MPQ missing - %SRC_MPQ%
  exit /b 1
)

set "CLIENT_DATA=%CONTENT_CLIENT_DATA%"
if not defined CLIENT_DATA if exist "%~dp0..\..\WoW-2.4.3-client\Data\" (
  set "CLIENT_DATA=%~dp0..\..\WoW-2.4.3-client\Data"
)
if not defined CLIENT_DATA (
  echo content: WoW-2.4.3-client\Data not found - set CONTENT_CLIENT_DATA or use in-repo lab client
  exit /b 1
)

rem Normalize trailing slash
if "%CLIENT_DATA:~-1%"=="\" set "CLIENT_DATA=%CLIENT_DATA:~0,-1%"
if not exist "%CLIENT_DATA%\" (
  echo content: client Data folder missing - %CLIENT_DATA%
  exit /b 1
)

set "CLIENT_ROOT=%CLIENT_DATA%\.."
set "LOCALE_DIR=%CLIENT_DATA%\enUS"
set "MOUNT_NAME=patch-enUS-3.MPQ"
set "DST_MPQ=%LOCALE_DIR%\%MOUNT_NAME%"

if not exist "%LOCALE_DIR%\" (
  mkdir "%LOCALE_DIR%"
  if errorlevel 1 (
    echo content: failed to create %LOCALE_DIR%
    exit /b 1
  )
)

if exist "%DST_MPQ%" (
  for /f %%T in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMddHHmmss"') do set "TS=%%T"
  copy /y "%DST_MPQ%" "%DST_MPQ%.bak.!TS!" >nul
  if errorlevel 1 (
    echo content: failed to backup existing %MOUNT_NAME%
    exit /b 1
  )
  echo content: backed up existing %MOUNT_NAME% to %MOUNT_NAME%.bak.!TS!
)

copy /y "%SRC_MPQ%" "%DST_MPQ%" >nul
if errorlevel 1 (
  echo content: failed to copy patch MPQ to %DST_MPQ%
  exit /b 1
)
echo content: installed %DST_MPQ%

rem Stale custom name is never opened by stock wow.exe - remove if present.
if exist "%CLIENT_DATA%\patch-tbc-custom.MPQ" (
  del /f /q "%CLIENT_DATA%\patch-tbc-custom.MPQ"
  echo content: removed unused Data\patch-tbc-custom.MPQ
)

call :clear_dir "%CLIENT_ROOT%\Cache" Cache
call :clear_dir "%CLIENT_ROOT%\WDB" WDB

echo content: NOTE - stock Wow.exe rejects custom Spell.dbc with ERROR #131 ^(signature/CRC^).
echo content: use an unsigned/CRC-patched 8606 client ^(e.g. wowme.exe^) or delete %MOUNT_NAME% to boot.
echo content: fully quit and restart the unsigned 8606 client to load the patch.
exit /b 0

:clear_dir
set "DIR=%~1"
set "LABEL=%~2"
if not exist "%DIR%\" goto :eof
echo content: clearing client %LABEL% ...
rd /s /q "%DIR%"
if exist "%DIR%\" (
  echo content: warning - could not fully remove %DIR% ^(client may be running^)
  goto :eof
)
mkdir "%DIR%" >nul 2>&1
goto :eof
