/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

/**
 * Represents the voting client public keys.
 */
public record VotingClientPublicKeys(GqGroup encryptionParameters, ElGamalMultiRecipientPublicKey electionPublicKey,
									 ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey) {

	public VotingClientPublicKeys {
		checkNotNull(encryptionParameters);
		checkNotNull(electionPublicKey);
		checkNotNull(choiceReturnCodesEncryptionPublicKey);
	}

}
