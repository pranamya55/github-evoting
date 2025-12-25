/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import java.util.Map;

import jakarta.jms.ConnectionFactory;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.mixnet.Mixnet;
import ch.post.it.evoting.cryptoprimitives.mixnet.MixnetFactory;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.Tenant;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantRoutingDataSource;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantRoutingSignatureKeystore;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.configuration.TenantProperties;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyKeyGenerationSchnorrProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsAlgorithm;

import reactor.core.publisher.Hooks;

@Configuration
public class ControlComponentsConfig {

	@Bean
	public Hash hash() {
		return HashFactory.createHash();
	}

	@Bean
	public Base64 base64() {
		return BaseEncodingFactory.createBase64();
	}

	@Bean
	public ZeroKnowledgeProof zeroKnowledgeProof() {
		return ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
	}

	@Bean
	public ElGamal elGamal() {
		return ElGamalFactory.createElGamal();
	}

	@Bean
	public Random randomService() {
		return RandomFactory.createRandom();
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	Mixnet mixnet() {
		return MixnetFactory.createMixnet();
	}

	@Bean
	ObjectMapper objectMapper() {
		return DomainObjectMapper.getNewInstance();
	}

	@Bean
	KeyDerivation keyDerivation() {
		return KeyDerivationFactory.createKeyDerivation();
	}

	@Bean
	GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm(final Hash hash, final Base64 base64, final ElGamal elGamal) {
		return new GetMixnetInitialCiphertextsAlgorithm(hash, base64, elGamal);
	}

	@Bean
	PrimesMappingTableAlgorithms primesMappingTableAlgorithms() {
		return new PrimesMappingTableAlgorithms();
	}

	@Bean
	GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm(final Base64 base64, final Hash hash) {
		return new GetHashElectionEventContextAlgorithm(base64, hash);
	}

	@Bean
	GetHashContextAlgorithm getHashContextAlgorithm(final Base64 base64, final Hash hash,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		return new GetHashContextAlgorithm(base64, hash, primesMappingTableAlgorithms);
	}

	@Bean
	GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm(final Base64 base64, final Hash hash) {
		return new GetHashExtractedElectionEventAlgorithm(base64, hash);
	}

	@Bean
	VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm(final ZeroKnowledgeProof zeroKnowledgeProof) {
		return new VerifyCCSchnorrProofsAlgorithm(zeroKnowledgeProof);
	}

	@Bean
	VerifyKeyGenerationSchnorrProofsAlgorithm verifyKeyGenerationSchnorrProofsAlgorithm(
			final VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm,
			final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm) {
		return new VerifyKeyGenerationSchnorrProofsAlgorithm(verifyCCSchnorrProofsAlgorithm, getHashElectionEventContextAlgorithm);
	}

	@Bean
	@Profile("dispute-resolution")
	ConfirmVoteAgreementAlgorithm confirmVoteAgreementAlgorithm(final Hash hash, final Base64 base64) {
		return new ConfirmVoteAgreementAlgorithm(hash, base64);
	}

	@Bean
	public JmsListenerContainerFactory<DefaultMessageListenerContainer> customFactory(
			final MessageErrorHandler errorHandler,
			final DefaultJmsListenerContainerFactoryConfigurer configurer,
			final ConnectionFactory connectionFactory) {

		final DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setConnectionFactory(connectionFactory);
		factory.setErrorHandler(errorHandler);

		return factory;
	}

	@Bean
	public TransactionManagerCustomizer<AbstractPlatformTransactionManager> transactionManagerCustomizer() {
		return transactionManager -> transactionManager.setValidateExistingTransaction(true);
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
	public TenantRoutingSignatureKeystore tenantRoutingSignatureKeystore(
			final TenantService tenantService,
			final ContextHolder contextHolder,
			@Value("${nodeID}")
			final int nodeId) {
		return new TenantRoutingSignatureKeystore(contextHolder, tenantService, Alias.getControlComponentByNodeId(nodeId));
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
