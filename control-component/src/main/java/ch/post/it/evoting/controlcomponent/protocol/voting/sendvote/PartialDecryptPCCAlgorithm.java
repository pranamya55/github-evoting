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

import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.GetHashExtractedElectionEventService;
import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;

/**
 * Implements the PartialDecryptPCC algorithm.
 */
@Service
public class PartialDecryptPCCAlgorithm {

	private final ZeroKnowledgeProof zeroKnowledgeProof;
	private final VerificationCardStateService verificationCardStateService;
	private final GetHashExtractedElectionEventService getHashExtractedElectionEventService;

	public PartialDecryptPCCAlgorithm(
			final ZeroKnowledgeProof zeroKnowledgeProof,
			final VerificationCardStateService verificationCardStateService,
			final GetHashExtractedElectionEventService getHashExtractedElectionEventService) {
		this.zeroKnowledgeProof = zeroKnowledgeProof;
		this.verificationCardStateService = verificationCardStateService;
		this.getHashExtractedElectionEventService = getHashExtractedElectionEventService;
	}

	/**
	 * Strips the partial Choice Return Codes' encryption layer.
	 * <p>
	 * By contract the context ids are verified prior to calling this method.
	 * </p>
	 *
	 * @param context the {@link DecryptPCCContext} containing necessary ids and group.
	 * @param input   the {@link PartialDecryptPCCInput} containing all needed inputs. Non-null.
	 * @return the exponentiated gamma elements and exponentiation proofs encapsulated in a {@link PartialDecryptPCCOutput}.
	 * @throws NullPointerException     if any input parameter is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>The context and input do not have the same group.</li>
	 *                                      <li>The encrypted partial Choice Return Codes size is not equal to &psi;.</li>
	 *                                      <li>The encrypted vote size is not equal to &delta;.</li>
	 *                                      <li>The partial Choice Return Codes have already been partially decrypted.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	public PartialDecryptPCCOutput partialDecryptPCC(final DecryptPCCContext context, final PartialDecryptPCCInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.getEncryptionGroup().equals(input.getGroup()), "The context and input must have the same group.");

		// Context.
		final int j = context.getNodeId();
		final String vc_id = context.getVerificationCardId();
		final int psi = context.getNumberOfSelections();
		final int delta = context.getNumberOfWriteInsPlusOne();
		// For performance reasons, we extract the ExtractedElectionEvent eee in ExtractedElectionEventHashService#computeAndSave.
		final GqGroup p_q_g = context.getEncryptionGroup();
		final GqElement g = p_q_g.getGenerator();
		final String ee = context.getElectionEventId();

		// Input.
		final ElGamalMultiRecipientCiphertext E1 = input.getEncryptedVote();
		final ElGamalMultiRecipientCiphertext E1_tilde = input.getExponentiatedEncryptedVote();
		final ElGamalMultiRecipientCiphertext E2 = input.getEncryptedPartialChoiceReturnCodes();
		final GqElement gamma_1 = E1.getGamma();
		final GqElement gamma_1_k_id = E1_tilde.getGamma();
		final GqElement Phi_1_0_k_id = E1_tilde.get(0);
		final GqElement gamma_2 = E2.getGamma();
		final ElGamalMultiRecipientPrivateKey sk_CCR_j = input.getCcrjChoiceReturnCodesEncryptionSecretKey();
		final ElGamalMultiRecipientPublicKey pk_CCR_j = input.getCcrjChoiceReturnCodesEncryptionPublicKey();

		// Cross-checks.
		checkArgument(E2.size() == psi,
				"There must be psi encrypted partial Choice Return Codes. [psi: %s]", psi);
		checkArgument(E1.size() == delta,
				"There must be delta encrypted vote elements. [delta: %s]", delta);

		// Require.
		// Ensure vc_id ∉ L_decPCC,j.
		checkArgument(verificationCardStateService.isNotPartiallyDecrypted(vc_id),
				"The partial Choice Return Codes have already been partially decrypted.");

		// Operation.
		final AuxiliaryInformation i_aux = AuxiliaryInformation.from(Streams.concat(
				Stream.of("PartialDecryptPCC", vc_id, getHashExtractedElectionEventService.getHashExtractedElectionEvent(ee)),
				Stream.of(integerToString(gamma_1.getValue())), E1.getPhis().stream().map(Phi_1_k -> integerToString(Phi_1_k.getValue())),
				Stream.of(integerToString(gamma_1_k_id.getValue()), integerToString(Phi_1_0_k_id.getValue())),
				Stream.of(integerToString(gamma_2.getValue())), E2.getPhis().stream().map(Phi_2_k -> integerToString(Phi_2_k.getValue())),
				Stream.of(integerToString(BigInteger.valueOf(j)))
		).collect(toImmutableList()));

		record VerifiablePartialDecryptPCC(GqElement d_i_j, ExponentiationProof pi) {
		}

		final ImmutableList<VerifiablePartialDecryptPCC> verifiablePartialDecryptPCCS = IntStream.range(0, psi).parallel()
				.mapToObj(i -> {
					final GqElement d_j_i = gamma_2.exponentiate(sk_CCR_j.get(i));

					final GroupVector<GqElement, GqGroup> bases = GroupVector.of(g, gamma_2);
					final GroupVector<GqElement, GqGroup> exponentiations = GroupVector.of(pk_CCR_j.get(i), d_j_i);
					final ExponentiationProof pi_decPCC_j_i = zeroKnowledgeProof.genExponentiationProof(bases, sk_CCR_j.get(i), exponentiations,
							i_aux);

					return new VerifiablePartialDecryptPCC(d_j_i, pi_decPCC_j_i);
				})
				.collect(toImmutableList());
		// Corresponds to L_decPCC,j = L_decPCC,j || vc_id.
		verificationCardStateService.setPartiallyDecrypted(vc_id);

		return new PartialDecryptPCCOutput(
				verifiablePartialDecryptPCCS.stream().map(VerifiablePartialDecryptPCC::d_i_j).collect(GroupVector.toGroupVector()),
				verifiablePartialDecryptPCCS.stream().map(VerifiablePartialDecryptPCC::pi).collect(GroupVector.toGroupVector()));
	}
}
