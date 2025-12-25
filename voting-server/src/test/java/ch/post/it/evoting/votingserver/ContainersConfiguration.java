/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PostgreSQLContainer;

import ch.post.it.evoting.domain.multitenancy.TenantConstants;

@Configuration
public class ContainersConfiguration {

	@Bean
	@ServiceConnection
	public PostgreSQLContainer<?> postgreSQLContainer() {
		try (final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:17.6")) {
			postgreSQLContainer.start();
			return postgreSQLContainer;
		}
	}

	@Bean
	DynamicPropertyRegistrar apiPropertiesRegistrar(final PostgreSQLContainer<?> postgreSQLContainer) {
		return registry -> {
			registry.add("multitenancy.tenants." + TenantConstants.TEST_TENANT_ID + ".datasource.url", postgreSQLContainer::getJdbcUrl);
			registry.add("multitenancy.tenants." + TenantConstants.TEST_TENANT_ID + ".datasource.username", postgreSQLContainer::getUsername);
			registry.add("multitenancy.tenants." + TenantConstants.TEST_TENANT_ID + ".datasource.password", postgreSQLContainer::getPassword);
		};
	}
}
