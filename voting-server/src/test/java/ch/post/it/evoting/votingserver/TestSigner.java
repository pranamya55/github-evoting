/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SignatureException;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystoreFactory;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;

/**
 * Test utility to create a signature from any Alias.
 */
public class TestSigner {
	private final SignatureKeystore<Alias> signatureKeystoreService;

	public TestSigner(final Path keystoreLocation, final Path keyStorePasswordLocation, final Alias alias) throws IOException {
		final InputStream keyStoreStream = Files.newInputStream(keystoreLocation);
		final char[] keyStorePassword = new String(Files.readAllBytes(keyStorePasswordLocation)).toCharArray();
		signatureKeystoreService = SignatureKeystoreFactory.createSignatureKeystore(keyStoreStream, "PKCS12", keyStorePassword, keystore -> true,
				alias);
	}

	public void sign(final SignedPayload toSign, final Hashable additionalContextData) throws SignatureException {
		final ImmutableByteArray signature = signatureKeystoreService.generateSignature(toSign, additionalContextData);
		toSign.setSignature(new CryptoPrimitivesSignature(signature));
	}
}
