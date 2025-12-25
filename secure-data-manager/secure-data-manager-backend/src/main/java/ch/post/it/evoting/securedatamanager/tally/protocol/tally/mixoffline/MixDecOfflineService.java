/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.securedatamanager.tally.process.decrypt.IdentifierValidationService;

/**
 * Handles the mixing and decryption step of the offline mixing.
 */
@Service
@ConditionalOnProperty("role.isTally")
public class MixDecOfflineService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecOfflineService.class);

	private final MixDecOfflineAlgorithm mixDecOfflineAlgorithm;
	private final IdentifierValidationService identifierValidationService;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;

	public MixDecOfflineService(
			final MixDecOfflineAlgorithm mixDecOfflineAlgorithm,
			final IdentifierValidationService identifierValidationService,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms) {
		this.mixDecOfflineAlgorithm = mixDecOfflineAlgorithm;
		this.identifierValidationService = identifierValidationService;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
	}

	/**
	 * Mix and decrypt the votes in the specified ballot box. Invokes the MixDecOffline algorithm.
	 *
	 * @param electionEventId                the identifier of the election. Must be non-null and a valid UUID.
	 * @param ballotBoxId                    the identifier of the ballot box. Must be non-null and a valid UUID.
	 * @param primesMappingTable             the primes mapping table. Must be non-null.
	 * @param controlComponentShufflePayload the control component shuffle payload of the last node. Must be non-null.
	 * @return the output of the algorithm MixDecOffline.
	 * @throws NullPointerException      if any input is null.
	 * @throws FailedValidationException if any of the IDs is not a valid UUID.
	 * @throws IllegalArgumentException  if number of write-ins + 1 is strictly smaller than 1.
	 */
	public MixDecOfflineOutput mixDecOffline(final String electionEventId, final String ballotBoxId, final PrimesMappingTable primesMappingTable,
			final ControlComponentShufflePayload controlComponentShufflePayload,
			final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		identifierValidationService.validateBallotBoxRelatedIds(electionEventId, ballotBoxId);
		checkNotNull(primesMappingTable);
		checkNotNull(controlComponentShufflePayload);
		checkNotNull(electoralBoardMembersPasswords);
		checkArgument(electoralBoardMembersPasswords.size() >= 2);

		final GqGroup encryptionGroup = primesMappingTable.getEncryptionGroup();
		checkArgument(encryptionGroup.equals(controlComponentShufflePayload.getEncryptionGroup()),
				"The primes mapping table encryption group and the control component shuffle payload encryption group do not match.");

		LOGGER.info("Mixing and decrypting. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts = controlComponentShufflePayload.getVerifiableDecryptions()
				.getCiphertexts();

		final int numberOfAllowedWriteInsPlusOne = primesMappingTableAlgorithms.getDelta(primesMappingTable);

		final MixDecOfflineContext mixDecOfflineContext = new MixDecOfflineContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberOfAllowedWriteInsPlusOne)
				.build();
		final MixDecOfflineInput mixDecOfflineInput = new MixDecOfflineInput(ciphertexts, electoralBoardMembersPasswords);

		final Instant start = Instant.now();

		LOGGER.debug("Performing MixDecOffline algorithm... [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		final MixDecOfflineOutput mixDecOfflineOutput = mixDecOfflineAlgorithm.mixDecOffline(mixDecOfflineContext, mixDecOfflineInput);

		LOGGER.info("Ballot box mixed and decrypted. [electionEventId: {}, ballotBoxId: {}, duration: {}]", electionEventId, ballotBoxId,
				Duration.between(start, Instant.now()));

		return mixDecOfflineOutput;
	}
}
