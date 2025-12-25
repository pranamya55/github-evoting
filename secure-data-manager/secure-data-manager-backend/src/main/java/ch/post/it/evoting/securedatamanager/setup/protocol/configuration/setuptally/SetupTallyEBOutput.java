/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;

/**
 * Regroups the outputs of the SetupTallyEB algorithm.
 * <ul>
 *     <li>EL<sub>pk</sub>, the election public key. Not null.</li>
 *     <li>EB<sub>pk</sub>, the electoral board public key. Not null.</li>
 *     <li>&pi;<sub>EB</sub>, the electoral board Schnorr proofs of knowledge. Not null.</li>
 * </ul>
 */
public class SetupTallyEBOutput {

	private final ElGamalMultiRecipientPublicKey electionPublicKey;
	private final ElGamalMultiRecipientPublicKey electoralBoardPublicKey;
	private final GroupVector<SchnorrProof, ZqGroup> electoralBoardSchnorrProofs;

	/**
	 * Constructor for a SetupTallyEBOutput.
	 *
	 * @param electionPublicKey           EL<sub>pk</sub>, the election public key. Non-null.
	 * @param electoralBoardPublicKey     EB<sub>pk</sub>, the electoral board public key. Non-null.
	 * @param electoralBoardSchnorrProofs &pi;<sub>EB</sub>, the Schnorr proofs of knowledge. Non-null.
	 * @throws NullPointerException     if any of the input parameters is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                      <li>the election public key has not the same size as the electoral board keys.</li>
	 *                                      <li>the election public key has not the same size as the  electoral board Schnorr proofs.</li>
	 *                                      <li>the election public key has not the same group as the electoral board keys.</li>
	 *                                      <li>the election public key and the electoral board Schnorr proofs have groups of different order.</li>
	 *                                  </ul>
	 */
	SetupTallyEBOutput(final ElGamalMultiRecipientPublicKey electionPublicKey, final ElGamalMultiRecipientPublicKey electoralBoardPublicKey, final
	GroupVector<SchnorrProof, ZqGroup> electoralBoardSchnorrProofs) {
		checkNotNull(electionPublicKey);
		checkNotNull(electoralBoardPublicKey);
		checkNotNull(electoralBoardSchnorrProofs);
		checkArgument(electionPublicKey.size() == electoralBoardPublicKey.size(),
				"The election public key and the electoral board keys must be of same size.");
		checkArgument(electionPublicKey.size() == electoralBoardSchnorrProofs.size(),
				"The election public key and the electoral board Schnorr proofs of knowledge must be of same size.");
		checkArgument(electionPublicKey.getGroup().equals(electoralBoardPublicKey.getGroup()),
				"The election public key and the electoral board keys must have the same group.");
		checkArgument(electionPublicKey.getGroup().hasSameOrderAs(electoralBoardSchnorrProofs.getGroup()),
				"The election public key and the electoral board Schnorr proofs must have groups of same order.");

		this.electionPublicKey = electionPublicKey;
		this.electoralBoardPublicKey = electoralBoardPublicKey;
		this.electoralBoardSchnorrProofs = electoralBoardSchnorrProofs;
	}

	public ElGamalMultiRecipientPublicKey getElectionPublicKey() {
		return this.electionPublicKey;
	}

	public ElGamalMultiRecipientPublicKey getElectoralBoardPublicKey() {
		return this.electoralBoardPublicKey;
	}

	public GroupVector<SchnorrProof, ZqGroup> getElectoralBoardSchnorrProofs() {
		return electoralBoardSchnorrProofs;
	}
}