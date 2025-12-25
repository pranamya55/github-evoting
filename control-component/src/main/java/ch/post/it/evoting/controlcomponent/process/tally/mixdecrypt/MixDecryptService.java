/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentVotesHashPayloadValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.EncryptedVerifiableVoteService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.controlcomponent.protocol.tally.mixonline.MixDecOnlineOutput;
import ch.post.it.evoting.controlcomponent.protocol.tally.mixonline.MixDecOnlineService;
import ch.post.it.evoting.controlcomponent.protocol.tally.mixonline.VerifyMixDecOnlineService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

@Service
class MixDecryptService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptService.class);

	private final ElectionEventService electionEventService;
	private final BallotBoxService ballotBoxService;
	private final ElectionEventContextService electionEventContextService;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;
	private final MixDecOnlineService mixDecOnlineService;
	private final VerifyMixDecOnlineService verifyMixDecOnlineService;
	private final EncryptedVerifiableVoteService encryptedVerifiableVoteService;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;
	private final MixnetInitialCiphertextsService mixnetInitialCiphertextsService;
	private final MixDecryptResultService mixDecryptResultService;

	@Value("${nodeID}")
	private int nodeId;

	MixDecryptService(
			final ElectionEventService electionEventService,
			final BallotBoxService ballotBoxService,
			final ElectionEventContextService electionEventContextService,
			final SetupComponentPublicKeysService setupComponentPublicKeysService,
			final MixDecOnlineService mixDecOnlineService,
			final VerifyMixDecOnlineService verifyMixDecOnlineService,
			final EncryptedVerifiableVoteService encryptedVerifiableVoteService,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms,
			final MixnetInitialCiphertextsService mixnetInitialCiphertextsService,
			final MixDecryptResultService mixDecryptResultService) {
		this.electionEventService = electionEventService;
		this.ballotBoxService = ballotBoxService;
		this.electionEventContextService = electionEventContextService;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
		this.mixDecOnlineService = mixDecOnlineService;
		this.verifyMixDecOnlineService = verifyMixDecOnlineService;
		this.encryptedVerifiableVoteService = encryptedVerifiableVoteService;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
		this.mixnetInitialCiphertextsService = mixnetInitialCiphertextsService;
		this.mixDecryptResultService = mixDecryptResultService;
	}

	public void performVerifyMixDecOnline(final String electionEventId, final String ballotBoxId,
			final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads,
			final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads) {
		// The first node omits the VerifyMixDecOnline algorithm, it has nothing to verify.
		if (nodeId == ControlComponentNode.first().id()) {
			return;
		}

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		validateIdsCorrectness(electionEventId, ballotBoxId);
		validate(electionEventId, ballotBoxId, controlComponentVotesHashPayloads);
		checkNotNull(controlComponentShufflePayloads);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		validateConsistency(encryptionGroup, electionEventId, ballotBoxId, controlComponentShufflePayloads);

		final PrimesMappingTable primesMappingTable = ballotBoxService.getPrimesMappingTableByBallotBoxId(ballotBoxId);
		final int numberOfWriteInsPlusOne = primesMappingTableAlgorithms.getDelta(primesMappingTable);

		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys = setupComponentPublicKeysService.getCcmElectionPublicKeys(
				electionEventId);
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = setupComponentPublicKeysService.getElectoralBoardPublicKey(electionEventId);

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> mixnetInitialCiphertexts = mixnetInitialCiphertextsService.getMixnetInitialCiphertexts(
				electionEventId, ballotBoxId);
		if (!verifyMixDecOnlineService.verifyMixDecOnline(controlComponentShufflePayloads, encryptionGroup, numberOfWriteInsPlusOne,
				ccmElectionPublicKeys, electoralBoardPublicKey, mixnetInitialCiphertexts)) {
			throw new IllegalStateException(String.format(
					"The preceding control-component's mixing and decryption proofs are invalid. [electionEventId: %s, ballotBoxId: %s]",
					electionEventId, ballotBoxId));
		}
		LOGGER.info("VerifyMixDecOnline algorithm successfully performed. "
						+ "The preceding control-component's mixing and decryption proofs are valid. [electionEventId: {}, ballotBoxId: {}]",
				electionEventId, ballotBoxId);
	}

	@Transactional
	public MixDecryptServiceOutput performMixDecOnline(final String electionEventId, final String ballotBoxId,
			final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads,
			final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		validateIdsCorrectness(electionEventId, ballotBoxId);
		validate(electionEventId, ballotBoxId, controlComponentVotesHashPayloads);
		checkNotNull(controlComponentShufflePayloads);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		validateConsistency(encryptionGroup, electionEventId, ballotBoxId, controlComponentShufflePayloads);

		final PrimesMappingTable primesMappingTable = ballotBoxService.getPrimesMappingTableByBallotBoxId(ballotBoxId);
		final int numberOfWriteInsPlusOne = primesMappingTableAlgorithms.getDelta(primesMappingTable);

		final String encryptedConfirmedVotesHash = mixnetInitialCiphertextsService.getEncryptedConfirmedVotesHash(electionEventId, ballotBoxId);

		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys = setupComponentPublicKeysService.getCcmElectionPublicKeys(
				electionEventId);
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = setupComponentPublicKeysService.getElectoralBoardPublicKey(electionEventId);

		final MixDecOnlineOutput mixDecOnlineOutput;
		if (nodeId == ControlComponentNode.first().id()) {
			// The first CCM uses the mix net initial ciphertexts from the internal view.
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> mixnetInitialCiphertexts = mixnetInitialCiphertextsService.getMixnetInitialCiphertexts(
					electionEventId, ballotBoxId);
			mixDecOnlineOutput = mixDecOnlineService.mixDecOnline(controlComponentVotesHashPayloads, encryptionGroup, numberOfWriteInsPlusOne,
					ccmElectionPublicKeys, electoralBoardPublicKey, encryptedConfirmedVotesHash, mixnetInitialCiphertexts);
		} else {
			// The other CCM use the partially decrypted votes from the previous CCM.
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes = controlComponentShufflePayloads.stream()
					.map(ControlComponentShufflePayload::getVerifiableDecryptions)
					.collect(toImmutableList())
					.getLast()
					.getCiphertexts();
			mixDecOnlineOutput = mixDecOnlineService.mixDecOnline(controlComponentVotesHashPayloads, encryptionGroup, numberOfWriteInsPlusOne,
					ccmElectionPublicKeys, electoralBoardPublicKey, encryptedConfirmedVotesHash, partiallyDecryptedVotes);
		}
		LOGGER.info("MixDecOnline algorithm successfully performed. Ballot box successfully mixed. [electionEventId: {}, ballotBoxId: {}]",
				electionEventId, ballotBoxId);

		mixDecryptResultService.save(electionEventId, ballotBoxId, mixDecOnlineOutput);

		final MixDecryptServiceOutput mixDecryptServiceOutput = createMixDecryptServiceOutput(electionEventId, ballotBoxId, encryptionGroup,
				mixDecOnlineOutput.verifiableShuffle(), mixDecOnlineOutput.verifiableDecryptions());
		LOGGER.info("Control component shuffle payload and Control component ballot box payload retrieved. [electionEventId: {}, ballotBoxId: {}]",
				electionEventId, ballotBoxId);

		return mixDecryptServiceOutput;
	}

	public MixDecryptServiceOutput createMixDecryptServiceOutput(final String electionEventId, final String ballotBoxId) {
		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		final VerifiableShuffle verifiableShuffle = mixDecryptResultService.getVerifiableShuffle(electionEventId, ballotBoxId);
		final VerifiableDecryptions verifiableDecryptions = mixDecryptResultService.getVerifiableDecryptions(electionEventId, ballotBoxId);

		return createMixDecryptServiceOutput(electionEventId, ballotBoxId, encryptionGroup, verifiableShuffle, verifiableDecryptions);
	}

	private MixDecryptServiceOutput createMixDecryptServiceOutput(final String electionEventId, final String ballotBoxId,
			final GqGroup encryptionGroup, final VerifiableShuffle verifiableShuffle, final VerifiableDecryptions verifiableDecryptions) {

		final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByBallotBoxId(ballotBoxId);
		final String verificationCardSetId = ballotBoxEntity.getVerificationCardSetEntity().getVerificationCardSetId();
		final ImmutableList<EncryptedVerifiableVote> confirmedVotes = encryptedVerifiableVoteService.getConfirmedVotes(verificationCardSetId);
		final ControlComponentBallotBoxPayload controlComponentBallotBoxPayload = new ControlComponentBallotBoxPayload(encryptionGroup,
				electionEventId, ballotBoxId, nodeId, confirmedVotes);

		final ControlComponentShufflePayload controlComponentShufflePayload = new ControlComponentShufflePayload(encryptionGroup, electionEventId,
				ballotBoxId, nodeId, verifiableShuffle, verifiableDecryptions);

		return new MixDecryptServiceOutput(controlComponentBallotBoxPayload, controlComponentShufflePayload);
	}

	private void validateConsistency(final GqGroup encryptionGroup, final String electionEventId, final String ballotBoxId,
			final ImmutableList<ControlComponentShufflePayload> shufflePayloads) {
		validateShufflePayload(encryptionGroup, electionEventId, ballotBoxId, shufflePayloads);
		validateMixIsAllowed(electionEventId, ballotBoxId, LocalDateTime::now);
	}

	private void validateIdsCorrectness(final String electionEventId, final String ballotBoxId) {
		checkArgument(electionEventService.exists(electionEventId), "The given election event ID does not exist. [electionEventId: %s]",
				electionEventId);
		checkArgument(ballotBoxService.existsForElectionEventId(ballotBoxId, electionEventId),
				"The given ballot box ID does not exist for the given election event ID. [ballotBoxId: %s, electionEventId: %s]", ballotBoxId,
				electionEventId);
	}

	@VisibleForTesting
	void validateShufflePayload(final GqGroup encryptionGroup, final String electionEventId, final String ballotBoxId,
			final ImmutableList<ControlComponentShufflePayload> shufflePayloads) {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(shufflePayloads);
		checkArgument(shufflePayloads.size() == nodeId - 1,
				"There must be exactly the expected number of shuffle payloads. [expected: %s, actual: %s]", nodeId - 1, shufflePayloads.size());

		shufflePayloads.forEach(payload -> {
			checkState(electionEventId.equals(payload.getElectionEventId()),
					"Election event ID must be identical in shuffle payload. [expected: %s, actual: %s]", electionEventId,
					payload.getElectionEventId());
			checkState(ballotBoxId.equals(payload.getBallotBoxId()),
					"Ballot box ID must be identical in shuffle payload. [expected: %s, actual: %s]", ballotBoxId,
					payload.getBallotBoxId());
			checkState(encryptionGroup.equals(payload.getEncryptionGroup()),
					"Gq groups must be identical in shuffle payload. [expected: %s, actual: %s]", encryptionGroup,
					payload.getEncryptionGroup());
		});

		final ImmutableSet<Integer> actualPayloadNodeIds = shufflePayloads.stream()
				.map(ControlComponentShufflePayload::getNodeId)
				.collect(toImmutableSet());
		final ImmutableSet<Integer> expectedPayloadNodeIds = IntStream.range(1, nodeId).boxed().collect(toImmutableSet());

		checkState(actualPayloadNodeIds.containsAll(expectedPayloadNodeIds),
				"Payloads must come from expected nodes. [expected: %s, actual: %s]", expectedPayloadNodeIds, actualPayloadNodeIds);
	}

	@VisibleForTesting
	void validateMixIsAllowed(final String electionEventId, final String ballotBoxId, final Supplier<LocalDateTime> now) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(now);

		final LocalDateTime currentTime = now.get();
		final LocalDateTime electionEndTime = electionEventContextService.getElectionEventFinishTime(electionEventId);
		final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByBallotBoxId(ballotBoxId);

		final boolean afterEndTime = currentTime.isAfter(electionEndTime.plusSeconds(ballotBoxEntity.getGracePeriod()));

		// Test ballot boxes can be mixed and decrypted at any time. Real ballot boxes can be mixed and decrypted only after the election event period ended.
		checkState(ballotBoxEntity.isTestBallotBox() || afterEndTime,
				"The ballot box can not be mixed. [isTestBallotBox: %s, finishTime: %s, electionEventId: %s, ballotBoxId: %s]",
				ballotBoxEntity.isTestBallotBox(), electionEndTime, electionEventId, ballotBoxId);
	}

	record MixDecryptServiceOutput(ControlComponentBallotBoxPayload controlComponentBallotBoxPayload,
								   ControlComponentShufflePayload controlComponentShufflePayload) {

		MixDecryptServiceOutput {
			checkNotNull(controlComponentBallotBoxPayload);
			checkNotNull(controlComponentShufflePayload);
		}

	}

}
