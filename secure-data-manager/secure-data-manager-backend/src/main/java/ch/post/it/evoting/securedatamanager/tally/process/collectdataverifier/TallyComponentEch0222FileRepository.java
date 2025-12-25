/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.collectdataverifier;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.evotinglibraries.xml.XsdConstants.TALLY_COMPONENT_ECH_0222;
import static ch.post.it.evoting.evotinglibraries.xml.XsdConstants.TALLY_COMPONENT_ECH_0222_VERSION;
import static ch.post.it.evoting.securedatamanager.shared.Constants.TALLY_COMPONENT_ECH_0222_XML;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import ch.ech.xmlns.ech_0222._3.Delivery;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.XMLSignatureService;
import ch.post.it.evoting.evotinglibraries.toolbox.OutputToInputStreamConverter;
import ch.post.it.evoting.evotinglibraries.xml.XmlFileRepository;
import ch.post.it.evoting.evotinglibraries.xml.XsdConstants;
import ch.post.it.evoting.securedatamanager.shared.KeystoreRepository;
import ch.post.it.evoting.securedatamanager.tally.process.TallyPathResolver;

@Repository
@ConditionalOnProperty("role.isTally")
public class TallyComponentEch0222FileRepository extends XmlFileRepository<Delivery> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TallyComponentEch0222FileRepository.class);

	private final TallyPathResolver tallyPathResolver;
	private final KeystoreRepository keystoreRepository;
	private final XMLSignatureService xmlSignatureService;
	private final String filename;

	public TallyComponentEch0222FileRepository(
			final TallyPathResolver tallyPathResolver,
			final KeystoreRepository keystoreRepository,
			final XMLSignatureService xmlSignatureService,
			@Value("${sdm.election-event-seed}")
			final String electionEventSeed) {
		this.tallyPathResolver = tallyPathResolver;
		this.keystoreRepository = keystoreRepository;
		this.xmlSignatureService = xmlSignatureService;
		this.filename = String.format(TALLY_COMPONENT_ECH_0222_XML, TALLY_COMPONENT_ECH_0222_VERSION, validateSeed(electionEventSeed));
	}

	/**
	 * Saves the given delivery in the {@value ch.post.it.evoting.securedatamanager.shared.Constants#TALLY_COMPONENT_ECH_0222_XML} file while
	 * validating it against the related {@value XsdConstants#TALLY_COMPONENT_ECH_0222}.
	 *
	 * @param delivery        the delivery. Must be non-null.
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 */
	public void save(final Delivery delivery, final String electionEventId) {
		checkNotNull(delivery);
		validateUUID(electionEventId);

		final Path xmlElectionEventPath = tallyPathResolver.resolveElectionEventPath(electionEventId).resolve(filename);
		final Path xmlTallyOutputPath = tallyPathResolver.resolveTallyOutputPath().resolve(filename);

		try {
			try (final OutputToInputStreamConverter converter = new OutputToInputStreamConverter();
					final InputStream eCH0222InputStream = converter.convert(os -> write(os, delivery, TALLY_COMPONENT_ECH_0222))) {
				LOGGER.debug("Signing tally component eCH-0222... [electionEventId: {}]", electionEventId);
				try (final OutputStream signedEch0222Output = Files.newOutputStream(xmlElectionEventPath)) {
					xmlSignatureService.genXMLSignature(eCH0222InputStream, signedEch0222Output, getSigningKey(), "eCH-0222:rawDataDelivery",
							"eCH-0222:extension");
					LOGGER.debug("Tally component eCH-0222 successfully signed. Saving file... [filename: {}, electionEventId: {}]", filename,
							electionEventId);
				}
			}

			Files.copy(xmlElectionEventPath, xmlTallyOutputPath);
			LOGGER.debug("Tally component eCH-0222 successfully copied. [source: {}, destination: {}, electionEventId: {}]",
					xmlElectionEventPath, xmlTallyOutputPath, electionEventId);

		} catch (final IOException e) {
			throw new UncheckedIOException("Could not save or copy tally component eCH-0222 file.", e);
		}

		LOGGER.debug("File successfully saved. [filename: {}, electionEventId: {}]", filename, electionEventId);
	}

	private PrivateKey getSigningKey() {
		try (final InputStream keystoreIn = keystoreRepository.getKeyStore()) {
			final char[] keystorePassword = keystoreRepository.getKeystorePassword();
			final KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(keystoreIn, keystorePassword);
			return (PrivateKey) keyStore.getKey(Alias.SDM_TALLY.get(), keystorePassword);
		} catch (final KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableKeyException e) {
			throw new IllegalStateException("Could not load signing key.", e);
		}
	}

	/**
	 * Loads the tally component eCH-0222 for the given election event id and validates it against the related XSD. The tally component eCH-0222 is
	 * located in the {@value ch.post.it.evoting.securedatamanager.shared.Constants#TALLY_COMPONENT_ECH_0222_XML} file and the related XSD in
	 * {@value XsdConstants#TALLY_COMPONENT_ECH_0222}.
	 * <p>
	 * If the contest configuration file or the related XSD does not exist this method returns an empty Optional.
	 * <p>
	 * This method also validates the signature of the loaded file.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the tally component eCH-0222 as an {@link Optional}.
	 * @throws NullPointerException      if the election event id is null.
	 * @throws IllegalArgumentException  if the contest identification is blank.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 * @throws IllegalStateException     if the signature is invalid, or it could not be verified.
	 */
	Optional<Delivery> load(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.debug("Loading file... [filename: {}, electionEventId: {}]", filename, electionEventId);

		final Path xmlFilePath = tallyPathResolver.resolveElectionEventPath(electionEventId).resolve(filename);

		if (!Files.exists(xmlFilePath)) {
			LOGGER.debug("The requested file does not exist. [path: {}, electionEventId: {}]", xmlFilePath, electionEventId);
			return Optional.empty();
		}

		checkState(isSignatureValid(xmlFilePath));

		final Delivery delivery = read(xmlFilePath, XsdConstants.TALLY_COMPONENT_ECH_0222, Delivery.class);

		LOGGER.debug("File successfully loaded [path: {}, electionEventId: {}].", filename, electionEventId);

		return Optional.of(delivery);
	}

	private boolean isSignatureValid(final Path deliveryPath) {
		try (final FileInputStream deliveryIn = new FileInputStream(deliveryPath.toFile())) {
			final PublicKey signatureVerificationKey = loadSignatureVerificationKey();
			return xmlSignatureService.verifyXMLSignature(deliveryIn, signatureVerificationKey);
		} catch (final FileNotFoundException e) {
			throw new IllegalStateException("Unable to verify eCH-0222 signature.", e);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private PublicKey loadSignatureVerificationKey() {
		try (final InputStream keyStoreStream = keystoreRepository.getKeyStore()) {
			final KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(keyStoreStream, keystoreRepository.getKeystorePassword());
			return keyStore.getCertificate(Alias.SDM_TALLY.get()).getPublicKey();
		} catch (final KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
			throw new IllegalStateException("Could not load keystore", e);
		}
	}

}
