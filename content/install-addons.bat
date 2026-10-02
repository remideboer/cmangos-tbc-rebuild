@echo off
setlocal EnableExtensions EnableDelayedExpansion
rem Copy content\addons\* into the lab client's Interface\AddOns.
rem Usage: install-addons.bat

set "SRC_ADDONS=%~dp0addons"
if not exist "%SRC_ADDONS%\" (
  echo content: no addons folder - %SRC_ADDONS%
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

if "%CLIENT_DATA:~-1%"=="\" set "CLIENT_DATA=%CLIENT_DATA:~0,-1%"
set "CLIENT_ROOT=%CLIENT_DATA%\.."
set "DST_ADDONS=%CLIENT_ROOT%\Interface\AddOns"

if not exist "%DST_ADDONS%\" (
  mkdir "%DST_ADDONS%"
  if errorlevel 1 (
    echo content: failed to create %DST_ADDONS%
    exit /b 1
  )
)

set "COPIED=0"
for /d %%D in ("%SRC_ADDONS%\*") do (
  set "NAME=%%~nxD"
  set "DST=%DST_ADDONS%\!NAME!"
  if exist "!DST!\" (
    echo content: updating AddOn !NAME!
    rd /s /q "!DST!"
  ) else (
    echo content: installing AddOn !NAME!
  )
  mkdir "!DST!"
  xcopy /e /i /y /q "%%D\*" "!DST!\" >nul
  if errorlevel 1 (
    echo content: failed to copy !NAME!
    exit /b 1
  )
  set /a COPIED+=1
)

if "!COPIED!"=="0" (
  echo content: no addon directories under %SRC_ADDONS%
  exit /b 1
)

echo content: installed !COPIED! AddOn(s) into %DST_ADDONS%
echo content: enable in character AddOns list, then /reload ^(FrameXML untouched^).
exit /b 0
