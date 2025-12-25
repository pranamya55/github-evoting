@echo off
rem Backend server startup and shutdown

setlocal

REM Get action "startup" or "shutdown"
if not "%1" == "startup" if not "%1" == "shutdown" goto errorAction
if "%1" == "startup" goto startupAction
if "%1" == "shutdown" goto shutdownAction

REM Action error handling
:errorAction
echo Missing action argument "startup" or "shutdown"
goto :end


REM Startup configuration
:startupAction

REM Loop through startup arguments
:setStartupOptions
if "%2" == "-p" set "PROFILE=true" & set "PROFILE_VALUE=%3" & shift & shift & goto :setStartupOptions
if "%2" == "-d" set "DEBUG_ENABLED=true" & set "DEBUG_PORT=%3" & shift & shift & goto :setStartupOptions
if "%2" == "-w" set "USE_WINDOW=true" & shift & goto :setStartupOptions
if "%2" == "-proxy" set PROXY_CONF=%~3 & shift & shift & goto :setStartupOptions

REM Profile argument is mandatory
if not "%PROFILE%" == "true" echo Missing "-p [profile]" & goto :end

REM Java configuration
set "JAVA_BIN=embedded-jre\bin\java"
set "JVM_OPTIONS=-Xms20G -Xmx20G -Dmemory.chunk.size=1024000 -Dstorage.diskCache.bufferSize=7800"

REM Debug
if "%DEBUG_ENABLED%" == "true" set "SDM_DEBUG=-Djava.rmi.server.hostname=localhost -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%DEBUG_PORT%"

REM Spring Boot runnable jar
set "SDM_JAR=-jar ./secure-data-manager-backend-runnable.jar"

REM Spring Boot configuration
set "SPRING_CONFIG_ADDITIONAL=--spring.config.additional-location=%cd%\"
set "SPRING_PROFILE=--spring.profiles.active=%PROFILE_VALUE%"

REM Build startup command
set "STARTUP=%JAVA_BIN% %JVM_OPTIONS% %PROXY_CONF% %SDM_DEBUG% %SDM_JAR% %SPRING_CONFIG_ADDITIONAL% %SPRING_PROFILE%"

if "%DEBUG_ENABLED%" == "true" (
    echo %STARTUP% > startup-command.txt
)

REM Start Spring boot backend server
if "%USE_WINDOW%" == "true" (
	start "sdm-backend-service" %STARTUP%
) else (
	%STARTUP%
)
goto :end


REM Shutdown configuration
:shutdownAction

REM Loop through arguments
:setShutdownOptions
if "%2" == "-i" set "PID=%3" & shift & shift & goto :setShutdownOptions
if "%2" == "-w" set "USE_WINDOW=true" & shift & goto :setShutdownOptions

rem Shutdown Spring boot backend server
taskkill /PID %PID% /T /F
if "%USE_WINDOW%" == "true" (
	taskkill /FI "WindowTitle eq sdm-backend-service*" /T /F
)
goto :end

:end