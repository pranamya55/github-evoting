/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools;

import static ch.post.it.evoting.tools.EncryptionDecryptionService.Mode.DECRYPT;
import static ch.post.it.evoting.tools.EncryptionDecryptionService.Mode.ENCRYPT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.StreamableSymmetricEncryptionDecryptionService;

class EncryptionDecryptionServiceTest {

	private final Random random = RandomFactory.createRandom();
	private final StreamableSymmetricEncryptionDecryptionService symmetricEncryptionDecryptionService = new StreamableSymmetricEncryptionDecryptionService(
			random, Argon2Factory.createArgon2(Argon2Profile.TEST));

	private final char[] password = "Password_Password_123456".toCharArray();

	@TempDir
	private Path tempDir;

	private Path plaintextFilePath;
	private Path encryptedFilePath;
	private Path decryptedFilePath;

	private EncryptionDecryptionService encryptionService;
	private EncryptionDecryptionService decryptionService;

	@BeforeEach
	void setUpEach() throws IOException {
		// initialize paths and services.
		final String file = "plaintext.txt";
		plaintextFilePath = tempDir.resolve(file);
		encryptedFilePath = tempDir.resolve("encrypted-" + file);
		decryptedFilePath = tempDir.resolve("decrypted-" + file);

		// initialize password file.
		final String passwordFile = "password.txt";
		final Path passwordFilePath = tempDir.resolve(passwordFile);
		Files.createFile(passwordFilePath);
		Files.writeString(passwordFilePath, new String(password));

		encryptionService = new EncryptionDecryptionService(symmetricEncryptionDecryptionService,
				ENCRYPT,
				password,
				null, // use password for encryption.
				plaintextFilePath,
				encryptedFilePath);

		decryptionService = new EncryptionDecryptionService(symmetricEncryptionDecryptionService,
				DECRYPT,
				null, // use password file for decryption.
				passwordFilePath,
				encryptedFilePath,
				decryptedFilePath);

		// create plaintext file.
		Files.createFile(plaintextFilePath);

		// create random plaintext.
		final int randomStringLength = random.genRandomInteger(40);
		final String randomString = random.genRandomString(randomStringLength, Base64Alphabet.getInstance());

		// write random plaintext to file.
		Files.writeString(plaintextFilePath, randomString);
	}

	@Test
	void cycleHappyPath() throws IOException {
		// encrypt plaintext file to new encrypted-plaintext file.
		assertDoesNotThrow(() -> encryptionService.run());

		// assert encrypted-file exists.
		assertTrue(Files.exists(encryptedFilePath));

		// assert plaintext file content and encrypted-plaintext file content are not equal.
		assertNotEquals(Files.readAllBytes(plaintextFilePath), Files.readAllBytes(encryptedFilePath));

		// decrypt encrypted-file to new decrypted-plaintext file.
		assertDoesNotThrow(() -> decryptionService.run());

		// assert decrypted-file exists.
		assertTrue(Files.exists(decryptedFilePath));

		// assert plaintext file content and decrypted-plaintext file content are equal.
		assertArrayEquals(Files.readAllBytes(plaintextFilePath), Files.readAllBytes(decryptedFilePath));
	}
}
