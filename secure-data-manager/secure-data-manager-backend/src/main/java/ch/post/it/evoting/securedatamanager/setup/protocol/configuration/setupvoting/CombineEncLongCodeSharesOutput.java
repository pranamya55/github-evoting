/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;

/**
 * Regroups the output values returned by the CombineEncLongCodeShares algorithm.
 *
 * <ul>
 *     <li>c<sub>pC</sub>, the vector of encrypted pre-Choice Return Codes. Non-null.</li>
 *     <li>p<sub>VCC</sub>, the vector of pre-Vote Cast Return Codes. Non-null</li>
 *     <li>L<sub>lVCC</sub>, the long Vote Cast Return Codes allow list. Non-null</li>
 * </ul>
 */
public class CombineEncLongCodeSharesOutput {

	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodesVector;
	private final GroupVector<GqElement, GqGroup> preVoteCastReturnCodesVector;
	private final ImmutableList<String> longVoteCastReturnCodesAllowList;

	private CombineEncLongCodeSharesOutput(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodesVector,
			final GroupVector<GqElement, GqGroup> preVoteCastReturnCodesVector, final ImmutableList<String> longVoteCastReturnCodesAllowList) {

		this.encryptedPreChoiceReturnCodesVector = encryptedPreChoiceReturnCodesVector;
		this.preVoteCastReturnCodesVector = preVoteCastReturnCodesVector;
		this.longVoteCastReturnCodesAllowList = longVoteCastReturnCodesAllowList;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getEncryptedPreChoiceReturnCodesVector() {
		return encryptedPreChoiceReturnCodesVector;
	}

	public GroupVector<GqElement, GqGroup> getPreVoteCastReturnCodesVector() {
		return preVoteCastReturnCodesVector;
	}

	public ImmutableList<String> getLongVoteCastReturnCodesAllowList() {
		return longVoteCastReturnCodesAllowList;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final CombineEncLongCodeSharesOutput that = (CombineEncLongCodeSharesOutput) o;
		return encryptedPreChoiceReturnCodesVector.equals(that.encryptedPreChoiceReturnCodesVector) && preVoteCastReturnCodesVector.equals(
				that.preVoteCastReturnCodesVector) && longVoteCastReturnCodesAllowList.equals(that.longVoteCastReturnCodesAllowList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptedPreChoiceReturnCodesVector, preVoteCastReturnCodesVector, longVoteCastReturnCodesAllowList);
	}

	public static class Builder {
		private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodesVector;
		private GroupVector<GqElement, GqGroup> preVoteCastReturnCodesVector;
		private ImmutableList<String> longVoteCastReturnCodesAllowList;

		public Builder setEncryptedPreChoiceReturnCodesVector(
				final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodesVector) {
			this.encryptedPreChoiceReturnCodesVector = encryptedPreChoiceReturnCodesVector;
			return this;
		}

		public Builder setPreVoteCastReturnCodesVector(final GroupVector<GqElement, GqGroup> preVoteCastReturnCodesVector) {
			this.preVoteCastReturnCodesVector = preVoteCastReturnCodesVector;
			return this;
		}

		public Builder setLongVoteCastReturnCodesAllowList(final ImmutableList<String> longVoteCastReturnCodesAllowList) {
			this.longVoteCastReturnCodesAllowList = longVoteCastReturnCodesAllowList;
			return this;
		}

		/**
		 * Creates the CombineEncLongCodeSharesOutput. All fields must have been set and be non-null.
		 *
		 * @return a new CombineEncLongCodeSharesOutput.
		 * @throws NullPointerException     if any of the fields is null.
		 * @throws IllegalArgumentException if
		 *                                  <ul>
		 *                                      <li>all lists/vectors do not have the exactly same size.</li>
		 *                                      <li>the vector of exponentiated, encrypted, hashed partial Choice Return Codes has elements with size greater than n<sub>sup</sub>.</li>
		 *                                      <li>the long vote cast return codes allow list is empty.</li>
		 *                                      <li>the long vote cast return codes allow list contains elements with size different than l<sub>HB64</sub>.</li>
		 *                                      <li>the long vote cast return codes allow list does not contain base64 encoded elements.</li>
		 *                                      <li>the vector of exponentiated, encrypted, hashed partial Choice Return Codes and the
		 *                                      vector of exponentiated, encrypted, hashed Confirmation Keys do not have the same group order.</li>
		 *                                  </ul>
		 */
		public CombineEncLongCodeSharesOutput build() {
			checkNotNull(encryptedPreChoiceReturnCodesVector);
			checkNotNull(preVoteCastReturnCodesVector);
			checkNotNull(longVoteCastReturnCodesAllowList);

			// Size checks.
			checkArgument(!encryptedPreChoiceReturnCodesVector.isEmpty(),
					"The vector of encrypted pre-Choice Return Codes must have more than zero elements.");

			final int N_E = encryptedPreChoiceReturnCodesVector.size();
			checkArgument(preVoteCastReturnCodesVector.size() == N_E,
					"The vector of pre-Vote Cast Return Codes is of incorrect size [size: expected: %s, actual: %s].",
					N_E, preVoteCastReturnCodesVector.size());
			checkArgument(longVoteCastReturnCodesAllowList.size() == N_E,
					"The long Vote Cast Return Codes allow list is of incorrect size [size: expected: %s, actual: %s].",
					N_E, longVoteCastReturnCodesAllowList.size());

			checkArgument(encryptedPreChoiceReturnCodesVector.getElementSize() <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
					"The size of the encrypted pre-Choice Return Codes must be smaller or equal to the maximum supported number of voting options.");

			longVoteCastReturnCodesAllowList.forEach(lVCC -> {
				checkArgument(lVCC.length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
						"The long Vote Cast Return Code must be of size l_HB64. [size: %s, l_HB64: %s]", lVCC.length(),
						BASE64_ENCODED_HASH_OUTPUT_LENGTH);
				validateBase64Encoded(lVCC);
			});

			// Cross-group checks.
			checkArgument(preVoteCastReturnCodesVector.getGroup().equals(encryptedPreChoiceReturnCodesVector.getGroup()),
					"The vector of encrypted pre-Choice Return Codes and the vector of pre-Vote Cast Return Codes do not have the same group order.");

			return new CombineEncLongCodeSharesOutput(encryptedPreChoiceReturnCodesVector, preVoteCastReturnCodesVector,
					longVoteCastReturnCodesAllowList);
		}
	}
}
