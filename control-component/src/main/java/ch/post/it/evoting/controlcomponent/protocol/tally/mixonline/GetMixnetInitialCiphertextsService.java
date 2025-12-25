/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsInput;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsOutput;

@Service
public class GetMixnetInitialCiphertextsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetMixnetInitialCiphertextsService.class);

	private final BallotBoxService ballotBoxService;
	private final ElectionEventContextService electionEventContextService;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;
	private final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm;

	public GetMixnetInitialCiphertextsService(
			final BallotBoxService ballotBoxService,
			final ElectionEventContextService electionEventContextService,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms,
			final GetMixnetInitialCiphertextsAlgorithm getMixnetInitialCiphertextsAlgorithm) {
		this.ballotBoxService = ballotBoxService;
		this.electionEventContextService = electionEventContextService;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
		this.getMixnetInitialCiphertextsAlgorithm = getMixnetInitialCiphertextsAlgorithm;
	}

	/**
	 * Invokes the GetMixnetInitialCiphertexts algorithm.
	 *
	 * @param encryptionGroup   the encryption group. Must be non-null.
	 * @param electionEventId   the election event id. Must be non-null and a valid UUID.
	 * @param ballotBoxEntity   the ballot box entity. Must be non-null.
	 * @param electionPublicKey the election public key. Must be non-null.
	 * @param confirmedVotes    the list of confirmed votes. Must be non-null.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if the election event id or the ballot box id are not a valid UUID.
	 */
	public GetMixnetInitialCiphertextsOutput getMixnetInitialCiphertexts(final GqGroup encryptionGroup, final String electionEventId,
			final BallotBoxEntity ballotBoxEntity, final ElGamalMultiRecipientPublicKey electionPublicKey,
			final ImmutableList<EncryptedVerifiableVote> confirmedVotes) {
		validateUUID(electionEventId);
		checkNotNull(ballotBoxEntity);
		checkNotNull(electionPublicKey);
		checkNotNull(confirmedVotes);
		validateGetMixnetInitialCiphertextsIsAllowed(electionEventId, ballotBoxEntity, LocalDateTime::now);

		final String ballotBoxId = ballotBoxEntity.getBallotBoxId();
		final int numberOfEligibleVoters = ballotBoxEntity.getNumberOfEligibleVoters();
		final PrimesMappingTable primesMappingTable = ballotBoxService.getPrimesMappingTableByBallotBoxId(ballotBoxId);
		final int numberOfAllowedWriteInsPlusOne = primesMappingTableAlgorithms.getDelta(primesMappingTable);

		final ImmutableMap<String, ElGamalMultiRecipientCiphertext> confirmedEncryptedVotesMap = confirmedVotes.stream()
				.collect(toImmutableMap(
						encryptedVerifiableVote -> encryptedVerifiableVote.contextIds().verificationCardId(),
						EncryptedVerifiableVote::encryptedVote)
				);

		final GetMixnetInitialCiphertextsContext getMixnetInitialCiphertextsContext = new GetMixnetInitialCiphertextsContext(encryptionGroup,
				numberOfEligibleVoters, numberOfAllowedWriteInsPlusOne, electionPublicKey);
		final GetMixnetInitialCiphertextsInput getMixnetInitialCiphertextsInput = new GetMixnetInitialCiphertextsInput(confirmedEncryptedVotesMap);

		LOGGER.debug("Performing GetMixnetInitialCiphertexts algorithm... [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		return getMixnetInitialCiphertextsAlgorithm.getMixnetInitialCiphertexts(getMixnetInitialCiphertextsContext, getMixnetInitialCiphertextsInput);
	}

	private void validateGetMixnetInitialCiphertextsIsAllowed(final String electionEventId, final BallotBoxEntity ballotBoxEntity,
			final Supplier<LocalDateTime> now) {

		final LocalDateTime currentTime = now.get();
		final LocalDateTime electionEndTime = electionEventContextService.getElectionEventFinishTime(electionEventId);

		final boolean afterEndTime = currentTime.isAfter(electionEndTime.plusSeconds(ballotBoxEntity.getGracePeriod()));

		// Test ballot boxes can produce mix net initial ciphertexts at any time. Real ballot boxes can produce mix net initial ciphertexts only after the election event period ended.
		checkState(ballotBoxEntity.isTestBallotBox() || afterEndTime,
				"Cannot produce mix net initial ciphertexts for the ballot box. [isTestBallotBox: %s, finishTime: %s, electionEventId: %s, ballotBoxId: %s]",
				ballotBoxEntity.isTestBallotBox(), electionEndTime, electionEventId, ballotBoxEntity.getBallotBoxId());
	}
}
