/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.mixnet.Mixnet;
import ch.post.it.evoting.cryptoprimitives.mixnet.ShuffleArgument;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyMixDecInput;

/**
 * Implements the VerifyMixDecOnline algorithm.
 */
@Service
public class VerifyMixDecOnlineAlgorithm {

	private static final String MIX_DEC_ONLINE = "MixDecOnline";

	private final Mixnet mixnet;
	private final ElGamal elGamal;
	private final ZeroKnowledgeProof zeroKnowledgeProof;

	public VerifyMixDecOnlineAlgorithm(
			final Mixnet mixnet,
			final ElGamal elGamal,
			final ZeroKnowledgeProof zeroKnowledgeProof) {
		this.mixnet = mixnet;
		this.elGamal = elGamal;
		this.zeroKnowledgeProof = zeroKnowledgeProof;
	}

	/**
	 * Verifies the preceding control components' mixing and decryption proofs. This is not done by the very first control component.
	 *
	 * @param context the {@link VerifyMixDecOnlineContext}.
	 * @param input   the {@link VerifyMixDecInput}.
	 * @return {@code true} all preceding shuffle and decryption proofs are verified, {@code false} otherwise.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>There are less than two votes.</li>
	 *                                      <li>The ciphertexts element size does not match the number of allowed write-ins + 1.</li>
	 *                                      <li>The ciphertexts element size is not smaller or equal to the election public key size.</li>
	 *                                      <li>There is not the expected number of verifiable shuffles.</li>
	 *                                      <li>There is not the expected number of verifiable decryptions.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	public boolean verifyMixDecOnline(final VerifyMixDecOnlineContext context, final VerifyMixDecInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.encryptionGroup().equals(input.initialCiphertexts().getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final int j = context.nodeId();
		final String ee = context.electionEventId();
		final String bb = context.ballotBoxId();
		final int delta = context.numberOfWriteInsPlusOne();
		final ElGamalMultiRecipientPublicKey EL_pk = context.electionPublicKey();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> EL_pk_vector = context.ccmElectionPublicKeys();
		final ElGamalMultiRecipientPublicKey EB_pk = context.electoralBoardPublicKey();

		// Input.
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_init_j = input.initialCiphertexts();
		final ImmutableList<VerifiableShuffle> c_mix_pi_mix_vector = input.precedingVerifiableShuffledVotes();
		final ImmutableList<VerifiableDecryptions> c_dec_pi_dec_vector = input.precedingVerifiableDecryptedVotes();
		final int N_c_hat = c_init_j.size();

		// Cross-checks.
		checkArgument(c_mix_pi_mix_vector.size() == j - 1,
				"Wrong number of verifiable shuffles. [expected: %s, actual: %s]", j - 1, c_mix_pi_mix_vector.size());
		checkArgument(c_dec_pi_dec_vector.size() == j - 1,
				"Wrong number of verifiable decryptions. [expected: %s, actual: %s]", j - 1, c_dec_pi_dec_vector.size());
		checkArgument(c_init_j.getElementSize() == delta,
				"The ciphertexts size must be the number of allowed write-ins + 1. [l: %s, delta: %s]",
				c_init_j.getElementSize(), delta);
		// Ciphertext size positivity ensured by ElGamalCiphertext.
		// Public keys consistency ensured by VerifyMixDecInput.

		// Require.
		checkArgument(N_c_hat >= 2, "There must be at least two votes.");

		// Operation.
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_mix_1 = c_mix_pi_mix_vector.get(0).shuffledCiphertexts();
		final ShuffleArgument pi_mix_1 = c_mix_pi_mix_vector.get(0).shuffleArgument();
		final boolean shuffleVerif_1 = mixnet.verifyShuffle(c_init_j, c_mix_1, pi_mix_1, EL_pk).isVerified();

		final AuxiliaryInformation i_aux_1 = AuxiliaryInformation.of(ee, bb, MIX_DEC_ONLINE, integerToString(1));

		final ElGamalMultiRecipientPublicKey EL_pk_1 = EL_pk_vector.get(0);
		final VerifiableDecryptions c_dec_1_pi_dec_1 = c_dec_pi_dec_vector.get(0);
		final boolean decryptVerif_1 = zeroKnowledgeProof.verifyDecryptions(c_mix_1, EL_pk_1, c_dec_1_pi_dec_1, i_aux_1).isVerified();

		record Verifs(boolean shuffleVerif_k, boolean decryptVerif_k) {
		}
		// Due to 0-indexing in java, indexes [2, j), become indexes [1, j - 1)
		final ImmutableList<Verifs> verifs = IntStream.range(1, j - 1).parallel()
				.mapToObj(k -> {
					final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> EL_pk_vector_EB_pk = Streams.concat(
									EL_pk_vector.subList(k, EL_pk_vector.size()).stream(),
									Stream.of(EB_pk))
							.collect(GroupVector.toGroupVector());
					final ElGamalMultiRecipientPublicKey EL_pk_bar = elGamal.combinePublicKeys(EL_pk_vector_EB_pk);

					final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_dec_k_minus_one = c_dec_pi_dec_vector.get(k - 1).getCiphertexts();
					final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_mix_k = c_mix_pi_mix_vector.get(k).shuffledCiphertexts();
					final ShuffleArgument pi_mix_k = c_mix_pi_mix_vector.get(k).shuffleArgument();
					final boolean shuffleVerif_k = mixnet.verifyShuffle(c_dec_k_minus_one, c_mix_k, pi_mix_k, EL_pk_bar).isVerified();

					final AuxiliaryInformation i_aux_k = AuxiliaryInformation.of(ee, bb, MIX_DEC_ONLINE, integerToString(k + 1));

					final ElGamalMultiRecipientPublicKey EL_pk_k = EL_pk_vector.get(k);
					final VerifiableDecryptions c_dec_k_pi_dec_k = c_dec_pi_dec_vector.get(k);
					final boolean decryptVerif_k = zeroKnowledgeProof.verifyDecryptions(c_mix_k, EL_pk_k, c_dec_k_pi_dec_k, i_aux_k).isVerified();

					return new Verifs(shuffleVerif_k, decryptVerif_k);
				})
				.collect(toImmutableList());

		return Streams.concat(
						Stream.of(decryptVerif_1 && shuffleVerif_1),
						verifs.stream().map(verifs_k -> verifs_k.decryptVerif_k && verifs_k.shuffleVerif_k)) // k in [2, j)
				.reduce(Boolean.TRUE, Boolean::logicalAnd);
	}
}
