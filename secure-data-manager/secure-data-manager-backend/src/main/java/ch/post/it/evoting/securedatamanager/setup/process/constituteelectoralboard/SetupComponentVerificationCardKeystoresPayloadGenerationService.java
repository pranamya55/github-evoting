/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.security.SignatureException;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.domain.configuration.VerificationCardKeystore;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodesPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKeyPayload;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKeyPayloadService;
import ch.post.it.evoting.securedatamanager.setup.process.VoterInitialCodesPayloadService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenCredDatOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenCredDatService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentTallyDataPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

/**
 * Creates and persists the setup component verification card keystores payload in the SDM file system.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class SetupComponentVerificationCardKeystoresPayloadGenerationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentVerificationCardKeystoresPayloadGenerationService.class);

	private final GenCredDatService genCredDatService;
	private final ElectionEventService electionEventService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final VerificationCardSetService verificationCardSetService;
	private final VoterInitialCodesPayloadService voterInitialCodesPayloadService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;
	private final SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService;
	private final VerificationCardSecretKeyPayloadService verificationCardSecretKeyPayloadService;
	private final SetupComponentVerificationCardKeystoresPayloadService setupComponentVerificationCardKeystoresPayloadService;

	public SetupComponentVerificationCardKeystoresPayloadGenerationService(
			final GenCredDatService genCredDatService,
			final ElectionEventService electionEventService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final VerificationCardSetService verificationCardSetService,
			final VoterInitialCodesPayloadService voterInitialCodesPayloadService,
			final ElectionEventContextPayloadService electionEventContextPayloadService,
			final SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService,
			final VerificationCardSecretKeyPayloadService verificationCardSecretKeyPayloadService,
			final SetupComponentVerificationCardKeystoresPayloadService setupComponentVerificationCardKeystoresPayloadService) {
		this.genCredDatService = genCredDatService;
		this.electionEventService = electionEventService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.verificationCardSetService = verificationCardSetService;
		this.voterInitialCodesPayloadService = voterInitialCodesPayloadService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
		this.setupComponentTallyDataPayloadService = setupComponentTallyDataPayloadService;
		this.verificationCardSecretKeyPayloadService = verificationCardSecretKeyPayloadService;
		this.setupComponentVerificationCardKeystoresPayloadService = setupComponentVerificationCardKeystoresPayloadService;
	}

	/**
	 * Creates and saves the list of setup component verification card keystores payloads.
	 *
	 * @param electionEventId                      the election event id. Must be non-null ana a valid UUID.
	 * @param choiceReturnCodesEncryptionPublicKey the choice return codes encryption public key. Must be non-null.
	 * @param electionPublicKey                    the election public key. Must be non-null.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 * @throws IllegalArgumentException  if the choice return codes encryption public key and the election public key do not have the same group.
	 */
	public void generate(final String electionEventId, final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey,
			final ElGamalMultiRecipientPublicKey electionPublicKey) {
		validateUUID(electionEventId);
		checkNotNull(choiceReturnCodesEncryptionPublicKey);
		checkNotNull(electionPublicKey);
		checkArgument(electionEventService.exists(electionEventId));
		checkArgument(choiceReturnCodesEncryptionPublicKey.getGroup().equals(electionPublicKey.getGroup()),
				"The choice return codes encryption public key and the election public key must have the same group");

		LOGGER.debug("Generating setup component verification card keystores payloads... [electionEventId: {}]", electionEventId);

		verificationCardSetService.getVerificationCardSetIds(electionEventId).stream()
				.parallel()
				.forEach(verificationCardSetId -> {
					final SetupComponentTallyDataPayload setupComponentTallyDataPayload = setupComponentTallyDataPayloadService.load(electionEventId,
							verificationCardSetId);
					final PrimesMappingTable primesMappingTable = electionEventContextPayloadService.loadPrimesMappingTable(electionEventId,
							verificationCardSetId);
					final VoterInitialCodesPayload voterInitialCodesPayload = voterInitialCodesPayloadService.load(electionEventId,
							verificationCardSetId);
					final VerificationCardSecretKeyPayload verificationCardSecretKeyPayload = verificationCardSecretKeyPayloadService.load(
							electionEventId, verificationCardSetId);

					final GenCredDatOutput genCredDatOutput = genCredDatService.genCredDat(setupComponentTallyDataPayload, primesMappingTable,
							voterInitialCodesPayload, electionPublicKey, choiceReturnCodesEncryptionPublicKey, verificationCardSecretKeyPayload);

					LOGGER.info("GenCredDat algorithm successfully performed. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
							verificationCardSetId);

					final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload = createSetupComponentVerificationCardKeystoresPayload(
							electionEventId, verificationCardSetId, setupComponentTallyDataPayload, genCredDatOutput);

					LOGGER.debug("Setup Component Verification Card Keystore Payload created. [electionEventId: {}, verificationCardSetId: {}]",
							electionEventId, verificationCardSetId);

					setupComponentVerificationCardKeystoresPayloadService.save(setupComponentVerificationCardKeystoresPayload);

					LOGGER.debug("Setup Component Verification Card Keystore Payload saved. [electionEventId: {}, verificationCardSetId: {}]",
							electionEventId, verificationCardSetId);
				});

		LOGGER.info("Generated and saved setup component verification card keystores payloads. [electionEventId: {}]", electionEventId);
	}

	private SetupComponentVerificationCardKeystoresPayload createSetupComponentVerificationCardKeystoresPayload(final String electionEventId,
			final String verificationCardSetId, final SetupComponentTallyDataPayload setupComponentTallyDataPayload,
			final GenCredDatOutput genCredDatOutput) {
		final ImmutableList<String> verificationCardIds = setupComponentTallyDataPayload.getVerificationCardIds();
		final ImmutableList<VerificationCardKeystore> verificationCardKeystores = IntStream.range(0, verificationCardIds.size()).parallel()
				.mapToObj(i -> new VerificationCardKeystore(verificationCardIds.get(i), genCredDatOutput.verificationCardKeystores().get(i)))
				.collect(toImmutableList());

		final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload = new SetupComponentVerificationCardKeystoresPayload(
				electionEventId, verificationCardSetId, verificationCardKeystores);

		final Hashable hashable = ChannelSecurityContextData.setupComponentVerificationCardKeystores(electionEventId, verificationCardSetId);

		final ImmutableByteArray signature;
		try {
			signature = signatureKeystoreService.generateSignature(setupComponentVerificationCardKeystoresPayload, hashable);
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format(
							"Could not sign setup component verification card keystores payload. [electionEventId: %s, , verificationCardSetId: %s]",
							electionEventId, verificationCardSetId));
		}

		final CryptoPrimitivesSignature setupComponentVerificationCardKeystoresPayloadSignature = new CryptoPrimitivesSignature(signature);
		setupComponentVerificationCardKeystoresPayload.setSignature(setupComponentVerificationCardKeystoresPayloadSignature);

		return setupComponentVerificationCardKeystoresPayload;
	}

}
