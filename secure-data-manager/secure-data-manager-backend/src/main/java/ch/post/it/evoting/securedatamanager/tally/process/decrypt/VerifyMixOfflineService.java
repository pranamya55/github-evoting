/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsOutput;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentBallotBoxPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentShufflePayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentPublicKeysPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentTallyDataPayloadService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.VerifyMixDecOfflineService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline.VerifyVotingClientProofsService;
import ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixonline.GetMixnetInitialCiphertextsService;

/**
 * Handles the verifying steps of the offline mixing: verifying voting client proofs and verifying mixing and decryption.
 */
@Service
@ConditionalOnProperty("role.isTally")
public class VerifyMixOfflineService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyMixOfflineService.class);

	private final SignatureKeystore<Alias> signatureKeystore;
	private final VerifyMixDecOfflineService verifyMixDecOfflineService;
	private final IdentifierValidationService identifierValidationService;
	private final VerifyVotingClientProofsService verifyVotingClientProofsService;
	private final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService;
	private final SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService;
	private final SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService;
	private final ControlComponentShufflePayloadFileRepository controlComponentShufflePayloadFileRepository;
	private final ControlComponentBallotBoxPayloadFileRepository controlComponentBallotBoxPayloadFileRepository;

	VerifyMixOfflineService(
			final SignatureKeystore<Alias> signatureKeystore,
			final VerifyMixDecOfflineService verifyMixDecOfflineService,
			final IdentifierValidationService identifierValidationService,
			final VerifyVotingClientProofsService verifyVotingClientProofsService,
			final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService,
			final SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService,
			final SetupComponentPublicKeysPayloadService setupComponentPublicKeysPayloadService,
			final ControlComponentShufflePayloadFileRepository controlComponentShufflePayloadFileRepository,
			final ControlComponentBallotBoxPayloadFileRepository controlComponentBallotBoxPayloadFileRepository) {
		this.signatureKeystore = signatureKeystore;
		this.verifyMixDecOfflineService = verifyMixDecOfflineService;
		this.identifierValidationService = identifierValidationService;
		this.verifyVotingClientProofsService = verifyVotingClientProofsService;
		this.getMixnetInitialCiphertextsService = getMixnetInitialCiphertextsService;
		this.setupComponentTallyDataPayloadService = setupComponentTallyDataPayloadService;
		this.setupComponentPublicKeysPayloadService = setupComponentPublicKeysPayloadService;
		this.controlComponentShufflePayloadFileRepository = controlComponentShufflePayloadFileRepository;
		this.controlComponentBallotBoxPayloadFileRepository = controlComponentBallotBoxPayloadFileRepository;
	}

	/**
	 * Verifies the control-component's mixing and decryption proofs.
	 *
	 * @param electionEventId            the identifier of the election. Must be non-null and a valid UUID.
	 * @param ballotBoxId                the identifier of the ballot box. Must be non-null and a valid UUID.
	 * @param verificationCardSetContext the verification card set context. Must be non-null.
	 * @return the control component shuffle payload of the last node.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any of the IDs is not a valid UUID.
	 */
	public ControlComponentShufflePayload verifyMixDecrypt(final String electionEventId, final String ballotBoxId,
			final VerificationCardSetContext verificationCardSetContext) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		identifierValidationService.validateBallotBoxRelatedIds(electionEventId, ballotBoxId);
		checkNotNull(verificationCardSetContext);
		checkArgument(verificationCardSetContext.getBallotBoxId().equals(ballotBoxId),
				"The verification card set context does not belong to the ballot box id.");

		// Read setup component public keys.
		final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload = loadSetupComponentPublicKeysPayload(electionEventId);
		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayload.getSetupComponentPublicKeys();

		// Read mix net payloads and verify mix net payloads signatures.
		final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads = loadControlComponentBallotBoxPayloads(
				electionEventId,
				ballotBoxId);
		final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads = loadControlComponentShufflePayloads(electionEventId,
				ballotBoxId);

		// Verify mix net payloads consistency.
		final GqGroup encryptionGroup = verificationCardSetContext.getPrimesMappingTable().getEncryptionGroup();
		verifyConsistency(encryptionGroup, controlComponentBallotBoxPayloads, controlComponentShufflePayloads);

		final SetupComponentTallyDataPayload setupComponentTallyDataPayload = loadSetupComponentTallyDataPayload(electionEventId,
				verificationCardSetContext.getVerificationCardSetId());

		// After consistency checks, we know all payloads have the same votes, so we can pick one (first here).
		final ImmutableList<EncryptedVerifiableVote> confirmedEncryptedVotes = controlComponentBallotBoxPayloads.get(0).getConfirmedEncryptedVotes();

		// The algorithm VerifyVotingClientProofs runs with at least one confirmed vote
		if (!confirmedEncryptedVotes.isEmpty()) {
			checkState(verifyVotingClientProofsService.verifyVotingClientProofs(verificationCardSetContext, setupComponentPublicKeys,
							setupComponentTallyDataPayload, confirmedEncryptedVotes),
					"The voting client's zero-knowledge proofs are invalid. [electionEventId: %s, ballotBoxId: %s]", electionEventId, ballotBoxId);

			LOGGER.info("The voting client's zero-knowledge proofs are valid. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);
		}

		final GetMixnetInitialCiphertextsOutput getMixnetInitialCiphertextsOutput = getMixnetInitialCiphertextsService.getMixnetInitialCiphertexts(
				electionEventId, verificationCardSetContext, setupComponentPublicKeys, controlComponentBallotBoxPayloads);

		LOGGER.info("Retrieved the mixnet's initial ciphertexts. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		checkState(verifyMixDecOfflineService.verifyMixDecOffline(electionEventId, verificationCardSetContext, setupComponentPublicKeys,
						controlComponentShufflePayloads, getMixnetInitialCiphertextsOutput),
				"The online control-component's mixing and decryption proofs are invalid. [electionEventId: %s, ballotBoxId: %s]",
				electionEventId, ballotBoxId);

		LOGGER.info("The online control-component's mixing and decryption proofs are valid. [electionEventId: {}, ballotBoxId: {}]", electionEventId,
				ballotBoxId);

		return Iterables.getLast(controlComponentShufflePayloads);
	}

	private void verifyConsistency(final GqGroup encryptionGroup,
			final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads,
			final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads) {

		checkState(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getEncryptionGroup),
				"All control component ballot box payloads must have the same group.");
		checkState(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getElectionEventId),
				"All control component ballot box payloads must have the same election event id.");
		checkState(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getBallotBoxId),
				"All control component ballot box payloads must have the same ballot box id.");
		checkState(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getConfirmedEncryptedVotes),
				"All control component ballot box payloads must have the same confirmed encrypted votes.");

		final ImmutableList<Integer> ballotBoxPayloadsNodeIds = controlComponentBallotBoxPayloads.stream()
				.map(ControlComponentBallotBoxPayload::getNodeId)
				.collect(toImmutableList());
		checkState(ControlComponentNode.ids().size() == ballotBoxPayloadsNodeIds.size() && ControlComponentNode.ids()
						.equals(ballotBoxPayloadsNodeIds.toImmutableSet()),
				"Wrong number of control component ballot box payloads.");

		checkState(allEqual(controlComponentShufflePayloads.stream(), ControlComponentShufflePayload::getEncryptionGroup),
				"All control component shuffle payloads must have the same group.");
		checkState(allEqual(controlComponentShufflePayloads.stream(), ControlComponentShufflePayload::getElectionEventId),
				"All control component shuffle payloads must have the same election event id.");
		checkState(allEqual(controlComponentShufflePayloads.stream(), ControlComponentShufflePayload::getBallotBoxId),
				"All control component shuffle payloads must have the same ballot box id.");

		final ImmutableList<Integer> shufflePayloadsNodeIds = controlComponentShufflePayloads.stream()
				.map(ControlComponentShufflePayload::getNodeId)
				.collect(toImmutableList());
		checkState(ControlComponentNode.ids().size() == shufflePayloadsNodeIds.size() && ControlComponentNode.ids()
						.equals(shufflePayloadsNodeIds.toImmutableSet()),
				"Wrong number of control component shuffle payloads.");

		// Cross-checks.
		checkState(controlComponentBallotBoxPayloads.get(0).getEncryptionGroup()
						.equals(controlComponentShufflePayloads.get(0).getEncryptionGroup()),
				"The control component ballot box and shuffle payloads must have the same group.");
		checkState(controlComponentBallotBoxPayloads.get(0).getEncryptionGroup().equals(encryptionGroup),
				"The control component ballot box and shuffle payloads must have the expected group.");
		checkState(controlComponentBallotBoxPayloads.get(0).getElectionEventId()
						.equals(controlComponentShufflePayloads.get(0).getElectionEventId()),
				"The control component ballot box and shuffle payloads must have the same election event id.");
		checkState(controlComponentBallotBoxPayloads.get(0).getBallotBoxId().equals(controlComponentShufflePayloads.get(0).getBallotBoxId()),
				"The control component ballot box and shuffle payloads must have the same ballot box id.");

	}

	private SetupComponentPublicKeysPayload loadSetupComponentPublicKeysPayload(final String electionEventId) {
		final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload = setupComponentPublicKeysPayloadService.load(electionEventId);

		final CryptoPrimitivesSignature signature = setupComponentPublicKeysPayload.getSignature();

		checkState(signature != null, "The signature of the setup component public keys payload is null. [electionEventId: %s]", electionEventId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentPublicKeys(electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystore.verifySignature(Alias.SDM_CONFIG, setupComponentPublicKeysPayload, additionalContextData,
					signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of the setup component public keys. [electionEventId: %s]", electionEventId));
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(SetupComponentPublicKeysPayload.class,
					String.format("[electionEventId: %s]", electionEventId));
		}
		return setupComponentPublicKeysPayload;
	}

	private SetupComponentTallyDataPayload loadSetupComponentTallyDataPayload(final String electionEventId, final String verificationCardSetId) {
		final SetupComponentTallyDataPayload setupComponentTallyDataPayload = setupComponentTallyDataPayloadService.load(electionEventId,
				verificationCardSetId);

		final CryptoPrimitivesSignature signature = setupComponentTallyDataPayload.getSignature();

		checkState(signature != null,
				"The signature of the setup component tally data payload is null. [electionEventId: %s, verificationCardSetId: %s]",
				electionEventId, verificationCardSetId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentTallyData(electionEventId, verificationCardSetId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystore.verifySignature(Alias.SDM_CONFIG, setupComponentTallyDataPayload, additionalContextData,
					setupComponentTallyDataPayload.getSignature().signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format(
							"Could not verify the signature of the setup component tally data payload. [electionEventId: %s, verificationCardSetId: %s]",
							electionEventId, verificationCardSetId));
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(SetupComponentTallyDataPayload.class,
					String.format("[electionEventId: %s, verificationCardSetId: %s]", electionEventId, verificationCardSetId));
		}

		return setupComponentTallyDataPayload;
	}

	private ImmutableList<ControlComponentBallotBoxPayload> loadControlComponentBallotBoxPayloads(final String electionEventId,
			final String ballotBoxId) {
		final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads = ControlComponentNode.ids().stream()
				.map(nodeId -> controlComponentBallotBoxPayloadFileRepository.getPayload(electionEventId, ballotBoxId, nodeId))
				.collect(toImmutableList());

		controlComponentBallotBoxPayloads.forEach(payload ->
				verifySignature(Alias.getControlComponentByNodeId(payload.getNodeId()), payload,
						ChannelSecurityContextData.controlComponentBallotBox(payload.getNodeId(), electionEventId, ballotBoxId),
						String.format("[nodeId: %s, electionEventId: %s, ballotBoxId: %s]", payload.getNodeId(), electionEventId, ballotBoxId)));

		return controlComponentBallotBoxPayloads;
	}

	private ImmutableList<ControlComponentShufflePayload> loadControlComponentShufflePayloads(final String electionEventId,
			final String ballotBoxId) {
		final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads = ControlComponentNode.ids().stream()
				.map(nodeId -> controlComponentShufflePayloadFileRepository.getPayload(electionEventId, ballotBoxId, nodeId))
				.collect(toImmutableList());

		controlComponentShufflePayloads.forEach(payload ->
				verifySignature(Alias.getControlComponentByNodeId(payload.getNodeId()), payload,
						ChannelSecurityContextData.controlComponentShuffle(payload.getNodeId(), electionEventId, ballotBoxId),
						String.format("[nodeId: %s, electionEventId: %s, ballotBoxId: %s]", payload.getNodeId(), electionEventId, ballotBoxId)));

		return controlComponentShufflePayloads;
	}

	private void verifySignature(final Alias alias, final SignedPayload payload, final Hashable additionalContext, final String contextMessage) {

		final CryptoPrimitivesSignature signature = payload.getSignature();

		checkState(signature != null, "The signature of the mix net payload is null. %s", contextMessage);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystore.verifySignature(alias, payload, additionalContext, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of payload. [name: %s, context: %s]", payload.getClass().getSimpleName(),
							contextMessage));
		}
		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(payload.getClass(), contextMessage);
		}
	}

}
