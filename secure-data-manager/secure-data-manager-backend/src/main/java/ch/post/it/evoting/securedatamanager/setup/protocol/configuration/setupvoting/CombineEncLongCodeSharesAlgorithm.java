/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupMatrix;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

/**
 * Implements the CombineEncLongCodeShares algorithm.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class CombineEncLongCodeSharesAlgorithm {

	private final Hash hash;
	private final ElGamal elGamal;
	private final Base64 base64;
	private final VerifyEncryptedPCCExponentiationProofsAlgorithm verifyEncryptedPCCExponentiationProofsAlgorithm;
	private final VerifyEncryptedCKExponentiationProofsAlgorithm verifyEncryptedCKExponentiationProofsAlgorithm;

	public CombineEncLongCodeSharesAlgorithm(
			final Hash hash,
			final ElGamal elGamal,
			final Base64 base64,
			final VerifyEncryptedPCCExponentiationProofsAlgorithm verifyEncryptedPCCExponentiationProofsAlgorithm,
			final VerifyEncryptedCKExponentiationProofsAlgorithm verifyEncryptedCKExponentiationProofsAlgorithm) {
		this.hash = hash;
		this.elGamal = elGamal;
		this.base64 = base64;
		this.verifyEncryptedPCCExponentiationProofsAlgorithm = verifyEncryptedPCCExponentiationProofsAlgorithm;
		this.verifyEncryptedCKExponentiationProofsAlgorithm = verifyEncryptedCKExponentiationProofsAlgorithm;
	}

	/**
	 * Combines the control components’ encrypted long return code shares.
	 *
	 * @param context the {@link CombineEncLongCodeSharesContext}. Must be non-null.
	 * @param input   the {@link CombineEncLongCodeSharesInput} containing all needed inputs. Must be non-null.
	 * @return the combined control components’ encrypted long return code shares in a {@link CombineEncLongCodeSharesOutput}.
	 * @throws NullPointerException if the context or input is null.
	 */
	@SuppressWarnings("java:S117")
	public CombineEncLongCodeSharesOutput combineEncLongCodeShares(final CombineEncLongCodeSharesContext context,
			final CombineEncLongCodeSharesInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.getEncryptionGroup().equals(input.getExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix().getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final GqGroup p_q_g = context.getEncryptionGroup();
		final String ee = context.getElectionEventId();
		final String vcs = context.getVerificationCardSetId();
		final ImmutableList<String> vc = context.getVerificationCardIds();
		final int n = context.getNumberOfVotingOptions();
		final int n_max = context.getMaximumNumberOfVotingOptions();
		final int N_E = vc.size();

		// Input.
		final ElGamalMultiRecipientPrivateKey sk_setup = input.getSetupSecretKey();
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_pCC = input.getEncryptedHashedPartialChoiceReturnCodes();
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_ck = input.getEncryptedHashedConfirmationKeys();
		final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> K = input.getVoterChoiceReturnCodeGenerationPublicKeysVectors();
		final GroupVector<GroupVector<ElGamalMultiRecipientPublicKey, GqGroup>, GqGroup> Kc = input.getVoterVoteCastReturnCodeGenerationPublicKeysVectors();
		final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> C_expPCC = input.getExponentiatedEncryptedHashedPartialChoiceReturnCodesMatrix();
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> pi_expPCC = input.getProofsOfCorrectPartialChoiceReturnCodesExponentiation();
		final GroupMatrix<ElGamalMultiRecipientCiphertext, GqGroup> C_expCK = input.getExponentiatedEncryptedHashedConfirmationKeysMatrix();
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> pi_expCK = input.getProofsOfCorrectConfirmationKeysExponentiation();

		// Cross-checks.
		checkArgument(sk_setup.size() == n_max, "The size of the setup secret key must be equal to the maximum number of voting options.");
		checkArgument(C_expPCC.get(0, 0).size() == n,
				"The size of each ciphertext in C_expPCC must be equal to the number of voting options.");
		checkArgument(C_expPCC.numRows() == N_E,
				"The number of rows of the matrices C_expPCC and C_expCK must be equal to the number of eligible voters.");

		// Operation.
		ControlComponentNode.ids().stream()
				// for j in [1, 4]
				.forEach(j -> {
					final VerifyEncryptedExponentiationProofsContext contextJ = new VerifyEncryptedExponentiationProofsContext(p_q_g, j, ee, vc, n);

					final VerifyEncryptedPCCExponentiationProofsInput inputPccJ = new VerifyEncryptedPCCExponentiationProofsInput(c_pCC, K.get(j - 1),
							C_expPCC.getColumn(j - 1), pi_expPCC.get(j - 1)); // Due to zero-indexing.
					final boolean piExpPccVerif_j = verifyEncryptedPCCExponentiationProofsAlgorithm.verifyEncryptedPCCExponentiationProofs(contextJ,
							inputPccJ);

					final VerifyEncryptedCKExponentiationProofsInput inputCkJ = new VerifyEncryptedCKExponentiationProofsInput(c_ck, Kc.get(j - 1),
							C_expCK.getColumn(j - 1), pi_expCK.get(j - 1)); // Due to zero-indexing.
					final boolean piExpCkVerif_j = verifyEncryptedCKExponentiationProofsAlgorithm.verifyEncryptedCKExponentiationProofs(contextJ,
							inputCkJ);

					checkState(piExpPccVerif_j && piExpCkVerif_j,
							"The proofs of correct exponentiation of the partial Choice Return Codes or of the Confirmation Keys are invalid. [j: %s, ee: %s, vcs: %s]",
							j, ee, vcs);
				});

		record VoterCombinedCodes(ElGamalMultiRecipientCiphertext c_pC_id, GqElement pVCC_id, String hhlVCC_id) {}
		final ElGamalMultiRecipientCiphertext neutralElement = elGamal.neutralElement(n, p_q_g);

		final ImmutableList<VoterCombinedCodes> voterCombinedCodes = IntStream.range(0, N_E)
				.parallel()
				.mapToObj(id -> {
					final String vc_id = vc.get(id);

					ElGamalMultiRecipientCiphertext c_pC_id = neutralElement;
					final List<GqElement> lVCC_id = new ArrayList<>();
					final List<HashableString> hlVCC_id = new ArrayList<>();

					// The specification uses 1 indexing, but we are bound to 0 indexing.
					for (int j = 0; j < 4; j++) {
						final ElGamalMultiRecipientCiphertext C_expPCC_j_id = C_expPCC.get(id, j);
						c_pC_id = c_pC_id.getCiphertextProduct(C_expPCC_j_id);

						final ElGamalMultiRecipientCiphertext C_expCK_j_id = C_expCK.get(id, j);
						final GqElement lVCC_j_id = getMessage(C_expCK_j_id, sk_setup);

						final AuxiliaryInformation i_aux_1 = AuxiliaryInformation.of("CreateLVCCShare", ee, vcs, vc_id,
								integerToString(j + 1)); // Due to zero-indexing.

						final HashableString hlVCC_j_id = HashableString.from(base64.base64Encode(hash.recursiveHash(i_aux_1, lVCC_j_id)));

						lVCC_id.add(lVCC_j_id);
						hlVCC_id.add(hlVCC_j_id);
					}

					// pVCC_id ← ∏ lVCC_j_id
					final GqElement pVCC_id = lVCC_id.stream()
							.parallel()
							.reduce(p_q_g.getIdentity(), GqElement::multiply);

					final AuxiliaryInformation i_aux_2 = AuxiliaryInformation.of("VerifyLVCCHash", ee, vcs, vc_id);

					final String hhlVCC_id = base64.base64Encode(hash.recursiveHash(i_aux_2, hlVCC_id.get(0), hlVCC_id.get(1),
							hlVCC_id.get(2), hlVCC_id.get(3))); // Due to zero-indexing.

					// L_lVCC ← L_lVCC || hhlVCC_id
					return new VoterCombinedCodes(c_pC_id, pVCC_id, hhlVCC_id);
				})
				.collect(toImmutableList());

		return new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(voterCombinedCodes.stream()
						.parallel()
						.map(VoterCombinedCodes::c_pC_id)
						.collect(GroupVector.toGroupVector()))
				.setPreVoteCastReturnCodesVector(voterCombinedCodes.stream()
						.parallel()
						.map(VoterCombinedCodes::pVCC_id)
						.collect(GroupVector.toGroupVector()))
				.setLongVoteCastReturnCodesAllowList(voterCombinedCodes.stream()
						.parallel()
						.map(VoterCombinedCodes::hhlVCC_id)
						.collect(toImmutableList()))
				.build();
	}

	private GqElement getMessage(final ElGamalMultiRecipientCiphertext ciphertext, final ElGamalMultiRecipientPrivateKey secretKey) {
		final ElGamalMultiRecipientMessage message = elGamal.getMessage(ciphertext, secretKey);

		checkArgument(message.getElements().size() == 1, "The message must have only one element.");

		return message.getElements().get(0);
	}

}
