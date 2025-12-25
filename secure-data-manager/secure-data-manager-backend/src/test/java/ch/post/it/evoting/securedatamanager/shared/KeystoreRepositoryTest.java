/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

class KeystoreRepositoryTest {

	private static final String KEYSTORE_CONTENT = "keystore-content";
	private static final String KEYSTORE_PASSWORD_CONTENT = "keystore-password-content";

	@TempDir
	static Path tempKeystorePath;

	static KeystoreRepository keystoreRepository;

	@BeforeAll
	static void setUp() throws IOException {

		final Path keystoreLocation = tempKeystorePath.resolve("signing_keystore_sdm_test.p12");
		final Path keystorePasswordLocation = tempKeystorePath.resolve("signing_pw_sdm_test.txt");

		try {
			Files.writeString(keystoreLocation, KEYSTORE_CONTENT);
			Files.writeString(keystorePasswordLocation, KEYSTORE_PASSWORD_CONTENT);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		keystoreRepository = new KeystoreRepository(keystoreLocation, keystorePasswordLocation, Alias.SDM_CONFIG, "signing_keystore_sdm_", "signing_pw_sdm_");
	}

	@Test
	void testGetKeyStore() throws IOException {
		// given

		// when
		final String keyStoreContentConfig = new String(keystoreRepository.getKeyStore().readAllBytes(), StandardCharsets.UTF_8);

		// then
		assertEquals(KEYSTORE_CONTENT, keyStoreContentConfig);
	}

	@Test
	void testGetKeystorePassword() throws IOException {
		// given

		// when
		final char[] passwordContentConfig = keystoreRepository.getKeystorePassword();

		// then
		assertArrayEquals(KEYSTORE_PASSWORD_CONTENT.toCharArray(), passwordContentConfig);
	}
}
