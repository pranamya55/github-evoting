/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally;

import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair.genKeyPair;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;

/**
 * Implements the SetupTallyCCM algorithm.
 */
@Service
public class SetupTallyCCMAlgorithm {

	private final Random random;
	private final ZeroKnowledgeProof zeroKnowledgeProof;
	private final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm;

	public SetupTallyCCMAlgorithm(
			final Random random,
			final ZeroKnowledgeProof zeroKnowledgeProof,
			final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm) {
		this.random = random;
		this.zeroKnowledgeProof = zeroKnowledgeProof;
		this.getHashElectionEventContextAlgorithm = getHashElectionEventContextAlgorithm;
	}

	/**
	 * Generates the CCM<sub>j</sub> election key pair (EL<sub>pk,j</sub>, EL<sub>sk,j</sub>) and the Schnorr proofs of knowledge &pi;<sub>EL<sub>pk,
	 * j</sub></sub>.
	 *
	 * @param context the {@link SetupTallyCCMContext}. Must be non-null.
	 * @return the {@link SetupTallyCCMOutput} containing the CCM<sub>j</sub> election key pair and the Schnorr proofs of knowledge. Must be non-null.
	 * @throws NullPointerException if the provided context is null.
	 */
	@SuppressWarnings("java:S117")
	public SetupTallyCCMOutput setupTallyCCM(final SetupTallyCCMContext context) {
		checkNotNull(context);

		// Context
		final int j = context.nodeId();
		final ElectionEventContext electionEventContext = context.electionEventContext();
		final GqGroup p_q_g = electionEventContext.encryptionGroup();
		final int delta_max = electionEventContext.maximumNumberOfWriteInsPlusOne();

		// Operation.
		final String hContext = getHashElectionEventContextAlgorithm.getHashElectionEventContext(electionEventContext);

		final ElGamalMultiRecipientKeyPair EL_pk_j_EL_sk_j = genKeyPair(p_q_g, delta_max, random);

		final AuxiliaryInformation i_aux = AuxiliaryInformation.of(hContext, "SetupTallyCCM", integerToString(j));

		final ElGamalMultiRecipientPrivateKey EL_sk_j = EL_pk_j_EL_sk_j.getPrivateKey();
		final ElGamalMultiRecipientPublicKey EL_pk_j = EL_pk_j_EL_sk_j.getPublicKey();
		final GroupVector<SchnorrProof, ZqGroup> pi_EL_pk_j = IntStream.range(0, delta_max).parallel()
				.mapToObj(i -> {
					final ZqElement EL_sk_j_i = EL_sk_j.get(i);
					final GqElement EL_pk_j_i = EL_pk_j.get(i);
					return zeroKnowledgeProof.genSchnorrProof(EL_sk_j_i, EL_pk_j_i, i_aux);
				})
				.collect(toGroupVector());

		return new SetupTallyCCMOutput.Builder()
				.setElGamalMultiRecipientKeyPair(EL_pk_j_EL_sk_j)
				.setSchnorrProofs(pi_EL_pk_j)
				.build();
	}
}
