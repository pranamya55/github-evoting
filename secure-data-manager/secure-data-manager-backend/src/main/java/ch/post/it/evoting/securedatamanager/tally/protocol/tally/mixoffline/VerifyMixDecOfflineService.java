/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyMixDecInput;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyMixDecOfflineAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyMixDecOfflineContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsOutput;
import ch.post.it.evoting.securedatamanager.tally.process.decrypt.IdentifierValidationService;

@Service
@ConditionalOnProperty("role.isTally")
public class VerifyMixDecOfflineService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyMixDecOfflineService.class);

	private final IdentifierValidationService identifierValidationService;
	private final VerifyMixDecOfflineAlgorithm verifyMixDecOfflineAlgorithm;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;

	public VerifyMixDecOfflineService(
			final IdentifierValidationService identifierValidationService,
			final VerifyMixDecOfflineAlgorithm verifyMixDecOfflineAlgorithm,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		this.identifierValidationService = identifierValidationService;
		this.verifyMixDecOfflineAlgorithm = verifyMixDecOfflineAlgorithm;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
	}

	/**
	 * Invokes the VerifyMixDecOffline algorithm.
	 *
	 * @param electionEventId                   the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetContext        the verification card set context. Must be non-null.
	 * @param setupComponentPublicKeys          the setup component public keys. Must be non-null.
	 * @param controlComponentShufflePayloads   the control component shuffle payloads. Must be non-null.
	 * @param getMixnetInitialCiphertextsOutput the output of the algorithm GetMixnetInitialCiphertexts. Must be non-null.
	 */
	public boolean verifyMixDecOffline(final String electionEventId, final VerificationCardSetContext verificationCardSetContext,
			final SetupComponentPublicKeys setupComponentPublicKeys,
			final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads,
			final GetMixnetInitialCiphertextsOutput getMixnetInitialCiphertextsOutput) {
		validateUUID(electionEventId);
		checkNotNull(verificationCardSetContext);
		checkNotNull(setupComponentPublicKeys);
		checkNotNull(controlComponentShufflePayloads);
		checkNotNull(getMixnetInitialCiphertextsOutput);

		final String ballotBoxId = verificationCardSetContext.getBallotBoxId();
		identifierValidationService.validateBallotBoxRelatedIds(electionEventId, ballotBoxId);

		final PrimesMappingTable primesMappingTable = verificationCardSetContext.getPrimesMappingTable();
		final GqGroup encryptionGroup = primesMappingTable.getEncryptionGroup();
		verifyConsistency(encryptionGroup, electionEventId, ballotBoxId, controlComponentShufflePayloads);

		final int numberOfAllowedWriteInsPlusOne = primesMappingTableAlgorithms.getDelta(primesMappingTable);

		final ElGamalMultiRecipientPublicKey electionPublicKey = setupComponentPublicKeys.electionPublicKey();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmjElectionPublicKey = setupComponentPublicKeys.combinedControlComponentPublicKeys()
				.stream()
				.map(ControlComponentPublicKeys::ccmjElectionPublicKey)
				.collect(GroupVector.toGroupVector());
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = setupComponentPublicKeys.electoralBoardPublicKey();

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> initialCiphertexts = getMixnetInitialCiphertextsOutput.mixnetInitialCiphertexts();

		final ImmutableList<VerifiableShuffle> precedingVerifiableShuffledVotes = controlComponentShufflePayloads.stream()
				.map(ControlComponentShufflePayload::getVerifiableShuffle)
				.collect(toImmutableList());
		final ImmutableList<VerifiableDecryptions> precedingVerifiableDecryptedVotes = controlComponentShufflePayloads.stream()
				.map(ControlComponentShufflePayload::getVerifiableDecryptions)
				.collect(toImmutableList());

		final VerifyMixDecOfflineContext verifyMixDecOfflineContext = new VerifyMixDecOfflineContext(encryptionGroup, electionEventId, ballotBoxId,
				numberOfAllowedWriteInsPlusOne, electionPublicKey, ccmjElectionPublicKey, electoralBoardPublicKey);
		final VerifyMixDecInput verifyMixDecInput = new VerifyMixDecInput(initialCiphertexts, precedingVerifiableShuffledVotes,
				precedingVerifiableDecryptedVotes);

		LOGGER.debug("Performing VerifyMixDecOffline algorithm... [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		return verifyMixDecOfflineAlgorithm.verifyMixDecOffline(verifyMixDecOfflineContext, verifyMixDecInput);
	}

	private void verifyConsistency(final GqGroup encryptionGroup, final String electionEventId, final String ballotBoxId,
			final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads) {
		final ImmutableList<Integer> shufflePayloadsNodeIds = controlComponentShufflePayloads.stream()
				.map(ControlComponentShufflePayload::getNodeId)
				.collect(toImmutableList());
		checkState(ControlComponentNode.ids().size() == shufflePayloadsNodeIds.size() && ControlComponentNode.ids()
						.equals(shufflePayloadsNodeIds.toImmutableSet()),
				"Wrong number of control component shuffle payloads.");

		controlComponentShufflePayloads.stream().parallel()
				.forEach(controlComponentShufflePayload -> {
					checkArgument(controlComponentShufflePayload.getEncryptionGroup().equals(encryptionGroup),
							"All control component shuffle payloads must have the same group.");
					checkArgument(controlComponentShufflePayload.getElectionEventId().equals(electionEventId),
							"All control component shuffle payloads must have the same election event id.");
					checkArgument(controlComponentShufflePayload.getBallotBoxId().equals(ballotBoxId),
							"All control component shuffle payloads must have the same ballot box id.");
				});
	}
}
