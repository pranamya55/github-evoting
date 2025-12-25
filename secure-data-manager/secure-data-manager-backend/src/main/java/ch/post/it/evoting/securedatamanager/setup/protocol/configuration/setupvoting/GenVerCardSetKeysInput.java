/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

/**
 * Regroups the inputs needed by the GenVerCardSetKeys algorithm.
 * <ul>
 *     <li>pk<sub>CCR</sub>, the CCR Choice Return Codes encryption public keys. Not null.</li>
 *     <li>&pi;<sub>pkCCR</sub>, the CCR Schnorr proofs of knowledge. Not null.</li>
 * </ul>
 */
public record GenVerCardSetKeysInput(GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccrChoiceReturnCodesEncryptionPublicKeys,
									 GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> ccrSchnorrProofs) {

	public GenVerCardSetKeysInput {
		checkNotNull(ccrChoiceReturnCodesEncryptionPublicKeys);
		checkNotNull(ccrSchnorrProofs);

		checkArgument(ccrChoiceReturnCodesEncryptionPublicKeys.getGroup().hasSameOrderAs(ccrSchnorrProofs.getGroup()),
				"The CCR election public keys and the Schnorr proofs must have the same group order.");

		checkArgument(ccrChoiceReturnCodesEncryptionPublicKeys.size() == ccrSchnorrProofs.size(),
				"There must be as many Schnorr proofs as CCR election public keys.");
		checkArgument(ccrChoiceReturnCodesEncryptionPublicKeys.size() == ControlComponentNode.ids().size(),
				"There must be exactly 4 CCR election public keys.");

		checkArgument(ccrSchnorrProofs.getElementSize() == ccrChoiceReturnCodesEncryptionPublicKeys.getElementSize(),
				"The size of the CCR Choice Return Codes encryption keys must be equal to the size of the Schnorr proofs.");
		checkArgument(ccrChoiceReturnCodesEncryptionPublicKeys.getElementSize() <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The size of the CCR Choice Return Codes encryption keys and Schnorr proofs must be smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				ccrChoiceReturnCodesEncryptionPublicKeys.getElementSize(), MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
	}
}
