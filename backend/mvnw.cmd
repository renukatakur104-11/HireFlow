@echo off
setlocal
set "DIR=%~dp0"
set "MAVEN_OPTS=-Djava.net.preferIPv4Stack=true %MAVEN_OPTS%"
if exist "%DIR%.mvn\apache-maven-3.9.9\bin\mvn.cmd" (
    call "%DIR%.mvn\apache-maven-3.9.9\bin\mvn.cmd" %*
) else (
    echo Maven binary not found in %DIR%.mvn\apache-maven-3.9.9 >&2
    exit /B 1
)
