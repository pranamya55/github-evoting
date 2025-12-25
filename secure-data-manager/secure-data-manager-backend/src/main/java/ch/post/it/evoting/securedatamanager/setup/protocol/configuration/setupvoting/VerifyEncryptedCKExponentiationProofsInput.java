/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

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
 * Regroups the input values needed by the VerifyEncryptedCKExponentiationProofs algorithm.
 * <ul>
 *     <li>c<sub>ck</sub>, the encrypted, hashed Confirmation Key. Non-null.</li>
 *     <li>Kc<sub>j</sub>, the Voter Vote Cast Return Code Generation public keys. Non-null.</li>
 *     <li>c<sub>expCK,j</sub>, the exponentiated, encrypted, hashed Confirmation Key. Non-null.</li>
 *     <li>&pi;<sub>expCK,j</sub>, the proofs of correct exponentiation. Non-null.</li>
 * </ul>
 */
public record VerifyEncryptedCKExponentiationProofsInput(GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKey,
														 GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterVoteCastReturnCodeGenerationPublicKeys,
														 GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKey,
														 GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation) {

	public VerifyEncryptedCKExponentiationProofsInput(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKey,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> voterVoteCastReturnCodeGenerationPublicKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKey,
			final GroupVector<ExponentiationProof, ZqGroup> proofsOfCorrectExponentiation) {
		this.encryptedHashedConfirmationKey = checkNotNull(encryptedHashedConfirmationKey);
		this.voterVoteCastReturnCodeGenerationPublicKeys = checkNotNull(voterVoteCastReturnCodeGenerationPublicKeys);
		this.exponentiatedEncryptedHashedConfirmationKey = checkNotNull(exponentiatedEncryptedHashedConfirmationKey);
		this.proofsOfCorrectExponentiation = checkNotNull(proofsOfCorrectExponentiation);

		// Cross-group checks.
		checkArgument(Stream.of(encryptedHashedConfirmationKey.getGroup(), voterVoteCastReturnCodeGenerationPublicKeys.getGroup(),
						exponentiatedEncryptedHashedConfirmationKey.getGroup()).distinct().count() == 1,
				"All input elements must have the same encryption group.");
		checkArgument(proofsOfCorrectExponentiation.getGroup().hasSameOrderAs(encryptedHashedConfirmationKey.getGroup()),
				"The group of the proofs of correct exponentiation must have the same order as the input's encryption group.");

		// Size checks.
		checkArgument(Stream.of(encryptedHashedConfirmationKey.size(), voterVoteCastReturnCodeGenerationPublicKeys.size(),
								exponentiatedEncryptedHashedConfirmationKey.size(), proofsOfCorrectExponentiation.size())
						.distinct()
						.count() == 1,
				"All input elements must have the same size.");
		checkArgument(!encryptedHashedConfirmationKey.isEmpty(), "The number of eligible voters must be strictly positive. [N_E: %s]",
				encryptedHashedConfirmationKey.size());

		checkArgument(exponentiatedEncryptedHashedConfirmationKey.getElementSize() == encryptedHashedConfirmationKey.getElementSize(),
				"The exponentiated, encrypted, hashed Confirmation Key must have the same size as the encrypted, hashed Confirmation Key.");
		checkArgument(encryptedHashedConfirmationKey.getElementSize() == 1,
				"The encrypted, hashed Confirmation Key and the exponentiated, encrypted, hashed Confirmation Key must have one gamma and one phi.");

		checkArgument(voterVoteCastReturnCodeGenerationPublicKeys.getElementSize() == 1,
				"The Voter Vote Cast Return Code Generation public keys must have 1 element.");
	}
}
