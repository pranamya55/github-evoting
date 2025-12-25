/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupMatrix;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

/**
 * Regroups the inputs needed by the CombineEncLongCodeShares algorithm.
 *
 * <ul>
 *    <li>sk<sub>setup</sub>, the setup secret key. Non-null.</li>
 *    <li>c<sub>pCC</sub>, the vector of encrypted, hashed partial Choice Return Codes. Non-null.</li>
 *    <li>c<sub>ck</sub>, the vector of encrypted, hashed Confirmation Keys. Non-null.</li>
 *    <li>(K<sub>1</sub>, K<sub>2</sub>, K<sub>3</sub>, K<sub>4</sub>), the control component's vectors of Voter Choice Return Code Generation public keys. Non-null.</li>
 *    <li>(Kc<sub>1</sub>, Kc<sub>2</sub>, Kc<sub>3</sub>, Kc<sub>4</sub>), the control component's vectors of Voter Vote Cast Return Code Generation public keys. Non-null.</li>
 *    <li>C<sub>expPCC</sub>, the matrix of exponentiated, encrypted, hashed partial Choice Return Codes. Non-null.</li>
 *    <li>(&pi;<sub>expPCC,1</sub>, &pi;<sub>expPCC,2</sub>, &pi;<sub>expPCC,3</sub>, &pi;<sub>expPCC,4</sub>), the control component's proofs of correct exponentiation of the partial Choice Return Codes. Non-null.</li>
 *    <li>C<sub>expCK</sub>, the matrix of exponentiated, encrypted, hashed Confirmation Keys. Non-null.</li>
 *    <li>(&pi;<sub>expCK,1</sub>, &pi;<sub>expCK,2</sub>, &pi;<sub>expCK,3</sub>, &pi;<sub>expCK,4</sub>), the control component's proofs of correct exponentiation of the Confirmation Keys. Non-null.</li>
 * </ul>
 **/
public class CombineEncLongCodeSharesInput {

	private final ElGamalMultiRecipientPrivateKey setupSecretKey;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;
	private final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors;
	private final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors;
	private final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix;
	private final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation;
	private final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKeysMatrix;
	private final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectConfirmationKeysExponentiation;

	private CombineEncLongCodeSharesInput(final ElGamalMultiRecipientPrivateKey setupSecretKey,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys,
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors,
			final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation,
			final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKeysMatrix,
			final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectConfirmationKeysExponentiation) {
		this.setupSecretKey = setupSecretKey;
		this.encryptedHashedPartialChoiceReturnCodes = encryptedHashedPartialChoiceReturnCodes;
		this.encryptedHashedConfirmationKeys = encryptedHashedConfirmationKeys;
		this.voterChoiceReturnCodeGenerationPublicKeysVectors = voterChoiceReturnCodeGenerationPublicKeysVectors;
		this.voterVoteCastReturnCodeGenerationPublicKeysVectors = voterVoteCastReturnCodeGenerationPublicKeysVectors;
		this.exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix = exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix;
		this.proofsOfCorrectPartialChoiceReturnCodesExponentiation = proofsOfCorrectPartialChoiceReturnCodesExponentiation;
		this.exponentiatedEncryptedHashedConfirmationKeysMatrix = exponentiatedEncryptedHashedConfirmationKeysMatrix;
		this.proofsOfCorrectConfirmationKeysExponentiation = proofsOfCorrectConfirmationKeysExponentiation;
	}

