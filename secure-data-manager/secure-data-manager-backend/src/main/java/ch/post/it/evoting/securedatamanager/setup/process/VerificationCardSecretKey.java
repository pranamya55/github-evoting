/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;

public record VerificationCardSecretKey(String verificationCardId,
										ElGamalMultiRecipientPrivateKey privateKey
) {
	public VerificationCardSecretKey {
		validateUUID(verificationCardId);
		checkNotNull(privateKey);
	}
}
