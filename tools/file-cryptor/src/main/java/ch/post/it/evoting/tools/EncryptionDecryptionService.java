/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.evotinglibraries.domain.validations.PasswordValidation;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.StreamableSymmetricEncryptionDecryptionService;

@Service
public class EncryptionDecryptionService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionDecryptionService.class);
	private static final ImmutableByteArray ASSOCIATED_DATA = ImmutableByteArray.EMPTY;

	private final StreamableSymmetricEncryptionDecryptionService symmetricEncryptionDecryptionService;

	private final Mode mode;
	private final char[] password;

	private final Path sourcePath;
	private final Path targetPath;

	public EncryptionDecryptionService(
			final StreamableSymmetricEncryptionDecryptionService symmetricEncryptionDecryptionService,
			@Value("${mode}")
			final Mode mode,
			@Value("${password}")
			final char[] password,
			@Value("${password-file-path}")
			final Path passwordFilePath,
			@Value("${source.file-path}")
			final Path sourceFilePath,
			@Value("${target.file-path}")
			final Path targetFilePath) {
		this.symmetricEncryptionDecryptionService = symmetricEncryptionDecryptionService;
		this.mode = checkNotNull(mode, "The mode is required.");
		this.password = determinePassword(password, passwordFilePath);
		this.sourcePath = checkNotNull(sourceFilePath, "The source file path is required.");
		this.targetPath = checkNotNull(targetFilePath, "The target file path is required.");
	}

	@SuppressWarnings("java:S1301")
		// By choice, we prefer the usage of switch statement over if-else clause.
	void run() {
		checkState(Files.exists(sourcePath), "The given source file does not exist. [path: %s]", sourcePath);
		checkState(!Files.exists(targetPath), "The given target file already exists. [path: %s]", targetPath);

		try (final InputStream inputStream = Files.newInputStream(sourcePath);
				final OutputStream outputStream = Files.newOutputStream(targetPath)) {
			switch (mode) {
			case ENCRYPT -> {
				LOGGER.info("Encrypting file. Please wait... [path: {}]", sourcePath);
				symmetricEncryptionDecryptionService.genStreamCiphertext(outputStream, inputStream, password, ASSOCIATED_DATA);
				LOGGER.info("File successfully encrypted. [path: {}]", targetPath);
			}
			case DECRYPT -> {
				LOGGER.info("Decrypting file. Please wait... [path: {}]", sourcePath);
				final InputStream decryptedStream = symmetricEncryptionDecryptionService.getStreamPlaintext(inputStream, password, ASSOCIATED_DATA);
				decryptedStream.transferTo(outputStream);
				LOGGER.info("File successfully decrypted. [path: {}]", targetPath);
			}
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public enum Mode {
		ENCRYPT,
		DECRYPT
	}

	private char[] determinePassword(final char[] password, final Path passwordFilePath) {
		checkArgument(password != null || passwordFilePath != null, "Either password or password file path is required.");

		if (password != null && passwordFilePath != null) {
			LOGGER.warn("Both password and password file path are provided. The password file path will be ignored.");
		}

		// use password directly and validate it.
		if (password != null) {
			return validatePassword(password);
		}

		// read password from file and validate it.
		final char[] passwordFromFile;
		try {
			passwordFromFile = Files.readString(passwordFilePath).toCharArray();
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		return validatePassword(passwordFromFile);
	}

	private char[] validatePassword(final char[] password) {
		PasswordValidation.validate(password, "file-cryptor");
		return password;
	}

}
