/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.generateenclongcodeshares;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.process.IdentifierValidationService;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryEntity;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@Service
public class GenerateEncryptedLongReturnCodeSharesProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEncryptedLongReturnCodeSharesProcessor.class);

	private final ObjectMapper objectMapper;
	private final BallotBoxService ballotBoxService;
	private final ElectionEventService electionEventService;
	private final VerificationCardService verificationCardService;
	private final PCCAllowListEntryService pccAllowListEntryService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ElectionEventStateService electionEventStateService;
	private final VerificationCardSetService verificationCardSetService;
	private final IdentifierValidationService identifierValidationService;
	private final EncryptedLongReturnCodeSharesService encryptedLongReturnCodeSharesService;
	private final GenerateEncryptedLongReturnCodeSharesService generateEncryptedLongReturnCodeSharesService;

	@Value("${nodeID}")
	private int nodeId;

	public GenerateEncryptedLongReturnCodeSharesProcessor(
			final ObjectMapper objectMapper,
			final BallotBoxService ballotBoxService,
			final ElectionEventService electionEventService,
			final VerificationCardService verificationCardService,
			final PCCAllowListEntryService pccAllowListEntryService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ElectionEventStateService electionEventStateService,
			final VerificationCardSetService verificationCardSetService,
			final IdentifierValidationService identifierValidationService,
			final EncryptedLongReturnCodeSharesService encryptedLongReturnCodeSharesService,
			final GenerateEncryptedLongReturnCodeSharesService generateEncryptedLongReturnCodeSharesService) {
		this.objectMapper = objectMapper;
		this.ballotBoxService = ballotBoxService;
		this.electionEventService = electionEventService;
		this.verificationCardService = verificationCardService;
		this.pccAllowListEntryService = pccAllowListEntryService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.electionEventStateService = electionEventStateService;
		this.verificationCardSetService = verificationCardSetService;
		this.identifierValidationService = identifierValidationService;
		this.encryptedLongReturnCodeSharesService = encryptedLongReturnCodeSharesService;
		this.generateEncryptedLongReturnCodeSharesService = generateEncryptedLongReturnCodeSharesService;
	}

	@Transactional
	public ControlComponentCodeSharesPayload onRequest(final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload) {
		checkNotNull(setupComponentVerificationDataPayload);

		final String electionEventId = setupComponentVerificationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationDataPayload.getVerificationCardSetId();
		final int chunkId = setupComponentVerificationDataPayload.getChunkId();

		// Validate encryption group.
		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		checkState(setupComponentVerificationDataPayload.getEncryptionGroup().equals(encryptionGroup),
				"The group of te setup component verification data payload must be equal to the encryption group.");
		identifierValidationService.validateIds(electionEventId, verificationCardSetId);

		// Validate election event state.
		final ElectionEventState expectedState = ElectionEventState.INITIAL;
		final ElectionEventState electionEventState = electionEventStateService.getElectionEventState(electionEventId);
		checkState(expectedState.equals(electionEventState),
				"The election event is not in the expected state. [electionEventId: %s, nodeId: %s, expected: %s, actual: %s]", electionEventId,
				nodeId, expectedState, electionEventState);

		// Sanity check the partial Choice Return Codes allow list before saving.
		final ImmutableList<String> payloadValidatedAllowList = validateAllowList(setupComponentVerificationDataPayload);

		// Save allow list chunk.
		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSet(verificationCardSetId);
		final ImmutableList<PCCAllowListEntryEntity> partialChoiceReturnCodeAllowList = payloadValidatedAllowList.stream()
				.map(partialChoiceCode -> new PCCAllowListEntryEntity(verificationCardSetEntity, partialChoiceCode, chunkId))
				.collect(toImmutableList());
		pccAllowListEntryService.saveAll(partialChoiceReturnCodeAllowList);
		LOGGER.info("AllowList saved chunk. [electionEventId: {}, verificationCardSetId: {}, nodeId: {}, chunkId: {}]", electionEventId,
				verificationCardSetId, nodeId, chunkId);

		final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByVerificationCardSetId(verificationCardSetId);
		final int numberOfEligibleVoters = ballotBoxEntity.getNumberOfEligibleVoters();

		final int savedNumberOfVerificationCards = verificationCardService.countNumberOfVerificationCards(electionEventId, verificationCardSetId);
		final ImmutableList<String> verificationCardIdsToSave = getVerificationCardIds(setupComponentVerificationDataPayload);
		checkArgument(numberOfEligibleVoters >= savedNumberOfVerificationCards + verificationCardIdsToSave.size(),
				"The number of verification cards saved + the number of verification cards to save must not exceed the number of eligible voters for the given election event and verification card set id. "
						+ "[electionEventId: %s, nodeId: %s, verificationCardSetId: %s, numberOfEligibleVoters: %s, savedNumberOfVerificationCards: %s, numberOfVerificationCardIdsToSave: %s]",
				electionEventId, nodeId, verificationCardSetId, numberOfEligibleVoters, savedNumberOfVerificationCards,
				verificationCardIdsToSave.size());

		final ImmutableList<ControlComponentCodeShare> controlComponentCodeShares = generateEncryptedLongReturnCodeSharesService.performGenEncLongCodeShares(
				setupComponentVerificationDataPayload);
		final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload = new ControlComponentCodeSharesPayload(electionEventId,
				verificationCardSetId, chunkId, encryptionGroup, controlComponentCodeShares, nodeId);

		final CryptoPrimitivesSignature controlComponentCodeSharesPayloadSignature = generatePayloadSignature(controlComponentCodeSharesPayload);
		controlComponentCodeSharesPayload.setSignature(controlComponentCodeSharesPayloadSignature);
		LOGGER.info(
				"Successfully signed control component code shares payload [electionEventId: {}, verificationCardSetId: {}, nodeId: {}, chunkId: {}]",
				electionEventId, verificationCardSetId, nodeId, chunkId);

		return controlComponentCodeSharesPayload;
	}

	public ControlComponentCodeSharesPayload onReplay(final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload) {
		checkNotNull(setupComponentVerificationDataPayload);

		final String electionEventId = setupComponentVerificationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationDataPayload.getVerificationCardSetId();
		final int chunkId = setupComponentVerificationDataPayload.getChunkId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		final ImmutableList<ControlComponentCodeShare> controlComponentCodeShares = encryptedLongReturnCodeSharesService.getControlComponentCodeShares(
				chunkId, verificationCardSetId);

		final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload = new ControlComponentCodeSharesPayload(electionEventId,
				verificationCardSetId, chunkId, encryptionGroup, controlComponentCodeShares, nodeId);

		final CryptoPrimitivesSignature controlComponentCodeSharesPayloadSignature = generatePayloadSignature(controlComponentCodeSharesPayload);
		controlComponentCodeSharesPayload.setSignature(controlComponentCodeSharesPayloadSignature);

		return controlComponentCodeSharesPayload;
	}

	public boolean verifyPayloadSignature(final SetupComponentVerificationDataPayload payload) {
		checkNotNull(payload);

		final String electionEventId = payload.getElectionEventId();
		final String verificationCardSetId = payload.getVerificationCardSetId();
		final String payloadId = String.format("[electionEventId: %s, verificationCardSetId: %s, nodeId: %s, chunkId: %s]", electionEventId,
				verificationCardSetId, nodeId, payload.getChunkId());

		final CryptoPrimitivesSignature signature = payload.getSignature();
		checkState(signature != null, "The signature of the setup component verification data payload is null. %s", payloadId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentVerificationData(electionEventId, verificationCardSetId);

		LOGGER.debug("Checking the signature of payload... {}", payloadId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, payload, additionalContextData,
					signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format("Unable to verify the setup component verification data payload signature. %s", payloadId),
					e);
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(SetupComponentVerificationDataPayload.class, payloadId);
		}

		LOGGER.info("Successfully verified the signature of the setup component verification data payload. {}", payloadId);

		return isSignatureValid;
	}

	public SetupComponentVerificationDataPayload deserializeRequest(final ImmutableByteArray messageBytes) {
		checkNotNull(messageBytes);
		try {
			return objectMapper.readValue(messageBytes.elements(), SetupComponentVerificationDataPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to deserialize Setup Component Verification Data Payload", e);
		}
	}

	public ImmutableByteArray serializeResponse(final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload) {
		checkNotNull(controlComponentCodeSharesPayload);

		try {
			return new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentCodeSharesPayload));
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to serialize Control Component Code Shares Payload", e);
		}
	}

	private ImmutableList<String> getVerificationCardIds(final SetupComponentVerificationDataPayload payload) {
		final ImmutableList<SetupComponentVerificationData> returnCodeGenerationInputs = payload.getSetupComponentVerificationData();

		return returnCodeGenerationInputs.stream()
				.map(SetupComponentVerificationData::verificationCardId)
				.collect(toImmutableList());
	}

	private ImmutableList<String> validateAllowList(final SetupComponentVerificationDataPayload payload) {
		final ImmutableList<String> payloadAllowList = payload.getPartialChoiceReturnCodesAllowList();
		payloadAllowList.stream().parallel()
				.forEach(element -> checkArgument(validateBase64Encoded(element).length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH, String.format(
						"At least one element in the partial Choice Return Codes allow list has incorrect length. [element: %s, allowed length: %s]",
						element, BASE64_ENCODED_HASH_OUTPUT_LENGTH)));
		final ImmutableList<String> payloadAllowListSorted = payloadAllowList.stream().sorted().collect(toImmutableList());
		checkArgument(payloadAllowList.equals(payloadAllowListSorted), "The allow list is not lexicographically sorted.");

		return payloadAllowList;
	}

	private CryptoPrimitivesSignature generatePayloadSignature(final ControlComponentCodeSharesPayload payload) {
		final String electionEventId = payload.getElectionEventId();
		final String verificationCardSetId = payload.getVerificationCardSetId();

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentCodeShares(nodeId, electionEventId, verificationCardSetId);

		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);

			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException se) {
			final String message = String.format(
					"Failed to generate the control component code shares payload signature [electionEventId: %s, verificationCardSetId: %s, nodeId: %s, chunkId: %s]",
					electionEventId, verificationCardSetId, nodeId, payload.getChunkId());
			throw new IllegalStateException(message, se);
		}
	}
}
