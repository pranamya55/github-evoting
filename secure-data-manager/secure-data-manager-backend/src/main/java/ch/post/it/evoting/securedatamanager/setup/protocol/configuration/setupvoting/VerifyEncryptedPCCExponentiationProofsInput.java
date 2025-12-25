/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;

/**
 * Regroups the input values needed by the VerifyEncryptedPCCExponentiationProofs algorithm.
 * <ul>
 *     <li>c<sub>pCC</sub>, the encrypted, hashed partial Choice Return Codes. Non-null.</li>
 *     <li>K<sub>j</sub>, the Voter Choice Return Code Generation public keys. Non-null.</li>
 *     <li>c<sub>expPCC,j</sub>, the exponentiated, encrypted, hashed partial Choice Return Codes. Non-null.</li>
 *     <li>&pi;<sub>expPCC,j</sub>, the proofs of correct exponentiation. Non-null.</li>
 * </ul>
 */
public record VerifyEncryptedPCCExponentiationProofsInput(
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
		GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterChoiceReturnCodeGenerationPublicKeys,
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodes,
		GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation) {

	public VerifyEncryptedPCCExponentiationProofsInput(
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterChoiceReturnCodeGenerationPublicKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation) {
		this.encryptedHashedPartialChoiceReturnCodes = checkNotNull(encryptedHashedPartialChoiceReturnCodes);
		this.voterChoiceReturnCodeGenerationPublicKeys = checkNotNull(voterChoiceReturnCodeGenerationPublicKeys);
		this.exponentiatedEncryptedHashedPartialChoiceReturnCodes = checkNotNull(exponentiatedEncryptedHashedPartialChoiceReturnCodes);
		this.proofsOfCorrectExponentiation = checkNotNull(proofsOfCorrectExponentiation);

		// Cross-group checks.
		checkArgument(Stream.of(encryptedHashedPartialChoiceReturnCodes.getGroup(), voterChoiceReturnCodeGenerationPublicKeys.getGroup(),
						exponentiatedEncryptedHashedPartialChoiceReturnCodes.getGroup()).distinct().count() == 1,
				"All input elements must have the same encryption group.");
		checkArgument(proofsOfCorrectExponentiation.getGroup().hasSameOrderAs(encryptedHashedPartialChoiceReturnCodes.getGroup()),
				"The group of the proofs of correct exponentiation must have the same order as the input's encryption group.");

		// Size checks.
		checkArgument(Stream.of(encryptedHashedPartialChoiceReturnCodes.size(), voterChoiceReturnCodeGenerationPublicKeys.size(),
								exponentiatedEncryptedHashedPartialChoiceReturnCodes.size(), proofsOfCorrectExponentiation.size())
						.distinct()
						.count() == 1,
				"All input elements must have the same size.");
		checkArgument(!encryptedHashedPartialChoiceReturnCodes.isEmpty(), "The number of eligible voters must be strictly positive. [N_E: %s]",
				encryptedHashedPartialChoiceReturnCodes.size());

		checkArgument(
				encryptedHashedPartialChoiceReturnCodes.getElementSize() == exponentiatedEncryptedHashedPartialChoiceReturnCodes.getElementSize(),
				"The encrypted, hashed partial Choice Return Codes must have the same size as the exponentiated, encrypted hashed partial Choice Return Codes.");
		checkArgument(encryptedHashedPartialChoiceReturnCodes.getElementSize() > 0, "The number of voting options must be strictly positive. [n: %s]",
				encryptedHashedPartialChoiceReturnCodes.getElementSize());
		checkArgument(encryptedHashedPartialChoiceReturnCodes.getElementSize() <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
				"The number of voting options must be smaller or equal to the maximum supported number of voting options. [n: %s, n_sup: %s]",
				encryptedHashedPartialChoiceReturnCodes.getElementSize(), MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);

		checkArgument(voterChoiceReturnCodeGenerationPublicKeys.getElementSize() == 1,
				"The Voter Choice Return Code Generation public keys must have 1 element.");
	}
}
