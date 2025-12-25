/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.regex.Pattern;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.evotinglibraries.domain.ConversionUtils;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

public class KeystoreRepository {

	private final String signingKeystoreFilename;
	private final String signingPwFilename;

	private final Path keystoreLocation;
	private final Path keystorePasswordLocation;
	private final Alias alias;

	public KeystoreRepository(
			final Path keystoreLocation,
			final Path keystorePasswordLocation,
			final Alias alias,
			final String directTrustKeystoreFilenamePattern,
			final String directTrustPasswordFilenamePattern) throws IOException {
		this.keystoreLocation = checkNotNull(keystoreLocation);
		this.keystorePasswordLocation = checkNotNull(keystorePasswordLocation);
		this.alias = checkNotNull(alias);
		this.signingKeystoreFilename = checkNotNull(directTrustKeystoreFilenamePattern);
		this.signingPwFilename = checkNotNull(directTrustPasswordFilenamePattern);

		checkArgument(Alias.SDM_CONFIG.equals(alias) || Alias.SDM_TALLY.equals(alias), "The given alias is not supported. [alias: %s]", alias);

		validateKeystoreLocations(keystoreLocation, keystorePasswordLocation);
	}

	/**
	 * @return the input stream containing the keystore.
	 * @throws IOException if an I/O error occurs during the creation of the input stream.
	 */
	public InputStream getKeyStore() throws IOException {
		return Files.newInputStream(keystoreLocation);
	}

	/**
	 * @return a new {@code char} array containing the keystore password characters.
	 * @throws IOException if an I/O error occurs reading the keystore password.
	 */
	public char[] getKeystorePassword() throws IOException {
		final ImmutableByteArray passwordBytes = new ImmutableByteArray(Files.readAllBytes(keystorePasswordLocation));
		return ConversionUtils.byteArrayToCharArray(passwordBytes);
	}

	/**
	 * @return the keystore alias (either {@link Alias#SDM_CONFIG} or {@link Alias#SDM_TALLY}).
	 */
	public Alias getKeystoreAlias() {
		return alias;
	}

	private void validateKeystoreLocations(final Path keystoreLocation, final Path keystorePasswordLocation) throws IOException {
		final Path keystoreLocationPath = keystoreLocation.toRealPath(LinkOption.NOFOLLOW_LINKS);
		checkArgument(Files.isRegularFile(keystoreLocationPath), "The given keystore location is not a file. [path: %s]", keystoreLocationPath);

		final Pattern signingKeystoreFilenamePattern = getSigningFilenamePattern(signingKeystoreFilename, Constants.P12);
		checkArgument(signingKeystoreFilenamePattern.matcher(keystoreLocation.toString()).matches(),
				"The given keystore location does not have the correct filename or extension. [pattern: <prefix>%s<suffix>%s, path: %s]",
				signingKeystoreFilename, Constants.P12, keystoreLocationPath);
		final Path keystorePasswordLocationPath = keystorePasswordLocation.toRealPath(LinkOption.NOFOLLOW_LINKS);
		checkArgument(Files.isRegularFile(keystorePasswordLocationPath), "The given keystore password location is not a file. [path: %s]",
				keystorePasswordLocationPath);

		final Pattern signingPwFilenamePattern = getSigningFilenamePattern(signingPwFilename, Constants.TXT);
		checkArgument(signingPwFilenamePattern.matcher(keystorePasswordLocation.toString()).matches(),
				"The given keystore password location does not have the correct filename or extension. [pattern: <prefix>%s<suffix>%s, path: %s]",
				signingPwFilename, Constants.TXT, keystorePasswordLocation);
	}

	private Pattern getSigningFilenamePattern(final String filename, final String extension) {
		return Pattern.compile(String.format(".*%s.*\\%s$", filename, extension));
	}
}
