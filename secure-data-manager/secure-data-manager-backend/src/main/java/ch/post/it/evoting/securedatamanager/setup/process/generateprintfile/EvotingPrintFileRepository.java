/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.SETUP_COMPONENT_EVOTING_PRINT_XML;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.XMLSignatureService;
import ch.post.it.evoting.evotinglibraries.toolbox.OutputToInputStreamConverter;
import ch.post.it.evoting.evotinglibraries.xml.XmlFileRepository;
import ch.post.it.evoting.evotinglibraries.xml.XsdConstants;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingprint.VotingCardList;
import ch.post.it.evoting.securedatamanager.shared.KeystoreRepository;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Repository
@ConditionalOnProperty("role.isSetup")
public class EvotingPrintFileRepository extends XmlFileRepository<VotingCardList> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvotingPrintFileRepository.class);

	private final PathResolver pathResolver;
	private final KeystoreRepository keystoreRepository;
	private final XMLSignatureService xmlSignatureService;
	private final String filename;

	public EvotingPrintFileRepository(
			final PathResolver pathResolver,
			final KeystoreRepository keystoreRepository,
			final XMLSignatureService xmlSignatureService,
			@Value("${sdm.election-event-seed}")
			final String electionEventSeed) {
		this.pathResolver = pathResolver;
		this.keystoreRepository = keystoreRepository;
		this.xmlSignatureService = xmlSignatureService;
		this.filename = String.format(SETUP_COMPONENT_EVOTING_PRINT_XML, validateSeed(electionEventSeed));
	}

	/**
	 * Saves the given voting card list in the {@value ch.post.it.evoting.securedatamanager.shared.Constants#SETUP_COMPONENT_EVOTING_PRINT_XML} file
	 * while validating it against the related {@value XsdConstants#SETUP_COMPONENT_EVOTING_PRINT_XSD}.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @param votingCardList  the voting card list. Must be non-null.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 */
	public void save(final String electionEventId, final VotingCardList votingCardList) {
		validateUUID(electionEventId);
		checkNotNull(votingCardList);

		final Path xmlFilePath = pathResolver.resolvePrintingOutputPath().resolve(filename);

		try (final OutputToInputStreamConverter converter = new OutputToInputStreamConverter();
				final InputStream printInput = converter.convert(os -> write(os, votingCardList, XsdConstants.SETUP_COMPONENT_EVOTING_PRINT_XSD));
				final OutputStream signedPrintOutput = Files.newOutputStream(xmlFilePath)) {

			LOGGER.debug("Signing setup component evoting print... [electionEventId: {}]", electionEventId);
			sign(printInput, signedPrintOutput);

			LOGGER.debug("Setup component evoting print successfully signed. Saving file... [filename: {}, electionEventId: {}]", filename,
					electionEventId);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Setup component evoting print not signed. [filename: %s, electionEventId: %s]".formatted(filename, electionEventId), e);
		}

		LOGGER.debug("File successfully saved. [electionEventId: {}, path: {}]", electionEventId, xmlFilePath);
	}

	private void sign(final InputStream print, final OutputStream signedPrintOutput) {
		final PrivateKey signatureVerificationKey;
		try (final InputStream keyStoreStream = keystoreRepository.getKeyStore()) {
			final KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(keyStoreStream, keystoreRepository.getKeystorePassword());
			signatureVerificationKey = (PrivateKey) keyStore.getKey(Alias.SDM_CONFIG.get(),
					keystoreRepository.getKeystorePassword());
		} catch (final KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
			throw new IllegalStateException("Could not load keystore", e);
		} catch (final UnrecoverableKeyException e) {
			throw new IllegalStateException("Could not get signing key. [alias: %s]".formatted(Alias.SDM_CONFIG.get()), e);
		}
		xmlSignatureService.genXMLSignature(print, signedPrintOutput, signatureVerificationKey);
	}

	public String getFilename() {
		return filename;
	}
}
