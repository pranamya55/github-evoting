/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixonline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsInput;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsOutput;
import ch.post.it.evoting.securedatamanager.tally.process.decrypt.IdentifierValidationService;

@Service
@ConditionalOnProperty("role.isTally")
public class GetMixnetInitialCiphertextsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetMixnetInitialCiphertextsService.class);

	private final IdentifierValidationService identifierValidationService;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;
	private final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm;

	public GetMixnetInitialCiphertextsService(
			final IdentifierValidationService identifierValidationService,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms,
			final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm) {
		this.identifierValidationService = identifierValidationService;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
		this.getMixnetInitialCiphertextsAlgorithm = getMixnetInitialCiphertextsAlgorithm;
	}

	/**
	 * Invokes the GetMixnetInitialCiphertexts algorithm.
	 *
	 * @param electionEventId                   the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetContext        the verification card set context. Must be non-null.
	 * @param setupComponentPublicKeys          the setup component public keys. Must be non-null.
	 * @param controlComponentBallotBoxPayloads the control component ballot box payloads. Must be non-null.
	 */
	public GetMixnetInitialCiphertextsOutput getMixnetInitialCiphertexts(final String electionEventId,
			final VerificationCardSetContext verificationCardSetContext, final SetupComponentPublicKeys setupComponentPublicKeys,
			final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads) {
		validateUUID(electionEventId);
		checkNotNull(verificationCardSetContext);
		checkNotNull(setupComponentPublicKeys);
		checkNotNull(controlComponentBallotBoxPayloads);

		final String ballotBoxId = verificationCardSetContext.getBallotBoxId();
		identifierValidationService.validateBallotBoxRelatedIds(electionEventId, ballotBoxId);

		final PrimesMappingTable primesMappingTable = verificationCardSetContext.getPrimesMappingTable();
		final GqGroup encryptionGroup = primesMappingTable.getEncryptionGroup();
		verifyConsistency(encryptionGroup, electionEventId, ballotBoxId, controlComponentBallotBoxPayloads);

		final int numberOfEligibleVoters = verificationCardSetContext.getNumberOfEligibleVoters();
		final int numberOfWriteInsPlusOne = primesMappingTableAlgorithms.getDelta(primesMappingTable);

		final ElGamalMultiRecipientPublicKey electionPublicKey = setupComponentPublicKeys.electionPublicKey();

		final ImmutableMap<String, ElGamalMultiRecipientCiphertext> confirmedEncryptedVotesMap = controlComponentBallotBoxPayloads.get(0)
				.getConfirmedEncryptedVotes()
				.stream()
				.collect(toImmutableMap(encryptedVerifiableVote -> encryptedVerifiableVote.contextIds().verificationCardId(),
						EncryptedVerifiableVote::encryptedVote));

		final GetMixnetInitialCiphertextsContext getMixnetInitialCiphertextsContext = new GetMixnetInitialCiphertextsContext(encryptionGroup,
				numberOfEligibleVoters, numberOfWriteInsPlusOne, electionPublicKey);
		final GetMixnetInitialCiphertextsInput getMixnetInitialCiphertextsInput = new GetMixnetInitialCiphertextsInput(confirmedEncryptedVotesMap);

		LOGGER.debug("Performing getMixnetInitialCiphertexts algorithm... [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		return getMixnetInitialCiphertextsAlgorithm.getMixnetInitialCiphertexts(getMixnetInitialCiphertextsContext, getMixnetInitialCiphertextsInput);
	}

	private void verifyConsistency(final GqGroup encryptionGroup, final String electionEventId, final String ballotBoxId,
			final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads) {
		final ImmutableList<Integer> shufflePayloadsNodeIds = controlComponentBallotBoxPayloads.stream()
				.map(ControlComponentBallotBoxPayload::getNodeId)
				.collect(toImmutableList());
		checkState(ControlComponentNode.ids().size() == shufflePayloadsNodeIds.size() && ControlComponentNode.ids()
						.equals(shufflePayloadsNodeIds.toImmutableSet()),
				"Wrong number of control component ballot box payloads.");

		controlComponentBallotBoxPayloads.stream().parallel()
				.forEach(controlComponentShufflePayload -> {
					checkArgument(controlComponentShufflePayload.getEncryptionGroup().equals(encryptionGroup),
							"All control component ballot box payloads must have the same group.");
					checkArgument(controlComponentShufflePayload.getElectionEventId().equals(electionEventId),
							"All control component ballot box payloads must have the same election event id.");
					checkArgument(controlComponentShufflePayload.getBallotBoxId().equals(ballotBoxId),
							"All control component ballot box payloads must have the same ballot box id.");
				});
	}
}
