/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

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

import ch.post.it.evoting.domain.multitenancy.TenantConstants;
import ch.post.it.evoting.evotinglibraries.direct.trust.KeystoreFilesCreator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

public class TestKeyStoreInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	public static final Path KEYSTORE_DIRECTORY_PATH = Path.of("target", "direct-trust");
	public static final Path KEYSTORE_FILENAME_PATH = KEYSTORE_DIRECTORY_PATH.resolve("signing_keystore_test.p12");
	public static final Path KEYSTORE_PASSWORD_FILENAME_PATH = KEYSTORE_DIRECTORY_PATH.resolve("signing_pw_test.txt");

	@Override
	public void initialize(final ConfigurableApplicationContext applicationContext) {
		try {
			if ((Files.notExists(KEYSTORE_FILENAME_PATH) || Files.notExists(KEYSTORE_PASSWORD_FILENAME_PATH)) && Files.exists(
					KEYSTORE_DIRECTORY_PATH)) {
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
			Files.createDirectories(KEYSTORE_DIRECTORY_PATH);

			final String keystoreLocation = KEYSTORE_FILENAME_PATH.toString();
			final String keystorePasswordLocation = KEYSTORE_PASSWORD_FILENAME_PATH.toString();

			KeystoreFilesCreator.create(keystoreLocation, keystorePasswordLocation, Alias.CONTROL_COMPONENT_1.get());

			final Map<String, String> properties = new HashMap<>();
			properties.put("multitenancy.tenants." + TenantConstants.TEST_TENANT_ID + ".direct-trust.keystore-location", keystoreLocation);
			properties.put("multitenancy.tenants." + TenantConstants.TEST_TENANT_ID + ".direct-trust.password-location", keystorePasswordLocation);
			TestPropertyValues.of(properties).applyTo(applicationContext);

		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
