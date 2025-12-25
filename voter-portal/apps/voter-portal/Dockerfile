ARG DOCKER_REGISTRY
ARG PROJECT_VERSION
FROM ${DOCKER_REGISTRY}e-voting/base-apache:${PROJECT_VERSION}

ARG SECOND_FACTOR
RUN mkdir -p ./htdocs/vote/
# The context is assumed to be target/dist/apps/voter-portal
COPY . ./htdocs/vote/

HEALTHCHECK --timeout=3s --retries=4 CMD curl --noproxy localhost -f http://localhost:${APACHE_HTTP_PORT}/vote/ || exit 1
