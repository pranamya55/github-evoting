/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature.keystore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeystoreRepositoryTest {
	private static final String KEYSTORE_CONTENT = "keystore-content";
	private static final String KEYSTORE_PASSWORD_CONTENT = "keystore-password-content";

	@TempDir
	static Path tempKeystorePath;

	static KeystoreRepository keystoreRepository;

	@BeforeAll
	static void setUp() {

		final String keystoreLocation = tempKeystorePath.resolve("signing_keystore_test.p12").toString();
		final String keystorePasswordLocation = tempKeystorePath.resolve("signing_pw_test.txt").toString();

		try {
			Files.writeString(Paths.get(keystoreLocation), KEYSTORE_CONTENT);
			Files.writeString(Paths.get(keystorePasswordLocation), KEYSTORE_PASSWORD_CONTENT);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		keystoreRepository = new KeystoreRepository(keystoreLocation, keystorePasswordLocation);
	}

	@Test
	void testGetKeyStore() throws IOException {
		// given

		// when
		final String keyStoreContent = new String(keystoreRepository.getKeyStore().readAllBytes(), StandardCharsets.UTF_8);

		// then
		assertEquals(KEYSTORE_CONTENT, keyStoreContent);
	}

	@Test
	void testGetKeystorePassword() throws IOException {
		// given

		// when
		final char[] passwordContent = keystoreRepository.getKeystorePassword();

		// then
		assertArrayEquals(KEYSTORE_PASSWORD_CONTENT.toCharArray(), passwordContent);
	}
}
