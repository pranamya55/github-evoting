/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsContext;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsInput;

/**
 * Implements the GenVerCardSetKeys algorithm.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class GenVerCardSetKeysAlgorithm {

	private final ElGamal elGamal;
	private final VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm;

	public GenVerCardSetKeysAlgorithm(
			final ElGamal elGamal,
			final VerifyCCSchnorrProofsAlgorithm verifyCCSchnorrProofsAlgorithm) {
		this.elGamal = elGamal;
		this.verifyCCSchnorrProofsAlgorithm = verifyCCSchnorrProofsAlgorithm;
	}

	/**
	 * Generates the verification card set keys by combining the CCR Choice Return Codes encryption public keys pk. Also verifies the Schnorr proofs
	 * associated to each CCR public key.
	 *
	 * @param context the {@link GenVerCardSetKeysContext}. Must be non-null.
	 * @param input   the {@link GenVerCardSetKeysInput}. Must be non-null.
	 * @throws NullPointerException     if any input is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>the context and input do not have the same encryption group.</li>
	 *                                      <li>the CCR Choice Return Codes encryption public keys don't have the expected element size.</li>
	 *                                      <li>the CCR Schnorr proofs don't have the expected element size.</li>
	 *                                  </ul>
	 * @throws IllegalStateException    if the CCR Schnorr proofs are invalid.
	 */
	@SuppressWarnings("java:S117")
	public ElGamalMultiRecipientPublicKey genVerCardSetKeys(final GenVerCardSetKeysContext context, final GenVerCardSetKeysInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Cross-group check.
		checkArgument(context.encryptionGroup().equals(input.ccrChoiceReturnCodesEncryptionPublicKeys().getGroup()),
				"The context and input must have the same encryption group.");

		// Context.
		final GqGroup p_q_g = context.encryptionGroup();
		final String ee = context.electionEventId();
		final int psi_max = context.maximumNumberOfSelections();

		// Input.
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> pk_CCR = input.ccrChoiceReturnCodesEncryptionPublicKeys();
		final GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> pi_pkCCR = input.ccrSchnorrProofs();

		// Cross-checks.
		checkArgument(pk_CCR.getElementSize() == psi_max,
				"The size of the CCR Choice Return Codes encryption keys and Schnorr proofs must be equal to the maximum number of selections. [psi_max: %s]",
				psi_max);

		// Operation.
		final VerifyCCSchnorrProofsContext verifyCCSchnorrProofsContext = new VerifyCCSchnorrProofsContext(p_q_g, ControlComponentNode.ids().size(),
				psi_max);
		final VerifyCCSchnorrProofsInput verifyCCSchnorrProofsInput = new VerifyCCSchnorrProofsInput(pk_CCR, pi_pkCCR,
				AuxiliaryInformation.of(ee, "GenKeysCCR"));
		final boolean verifSch = verifyCCSchnorrProofsAlgorithm.verifyCCSchnorrProofs(verifyCCSchnorrProofsContext, verifyCCSchnorrProofsInput);

		checkState(verifSch, "The CCR Schnorr proofs are invalid.");

		return elGamal.combinePublicKeys(pk_CCR);
	}

}
