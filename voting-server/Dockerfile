ARG DOCKER_REGISTRY
ARG PROJECT_VERSION
FROM ${DOCKER_REGISTRY}e-voting/base-java:${PROJECT_VERSION}

ENV TZ Europe/Zurich
ENV LD_LIBRARY_PATH=/home/evote/libs://${LD_LIBRARY_PATH}
ARG JAR_FILE=target/*-runnable.jar

COPY ${JAR_FILE} /home/evote/voting-server.jar

RUN chown -R evote:evote /home/evote/

USER evote
CMD java -Dloader.path="libs" -jar voting-server.jar

HEALTHCHECK --timeout=3s --retries=4 CMD curl --noproxy localhost -fk http://localhost:${SERVER_PORT}/vs-ws-rest/actuator/health | grep UP || exit 1
