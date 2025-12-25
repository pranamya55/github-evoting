ARG DOCKER_REGISTRY
ARG PROJECT_VERSION
FROM ${DOCKER_REGISTRY}e-voting/base-apache:${PROJECT_VERSION}

# The context is assumed to be target/dist/apps/landing-page
COPY . ./htdocs/

HEALTHCHECK --timeout=3s --retries=4 CMD curl --noproxy localhost -f http://localhost:${APACHE_HTTP_PORT}/ || exit 1
