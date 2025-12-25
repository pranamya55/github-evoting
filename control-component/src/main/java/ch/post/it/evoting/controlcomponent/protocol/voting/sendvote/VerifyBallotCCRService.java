/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;

@Service
public class VerifyBallotCCRService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyBallotCCRService.class);

	private final VerificationCardService verificationCardService;
	private final VerifyBallotCCRAlgorithm verifyBallotCCRAlgorithm;

	@Value("${nodeID}")
	private int nodeId;

	public VerifyBallotCCRService(final VerificationCardService verificationCardService,
			final VerifyBallotCCRAlgorithm verifyBallotCCRAlgorithm) {
		this.verificationCardService = verificationCardService;
		this.verifyBallotCCRAlgorithm = verifyBallotCCRAlgorithm;
	}

	/**
	 * Invokes the VerifyBallotCCR algorithm.
	 *
	 * @param encryptionGroup                      the encryption group. Must be non-null.
	 * @param primesMappingTable                   the primes mapping table. Must be non-null.
	 * @param electionPublicKey                    the election public key. Must be non-null.
	 * @param choiceReturnCodesEncryptionPublicKey the choice return codes encryption public key. Must be non-null.
	 * @param encryptedVerifiableVote              the encrypted vote. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if the inputs have different encryption groups.
	 */
	public boolean verifyBallotCCR(final GqGroup encryptionGroup, final PrimesMappingTable primesMappingTable,
			final ElGamalMultiRecipientPublicKey electionPublicKey, final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey,
			final EncryptedVerifiableVote encryptedVerifiableVote) {
		checkNotNull(encryptionGroup);
		checkNotNull(primesMappingTable);
		checkNotNull(electionPublicKey);
		checkNotNull(choiceReturnCodesEncryptionPublicKey);
		checkNotNull(encryptedVerifiableVote);
		checkArgument(allEqual(
				Stream.of(encryptionGroup, primesMappingTable.getEncryptionGroup(), electionPublicKey.getGroup(),
						choiceReturnCodesEncryptionPublicKey.getGroup(), encryptedVerifiableVote.encryptedVote().getGroup()),
				Function.identity()));

		final ContextIds contextIds = encryptedVerifiableVote.contextIds();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final GqElement verificationCardPublicKey = verificationCardService.getVerificationCard(verificationCardId)
				.verificationCardPublicKey()
				.get(0);

		final VerifyBallotCCRContext verifyBallotCCRContext = new VerifyBallotCCRContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardId(verificationCardId)
				.setPrimesMappingTable(primesMappingTable)
				.setVerificationCardPublicKey(verificationCardPublicKey)
				.setElectionPublicKey(electionPublicKey)
				.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
				.build();

		final VerifyBallotCCRInput verifyBallotCCRInput = new VerifyBallotCCRInput.Builder()
				.setEncryptedVote(encryptedVerifiableVote.encryptedVote())
				.setExponentiatedEncryptedVote(encryptedVerifiableVote.exponentiatedEncryptedVote())
				.setEncryptedPartialChoiceReturnCodes(encryptedVerifiableVote.encryptedPartialChoiceReturnCodes())
				.setExponentiationProof(encryptedVerifiableVote.exponentiationProof())
				.setPlaintextEqualityProof(encryptedVerifiableVote.plaintextEqualityProof())
				.build();

		LOGGER.debug("Performing Verify Ballot CCR algorithm... [contextIds: {}, nodeId: {}]", contextIds, nodeId);

		return verifyBallotCCRAlgorithm.verifyBallotCCR(verifyBallotCCRContext, verifyBallotCCRInput);
	}
}
