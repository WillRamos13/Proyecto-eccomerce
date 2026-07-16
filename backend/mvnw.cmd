@echo off
setlocal EnableExtensions
set "BASE_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "DIST_NAME=apache-maven-%MAVEN_VERSION%"
if defined MAVEN_USER_HOME (
  set "CACHE_BASE=%MAVEN_USER_HOME%\wrapper\dists\fastmarket-%MAVEN_VERSION%"
) else (
  set "CACHE_BASE=%USERPROFILE%\.m2\wrapper\dists\fastmarket-%MAVEN_VERSION%"
)
set "MAVEN_HOME=%CACHE_BASE%\%DIST_NAME%"
set "ZIP_FILE=%CACHE_BASE%\%DIST_NAME%-bin.zip"
set "DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/%DIST_NAME%-bin.zip"

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  cd /d "%BASE_DIR%"
  mvn %*
  exit /b %ERRORLEVEL%
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%CACHE_BASE%" mkdir "%CACHE_BASE%"
  if not exist "%ZIP_FILE%" (
    echo Descargando Maven %MAVEN_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile '%ZIP_FILE%'"
    if ERRORLEVEL 1 exit /b 1
  )
  if exist "%MAVEN_HOME%" rmdir /s /q "%MAVEN_HOME%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%CACHE_BASE%' -Force"
  if ERRORLEVEL 1 exit /b 1
)

cd /d "%BASE_DIR%"
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
