/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

/**
 * Regroups the inputs needed by the PartialDecryptPCC algorithm.
 *
 * <ul>
 *     <li>E1, the encrypted vote. Not null.</li>
 *     <li>E&#771;1, the exponentiated encrypted vote. Not null.</li>
 *     <li>E2, the encrypted partial Choice Return Codes. Not null.</li>
 *     <li>(sk<sub>CCRj,0</sub>,...,sk<sub>CCRj,&psi;<sub>max</sub>−1</sub>), the CCR<sub>j</sub> Choice Return Codes encryption secret key. Not null.</li>
 *     <li>(pk<sub>CCRj,0</sub>,...,pk<sub>CCRj,&psi;<sub>max</sub>−1</sub>), the CCR<sub>j</sub> Choice Return Codes encryption public key. Not null.</li>
 * </ul>
 */
public class PartialDecryptPCCInput {

	private final ElGamalMultiRecipientCiphertext encryptedVote;
	private final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
	private final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;
	private final ElGamalMultiRecipientPrivateKey ccrjChoiceReturnCodesEncryptionSecretKey;
	private final ElGamalMultiRecipientPublicKey ccrjChoiceReturnCodesEncryptionPublicKey;

	private PartialDecryptPCCInput(final ElGamalMultiRecipientCiphertext encryptedVote,
			final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote,
			final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes,
			final ElGamalMultiRecipientPrivateKey ccrjChoiceReturnCodesEncryptionSecretKey,
			final ElGamalMultiRecipientPublicKey ccrjChoiceReturnCodesEncryptionPublicKey) {
		this.encryptedVote = encryptedVote;
		this.exponentiatedEncryptedVote = exponentiatedEncryptedVote;
		this.encryptedPartialChoiceReturnCodes = encryptedPartialChoiceReturnCodes;
		this.ccrjChoiceReturnCodesEncryptionSecretKey = ccrjChoiceReturnCodesEncryptionSecretKey;
		this.ccrjChoiceReturnCodesEncryptionPublicKey = ccrjChoiceReturnCodesEncryptionPublicKey;
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

	ElGamalMultiRecipientPrivateKey getCcrjChoiceReturnCodesEncryptionSecretKey() {
		return ccrjChoiceReturnCodesEncryptionSecretKey;
	}

	ElGamalMultiRecipientPublicKey getCcrjChoiceReturnCodesEncryptionPublicKey() {
		return ccrjChoiceReturnCodesEncryptionPublicKey;
	}

	GqGroup getGroup() {
		return encryptedVote.getGroup();
	}

	/**
	 * Builder performing input validations and cross-validations before constructing a {@link PartialDecryptPCCInput}.
	 */
	public static class Builder {

		private ElGamalMultiRecipientCiphertext encryptedVote;
		private ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote;
		private ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes;
		private ElGamalMultiRecipientPrivateKey ccrjChoiceReturnCodesEncryptionSecretKey;
		private ElGamalMultiRecipientPublicKey ccrjChoiceReturnCodesEncryptionPublicKey;

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

		public Builder setCcrjChoiceReturnCodesEncryptionSecretKey(final ElGamalMultiRecipientPrivateKey ccrjChoiceReturnCodesEncryptionSecretKey) {
			this.ccrjChoiceReturnCodesEncryptionSecretKey = ccrjChoiceReturnCodesEncryptionSecretKey;
			return this;
		}

		public Builder setCcrjChoiceReturnCodesEncryptionPublicKey(final ElGamalMultiRecipientPublicKey ccrjChoiceReturnCodesEncryptionPublicKey) {
			this.ccrjChoiceReturnCodesEncryptionPublicKey = ccrjChoiceReturnCodesEncryptionPublicKey;
			return this;
		}

		/**
		 * Creates the PartialDecryptPCCInput. All fields must have been set and be non-null.
		 *
		 * @return a new PartialDecryptPCCInput.
		 * @throws NullPointerException     if any of the fields is null.
		 * @throws IllegalArgumentException if
		 *                                  <ul>
		 *                                      <li>The exponentiated encrypted vote is not of size 1.</li>
		 *                                      <li>The secret key does not have as many elements as the public key.</li>
		 *                                      <li>Not all inputs have the same Gq group.</li>
		 *                                      <li>The secret and public key do not match.</li>
		 *                                  </ul>
		 */
		public PartialDecryptPCCInput build() {
			checkNotNull(encryptedVote);
			checkNotNull(exponentiatedEncryptedVote);
			checkNotNull(encryptedPartialChoiceReturnCodes);
			checkNotNull(ccrjChoiceReturnCodesEncryptionSecretKey);
			checkNotNull(ccrjChoiceReturnCodesEncryptionPublicKey);

			// Size checks.
			checkArgument(exponentiatedEncryptedVote.size() == 1, "The exponentiated encrypted vote must be of size 1.");
			checkArgument(ccrjChoiceReturnCodesEncryptionSecretKey.size() == ccrjChoiceReturnCodesEncryptionPublicKey.size(),
					"CCRj Choice Return Codes encryption secret key and public key must have the same size.");

			// Cross-size checks
			checkArgument(encryptedPartialChoiceReturnCodes.size() <= ccrjChoiceReturnCodesEncryptionSecretKey.size(),
					"The encrypted partial Choice Return Codes must have at most as many elements as the CCR Choice Return Codes encryption secret key.");

			// Cross-group checks.
			final ImmutableList<GqGroup> gqGroups = ImmutableList.of(encryptedVote.getGroup(), exponentiatedEncryptedVote.getGroup(),
					encryptedPartialChoiceReturnCodes.getGroup(), ccrjChoiceReturnCodesEncryptionPublicKey.getGroup());
			checkArgument(allEqual(gqGroups.stream(), Function.identity()), "All input encryption groups must be the same.");

			// Keypair validation.
			final ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.from(ccrjChoiceReturnCodesEncryptionSecretKey,
					ccrjChoiceReturnCodesEncryptionPublicKey.getGroup().getGenerator());
			checkArgument(keyPair.getPublicKey().equals(ccrjChoiceReturnCodesEncryptionPublicKey), "The secret and public keys do not match.");

			return new PartialDecryptPCCInput(encryptedVote, exponentiatedEncryptedVote,
					encryptedPartialChoiceReturnCodes, ccrjChoiceReturnCodesEncryptionSecretKey, ccrjChoiceReturnCodesEncryptionPublicKey);
		}
	}
}
