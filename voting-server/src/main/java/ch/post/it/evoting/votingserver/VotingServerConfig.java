/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver;

import java.time.Duration;
import java.util.Map;

import jakarta.jms.ConnectionFactory;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.symmetric.Symmetric;
import ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricFactory;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.mapper.CBORObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.Tenant;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantRoutingDataSource;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantRoutingSignatureKeystore;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.configuration.TenantProperties;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.votingserver.messaging.MessageErrorHandler;

import io.rsocket.frame.decoder.PayloadDecoder;
import reactor.core.publisher.Hooks;

@Configuration
@EnableJpaRepositories
public class VotingServerConfig {

	@Bean
	public ObjectMapper objectMapper() {
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		objectMapper.registerModule(new JavaTimeModule());
		return objectMapper;
	}

	@Bean
	public Hash hash() {
		return HashFactory.createHash();
	}

	@Bean
	public KeyDerivation keyDerivation() {
		return KeyDerivationFactory.createKeyDerivation();
	}

	@Bean
	public Base64 base64() {
		return BaseEncodingFactory.createBase64();
	}

	@Bean
	public Symmetric symmetric() {
		return SymmetricFactory.createSymmetric();
	}

	@Bean
	public Argon2 argon2() {
		return Argon2Factory.createArgon2(Argon2Profile.LESS_MEMORY);
	}

	@Bean
	public PrimesMappingTableAlgorithms primesMappingTableAlgorithms() {
		return new PrimesMappingTableAlgorithms();
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

	@Bean
	public JmsListenerContainerFactory<DefaultMessageListenerContainer> customFactory(
			final MessageErrorHandler errorHandler,
			final DefaultJmsListenerContainerFactoryConfigurer configurer,
			final ConnectionFactory connectionFactory) {

		final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setErrorHandler(errorHandler);

		return factory;
	}

	@Bean
	public JmsListenerContainerFactory<DefaultMessageListenerContainer> multicastConnectionFactory(
			final MessageErrorHandler errorHandler,
			final ConnectionFactory connectionFactory,
			final DefaultJmsListenerContainerFactoryConfigurer configurer) {

		final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		// The configurer has to be called before other factory changes.
		configurer.configure(factory, connectionFactory);
		factory.setPubSubDomain(true);
		factory.setErrorHandler(errorHandler);

		return factory;
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

	@Bean
	public TransactionManagerCustomizer<AbstractPlatformTransactionManager> transactionManagerCustomizer() {
		return transactionManager -> transactionManager.setValidateExistingTransaction(true);
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public RSocketStrategiesCustomizer rSocketStrategiesCustomizer() {
		final CBORMapper cborMapper = CBORObjectMapper.getNewInstance();

		final MediaType supportedMediaType = MediaType.APPLICATION_CBOR;

		return strategies -> strategies
				.encoder(new Jackson2CborEncoder(cborMapper, supportedMediaType))
				.decoder(new Jackson2CborDecoder(cborMapper, supportedMediaType));
	}

	@Bean
	public RSocketServerCustomizer rSocketServerCustomizer(
			@Value("${voting-server.rsocket.server.fragment-size}")
			final int fragmentSize) {

		return rSocketServer -> rSocketServer
				.fragment(fragmentSize)
				.payloadDecoder(PayloadDecoder.ZERO_COPY);
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.hikari")
	public HikariConfig hikariConfig() {
		// Use Hikari default configuration, but overridden by application properties.
		return new HikariConfig();
	}

	@Bean
	public TenantService tenantService(
			final HikariConfig hikariConfig,
			final TenantProperties tenantProperties) {

		return new TenantService(tenantProperties, (tenantId, property) -> {

			final HikariConfig tenantHikariConfig = new HikariConfig();
			hikariConfig.copyStateTo(tenantHikariConfig);
			tenantHikariConfig.setJdbcUrl(property.getUrl());
			tenantHikariConfig.setUsername(property.getUsername());
			tenantHikariConfig.setPassword(property.getPassword());
			tenantHikariConfig.setPoolName(hikariConfig().getPoolName() + "-" + tenantId);

			return new HikariDataSource(tenantHikariConfig);
		}, true);
	}

	@Bean
	public ContextHolder contextHolder(final TenantService tenantService) {
		return new ContextHolder(Map.of(ContextHolder.TENANT_ID, tenantService::existTenant), Hooks::enableAutomaticContextPropagation);
	}

	@Bean
	@ConfigurationProperties(prefix = "multitenancy")
	public TenantProperties tenantProperties() {
		return new TenantProperties();
	}

	@Bean
	public TenantRoutingDataSource tenantRoutingDataSource(final TenantService tenantService, final ContextHolder contextHolder) {
		return new TenantRoutingDataSource(contextHolder, tenantService) {
			@EventListener(ApplicationReadyEvent.class)
			private void applicationStarted() {
				setApplicationAsStarted();
			}
		};
	}

	@Bean
	public TenantRoutingSignatureKeystore tenantRoutingSignatureKeystore(final TenantService tenantService, final ContextHolder contextHolder) {
		return new TenantRoutingSignatureKeystore(contextHolder, tenantService, Alias.VOTING_SERVER);
	}

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy(final TenantService tenantService) {
		return flyway -> {
			for (final Tenant tenant : tenantService.getTenants()) {
				Flyway.configure().configuration(flyway.getConfiguration())
						.dataSource(tenant.dataSource())
						.load()
						.migrate();
			}
		};
	}
}
