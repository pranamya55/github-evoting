/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;

/**
 * Regroups the output values returned by the SetupTallyCCM algorithm.
 *
 * <ul>
 *     <li>EL<sub>pk,j</sub>, the CCM<sub>j</sub> election public key. Not null.</li>
 *     <li>EL<sub>sk,j</sub>, the CCM<sub>j</sub> election secret key. Not null.</li>
 *     <li>&pi;<sub>EL<sub>pk,j</sub></sub>, the CCM<sub>j</sub> Schnorr proofs of knowledge. Not null.</li>
 * </ul>
 */
public class SetupTallyCCMOutput {

	private final ElGamalMultiRecipientKeyPair ccmjElectionKeyPair;
	private final GroupVector<SchnorrProof, ZqGroup> schnorrProofs;

	private SetupTallyCCMOutput(final ElGamalMultiRecipientKeyPair ccmjElectionKeyPair, final GroupVector<SchnorrProof, ZqGroup> schnorrProofs) {
		this.ccmjElectionKeyPair = ccmjElectionKeyPair;
		this.schnorrProofs = schnorrProofs;
	}

	public final ElGamalMultiRecipientKeyPair getCcmjElectionKeyPair() {
		return ccmjElectionKeyPair;
	}

	public final GroupVector<SchnorrProof, ZqGroup> getSchnorrProofs() {
		return schnorrProofs;
	}

	/**
	 * Builder performing input validations and cross-validations before constructing a {@link SetupTallyCCMOutput}.
	 */
	public static class Builder {
		private ElGamalMultiRecipientKeyPair elGamalMultiRecipientKeyPair;
		private GroupVector<SchnorrProof, ZqGroup> schnorrProofs;

		public Builder setElGamalMultiRecipientKeyPair(final ElGamalMultiRecipientKeyPair elGamalMultiRecipientKeyPair) {
			this.elGamalMultiRecipientKeyPair = elGamalMultiRecipientKeyPair;
			return this;
		}

		public Builder setSchnorrProofs(final GroupVector<SchnorrProof, ZqGroup> schnorrProofs) {
			this.schnorrProofs = schnorrProofs;
			return this;
		}

		/**
		 * Creates the SetupTallyCCMOutput object.
		 *
		 * @throws NullPointerException     if any of the fields are null.
		 * @throws IllegalArgumentException if
		 *                                  <ul>
		 *                                      <li>the Schnorr proofs of knowledge and election public key do not have the same group order.</li>
		 *                                      <li>the number of Schnorr proofs of knowledge and key elements are not same.</li>
		 *                                      <li>the number of Schnorr proofs of knowledge is strictly greater than &delta;<sub>sup</sub>.</li>
		 *                                  </ul>
		 */
		public SetupTallyCCMOutput build() {
			checkNotNull(elGamalMultiRecipientKeyPair);
			checkNotNull(schnorrProofs);

			// Cross-group checks.
			checkArgument(schnorrProofs.getGroup()
							.hasSameOrderAs(elGamalMultiRecipientKeyPair.getGroup()),
					"The Schnorr proofs of knowledge and election public key do not have the same group order.");

			checkArgument(schnorrProofs.size() == elGamalMultiRecipientKeyPair.getPublicKey().getKeyElements().size(),
					"The number of Schnorr proofs of knowledge and key elements are not same.");

			final int delta_sup = MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1;
			checkArgument(schnorrProofs.size() <= delta_sup,
					"The number of Schnorr proofs of knowledge must be smaller or equal to the maximum supported number of write-ins plus one. [delta_max: %s, delta_sup: %s]",
					schnorrProofs.size(), delta_sup);

			return new SetupTallyCCMOutput(elGamalMultiRecipientKeyPair, schnorrProofs);
		}
	}
}
