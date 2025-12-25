/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys;

import static ch.post.it.evoting.directtrusttool.backend.process.CertificateHashCalculator.calculateFingerprintForCertificate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.utils.VerificationResult;
import ch.post.it.evoting.evotinglibraries.direct.trust.KeystoreValidator;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

public class KeystoreKeysImporter {

	private KeystoreKeysImporter() {
		// utility class
	}

	public static ImmutableByteArray importPublicKeys(final ImmutableByteArray keyStoreBytes, final SafePasswordHolder password, final Alias component,
			final ImmutableMap<Alias, Certificate> componentKeys) {
		checkNotNull(keyStoreBytes);
		checkNotNull(password);
		checkNotNull(component);
		checkArgument(componentKeys.size() == Alias.values().length - 1);

		try {
			final KeyStore keystore = KeyStore.getInstance("PKCS12");

			try (final ByteArrayInputStream keyStoreStream = new ByteArrayInputStream(keyStoreBytes.elements())) {
				keystore.load(keyStoreStream, password.get());
			}

			componentKeys.entrySet().forEach(componentAndPublicKey -> {

				try {
					if (keystore.containsAlias(componentAndPublicKey.key().get())) {
						compareCertificateInKeystore(componentAndPublicKey.key(), componentAndPublicKey.value(), keystore);
					} else {
						saveCertificateInKeystore(componentAndPublicKey.key(), componentAndPublicKey.value(), keystore);
					}
				} catch (final KeyStoreException e) {
					throw new KeystoreImportException(e);
				}

			});

			final Alias signingAlias = component.hasPrivateKey() ? component : null;
			final VerificationResult result = KeystoreValidator.validateKeystore(keystore, signingAlias, password.get());
			checkState(result.isVerified(), "Keystore validation failed. [component: %s, errorMessages: %s]", component.get(),
					result.isVerified() ? "" : result.getErrorMessages());

			try (final ByteArrayOutputStream updatedKeystoreStream = new ByteArrayOutputStream()) {
				keystore.store(updatedKeystoreStream, password.get());
				return new ImmutableByteArray(updatedKeystoreStream.toByteArray());
			}
		} catch (final IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
			throw new KeystoreImportException(e);
		}
	}

	private static void compareCertificateInKeystore(final Alias component, final Certificate certificateFromExchange, final KeyStore keystore) {

		final Certificate certificateFromKeystore;

		try {
			certificateFromKeystore = keystore.getCertificate(component.get());
		} catch (final KeyStoreException e) {
			throw new KeystoreImportException(e);
		}
		final String keystoreCertificateFingerprint = calculateFingerprintForCertificate(certificateFromKeystore);
		final String exchangeCertificateFingerprint = calculateFingerprintForCertificate(certificateFromExchange);

		checkState(certificateFromKeystore.equals(certificateFromExchange),
				"Certificate from exchange does not match the one in keystore. [component: %s, keystore_certificate_hash: %s, exchange_certificate_hash: %s]",
				component.get(), keystoreCertificateFingerprint, exchangeCertificateFingerprint);
	}

	private static void saveCertificateInKeystore(final Alias component, final Certificate key, final KeyStore keystore) {
		try {
			keystore.setCertificateEntry(component.get(), key);
		} catch (final KeyStoreException e) {
			throw new KeystoreImportException(e);
		}
	}

	public static class KeystoreImportException extends RuntimeException {
		public KeystoreImportException(final Throwable cause) {
			super(cause);
		}
	}
}
