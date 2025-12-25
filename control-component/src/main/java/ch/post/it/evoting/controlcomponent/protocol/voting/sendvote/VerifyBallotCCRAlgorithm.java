/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqElement.GqElementFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

/**
 * Implements the VerifyBallotCCR algorithm.
 */
@Service
public class VerifyBallotCCRAlgorithm {

	private final ZeroKnowledgeProof zeroKnowledgeProof;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;
	private final GetHashContextAlgorithm getHashContextAlgorithm;

	public VerifyBallotCCRAlgorithm(
			final ZeroKnowledgeProof zeroKnowledgeProof,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms,
			final GetHashContextAlgorithm getHashContextAlgorithm) {
		this.zeroKnowledgeProof = zeroKnowledgeProof;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
		this.getHashContextAlgorithm = getHashContextAlgorithm;
	}

	/**
	 * Checks the voting client's encrypted vote by verifying the zero-knowledge proofs.
	 * <p>
	 * By contract the context ids are verified prior to calling this method.
	 * </p>
	 *
	 * @param context the {@link VerifyBallotCCRContext} containing necessary ids and group.
	 * @param input   the {@link VerifyBallotCCRInput} containing all needed inputs. Non-null.
	 * @return {@code true} if the verification is successful, {@code false} otherwise.
	 * @throws NullPointerException     if any input parameter is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>The context and input do not have the same group.</li>
	 *                                      <li>The encrypted partial Choice Return Codes size is not equal to &psi;.</li>
	 *                                      <li>The encrypted vote size is not equal to &delta;.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	public boolean verifyBallotCCR(final VerifyBallotCCRContext context, final VerifyBallotCCRInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross group check.
		checkArgument(context.getEncryptionGroup().equals(input.getEncryptedVote().getGroup()), "The context and input must have the same group.");

		// Context.
		final GqGroup p_q_g = context.getEncryptionGroup();
		final String ee = context.getElectionEventId();
		final String vcs = context.getVerificationCardSetId();
		final String vc_id = context.getVerificationCardId();
		final PrimesMappingTable pTable = context.getPrimesMappingTable();
		final int psi = primesMappingTableAlgorithms.getPsi(pTable);
		final int delta = primesMappingTableAlgorithms.getDelta(pTable);
		final GqElement K_id = context.getVerificationCardPublicKey();
		final ElGamalMultiRecipientPublicKey EL_pk = context.getElectionPublicKey();
		final ElGamalMultiRecipientPublicKey pk_CCR = context.getChoiceReturnCodesEncryptionPublicKey();

		// Input.
		final ElGamalMultiRecipientCiphertext E1 = input.getEncryptedVote();
		final ElGamalMultiRecipientCiphertext E1_tilde = input.getExponentiatedEncryptedVote();
		final ElGamalMultiRecipientCiphertext E2 = input.getEncryptedPartialChoiceReturnCodes();
		final ExponentiationProof pi_Exp = input.getExponentiationProof();
		final PlaintextEqualityProof pi_EqEnc = input.getPlaintextEqualityProof();

		// Cross size checks.
		checkArgument(E2.size() == psi,
				"The encrypted partial Choice Return Codes size must be equal to number of selectable voting options. [E2_size: %s, psi: %s]",
				E2.size(), psi);
		checkArgument(E1.size() == delta,
				"The encrypted vote size must be equal to the number of allowed write-ins + 1. [E1_size: %s, delta: %s]", E1.size(), delta);

		// Operation.
		final GqElement gamma_2 = E2.getGamma();
		final GqElement one_vector = GqElementFactory.fromValue(BigInteger.ONE, p_q_g);
		final GqElement Phi_2 = E2.getPhis().stream().reduce(one_vector, GqElement::multiply);
		final ElGamalMultiRecipientCiphertext E2_tilde = ElGamalMultiRecipientCiphertext.create(gamma_2, GroupVector.of(Phi_2));

		final GqElement pk_CCR_tilde = pk_CCR.stream().sequential().limit(psi).reduce(one_vector, GqElement::multiply);

		final AuxiliaryInformation i_aux = AuxiliaryInformation.from(Streams.concat(
				Stream.of("CreateVote", vc_id, getHashContextAlgorithm.getHashContext(p_q_g, ee, vcs, pTable, EL_pk, pk_CCR)),
				Stream.of(integerToString(E1.getGamma().getValue())), E1.getPhis().stream().map(Phi_1_i -> integerToString(Phi_1_i.getValue())),
				Stream.of(integerToString(E2.getGamma().getValue())), E2.getPhis().stream().map(Phi_2_i -> integerToString(Phi_2_i.getValue()))
		).collect(toImmutableList()));

		final GqElement g = p_q_g.getGenerator();
		final GqElement gamma_1 = E1.getGamma();
		final GqElement Phi_1_0 = E1.get(0);
		final GroupVector<GqElement, GqGroup> bases = GroupVector.of(g, gamma_1, Phi_1_0);

		final GqElement gamma_1_k_id = E1_tilde.getGamma();
		final GqElement Phi_1_0_k_id = E1_tilde.get(0);
		final GroupVector<GqElement, GqGroup> exponentiations = GroupVector.of(K_id, gamma_1_k_id, Phi_1_0_k_id);
		final boolean verifExp = zeroKnowledgeProof.verifyExponentiation(bases, exponentiations, pi_Exp, i_aux);

		final boolean verifEqEnc = zeroKnowledgeProof.verifyPlaintextEquality(E1_tilde, E2_tilde, EL_pk.get(0), pk_CCR_tilde, pi_EqEnc, i_aux);

		return verifExp && verifEqEnc;
	}
}
