/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.xmlsignature;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.tools.xmlsignature.keystore.KeystoreRepository;

public enum Action {

	SIGN() {
		@Override
		public Result apply(final XmlSigner xmlSigner, final Path filePath, final KeystoreRepository keystoreRepository, final Alias alias) {
			try {
				final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
				keyStore.load(keystoreRepository.getKeyStore(), keystoreRepository.getKeystorePassword());
				final PrivateKey signingKey = (PrivateKey) keyStore.getKey(alias.get(), keystoreRepository.getKeystorePassword());
				xmlSigner.sign(filePath, signingKey);
				return new Result(true, null);
			} catch (final Exception e) {
				throw new IllegalStateException(String.format("An error occurred signing the file. [filePath: %s]", filePath), e);
			}
		}
	},
	VERIFY() {
		@Override
		public Result apply(final XmlSigner xmlSigner, final Path filePath, final KeystoreRepository keystoreRepository, final Alias alias) {
			try {
				final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
				keyStore.load(keystoreRepository.getKeyStore(), keystoreRepository.getKeystorePassword());
				final PublicKey signatureVerificationKey = keyStore.getCertificate(alias.get()).getPublicKey();
				return new Result(xmlSigner.verify(filePath, signatureVerificationKey), null);
			} catch (final Exception e) {
				throw new IllegalStateException(String.format("An error occurred verifying the signature of the file. [filePath: %s]", filePath), e);
			}
		}
	};

	private static final String KEYSTORE_TYPE = "PKCS12";

	public abstract Result apply(final XmlSigner xmlSigner, final Path filePath, final KeystoreRepository keystoreRepository, final Alias alias);
}
