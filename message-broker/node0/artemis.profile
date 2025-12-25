ARTEMIS_HOME='/opt/activemq-artemis'
ARTEMIS_INSTANCE='/var/lib/artemis-instance'
ARTEMIS_DATA_DIR='/var/lib/artemis-instance/data'
ARTEMIS_ETC_DIR='/var/lib/artemis-instance/etc'
ARTEMIS_OOME_DUMP='/var/lib/artemis-instance/log/oom_dump.hprof'
ARTEMIS_INSTANCE_URI='file:/var/lib/artemis-instance/./'
ARTEMIS_INSTANCE_ETC_URI='file:/var/lib/artemis-instance/./etc/'
HAWTIO_ROLES='admin'

if [ -z "$JAVA_ARGS" ]; then
    JAVA_ARGS="-XX:AutoBoxCacheMax=20000 -XX:+PrintClassHistogram -XX:+UseG1GC -XX:+UseStringDeduplication -Xms512M -Xmx2G -Dhawtio.disableProxy=true -Dhawtio.realm=activemq -Dhawtio.offline=true -Dhawtio.rolePrincipalClasses=org.apache.activemq.artemis.spi.core.security.jaas.RolePrincipal -Dhawtio.http.strictTransportSecurity=max-age=31536000;includeSubDomains;preload -Djolokia.policyLocation=${ARTEMIS_INSTANCE_ETC_URI}jolokia-access.xml -Dlog4j2.disableJmx=true "
fi
