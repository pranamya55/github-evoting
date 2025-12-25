#!/bin/bash
#
# (c) Copyright 2025 Swiss Post Ltd.
#
# Backend server startup and shutdown

# Get action "startup" or "shutdown"
if [[ "$1" != "startup" && "$1" != "shutdown" ]]; then
    echo "Missing action argument 'startup' or 'shutdown'"
    exit 1
fi

ACTION=$1
shift

# Set startup options
while getopts "p:d:w:i:" opt; do
  case ${opt} in
    p)
      PROFILE=true
      PROFILE_VALUE=$OPTARG
      ;;
    d)
      DEBUG_ENABLED=true
      DEBUG_PORT=$OPTARG
      ;;
    w)
      USE_WINDOW=true
      ;;
    i)
      PID=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" 1>&2
      exit 1
      ;;
  esac
done
shift $((OPTIND -1))

# Profile
if [[ "$PROFILE" != "true" ]]; then
    echo "Missing -p profile"
    exit 1
fi

# Local environment variables
CURRENT_DIRECTORY=$(pwd)
SDM_JAR="-jar ./secure-data-manager-backend-runnable.jar"
JAVA_BIN="embedded-jre/bin/java"
JVM_OPTIONS="-Xms20G -Xmx20G -Dmemory.chunk.size=1024000 -Dstorage.diskCache.bufferSize=7800"
SPRING_CONFIG_ADDITIONAL="--spring.config.additional-location=$CURRENT_DIRECTORY/"
SPRING_PROFILE="--spring.profiles.active=$PROFILE_VALUE"

# Debug
if [[ "$DEBUG_ENABLED" == "true" ]]; then
    SDM_DEBUG="-Djava.rmi.server.hostname=localhost -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"
fi

# Startup Spring Boot
STARTUP="$JAVA_BIN $JVM_OPTIONS $SDM_DEBUG $SDM_JAR $SPRING_CONFIG_ADDITIONAL $SPRING_PROFILE"

if [[ "$ACTION" == "startup" ]]; then
    if [[ "$USE_WINDOW" == "true" ]]; then
        xterm -e "$STARTUP" &
    else
        $STARTUP
    fi
elif [[ "$ACTION" == "shutdown" ]]; then
    # Shutdown Spring boot backend server
    kill -9 $PID
    if [[ "$USE_WINDOW" == "true" ]]; then
        pkill -f "xterm -e $STARTUP"
    fi
fi
