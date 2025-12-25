/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the input values needed for the DecryptPCC algorithm.
 *
 * <ul>
 *     <li>d<sub>j</sub>, CCR<sub>j</sub>'s exponentiated gamma elements. Not null.</li>
 *     <li>(d<sub>j&#770;_1</sub>, d<sub>j&#770;_2</sub>, d<sub>j&#770;_3</sub>), the other CCR's exponentiated gamma elements. Not null.</li>
 *     <li>(&pi;<sub>decPCC, j&#770;_1</sub>, &pi;<sub>decPCC, j&#770;_2</sub>, &pi;<sub>decPCC, j&#770;_3</sub>), the other CCR's exponentiation proofs. Not null.</li>
 *     <li>E1, the encrypted vote. Not null.</li>
 *     <li>E&#771;1, the exponentiated encrypted vote. Not null.</li>
 *     <li>E2, the encrypted partial Choice Return Codes. Not null.</li>
 * </ul>
 */
public class DecryptPCCInput {

	private final GroupVector<GqElement, GqGroup> exponentiatedGammaElements;
	private final GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> otherCcrExponentiatedGammaElements;
	private final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherCcrExponentiationProofs;

	private final ElGamalMultiRecipientCiphertext encryptedVote;
	private final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
	private final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;

	private DecryptPCCInput(final GroupVector<GqElement, GqGroup> exponentiatedGammaElements,
			final GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> otherCcrExponentiatedGammaElements,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherCcrExponentiationProofs,
			final ElGamalMultiRecipientCiphertext encryptedVote,
			final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote,
			final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes) {
		this.exponentiatedGammaElements = exponentiatedGammaElements;
		this.otherCcrExponentiatedGammaElements = otherCcrExponentiatedGammaElements;
		this.otherCcrExponentiationProofs = otherCcrExponentiationProofs;
		this.encryptedVote = encryptedVote;
		this.exponentiatedEncryptedVote = exponentiatedEncryptedVote;
		this.encryptedPartialChoiceReturnCodes = encryptedPartialChoiceReturnCodes;
	}

	GroupVector<GqElement, GqGroup> getExponentiatedGammaElements() {
		return exponentiatedGammaElements;
	}

	GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> getOtherCcrExponentiatedGammaElements() {
		return otherCcrExponentiatedGammaElements;
	}

	GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> getOtherCcrExponentiationProofs() {
		return otherCcrExponentiationProofs;
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

	/**
	 * Builder performing input validations and cross-validations before constructing a {@link DecryptPCCInput}.
	 */
	public static class Builder {

		private GroupVector<GqElement, GqGroup> exponentiatedGammaElements;
		private GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> otherCcrExponentiatedGammaElements;
		private GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherCcrExponentiationProofs;
		private ElGamalMultiRecipientCiphertext encryptedVote;
		private ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
		private ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;

		public Builder setExponentiatedGammaElements(final GroupVector<GqElement, GqGroup> exponentiatedGammaElements) {
			this.exponentiatedGammaElements = exponentiatedGammaElements;
			return this;
		}

		public Builder setOtherCcrExponentiatedGammaElements(
				final GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> otherCcrExponentiatedGammaElements) {
			this.otherCcrExponentiatedGammaElements = otherCcrExponentiatedGammaElements;
			return this;
		}

		public Builder setOtherCcrExponentiationProofs(
				final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> otherCcrExponentiationProofs) {
			this.otherCcrExponentiationProofs = otherCcrExponentiationProofs;
			return this;
		}

		public Builder setEncryptedVote(final ElGamalMultiRecipientCiphertext encryptedVote) {
			this.encryptedVote = encryptedVote;
			return this;
		}

		public Builder setExponentiatedEncryptedVote(final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote) {
			this.exponentiatedEncryptedVote = exponentiatedEncryptedVote;
			return this;
		}

		public Builder setEncryptedPartialChoiceReturnCodes(final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes) {
			this.encryptedPartialChoiceReturnCodes = encryptedPartialChoiceReturnCodes;
			return this;
		}

		/**
		 * Creates the DecryptPCCInput object.
		 *
		 * @throws NullPointerException      if any of the fields are null.
		 * @throws FailedValidationException if the verification card id is not a valid UUID.
		 * @throws IllegalArgumentException  if
		 *                                   <ul>
		 *                                       <li>at least one number of selections is different.</li>
		 *                                       <li>the number of selections is not in the range [1, &psi;<sub>sup</sub>].</li>
		 *                                       <li>the other CCR's lists are not of size 3.</li>
		 *                                       <li>the number of write-ins is not strictly positive.</li>
		 *                                       <li>at least one group is different.</li>
		 *                                       <li>the number of write-ins is strictly greater than the number of selections.</li>
		 *                                   </ul>
		 */
		public DecryptPCCInput build() {
			checkNotNull(exponentiatedGammaElements);
			checkNotNull(otherCcrExponentiatedGammaElements);
			checkNotNull(otherCcrExponentiationProofs);
			checkNotNull(encryptedVote);
			checkNotNull(exponentiatedEncryptedVote);
			checkNotNull(encryptedPartialChoiceReturnCodes);

			// Check sizes
			final ImmutableList<Integer> sizes = ImmutableList.of(exponentiatedGammaElements.size(),
					otherCcrExponentiatedGammaElements.getElementSize(),
					otherCcrExponentiationProofs.getElementSize(), encryptedPartialChoiceReturnCodes.size());
			checkArgument(allEqual(sizes.stream(), Function.identity()),
					"The exponentiated gamma elements, the other CCR's exponentiated gamma elements, the other CCR's exponentiation proofs "
							+ "and the encrypted partial Choice Return Codes must have the same size.");
			checkArgument(!exponentiatedGammaElements.isEmpty(), "The number of selections should be at least 1.");
			checkArgument(exponentiatedGammaElements.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
					"The number of selections should be at most %s.", MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);

			checkArgument(otherCcrExponentiatedGammaElements.size() == 3,
					"There must be exactly 3 vectors of other CCR's exponentiated gamma elements.");
			checkArgument(otherCcrExponentiatedGammaElements.allEqual(GroupVector::size),
					"All other CCR's exponentiated gamma elements must have the same size.");

			checkArgument(otherCcrExponentiationProofs.size() == 3, "There must be exactly 3 vectors of other CCR's exponentiation proofs.");
			checkArgument(otherCcrExponentiationProofs.allEqual(GroupVector::size),
					"All other CCR's exponentiation proof vectors must have the same size.");

			checkArgument(encryptedVote.size() > 0, "The size of the encrypted vote must be at least one.");

			checkArgument(exponentiatedEncryptedVote.size() == 1, "The exponentiated encrypted votes must have exactly one phi element.");

			// Cross-group checks
			final ImmutableList<GqGroup> gqGroups = ImmutableList.of(
					exponentiatedGammaElements.getGroup(), otherCcrExponentiatedGammaElements.getGroup(),
					encryptedVote.getGroup(), exponentiatedEncryptedVote.getGroup(),
					encryptedPartialChoiceReturnCodes.getGroup());
			checkArgument(allEqual(gqGroups.stream(), Function.identity()), "All input GqGroups must be the same.");
			checkArgument(otherCcrExponentiationProofs.getGroup().hasSameOrderAs(encryptedVote.getGroup()),
					"The other CCR's exponentiation proofs' group must have the same order as the encrypted vote's group.");

			// Requires
			final int psi = exponentiatedGammaElements.size();
			final int delta = encryptedVote.size();
			checkArgument(delta - 1 <= psi,
					"The number of write-ins must be smaller or equal to the number of selections. [delta - 1: %s, psi: %s]",
					delta - 1, psi);

			return new DecryptPCCInput(exponentiatedGammaElements, otherCcrExponentiatedGammaElements, otherCcrExponentiationProofs, encryptedVote,
					exponentiatedEncryptedVote, encryptedPartialChoiceReturnCodes);
		}
	}
}
