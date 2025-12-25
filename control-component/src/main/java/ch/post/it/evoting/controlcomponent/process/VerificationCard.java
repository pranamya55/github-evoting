/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;

public record VerificationCard(String verificationCardId, String verificationCardSetId, ElGamalMultiRecipientPublicKey verificationCardPublicKey) {

	public VerificationCard {
		validateUUID(verificationCardId);
		validateUUID(verificationCardSetId);
		checkNotNull(verificationCardPublicKey);
	}

}
