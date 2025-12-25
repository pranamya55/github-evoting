/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.Optional;

import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.XMLSignatureService;
import ch.post.it.evoting.evotinglibraries.xml.XmlFileRepository;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.securedatamanager.shared.KeystoreRepository;

public abstract class EvotingConfigFileRepository extends XmlFileRepository<Configuration> {

	private final KeystoreRepository keystoreRepository;
	private final XMLSignatureService xmlSignatureService;

	protected EvotingConfigFileRepository(final KeystoreRepository keystoreRepository, final XMLSignatureService xmlSignatureService) {
		this.keystoreRepository = keystoreRepository;
		this.xmlSignatureService = xmlSignatureService;
	}

	public abstract void saveFromExternalConfiguration();

	public abstract Optional<Configuration> load();

	protected boolean isSignatureValid(final Path configurationPath) {
		try (final FileInputStream configurationIn = new FileInputStream(configurationPath.toFile())) {
			final PublicKey signatureVerificationKey = loadSignatureVerificationKey();
			return xmlSignatureService.verifyXMLSignature(configurationIn, signatureVerificationKey);
		} catch (final FileNotFoundException e) {
			throw new IllegalStateException("Unable to verify evoting-config signature.", e);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private PublicKey loadSignatureVerificationKey() {
		try (final InputStream keyStoreStream = keystoreRepository.getKeyStore()) {
			final KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(keyStoreStream, keystoreRepository.getKeystorePassword());
			return keyStore.getCertificate(Alias.CANTON.get()).getPublicKey();
		} catch (final KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
			throw new IllegalStateException("Could not load keystore", e);
		}
	}
}
