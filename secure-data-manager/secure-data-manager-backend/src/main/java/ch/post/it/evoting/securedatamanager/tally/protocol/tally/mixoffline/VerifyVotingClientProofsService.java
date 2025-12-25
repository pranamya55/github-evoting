/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyVotingClientProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyVotingClientProofsContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyVotingClientProofsInput;
import ch.post.it.evoting.securedatamanager.tally.process.decrypt.IdentifierValidationService;

@Service
@ConditionalOnProperty("role.isTally")
public class VerifyVotingClientProofsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyVotingClientProofsService.class);

	private final IdentifierValidationService identifierValidationService;
	private final VerifyVotingClientProofsAlgorithm verifyVotingClientProofsAlgorithm;

	public VerifyVotingClientProofsService(
			final IdentifierValidationService identifierValidationService,
			final VerifyVotingClientProofsAlgorithm verifyVotingClientProofsAlgorithm) {
		this.identifierValidationService = identifierValidationService;
		this.verifyVotingClientProofsAlgorithm = verifyVotingClientProofsAlgorithm;
	}

	/**
	 * Invokes the VerifyVotingClientProofs algorithm.
	 *
	 * @param verificationCardSetContext     the verification card set context. Must be non-null.
	 * @param setupComponentPublicKeys       the setup component public keys. Must be non-null.
	 * @param setupComponentTallyDataPayload the setup component tally data payload. Must be non-null.
	 * @param confirmedEncryptedVotes        the confirmed encrypted votes. Must be non-null.
	 */
	public boolean verifyVotingClientProofs(final VerificationCardSetContext verificationCardSetContext,
			final SetupComponentPublicKeys setupComponentPublicKeys, final SetupComponentTallyDataPayload setupComponentTallyDataPayload,
			final ImmutableList<EncryptedVerifiableVote> confirmedEncryptedVotes) {
		checkNotNull(verificationCardSetContext);
		checkNotNull(setupComponentPublicKeys);
		checkNotNull(setupComponentTallyDataPayload);
		checkNotNull(confirmedEncryptedVotes);

		final String electionEventId = setupComponentTallyDataPayload.getElectionEventId();
		final String ballotBoxId = verificationCardSetContext.getBallotBoxId();
		final String verificationCardSetId = verificationCardSetContext.getVerificationCardSetId();
		final ImmutableList<String> verificationCardIds = setupComponentTallyDataPayload.getVerificationCardIds();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeys = setupComponentTallyDataPayload.getVerificationCardPublicKeys();

		identifierValidationService.validateBallotBoxRelatedIds(electionEventId, ballotBoxId);

		final ImmutableMap<String, ElGamalMultiRecipientPublicKey> verificationCardPublicKeysMap = IntStream.range(0, verificationCardIds.size())
				.boxed()
				.collect(toImmutableMap(verificationCardIds::get, verificationCardPublicKeys::get));

		final PrimesMappingTable primesMappingTable = verificationCardSetContext.getPrimesMappingTable();
		final GqGroup encryptionGroup = primesMappingTable.getEncryptionGroup();
		final VerifyVotingClientProofsContext verifyVotingClientProofsContext = new VerifyVotingClientProofsContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setPrimesMappingTable(primesMappingTable)
				.setNumberOfEligibleVoters(verificationCardSetContext.getNumberOfEligibleVoters())
				.setElectionPublicKey(setupComponentPublicKeys.electionPublicKey())
				.setChoiceReturnCodesEncryptionPublicKey(setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey())
				.build();
		final VerifyVotingClientProofsInput verifyVotingClientProofsInput = new VerifyVotingClientProofsInput(confirmedEncryptedVotes,
				verificationCardPublicKeysMap);

		LOGGER.debug("Performing VerifyVotingClientProofs algorithm... [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		return verifyVotingClientProofsAlgorithm.verifyVotingClientProofs(verifyVotingClientProofsContext, verifyVotingClientProofsInput);
	}

}