	public ElGamalMultiRecipientPrivateKey getSetupSecretKey() {
		return setupSecretKey;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getEncryptedHashedPartialChoiceReturnCodes() {
		return encryptedHashedPartialChoiceReturnCodes;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getEncryptedHashedConfirmationKeys() {
		return encryptedHashedConfirmationKeys;
	}

	public GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> getVoterChoiceReturnCodeGenerationPublicKeysVectors() {
		return voterChoiceReturnCodeGenerationPublicKeysVectors;
	}

	public GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> getVoterVoteCastReturnCodeGenerationPublicKeysVectors() {
		return voterVoteCastReturnCodeGenerationPublicKeysVectors;
	}

	public GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> getExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix() {
		return exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix;
	}

	public GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> getProofsOfCorrectPartialChoiceReturnCodesExponentiation() {
		return proofsOfCorrectPartialChoiceReturnCodesExponentiation;
	}

	public GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> getExponentiatedEncryptedHashedConfirmationKeysMatrix() {
		return exponentiatedEncryptedHashedConfirmationKeysMatrix;
	}

	public GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> getProofsOfCorrectConfirmationKeysExponentiation() {
		return proofsOfCorrectConfirmationKeysExponentiation;
	}

	public static class Builder {
		private ElGamalMultiRecipientPrivateKey setupSecretKey;
		private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes;
		private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedConfirmationKeys;
		private GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors;
		private GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors;
		private GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix;
		private GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation;
		private GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKeysMatrix;
		private GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectConfirmationKeysExponentiation;

		public Builder setSetupSecretKey(final ElGamalMultiRecipientPrivateKey setupSecretKey) {
			this.setupSecretKey = setupSecretKey;
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

		public Builder setVoterChoiceReturnCodeGenerationPublicKeysVectors(
				final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterChoiceReturnCodeGenerationPublicKeysVectors) {
			this.voterChoiceReturnCodeGenerationPublicKeysVectors = voterChoiceReturnCodeGenerationPublicKeysVectors;
			return this;
		}

		public Builder setVoterVoteCastReturnCodeGenerationPublicKeysVectors(
				final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> voterVoteCastReturnCodeGenerationPublicKeysVectors) {
			this.voterVoteCastReturnCodeGenerationPublicKeysVectors = voterVoteCastReturnCodeGenerationPublicKeysVectors;
			return this;
		}

		public Builder setExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix(
				final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix) {
			this.exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix = exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix;
			return this;
		}

		public Builder setProofsOfCorrectPartialChoiceReturnCodesExponentiation(
				final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectPartialChoiceReturnCodesExponentiation) {
			this.proofsOfCorrectPartialChoiceReturnCodesExponentiation = proofsOfCorrectPartialChoiceReturnCodesExponentiation;
			return this;
		}

		public Builder setExponentiatedEncryptedHashedConfirmationKeysMatrix(
				final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> exponentiatedEncryptedHashedConfirmationKeysMatrix) {
			this.exponentiatedEncryptedHashedConfirmationKeysMatrix = exponentiatedEncryptedHashedConfirmationKeysMatrix;
			return this;
		}

		public Builder setProofsOfCorrectConfirmationKeysExponentiation(
				final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> proofsOfCorrectConfirmationKeysExponentiation) {
			this.proofsOfCorrectConfirmationKeysExponentiation = proofsOfCorrectConfirmationKeysExponentiation;
			return this;
		}

		/**
		 * Creates the CombineEncLongCodeShares input. All fields must have been set and be non-null.
		 *
		 * @throws NullPointerException     if any of the fields is null.
		 * @throws IllegalArgumentException if
		 *                                  <ul>
		 *                                    <li>Not all inputs have the same encryption group.</li>
		 *                                    <li>Not all inputs have the same {@link ZqGroup}.</li>
		 *                                    <li>The input's {@link ZqGroup} does not have the same order as the input's encryption group.</li>
		 *                                    <li>n_max > n_sup.</li>
		 *                                    <li>All input elements do not have the same size.</li>
		 *                                    <li>N_E = 0.</li>
		 *                                    <li>The number of K, Kc, &pi;<sub>expPCC</sub>, and &pi;<sub>expCK</sub> is not equal to the number of node ids.</li>
		 *                                    <li>The number of columns in C_expPCC and C_expCK is different from to the number of node ids.</li>
		 *                                    <li>The ciphertexts of c_pCC and C_expPCC do not have the same number of elements.</li>
		 *                                    <li>n is not in (0, n_max].</li>
		 *                                    <li>The ciphertexts of c_ck and C_expCK do not have the same number of elements.</li>
		 *                                    <li>The ciphertexts of c_ck and C_expCK have more elements than expected.</li>
		 *                                  </ul>
		 */
		public CombineEncLongCodeSharesInput build() {
			checkNotNull(setupSecretKey);
			checkNotNull(encryptedHashedPartialChoiceReturnCodes);
			checkNotNull(encryptedHashedConfirmationKeys);
			checkNotNull(voterChoiceReturnCodeGenerationPublicKeysVectors);
			checkNotNull(voterVoteCastReturnCodeGenerationPublicKeysVectors);
			checkNotNull(exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix);
			checkNotNull(proofsOfCorrectPartialChoiceReturnCodesExponentiation);
			checkNotNull(exponentiatedEncryptedHashedConfirmationKeysMatrix);
			checkNotNull(proofsOfCorrectConfirmationKeysExponentiation);

			// Cross-group checks.
			checkArgument(Stream.of(encryptedHashedPartialChoiceReturnCodes.getGroup(), encryptedHashedConfirmationKeys.getGroup(),
									voterChoiceReturnCodeGenerationPublicKeysVectors.getGroup(), voterVoteCastReturnCodeGenerationPublicKeysVectors.getGroup(),
									exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix.getGroup(),
									exponentiatedEncryptedHashedConfirmationKeysMatrix.getGroup())
							.distinct()
							.count() == 1,
					"All input elements must have the same encryption group.");
			checkArgument(Stream.of(setupSecretKey.getGroup(), proofsOfCorrectPartialChoiceReturnCodesExponentiation.getGroup(),
							proofsOfCorrectConfirmationKeysExponentiation.getGroup()).distinct().count() == 1,
					"All input elements must have the same ZqGroup.");
			checkArgument(setupSecretKey.getGroup().hasSameOrderAs(encryptedHashedPartialChoiceReturnCodes.getGroup()),
					"The input's ZqGroup must have the same order as the input's encryption group.");

			// Size checks.
			checkArgument(setupSecretKey.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
					"The size of the setup secret key must be smaller or equal to the maximum supported number of voting options.");
			checkArgument(Stream.of(encryptedHashedPartialChoiceReturnCodes.size(),
									encryptedHashedConfirmationKeys.size(),
									voterChoiceReturnCodeGenerationPublicKeysVectors.getElementSize(),
									voterVoteCastReturnCodeGenerationPublicKeysVectors.getElementSize(),
									exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix.numRows(),
									proofsOfCorrectPartialChoiceReturnCodesExponentiation.getElementSize(),
									exponentiatedEncryptedHashedConfirmationKeysMatrix.numRows(),
									proofsOfCorrectConfirmationKeysExponentiation.getElementSize())
							.distinct()
							.count() == 1,
					"All input elements must have the same size.");
			checkArgument(!encryptedHashedPartialChoiceReturnCodes.isEmpty(), "All input elements must be non-empty.");
			checkArgument(Stream.of(voterChoiceReturnCodeGenerationPublicKeysVectors.size(),
									voterVoteCastReturnCodeGenerationPublicKeysVectors.size(),
									exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix.numColumns(),
									proofsOfCorrectPartialChoiceReturnCodesExponentiation.size(),
									exponentiatedEncryptedHashedConfirmationKeysMatrix.numColumns(),
									proofsOfCorrectConfirmationKeysExponentiation.size())
							.distinct()
							.count() == 1,
					"The size of the control component's vectors must be equal to the number of columns of the matrices.");
			checkArgument(voterChoiceReturnCodeGenerationPublicKeysVectors.size() == ControlComponentNode.ids().size(),
					"There must be exactly %s elements in each control component's vector and %s columns in each matrix.",
					ControlComponentNode.ids().size(), ControlComponentNode.ids().size());

			checkArgument(encryptedHashedPartialChoiceReturnCodes.getElementSize() ==
							exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix.get(0, 0).size(),
					"The size of each encrypted, hashed partial Choice Return Codes must be equal to the size of each exponentiated, encrypted, hashed partial Choice Return Codes.");
			checkArgument(encryptedHashedPartialChoiceReturnCodes.getElementSize() <= setupSecretKey.size(),
					"The size of each encrypted, hashed partial Choice Return Codes must be smaller or equal to the size of the setup secret key. [n: %s, n_max: %s]",
					encryptedHashedPartialChoiceReturnCodes.getElementSize(), setupSecretKey.size());
			checkArgument(encryptedHashedPartialChoiceReturnCodes.getElementSize() > 0,
					"Each encrypted, hashed partial Choice Return Codes must have at least one element.",
					encryptedHashedPartialChoiceReturnCodes.getElementSize(), setupSecretKey.size());

			checkArgument(exponentiatedEncryptedHashedConfirmationKeysMatrix.get(0, 0).size() ==
							encryptedHashedConfirmationKeys.getElementSize(),
					"The size of each exponentiated, encrypted, hashed Confirmation Keys must be equal to the size of each encrypted, hashed Confirmation Keys.");
			checkArgument(encryptedHashedConfirmationKeys.getElementSize() == 1,
					"Each ciphertext of the encrypted, hashed Confirmation Keys must have 1 gamma element and 1 phi element.",
					encryptedHashedPartialChoiceReturnCodes.getElementSize());

			return new CombineEncLongCodeSharesInput(setupSecretKey, encryptedHashedPartialChoiceReturnCodes,
					encryptedHashedConfirmationKeys, voterChoiceReturnCodeGenerationPublicKeysVectors,
					voterVoteCastReturnCodeGenerationPublicKeysVectors, exponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix,
					proofsOfCorrectPartialChoiceReturnCodesExponentiation, exponentiatedEncryptedHashedConfirmationKeysMatrix,
					proofsOfCorrectConfirmationKeysExponentiation);
		}
	}
}
