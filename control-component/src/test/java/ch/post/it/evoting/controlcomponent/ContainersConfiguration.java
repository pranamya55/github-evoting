/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import java.time.Duration;

import jakarta.jms.ConnectionFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
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

	@Bean
	@Primary
	public JmsTemplate jmsTemplate(
			final ConnectionFactory connectionFactory,
			final JmsProperties jmsProperties,
			final ObjectProvider<DestinationResolver> destinationResolver,
			final ObjectProvider<MessageConverter> messageConverter) {

		final PropertyMapper map = PropertyMapper.get();
		final JmsTemplate template = new JmsTemplate(connectionFactory);
		template.setPubSubDomain(jmsProperties.isPubSubDomain());
		map.from(destinationResolver::getIfUnique).whenNonNull().to(template::setDestinationResolver);
		map.from(messageConverter::getIfUnique).whenNonNull().to(template::setMessageConverter);
		mapTemplateProperties(jmsProperties.getTemplate(), template);
		return template;
	}

	@Bean(name = "multicastJmsTemplate")
	public JmsTemplate multicastJmsTemplate(final JmsTemplate jmsTemplate) {
		final JmsTemplate multicastJmsTemplate = new JmsTemplate();
		BeanUtils.copyProperties(jmsTemplate, multicastJmsTemplate);
		multicastJmsTemplate.setPubSubDomain(true);
		return multicastJmsTemplate;
	}

	@Bean(name = "dlqListenerJmsTemplate")
	public JmsTemplate dlqListenerJmsTemplate(final JmsTemplate jmsTemplate) {
		final JmsTemplate dlqListenerJmsTemplate = new JmsTemplate();
		BeanUtils.copyProperties(jmsTemplate, dlqListenerJmsTemplate);
		final long dlqReceiveTimeout = jmsTemplate.getReceiveTimeout() * 2;
		dlqListenerJmsTemplate.setReceiveTimeout(dlqReceiveTimeout);
		return dlqListenerJmsTemplate;
	}

	// Taken from Spring autoconfiguration. See JmsAutoConfiguration.class.
	private void mapTemplateProperties(final JmsProperties.Template properties, final JmsTemplate template) {
		final PropertyMapper map = PropertyMapper.get();
		map.from(properties::getDefaultDestination).whenNonNull().to(template::setDefaultDestinationName);
		map.from(properties::getDeliveryDelay).whenNonNull().as(Duration::toMillis).to(template::setDeliveryDelay);
		map.from(properties::determineQosEnabled).to(template::setExplicitQosEnabled);
		map.from(properties::getDeliveryMode)
				.whenNonNull()
				.as(JmsProperties.DeliveryMode::getValue)
				.to(template::setDeliveryMode);
		map.from(properties::getPriority).whenNonNull().to(template::setPriority);
		map.from(properties::getTimeToLive).whenNonNull().as(Duration::toMillis).to(template::setTimeToLive);
		map.from(properties::getReceiveTimeout)
				.whenNonNull()
				.as(Duration::toMillis)
				.to(template::setReceiveTimeout);
	}

}
