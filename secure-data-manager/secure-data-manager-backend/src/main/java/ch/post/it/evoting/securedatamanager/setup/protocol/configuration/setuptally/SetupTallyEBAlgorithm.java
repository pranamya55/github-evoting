/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsInput;

/**
 * Implements the SetupTallyEB algorithm.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class SetupTallyEBAlgorithm {
	private final Hash hash;
	private final ElGamal elGamal;
	private final ZeroKnowledgeProof zeroKnowledgeProof;
	private final VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm;
	private final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm;

	public SetupTallyEBAlgorithm(final Hash hash,
			final ElGamal elGamal,
			final ZeroKnowledgeProof zeroKnowledgeProof,
			final VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm,
			final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm) {
		this.hash = hash;
		this.elGamal = elGamal;
		this.zeroKnowledgeProof = zeroKnowledgeProof;
		this.verifyCCSchnorrProofsAlgorithm = verifyCCSchnorrProofsAlgorithm;
		this.getHashElectionEventContextAlgorithm = getHashElectionEventContextAlgorithm;
	}

	/**
	 * Generates the last key pair (electoral board key pair (EB<sub>pk</sub>, EB<sub>sk</sub>)) and combines the CCMs' election public keys to yield
	 * the election public key (EL<sub>pk</sub>).
	 *
	 * @param electionEventContext election event context, the {@link ElectionEventContext}. Non-null.
	 * @param input                the {@link SetupTallyEBInput} containing all needed inputs. Non-null.
	 * @return the election public key and the electoral board public key encapsulated in a {@link SetupTallyEBOutput}.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalStateException    if a CCM Schnorr proof is invalid
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>the context and input do not have the same encryption group.</li>
	 *                                      <li>the size of the CCM election public keys and Schnorr proofs must be equal to the maximum number of write-in options + 1.</li>
	 *                                      <li>k is not greater or equal to 2.</li>
	 *                                  </ul>
	 */
	@SuppressWarnings("java:S117")
	public SetupTallyEBOutput setupTallyEB(final ElectionEventContext electionEventContext, final SetupTallyEBInput input) {
		checkNotNull(electionEventContext);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(electionEventContext.encryptionGroup().equals(input.ccmElectionPublicKeys().getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final GqGroup p_q_g = electionEventContext.encryptionGroup();
		final BigInteger q = p_q_g.getQ();
		final GqElement g = p_q_g.getGenerator();
		final String ee = electionEventContext.electionEventId();
		final int delta_max = electionEventContext.maximumNumberOfWriteInsPlusOne();

		// Input.
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> EL_pk_vector = input.ccmElectionPublicKeys();
		final GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> pi_ELpk_vector = input.ccmSchnorrProofs();
		final ImmutableList<ImmutableByteArray> PW = input.electoralBoardMembersPasswords().stream()
				.map(SafePasswordHolder::get)
				.map(CharBuffer::wrap)
				.map(StandardCharsets.UTF_8::encode)
				.map(ByteBuffer::array)
				.map(ImmutableByteArray::new)
				.collect(toImmutableList());
		final int k = PW.size();

		// Cross-checks.
		checkArgument(EL_pk_vector.getElementSize() == delta_max,
				"The size of the CCM election public keys and Schnorr proofs must be equal to the maximum number of write-in options + 1.");

		// Require.
		checkArgument(k >= 2, "There must be at least 2 electoral board members.");

		// Operation.
		final String hContext = getHashElectionEventContextAlgorithm.getHashElectionEventContext(electionEventContext);

		final AuxiliaryInformation i_aux_CCM = AuxiliaryInformation.of(hContext, "SetupTallyCCM");

		final VerifyCCSchnorrProofsContext verifyCCSchnorrProofsContext = new VerifyCCSchnorrProofsContext(p_q_g, ControlComponentNode.ids().size(),
				delta_max);
		final VerifyCCSchnorrProofsInput verifyCCSchnorrProofsInput = new VerifyCCSchnorrProofsInput(EL_pk_vector, pi_ELpk_vector, i_aux_CCM);
		final boolean VerifSch = verifyCCSchnorrProofsAlgorithm.verifyCCSchnorrProofs(verifyCCSchnorrProofsContext, verifyCCSchnorrProofsInput);

		checkState(VerifSch, "The CCM Schnorr proofs are invalid.");

		final AuxiliaryInformation i_aux_EB = AuxiliaryInformation.of(hContext, "SetupTallyEB", integerToString(1));

		final List<GqElement> EB_pk_elements = new ArrayList<>();
		final List<SchnorrProof> pi_EB_elements = new ArrayList<>();
		for (int i = 0; i < delta_max; i++) {
			final ZqElement EB_sk_i = hash.recursiveHashToZq(q,
					Streams.concat(Stream.of(HashableString.from("ElectoralBoardSecretKey"), HashableString.from(ee),
							HashableBigInteger.from(BigInteger.valueOf(i))), PW.stream()).collect(HashableList.toHashableList()));

			final GqElement EB_pk_i = g.exponentiate(EB_sk_i);

			final SchnorrProof pi_EB_i = zeroKnowledgeProof.genSchnorrProof(EB_sk_i, EB_pk_i, i_aux_EB);

			EB_pk_elements.add(EB_pk_i);
			pi_EB_elements.add(pi_EB_i);
		}

		final ElGamalMultiRecipientPublicKey EB_pk = new ElGamalMultiRecipientPublicKey(GroupVector.from(EB_pk_elements));

		final GroupVector<SchnorrProof, ZqGroup> pi_EB = GroupVector.from(pi_EB_elements);

		// Since we have 0-indexing in Java, the indexes 1-4 become indexes 0-3
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> EL_pk_vector_EB_pk = GroupVector.of(EL_pk_vector.get(0), EL_pk_vector.get(1),
				EL_pk_vector.get(2), EL_pk_vector.get(3), EB_pk);
		final ElGamalMultiRecipientPublicKey EL_pk = elGamal.combinePublicKeys(EL_pk_vector_EB_pk);

		// Wipe the passwords after usage.
		input.electoralBoardMembersPasswords().forEach(SafePasswordHolder::clear);

		// Output.
		return new SetupTallyEBOutput(EL_pk, EB_pk, pi_EB);
	}
}
