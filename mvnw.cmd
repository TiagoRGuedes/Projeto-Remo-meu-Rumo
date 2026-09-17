@echo off
setlocal

set "BASE_DIR=%~dp0"
set "MAVEN_VERSION=3.9.11"
set "MAVEN_DIR=%BASE_DIR%.mvn\wrapper\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_DIR%\bin\mvn.cmd"
set "MAVEN_ZIP=%BASE_DIR%.mvn\wrapper\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_BIN%" (
  if not exist "%BASE_DIR%.mvn\wrapper" mkdir "%BASE_DIR%.mvn\wrapper"
  echo Baixando Apache Maven %MAVEN_VERSION% para o wrapper local...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'; Expand-Archive -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%BASE_DIR%.mvn\wrapper' -Force"
)

call "%MAVEN_BIN%" %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
