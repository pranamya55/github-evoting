/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;

/**
 * Implements the GenKeysCCR algorithm.
 */
@Service
public class GenKeysCCRAlgorithm {

	private final Random random;
	private final ZeroKnowledgeProof zeroKnowledgeProof;

	public GenKeysCCRAlgorithm(
			final Random random,
			final ZeroKnowledgeProof zeroKnowledgeProof) {
		this.random = random;
		this.zeroKnowledgeProof = zeroKnowledgeProof;
	}

	/**
	 * Generates the CCR<sub>j</sub> Choice Return Codes encryption key pair and the CCR<sub>j</sub> Return Codes Generation secret key.
	 *
	 * @param context the {@link GenKeysCCRContext}. Must be non-null.
	 * @return the CCR<sub>j</sub> Choice Return Codes encryption key pair, the CCR<sub>j</sub> Return Codes Generation secret key and the
	 * CCR<sub>j</sub> Schnorr proofs of knowledge as a {@link GenKeysCCROutput}.
	 * @throws NullPointerException if the provided context is null.
	 */
	@SuppressWarnings("java:S117")
	public GenKeysCCROutput genKeysCCR(final GenKeysCCRContext context) {
		checkNotNull(context);

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final BigInteger q = p_q_g.getQ();
		final int j = context.nodeId();
		final String ee = context.electionEventId();
		final int psi_max = context.maximumNumberOfSelections();

		// Operation.
		final ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(p_q_g, psi_max, random);
		final ElGamalMultiRecipientPublicKey pk_CCR_j = keyPair.getPublicKey();
		final ElGamalMultiRecipientPrivateKey sk_CCR_j = keyPair.getPrivateKey();

		final AuxiliaryInformation i_aux = AuxiliaryInformation.of(ee, "GenKeysCCR", integerToString(j));
		final GroupVector<SchnorrProof, ZqGroup> pi_pkCCR_j = IntStream.range(0, psi_max)
				.parallel()
				.mapToObj(i -> zeroKnowledgeProof.genSchnorrProof(sk_CCR_j.get(i), pk_CCR_j.get(i), i_aux))
				.collect(GroupVector.toGroupVector());

		final ZqElement k_j_prime = ZqElement.create(random.genRandomInteger(q), ZqGroup.sameOrderAs(p_q_g));

		// Output.
		return new GenKeysCCROutput(keyPair, k_j_prime, pi_pkCCR_j);
	}

}
