/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.evotinglibraries.direct.trust.KeystoreFilesCreator;
import ch.post.it.evoting.evotinglibraries.direct.trust.SignatureKeystoreFactory;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.securedatamanager.shared.KeystoreRepository;

/**
 * Validate that the SDM context can be booted with a combination of direct trust key stores.
 */
class DirectTrustConfigurationTest {

	@TempDir
	static Path tempKeystorePath;
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(new Initializer())
			.withUserConfiguration(InternalTestConfiguration.class);

	@Test
	void sdmConfigOnly() {
		contextRunner
				.withPropertyValues("role.isSetup=true")
				.run(context -> assertAll(
						() -> assertTrue(context.containsBean("signatureKeystoreService")),
						() -> assertDoesNotThrow(() -> context.getBean(SignatureKeystore.class)))
				);
	}

	@Test
	void sdmTally() {
		contextRunner
				.withPropertyValues("role.isTally=true")
				.run(context -> assertAll(
						() -> assertTrue(context.containsBean("signatureKeystoreService")),
						() -> assertDoesNotThrow(() -> context.getBean(SignatureKeystore.class)))
				);
	}

	@Test
	void sdmTallyAndConfig() {
		contextRunner
				.withPropertyValues("role.isSetup=true")
				.withPropertyValues("role.isTally=true")
				.run(context -> assertThrows(IllegalStateException.class, () -> context.getBean(SignatureKeystore.class)));
	}

	@Test
	void sdmNoTallyAndNoConfig() {
		contextRunner
				.run(context -> assertAll(
						() -> assertFalse(context.containsBean("signatureKeystoreService")),
						() -> assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(SignatureKeystore.class)))
				);
	}

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(final ConfigurableApplicationContext applicationContext) {
			createSdmKeystore("config", applicationContext, Alias.SDM_CONFIG);
			createSdmKeystore("tally", applicationContext, Alias.SDM_TALLY);
		}

		private void createSdmKeystore(final String directoryName, final ConfigurableApplicationContext applicationContext, final Alias alias) {
			try {
				Files.createDirectories(tempKeystorePath.resolve(directoryName));
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}

			final String keystoreLocation = tempKeystorePath.resolve(directoryName).resolve("signing_keystore_sdm_test.p12").toString();
			final String keystorePasswordLocation = tempKeystorePath.resolve(directoryName).resolve("signing_pw_sdm_test.txt").toString();

			final Map<String, String> properties = new HashMap<>();
			properties.put("direct-trust.keystore.location." + directoryName, keystoreLocation);
			properties.put("direct-trust.password.location." + directoryName, keystorePasswordLocation);
			properties.put("direct-trust.keystore.filename-pattern", "signing_keystore_sdm_");
			properties.put("direct-trust.password.filename-pattern", "signing_pw_sdm_");
			TestPropertyValues.of(properties).applyTo(applicationContext);

			KeystoreFilesCreator.create(keystoreLocation, keystorePasswordLocation, alias.get());
		}
	}

	@Configuration
	static class InternalTestConfiguration {

		@Bean
		Hash getHash() {
			return HashFactory.createHash();
		}

		@Bean
		@ConditionalOnProperty("role.isSetup")
		KeystoreRepository keystoreConfigRepository(
				@Value("${direct-trust.keystore.location.config:}")
				final Path keystoreLocation,
				@Value("${direct-trust.password.location.config:}")
				final Path keystorePasswordLocation,
				@Value("${direct-trust.keystore.filename-pattern}")
				final String directTrustKeystoreFilenamePattern,
				@Value("${direct-trust.password.filename-pattern}")
				final String directTrustPasswordFilenamePattern) throws IOException {
			return new KeystoreRepository(keystoreLocation, keystorePasswordLocation, Alias.SDM_CONFIG, directTrustKeystoreFilenamePattern,
					directTrustPasswordFilenamePattern);
		}

		@Bean
		@ConditionalOnProperty("role.isTally")
		KeystoreRepository keystoreTallyRepository(
				@Value("${direct-trust.keystore.location.tally:}")
				final Path keystoreLocation,
				@Value("${direct-trust.password.location.tally:}")
				final Path keystorePasswordLocation,
				@Value("${direct-trust.keystore.filename-pattern}")
				final String directTrustKeystoreFilenamePattern,
				@Value("${direct-trust.password.filename-pattern}")
				final String directTrustPasswordFilenamePattern) throws IOException {
			return new KeystoreRepository(keystoreLocation, keystorePasswordLocation, Alias.SDM_TALLY, directTrustKeystoreFilenamePattern,
					directTrustPasswordFilenamePattern);
		}

		@Bean
		@Conditional(SecureDataManagerConfig.RoleCondition.class)
		SignatureKeystore<Alias> signatureKeystoreService(final KeystoreRepository repository) throws IOException {
			return SignatureKeystoreFactory.createSignatureKeystore(repository.getKeyStore(), repository.getKeystorePassword(),
					repository.getKeystoreAlias());
		}
	}
}
