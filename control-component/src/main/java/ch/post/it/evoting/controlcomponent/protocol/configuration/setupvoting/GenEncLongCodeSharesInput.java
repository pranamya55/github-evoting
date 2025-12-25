/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the input values needed by the GenEncLongCodeShares algorithm.
 *
 * <ul>
 *     <li>k'<sub>j</sub>, the CCR<sub>j</sub> Return Codes Generation secret key. Not null.</li>
 *     <li>c<sub>pCC</sub>, a vector of encrypted, hashed partial Choice Return Codes. Not null.</li>
 *     <li>c<sub>ck</sub>, a vector of encrypted, hashed Confirmation Keys. Not null.</li>
 * </ul>
 */
public class GenEncLongCodeSharesInput {

	private final ZqElement returnCodesGenerationSecretKey;
	// The verification card public keys are needed for saving the verification cards to the database.
	private final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeys;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;

	private GenEncLongCodeSharesInput(
			final ZqElement returnCodesGenerationSecretKey,
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeys,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys) {
		this.returnCodesGenerationSecretKey = returnCodesGenerationSecretKey;
		this.verificationCardPublicKeys = verificationCardPublicKeys;
		this.encryptedHashedPartialChoiceReturnCodes = encryptedHashedPartialChoiceReturnCodes;
		this.encryptedHashedConfirmationKeys = encryptedHashedConfirmationKeys;
	}

	ZqElement getReturnCodesGenerationSecretKey() {
		return returnCodesGenerationSecretKey;
	}

	GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> getVerificationCardPublicKeys() {
		return verificationCardPublicKeys;
	}

	GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getEncryptedHashedPartialChoiceReturnCodes() {
		return encryptedHashedPartialChoiceReturnCodes;
	}

	GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getEncryptedHashedConfirmationKeys() {
		return encryptedHashedConfirmationKeys;
	}

	/**
	 * Builder performing input validations and cross-validations before constructing a {@link GenEncLongCodeSharesInput}.
	 */
	public static class Builder {

		private ZqElement returnCodesGenerationSecretKey;
		private GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeys;
		private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
		private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;

		public Builder setReturnCodesGenerationSecretKey(final ZqElement returnCodesGenerationSecretKey) {
			this.returnCodesGenerationSecretKey = returnCodesGenerationSecretKey;
			return this;
		}

		public Builder setVerificationCardPublicKeys(final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeys) {
			this.verificationCardPublicKeys = verificationCardPublicKeys;
			return this;
		}

		public Builder setEncryptedHashedPartialChoiceReturnCodes(
				final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes) {
			this.encryptedHashedPartialChoiceReturnCodes = encryptedHashedPartialChoiceReturnCodes;
			return this;
		}

		public Builder setEncryptedHashedConfirmationKeys(
				final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys) {
			this.encryptedHashedConfirmationKeys = encryptedHashedConfirmationKeys;
			return this;
		}

		/**
		 * Creates the GenEncLongCodeSharesInput. All fields must have been set and be non-null.
		 *
		 * @return a new GenEncLongCodeSharesInput.
		 * @throws NullPointerException      if any of the fields is null.
		 * @throws FailedValidationException if any of the verification card ids do not comply with the required UUID format
		 * @throws IllegalArgumentException  if
		 *                                   <ul>
		 *                                       <li>All list/vectors do not have the exactly same size.</li>
		 *                                       <li>The partial Choice Return Codes and Confirmation Keys do not have the same group order.</li>
		 *                                       <li>The verification card IDs contain duplicated values.</li>
		 *                                   </ul>
		 */
		public GenEncLongCodeSharesInput build() {
			checkNotNull(returnCodesGenerationSecretKey);
			checkNotNull(verificationCardPublicKeys);
			checkNotNull(encryptedHashedPartialChoiceReturnCodes);
			checkNotNull(encryptedHashedConfirmationKeys);

			// Size checks.
			final int N_E = verificationCardPublicKeys.size();
			checkArgument(encryptedHashedPartialChoiceReturnCodes.size() == N_E,
					"The vector encrypted, hashed partial Choice Return Codes is of incorrect size [size: expected: %s, actual: %s]",
					N_E, encryptedHashedPartialChoiceReturnCodes.size());
			checkArgument(encryptedHashedConfirmationKeys.size() == N_E,
					"The vector encrypted, hashed Confirmation Keys is of incorrect size [size: expected: %s, actual: %s]",
					N_E, encryptedHashedConfirmationKeys.size());
			checkArgument(encryptedHashedConfirmationKeys.getElementSize() == 1,
					"The encrypted hashed Confirmation keys must be of size 1. [actual phi: %s]", encryptedHashedConfirmationKeys.getElementSize());

			// Cross-group checks.
			checkArgument(encryptedHashedPartialChoiceReturnCodes.getGroup().hasSameOrderAs(encryptedHashedConfirmationKeys.getGroup()),
					"The Vector of exponentiated, encrypted, hashed partial Choice Return Codes and the Vector of exponentiated, encrypted, hashed Confirmation Keys do not have the same group order.");
			checkArgument(verificationCardPublicKeys.getGroup().equals(encryptedHashedPartialChoiceReturnCodes.getGroup()),
					"The exponentiated, encrypted, hashed partial Choice Return Codes and the verification card public keys must have the same group.");

			return new GenEncLongCodeSharesInput(returnCodesGenerationSecretKey, verificationCardPublicKeys,
					encryptedHashedPartialChoiceReturnCodes, encryptedHashedConfirmationKeys);
		}
	}
}
