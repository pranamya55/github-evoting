/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Streams;

import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.mixnet.Mixnet;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;

/**
 * Implements the MixDecOnline algorithm.
 */
@Service
public class MixDecOnlineAlgorithm {

	private final BallotBoxService ballotBoxService;
	private final ElGamal elGamal;
	private final Mixnet mixnet;
	private final ZeroKnowledgeProof zeroKnowledgeProof;

	public MixDecOnlineAlgorithm(final BallotBoxService ballotBoxService,
			final ElGamal elGamal,
			final Mixnet mixnet,
			final ZeroKnowledgeProof zeroKnowledgeProof) {
		this.ballotBoxService = ballotBoxService;
		this.elGamal = elGamal;
		this.mixnet = mixnet;
		this.zeroKnowledgeProof = zeroKnowledgeProof;
	}

	/**
	 * Mixes and partially decrypts ciphertexts, providing proofs of knowledge for the shuffle and the decryption.
	 *
	 * @param context the context of the mixing and decryption. Must be non-null.
	 * @param input   the input to the mixing and decryption. Must be non-null.
	 * @return the output of the mixing and decryption of the ciphertexts.
	 */
	@SuppressWarnings("java:S117")
	@Transactional
	public MixDecOnlineOutput mixDecOnline(final MixDecOnlineContext context, final MixDecOnlineInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group checks.
		checkArgument(context.getEncryptionGroup().equals(input.getPartiallyDecryptedVotes().getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final String ee = context.getElectionEventId();
		final String bb = context.getBallotBoxId();
		final int j = context.getNodeId();
		final int delta = context.getNumberOfAllowedWriteInsPlusOne();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> EL_pk_vector = context.getCcmElectionPublicKeys();
		final ElGamalMultiRecipientPublicKey EB_pk = context.getElectoralBoardPublicKey();

		// Input.
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_dec_j_minus_one = input.getPartiallyDecryptedVotes();
		final ElGamalMultiRecipientPrivateKey EL_sk_j = input.getCcmjElectionSecretKey();

		// Cross-checks.
		checkArgument(c_dec_j_minus_one.getElementSize() == delta,
				"The number of element of each partially decrypted vote must be the allowed number of write-ins + 1. [c_dec_j_minus_one_numberOfElements: %s, delta: %s]",
				c_dec_j_minus_one.getElementSize(), delta);
		// The specification uses 1 indexing, but we are bound to 0 indexing
		final ElGamalMultiRecipientPublicKey EL_pk_j = EL_pk_vector.get(j - 1);
		final ElGamalMultiRecipientKeyPair EL_pk_j_EL_sk_j = ElGamalMultiRecipientKeyPair.from(EL_sk_j, EL_pk_j.getGroup().getGenerator());
		checkArgument(EL_pk_j.equals(EL_pk_j_EL_sk_j.getPublicKey()),
				"The public key of the reconstituted CCM_j election public key pair does not correspond to the given CCM_j election public key.");
		final int delta_max = EB_pk.size();
		checkArgument(delta_max == EL_sk_j.size(),
				"The electoral board public key must have as many elements as the CCM election keys. [EB_pk size: %s, EL_sk_j size: %s]",
				delta_max, EL_sk_j.size());

		// Require.
		// hvcj = hvc1 = hvc2 = hvc3 = hvc4 ensured by MixDecOnlineInput.
		final int N_c_hat = c_dec_j_minus_one.size();
		checkArgument(N_c_hat >= 2, "There must be at least 2 partially decrypted votes. [N_c_hat: %s]", N_c_hat);
		// Corresponds to bb not in L_bb_j
		checkArgument(!ballotBoxService.isMixed(bb), "The ballot box has already been mixed by the control component. [ballotBoxId: %s]", bb);

		// Operation.
		// The specification uses 1 indexing, but we are bound to 0 indexing
		final ElGamalMultiRecipientPublicKey EL_pk_bar = elGamal.combinePublicKeys(
				Streams.concat(EL_pk_vector.subList(j - 1, EL_pk_vector.size()).stream(), Stream.of(EB_pk))
						.collect(GroupVector.toGroupVector()));

		final AuxiliaryInformation i_aux = AuxiliaryInformation.of(ee, bb, "MixDecOnline", integerToString(j));

		final VerifiableShuffle c_mix_j_pi_mix_j = mixnet.genVerifiableShuffle(c_dec_j_minus_one, EL_pk_bar);

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c_mix_j = c_mix_j_pi_mix_j.shuffledCiphertexts();
		final VerifiableDecryptions c_dec_j_pi_dec_j = zeroKnowledgeProof.genVerifiableDecryptions(c_mix_j, EL_pk_j_EL_sk_j, i_aux);

		// Corresponds to L_bb_j ← L_bb_j || bb
		ballotBoxService.setMixed(bb);

		return new MixDecOnlineOutput(c_mix_j_pi_mix_j, c_dec_j_pi_dec_j);
	}
}
