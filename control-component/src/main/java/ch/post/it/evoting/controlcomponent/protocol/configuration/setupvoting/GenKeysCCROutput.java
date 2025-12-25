/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;

/**
 * Holds the output of the GenKeysCCR algorithm.
 *
 * <ul>
 *     <li>pk<sub>CCRj</sub>, CCR<sub>j</sub> Choice Return Codes encryption public key. Not null.</li>
 *     <li>sk<sub>CCRj</sub>, CCR<sub>j</sub> Choice Return Codes encryption secret key. Not null.</li>
 *     <li>k'<sub>j</sub>, CCR<sub>j</sub> Return Codes Generation secret key. Not null.</li>
 *     <li>&pi;<sub>pkCCR,j</sub>, CCR<sub>j</sub> Schnorr proofs of knowledge. Not null.</li>
 * </ul>
 */
public record GenKeysCCROutput(ElGamalMultiRecipientKeyPair ccrjChoiceReturnCodesEncryptionKeyPair,
							   ZqElement ccrjReturnCodesGenerationSecretKey,
							   GroupVector<SchnorrProof, ZqGroup> ccrjSchnorrProofs) {

	public GenKeysCCROutput {
		checkNotNull(ccrjChoiceReturnCodesEncryptionKeyPair);
		checkNotNull(ccrjReturnCodesGenerationSecretKey);
		checkNotNull(ccrjSchnorrProofs);

		// Size check.
		checkArgument(ccrjChoiceReturnCodesEncryptionKeyPair.size() == ccrjSchnorrProofs.size(),
				"The size of the ccrj Choice Return Codes encryption key pair must be equal to the number of Schnorr proofs.");
		checkArgument(ccrjChoiceReturnCodesEncryptionKeyPair.size() <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The ccrj Choice Return Codes encryption key pair must be of size smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				ccrjChoiceReturnCodesEncryptionKeyPair.size(), MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);

		// Cross group check.
		checkArgument(ccrjChoiceReturnCodesEncryptionKeyPair.getGroup().hasSameOrderAs(ccrjReturnCodesGenerationSecretKey.getGroup()),
				"The ccrj Return Codes generation secret key must have the same order than the ccr Choice Return Codes encryption key pair.");
		checkArgument(ccrjChoiceReturnCodesEncryptionKeyPair.getGroup().hasSameOrderAs(ccrjSchnorrProofs.getGroup()),
				"The Schnorr proofs must have the same group order as the ccr Choice Return Codes encryption key pair.");

	}

}
