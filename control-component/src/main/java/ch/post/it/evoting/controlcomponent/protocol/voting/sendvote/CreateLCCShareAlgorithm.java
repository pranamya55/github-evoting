/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToByteArray;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponent.process.PartialChoiceReturnCodeAllowList;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;

/**
 * Implements the CreateLCCShare algorithm.
 */
@Service
public class CreateLCCShareAlgorithm {

	private final Hash hash;
	private final Base64 base64;
	private final KeyDerivation keyDerivation;
	private final VerificationCardStateService verificationCardStateService;

	public CreateLCCShareAlgorithm(
			final Hash hash,
			final Base64 base64,
			final KeyDerivation keyDerivation,
			final VerificationCardStateService verificationCardStateService) {
		this.hash = hash;
		this.base64 = base64;
		this.keyDerivation = keyDerivation;
		this.verificationCardStateService = verificationCardStateService;
	}

	/**
	 * Generates the CCR<sub>j</sub>'s long Choice Return Code shares.
	 * <p>
	 * By contract the context ids are verified prior to calling this method.
	 * </p>
	 *
	 * @param context the {@link CreateLCCShareContext}. Not null.
	 * @param input   the {@link CreateLCCShareInput}. Not null.
	 * @return the CCR<sub>j</sub>'s long Choice Return Code shares encapsulated in a {@link CreateLCCShareOutput}.
	 * @throws NullPointerException     if any input parameter is null.
	 * @throws IllegalStateException    if the partial Choice Return Codes allow list does not contain all partial Choice Return Codes.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>The context and input do not have the same group.</li>
	 *                                      <li>The number of partial choice return codes is different from &psi;.</li>
	 *                                      <li>The verification card is not in L<sub>decPCC,j</sub>.</li>
	 *                                      <li>The verification card is in L<sub>sentVotes,j</sub></li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	@Transactional
	public CreateLCCShareOutput createLCCShare(final CreateLCCShareContext context, final CreateLCCShareInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.encryptionGroup().equals(input.partialChoiceReturnCodes().getGroup()),
				"The context and input must have the same group.");

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final BigInteger q = p_q_g.getQ();
		final String ee = context.electionEventId();
		final String vcs = context.verificationCardSetId();
		final String vc_id = context.verificationCardId();
		final ImmutableList<String> tau_hat = context.blankCorrectnessInformation();
		final int psi = tau_hat.size();

		// Input.
		final PartialChoiceReturnCodeAllowList L_pCC = input.pCCAllowList();
		final GroupVector<GqElement, GqGroup> pCC_id = input.partialChoiceReturnCodes();
		final ZqElement k_j_prime = input.ccrjReturnCodesGenerationSecretKey();

		// Cross-checks.
		checkArgument(pCC_id.size() == psi, "The number of partial choice return codes must be equal to psi. [psi: %s]", psi);

		// Require.
		// all pCC distinct ensured by CreateLCCShareInput.
		// Corresponds to vc_id ∈ L_decPCC,j.
		checkArgument(verificationCardStateService.isPartiallyDecrypted(vc_id),
				"The partial Choice Return Codes have not yet been partially decrypted. [vc_id: %s].", vc_id);
		// Corresponds to vc_id ∉ L_sentVotes,j.
		checkArgument(verificationCardStateService.isNotSentVote(vc_id),
				"The CCR_j already generated the long Choice Return Code share in a previous attempt. [vc_id: %s].", vc_id);

		// Operation.
		final ImmutableByteArray PRK = integerToByteArray(k_j_prime.getValue());

		final ImmutableList<String> info = ImmutableList.of("VoterChoiceReturnCodeGeneration", ee, vcs, vc_id);

		final ZqElement k_j_id = keyDerivation.KDFToZq(PRK, info, q);

		final GroupVector<GqElement, GqGroup> lCC_j_id = IntStream.range(0, psi)
				.mapToObj(i -> {
					final GqElement pCC_id_i = pCC_id.get(i);
					final GqElement hpCC_id_i = hash.hashAndSquare(pCC_id_i.getValue(), pCC_id_i.getGroup());

					final ImmutableByteArray lpCC_id_i = hash.recursiveHash(hpCC_id_i, HashableString.from(vc_id), HashableString.from(ee),
							HashableString.from(tau_hat.get(i)));

					if (!L_pCC.exists(base64.base64Encode(lpCC_id_i))) {
						throw new IllegalStateException(
								"The partial Choice Return Codes allow list does not contain the partial Choice Return Code.");
					} else {
						return hpCC_id_i.exponentiate(k_j_id);
					}
				})
				.collect(GroupVector.toGroupVector());

		// Corresponds to L_sentVotes,j ← L_sentVotes,j || vc_id
		verificationCardStateService.setSentVote(vc_id);

		return new CreateLCCShareOutput(lCC_j_id);
	}
}
