/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.TallyComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.ProcessPlaintextsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.ProcessPlaintextsContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.ProcessPlaintextsOutput;

/**
 * Handles the processing plaintexts step of the offline mixing.
 */
@Service
@ConditionalOnProperty("role.isTally")
public class ProcessPlaintextsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPlaintextsService.class);

	private final ProcessPlaintextsAlgorithm processPlaintextsAlgorithm;

	public ProcessPlaintextsService(final ProcessPlaintextsAlgorithm processPlaintextsAlgorithm) {
		this.processPlaintextsAlgorithm = processPlaintextsAlgorithm;
	}

	/**
	 * Invokes the ProcessPlaintexts algorithm.
	 *
	 * @param electionEventId              the identifier of the election. Must be non-null and a valid UUID.
	 * @param ballotBoxId                  the identifier of the ballot box. Must be non-null and a valid UUID.
	 * @param tallyComponentShufflePayload the tally component shuffle payload. Must be non-null.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any of the IDs is not a valid UUID.
	 */
	public ProcessPlaintextsOutput processPlaintexts(final String electionEventId, final String ballotBoxId,
			final TallyComponentShufflePayload tallyComponentShufflePayload, final PrimesMappingTable primesMappingTable) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(tallyComponentShufflePayload);
		checkNotNull(primesMappingTable);

		checkArgument(electionEventId.equals(tallyComponentShufflePayload.getElectionEventId()),
				"The tally component shuffle payload's election event ID must be the same as the given election event ID.");
		checkArgument(ballotBoxId.equals(tallyComponentShufflePayload.getBallotBoxId()),
				"The tally component shuffle payload's ballot box ID must be the same as the given election event ID.");
		checkArgument(tallyComponentShufflePayload.getEncryptionGroup().equals(primesMappingTable.getEncryptionGroup()),
				"The tally component shuffle payload and primes mapping table must have the same group.");

		final GqGroup encryptionGroup = primesMappingTable.getEncryptionGroup();

		final ProcessPlaintextsContext context = new ProcessPlaintextsContext(encryptionGroup, primesMappingTable);

		final GroupVector<ElGamalMultiRecipientMessage, GqGroup> decryptedVotes = tallyComponentShufflePayload.getVerifiablePlaintextDecryption()
				.getDecryptedVotes();

		LOGGER.debug("Performing ProcessPlaintexts algorithm... [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		return processPlaintextsAlgorithm.processPlaintexts(context, decryptedVotes);
	}
}
