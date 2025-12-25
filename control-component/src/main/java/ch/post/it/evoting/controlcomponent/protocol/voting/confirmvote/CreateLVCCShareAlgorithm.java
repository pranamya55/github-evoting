/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToByteArray;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;

/**
 * Implements the CreateLVCCShare algorithm.
 */
@Service
public class CreateLVCCShareAlgorithm {

	private final Hash hash;
	private final Base64 base64;
	private final KeyDerivation keyDerivation;
	private final VerificationCardStateService verificationCardStateService;

	public CreateLVCCShareAlgorithm(
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
	 * Generates the long Vote Cast Return Code share and its hash.
	 * <p>
	 * By contract the context ids are verified prior to calling this method.
	 * </p>
	 *
	 * @param context the {@link LVCCHashContext} Not null.
	 * @param input   the {@link CreateLVCCShareInput}. Not null.
	 * @return the {@link CreateLVCCShareOutput}.
	 * @throws NullPointerException     if any input parameter is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>The context and input do not have the same group.</li>
	 *                                      <li>The verification card is not in L<sub>sentVotes,j</sub>.</li>
	 *                                      <li>The verification card is in in L<sub>confirmedVotes, j</sub>.</li>
	 *                                      <li>The verification card exceeded the maximum number of confirmation attempts.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	@Transactional
	public CreateLVCCShareOutput createLVCCShare(final LVCCHashContext context, final CreateLVCCShareInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-checks.
		checkArgument(context.encryptionGroup().equals(input.confirmationKey().getGroup()), "The context and input must have the same group.");

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final BigInteger q = p_q_g.getQ();
		final int j = context.nodeId();
		final String ee = context.electionEventId();
		final String vcs = context.verificationCardSetId();
		final String vc_id = context.verificationCardId();

		// Input.
		final GqElement CK_id = input.confirmationKey();
		final ZqElement k_j_prime = input.ccrjReturnCodesGenerationSecretKey();

		// Require.
		// Ensure vc_id ∈ L_sentVotes,j.
		checkArgument(verificationCardStateService.isSentVote(vc_id),
				String.format(
						"The CCR_j cannot create the LVCC Share since it did not compute the long Choice Return Code shares for the verification card. [vc_id: %s]",
						vc_id));
		// Ensure vc_id ∉ L_confirmedVotes,j
		checkArgument(verificationCardStateService.isNotConfirmedVote(vc_id),
				String.format("The CCR_j already confirmed the vote for this verification card. [vc_id: %s]", vc_id));

		// Operation.
		final int attempts_id = verificationCardStateService.getNextConfirmationAttemptId(vc_id);

		checkArgument(attempts_id < MAX_CONFIRMATION_ATTEMPTS, String.format("Max confirmation attempts of %s exceeded.", MAX_CONFIRMATION_ATTEMPTS));

		final ImmutableByteArray PRK = integerToByteArray(k_j_prime.getValue());

		final ImmutableList<String> info_CK = ImmutableList.of("VoterVoteCastReturnCodeGeneration", ee, vcs, vc_id);

		final ZqElement kc_j_id = keyDerivation.KDFToZq(PRK, info_CK, q);

		final GqElement hCK_id = hash.hashAndSquare(CK_id.getValue(), CK_id.getGroup());

		final GqElement lVCC_j_id = hCK_id.exponentiate(kc_j_id);

		final AuxiliaryInformation i_aux = AuxiliaryInformation.of("CreateLVCCShare", ee, vcs, vc_id, integerToString(j));

		final String hlVCC_j_id = base64.base64Encode(hash.recursiveHash(i_aux, lVCC_j_id));

		// Corresponds to L_confirmationAttempts,j(vc_id) ← attempts_id + 1
		verificationCardStateService.incrementConfirmationAttempts(vc_id);

		// Output.
		return new CreateLVCCShareOutput(lVCC_j_id, hlVCC_j_id, attempts_id);
	}
}
