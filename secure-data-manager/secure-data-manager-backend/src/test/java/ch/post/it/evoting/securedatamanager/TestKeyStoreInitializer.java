/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import ch.post.it.evoting.evotinglibraries.direct.trust.KeystoreFilesCreator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

public class TestKeyStoreInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	public static final Path KEYSTORE_DIRECTORY_PATH = Path.of("target", "direct-trust");
	public static final Path TALLY_DIRECTORY = Path.of("tally");
	public static final Path CONFIG_DIRECTORY = Path.of("config");
	public static final Path KEYSTORE_FILENAME = Path.of("signing_keystore_sdm_test.p12");
	public static final Path KEYSTORE_PASSWORD_FILENAME = Path.of("signing_pw_sdm_test.txt");

	@Override
	public void initialize(final ConfigurableApplicationContext applicationContext) {
		createSdmKeystore(CONFIG_DIRECTORY, applicationContext, Alias.SDM_CONFIG);
		createSdmKeystore(TALLY_DIRECTORY, applicationContext, Alias.SDM_TALLY);
	}

	private void createSdmKeystore(final Path directoryName, final ConfigurableApplicationContext applicationContext, final Alias alias) {

		final Path keystoreFilenamePath = KEYSTORE_DIRECTORY_PATH.resolve(directoryName).resolve(KEYSTORE_FILENAME);
		final Path keystorePasswordFilenamePath = KEYSTORE_DIRECTORY_PATH.resolve(directoryName).resolve(KEYSTORE_PASSWORD_FILENAME);

		try {
			if ((Files.notExists(keystoreFilenamePath) || Files.notExists(keystorePasswordFilenamePath)) && Files.exists(KEYSTORE_DIRECTORY_PATH)) {
				// Traverse the directory from the bottom up and delete all files and directories
				try (final Stream<Path> paths = Files.walk(KEYSTORE_DIRECTORY_PATH).sorted(Comparator.reverseOrder())) {
					paths.forEach(path -> {
						try {
							Files.delete(path);
							System.out.println("Deleted: " + path);
						} catch (final IOException e) {
							throw new UncheckedIOException(e);
						}
					});
				}
			}

			Files.createDirectories(KEYSTORE_DIRECTORY_PATH.resolve(directoryName));

			final String keystoreLocation = KEYSTORE_DIRECTORY_PATH.resolve(directoryName).resolve(KEYSTORE_FILENAME).toString();
			final String keystorePasswordLocation = KEYSTORE_DIRECTORY_PATH.resolve(directoryName).resolve(KEYSTORE_PASSWORD_FILENAME).toString();

			KeystoreFilesCreator.create(keystoreLocation, keystorePasswordLocation, alias.get());

			final Map<String, String> properties = new HashMap<>();
			properties.put("direct-trust.keystore.location." + directoryName, keystoreLocation);
			properties.put("direct-trust.password.location." + directoryName, keystorePasswordLocation);
			TestPropertyValues.of(properties).applyTo(applicationContext);

		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

