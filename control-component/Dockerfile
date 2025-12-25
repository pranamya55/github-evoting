ARG DOCKER_REGISTRY
ARG PROJECT_VERSION
FROM ${DOCKER_REGISTRY}e-voting/base-java:${PROJECT_VERSION}

ENV TZ Europe/Zurich
ENV SPRING_RABBITMQ_SSL_ENABLED=true
ENV LD_LIBRARY_PATH=/home/evote/libs://${LD_LIBRARY_PATH}

COPY ./target/*-runnable.jar /home/evote/control-component.jar

RUN chown -R evote:evote /home/evote/
USER evote
CMD java -Dloader.path="libs" -jar control-component.jar

HEALTHCHECK --timeout=3s --retries=4 CMD curl --noproxy localhost -fk http://localhost:${SERVER_PORT}/actuator/health | grep UP || exit 1
