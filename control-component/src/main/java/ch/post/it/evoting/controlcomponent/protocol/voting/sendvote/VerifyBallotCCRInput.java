/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the input values needed by the VerifyBallotCCR algorithm.
 *
 * <ul>
 *     <li>E1, the encrypted vote. Not null.</li>
 *     <li>E&#771;1, the exponentiated encrypted vote. Not null.</li>
 *     <li>E2, the encrypted partial Choice Return Codes. Not null.</li>
 *     <li>&pi;<sub>Exp</sub>, the exponentiation proof. Not null.</li>
 *     <li>&pi;<sub>EqEnc</sub>, the plaintext equality proof. Not null.</li>
 * </ul>
 */
public class VerifyBallotCCRInput {

	private final ElGamalMultiRecipientCiphertext encryptedVote;
	private final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
	private final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;
	private final ExponentiationProof exponentiationProof;
	private final PlaintextEqualityProof plaintextEqualityProof;

	private VerifyBallotCCRInput(final ElGamalMultiRecipientCiphertext encryptedVote,
			final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote,
			final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes,
			final ExponentiationProof exponentiationProof,
			final PlaintextEqualityProof plaintextEqualityProof) {
		this.encryptedVote = encryptedVote;
		this.exponentiatedEncryptedVote = exponentiatedEncryptedVote;
		this.encryptedPartialChoiceReturnCodes = encryptedPartialChoiceReturnCodes;
		this.exponentiationProof = exponentiationProof;
		this.plaintextEqualityProof = plaintextEqualityProof;
	}

	ElGamalMultiRecipientCiphertext getEncryptedVote() {
		return encryptedVote;
	}

	ElGamalMultiRecipientCiphertext getExponentiatedEncryptedVote() {
		return exponentiatedEncryptedVote;
	}

	ElGamalMultiRecipientCiphertext getEncryptedPartialChoiceReturnCodes() {
		return encryptedPartialChoiceReturnCodes;
	}

	ExponentiationProof getExponentiationProof() {
		return exponentiationProof;
	}

	PlaintextEqualityProof getPlaintextEqualityProof() {
		return plaintextEqualityProof;
	}

	/**
	 * Builder performing input validations and cross-validations before constructing a {@link VerifyBallotCCRInput}.
	 */
	public static class Builder {

		private ElGamalMultiRecipientCiphertext encryptedVote;
		private ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
		private ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;
		private ExponentiationProof exponentiationProof;
		private PlaintextEqualityProof plaintextEqualityProof;

		public Builder setEncryptedVote(final ElGamalMultiRecipientCiphertext encryptedVote) {
			this.encryptedVote = encryptedVote;
			return this;
		}

		public Builder setExponentiatedEncryptedVote(final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote) {
			this.exponentiatedEncryptedVote = exponentiatedEncryptedVote;
			return this;
		}

		public Builder setEncryptedPartialChoiceReturnCodes(
				final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes) {
			this.encryptedPartialChoiceReturnCodes = encryptedPartialChoiceReturnCodes;
			return this;
		}

		public Builder setExponentiationProof(final ExponentiationProof exponentiationProof) {
			this.exponentiationProof = exponentiationProof;
			return this;
		}

		public Builder setPlaintextEqualityProof(final PlaintextEqualityProof plaintextEqualityProof) {
			this.plaintextEqualityProof = plaintextEqualityProof;
			return this;
		}

		/**
		 * Creates the VerifyBallotCCRInput. All fields must have been set and be non-null.
		 *
		 * @return a new VerifyBallotCCRInput.
		 * @throws NullPointerException      if any of the fields is null.
		 * @throws FailedValidationException if the verification card id is not a valid UUID.
		 * @throws IllegalArgumentException  if
		 *                                   <ul>
		 *                                       <li>The encrypted vote and the exponentiated encrypted vote do not have the same size.</li>
		 *                                       <li>The choice return codes encryption public key has a size greater than &psi;<sub>sup</sub>.</li>
		 *                                       <li>Not all inputs have the same Gq group.</li>
		 *                                       <li>The exponentiation proof does not have the same group order as the other inputs.</li>
		 *                                       <li>The plaintext equality proof does not have the same group order as the other inputs.</li>
		 *                                   </ul>
		 */
		public VerifyBallotCCRInput build() {
			checkNotNull(encryptedVote);
			checkNotNull(exponentiatedEncryptedVote);
			checkNotNull(encryptedPartialChoiceReturnCodes);
			checkNotNull(exponentiationProof);
			checkNotNull(plaintextEqualityProof);

			// Size checks.
			final int delta = encryptedVote.size();
			checkArgument(delta > 0, "The size of the encrypted vote must be strictly positive. [delta: %s]", delta);
			checkArgument(delta <= MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1,
					"The size of the encrypted vote must be smaller or equal to the maximum number of writing options + 1. [delta: %s]",
					delta);
			checkArgument(exponentiatedEncryptedVote.size() == 1,
					"The size of the exponentiated encrypted vote must be equal to 1.");
			final int psi = encryptedPartialChoiceReturnCodes.size();
			checkArgument(psi <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
					"The size of the encrypted partial Choice Return Codes must be smaller or equal to the maximum supported number of selections. [psi: %s, psi_sup: %s]",
					psi, MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);

			// Cross-group checks.
			final ImmutableList<GqGroup> gqGroups = ImmutableList.of(encryptedVote.getGroup(), exponentiatedEncryptedVote.getGroup(),
					encryptedPartialChoiceReturnCodes.getGroup());
			checkArgument(allEqual(gqGroups.stream(), Function.identity()), "All input GqGroups must be the same.");

			checkArgument(gqGroups.get(0).hasSameOrderAs(exponentiationProof.getGroup()),
					"The exponentiation proof must have the same group order than the other inputs.");
			checkArgument(gqGroups.get(0).hasSameOrderAs(plaintextEqualityProof.getGroup()),
					"The plaintext equality proof must have the same group order than the other inputs.");

			return new VerifyBallotCCRInput(encryptedVote, exponentiatedEncryptedVote,
					encryptedPartialChoiceReturnCodes, exponentiationProof, plaintextEqualityProof);
		}
	}
}
