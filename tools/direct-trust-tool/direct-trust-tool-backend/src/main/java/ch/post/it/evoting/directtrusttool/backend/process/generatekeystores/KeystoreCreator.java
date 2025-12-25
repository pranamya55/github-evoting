/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.generatekeystores;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.signing.AuthorityInformation;
import ch.post.it.evoting.cryptoprimitives.signing.GenKeysAndCert;
import ch.post.it.evoting.cryptoprimitives.signing.KeysAndCert;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureFactory;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateUtils;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

class KeystoreCreator {
	private static final int PASSWORD_LENGTH = 32;
	private final LocalDate validFrom;
	private final LocalDate validUntil;
	private final Random random;
	private final Alphabet base64Alphabet;
	private final AuthorityInformation authorityInformation;

	public KeystoreCreator(final LocalDate validUntil, final AuthorityInformation authorityInformation) {
		this.validUntil = checkNotNull(validUntil);
		this.authorityInformation = checkNotNull(authorityInformation);
		this.validFrom = LocalDateUtils.now();

		checkArgument(this.validUntil.isAfter(validFrom));

		this.random = RandomFactory.createRandom();
		this.base64Alphabet = Base64Alphabet.getInstance();
	}

	public Output generateKeystore(final Alias component) {
		try (final ByteArrayOutputStream keyStoreStream = new ByteArrayOutputStream();
				final SafePasswordHolder passwordHolder = new SafePasswordHolder(
						random.genRandomString(PASSWORD_LENGTH, base64Alphabet).toCharArray())) {

			final KeyStore keystore = KeyStore.getInstance("PKCS12");
			keystore.load(null, passwordHolder.get());
			final Optional<KeysAndCert> keysAndCert = generateKeyAndCert(component);
			keysAndCert.ifPresent(k -> writeEntryToKeystore(component, k, keystore, passwordHolder));
			keystore.store(keyStoreStream, passwordHolder.get());

			return new Output(
					new ImmutableByteArray(keyStoreStream.toByteArray()),
					passwordHolder.copy(),
					keysAndCert
							.map(KeysAndCert::certificate)
							.orElse(null)
			);
		} catch (final CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
			throw new KeystoreCreationException(e);
		}
	}

	private Optional<KeysAndCert> generateKeyAndCert(final Alias component) {
		if (component.hasPrivateKey()) {
			final SignatureFactory signatureFactory = SignatureFactory.getInstance();
			final GenKeysAndCert keyAndCertGenerator = signatureFactory.createGenKeysAndCert(getAuthorityInformationForAlias(component));
			return Optional.of(keyAndCertGenerator.genKeysAndCert(validFrom, validUntil));
		}
		return Optional.empty();
	}

	private AuthorityInformation getAuthorityInformationForAlias(final Alias component) {
		return AuthorityInformation.builder()
				.setCommonName(component.get())
				.setCountry(authorityInformation.getCountry())
				.setLocality(authorityInformation.getLocality())
				.setOrganisation(authorityInformation.getOrganisation())
				.setState(authorityInformation.getState())
				.build();
	}

	private static void writeEntryToKeystore(final Alias component, final KeysAndCert k, final KeyStore keystore, final SafePasswordHolder password) {
		try {
			keystore.setKeyEntry(component.get(), k.privateKey(), password.get(), new Certificate[] { k.certificate() });
		} catch (final KeyStoreException e) {
			throw new KeystoreCreationException(e);
		}
	}

	public record Output(ImmutableByteArray keyStore, SafePasswordHolder password, Certificate publicKey) implements Closeable {

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final Output output = (Output) o;
			return Objects.equals(keyStore, output.keyStore) && password.equals(output.password) && Objects.equals(
					publicKey, output.publicKey);
		}

		@Override
		public int hashCode() {
			return Objects.hash(keyStore, password, publicKey);
		}

		@Override
		public String toString() {
			return "Output{" +
					"keyStore=" + keyStore +
					", password='" + "*".repeat(password.length()) + "'" +
					", publicKey=" + publicKey +
					'}';
		}

		@Override
		public void close() {
			password.close();
		}
	}

	public static class KeystoreCreationException extends RuntimeException {
		public KeystoreCreationException(final Throwable cause) {
			super(cause);
		}
	}
}
