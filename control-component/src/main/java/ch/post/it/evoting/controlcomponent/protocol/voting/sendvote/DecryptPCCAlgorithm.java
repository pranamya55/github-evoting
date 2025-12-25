/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;

import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.GetHashExtractedElectionEventService;
import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;

/**
 * Implements the DecryptPCC algorithm.
 */
@Service
public class DecryptPCCAlgorithm {

	private final ZeroKnowledgeProof zeroKnowledgeProof;
	private final GetHashExtractedElectionEventService getHashExtractedElectionEventService;

	public DecryptPCCAlgorithm(
			final ZeroKnowledgeProof zeroKnowledgeProof,
			final GetHashExtractedElectionEventService getHashExtractedElectionEventService) {
		this.zeroKnowledgeProof = zeroKnowledgeProof;
		this.getHashExtractedElectionEventService = getHashExtractedElectionEventService;
	}

	/**
	 * Decrypts the partial Choice Return Codes.
	 * <p>
	 * By contract the context ids are verified prior to calling this method.
	 * </p>
	 *
	 * @param context the {@link DecryptPCCContext} containing necessary ids and group. Non-null.
	 * @param input   the {@link DecryptPCCInput} containing all needed inputs. Non-null.
	 * @return the decrypted partial choice return codes.
	 * @throws NullPointerException     if any of the parameters is null.
	 * @throws IllegalStateException    if the verification of the other control components' exponentiation proofs failed.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>the context's encryption group is different from the input's group.</li>
	 *                                      <li>the other CCRs' choice return codes encryption keys are missing.</li>
	 *                                      <li>The encrypted partial Choice Return Codes size is not equal to &psi;.</li>
	 *                                      <li>The encrypted vote size is not equal to &delta;.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	public GroupVector<GqElement, GqGroup> decryptPCC(final DecryptPCCContext context, final DecryptPCCInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.getEncryptionGroup().equals(input.getExponentiatedGammaElements().getGroup()),
				"The context and input must have the same group.");

		// Context.
		final ImmutableList<Integer> j_hat = context.getOtherNodeIds();
		final String vc_id = context.getVerificationCardId();
		final int psi = context.getNumberOfSelections();
		final int delta = context.getNumberOfWriteInsPlusOne();
		// For performance reasons, we extract the ExtractedElectionEvent eee in ExtractedElectionEventHashService#computeAndSave.
		final GqGroup p_q_g = context.getEncryptionGroup();
		final GqElement g = p_q_g.getGenerator();
		final String ee = context.getElectionEventId();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> pk_CCR_j_hat = context.getOtherCcrChoiceReturnCodesEncryptionKeys()
				.orElseThrow(() -> new IllegalArgumentException("The other CCR's choice return codes encryption keys are missing."));

		// Input.
		final GroupVector<GqElement, GqGroup> d_j = input.getExponentiatedGammaElements();
		final GroupVector<GroupVector<GqElement, GqGroup>, GqGroup> d_j_hat = input.getOtherCcrExponentiatedGammaElements();
		final GroupVector<GroupVector<ExponentiationProof, ZqGroup>, ZqGroup> pi_decPCC_j_hat = input.getOtherCcrExponentiationProofs();
		final ElGamalMultiRecipientCiphertext E1 = input.getEncryptedVote();
		final ElGamalMultiRecipientCiphertext E1_tilde = input.getExponentiatedEncryptedVote();
		final ElGamalMultiRecipientCiphertext E2 = input.getEncryptedPartialChoiceReturnCodes();

		// Cross-checks.
		checkArgument(E2.size() == psi, "The encrypted partial Choice Return Codes size must be equal to psi. [psi: %s]", psi);
		checkArgument(E1.size() == delta, "The encrypted vote size must be equal to delta. [delta: %s]", delta);

		// Operations.
		for (int index = 0; index < j_hat.size(); index++) {
			// The For-loop is equivalent to the system specification's k ∈ j_hat. However, since we have 0-indexing in Java, the above implementation
			// is more convenient to work with. The For-loop works in the following way: if the control component's nodeID is 3, we loop over the
			// elements 1, 2, and 4.
			final int k = j_hat.get(index);

			final GqElement gamma_1_k_id = E1_tilde.getGamma();
			final GqElement Phi_1_0_k_id = E1_tilde.get(0);
			final GqElement gamma_1 = E1.getGamma();
			final GqElement gamma_2 = E2.getGamma();
			final AuxiliaryInformation i_aux = AuxiliaryInformation.from(Streams.concat(
					Stream.of("PartialDecryptPCC", vc_id, getHashExtractedElectionEventService.getHashExtractedElectionEvent(ee)),
					Stream.of(integerToString(gamma_1.getValue())), E1.getPhis().stream().map(Phi_1_k -> integerToString(Phi_1_k.getValue())),
					Stream.of(integerToString(gamma_1_k_id.getValue()), integerToString(Phi_1_0_k_id.getValue())),
					Stream.of(integerToString(gamma_2.getValue())), E2.getPhis().stream().map(Phi_2_k -> integerToString(Phi_2_k.getValue())),
					Stream.of(integerToString(BigInteger.valueOf(k)))
			).collect(toImmutableList()));

			final ElGamalMultiRecipientPublicKey pk_CCR_k = pk_CCR_j_hat.get(index);

			final GroupVector<GqElement, GqGroup> d_k = d_j_hat.get(index);

			final GroupVector<ExponentiationProof, ZqGroup> pi_decPCC_k = pi_decPCC_j_hat.get(index);

			// for i ∈ [0, ψ)
			if (IntStream.range(0, psi).parallel().anyMatch(i ->
					!zeroKnowledgeProof.verifyExponentiation(GroupVector.of(g, gamma_2), GroupVector.of(pk_CCR_k.get(i), d_k.get(i)),
							pi_decPCC_k.get(i), i_aux))) {
				throw new IllegalStateException(
						String.format("The verification of the other control component's exponentiation proof failed [control component: %d]", k));
			}
		}

		// for i ∈ [0, ψ)
		final GroupVector<GqElement, GqGroup> d = IntStream.range(0, psi)
				.mapToObj(i -> d_j.get(i).multiply(d_j_hat.get(0).get(i)).multiply(d_j_hat.get(1).get(i)).multiply(d_j_hat.get(2).get(i)))
				.collect(GroupVector.toGroupVector());

		final GroupVector<GqElement, GqGroup> Phi_2 = E2.getPhis();
		return IntStream.range(0, psi)
				.mapToObj(i -> Phi_2.get(i).divide(d.get(i)))
				.collect(GroupVector.toGroupVector());
	}
}
