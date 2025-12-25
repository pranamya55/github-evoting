/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.DecryptionProof;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.VerifiablePlaintextDecryption;

/**
 * Contains the output of the MixDecOffline algorithm.
 *
 * <ul>
 *     <li>a verifiable shuffle:</li>
 *     <ul>
 *         <li>c<sub>mix,5</sub>, the shuffled votes. Non-null.</li>
 *         <li>&pi;<sub>mix,5</sub>, the shuffle proof. Non-null.</li>
 *     </ul>
 *     <li>a verifiable plaintext decryption:</li>
 *     <ul>
 *         <li>m, the decrypted votes. Non-null.</li>
 *         <li>&pi;<sub>dec,5</sub>, the decryption proofs. Non-null.</li>
 *     </ul>
 * </ul>
 * </p>
 */
public class MixDecOfflineOutput {

	private final VerifiableShuffle verifiableShuffle;
	private final VerifiablePlaintextDecryption verifiablePlaintextDecryption;

	public MixDecOfflineOutput(final VerifiableShuffle verifiableShuffle, final GroupVector<ElGamalMultiRecipientMessage, GqGroup> decryptedVotes,
			final GroupVector<DecryptionProof, ZqGroup> decryptionProofs) {
		checkNotNull(verifiableShuffle);
		checkNotNull(decryptedVotes);
		checkNotNull(decryptionProofs);

		// Cross-group checks
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> shuffledCiphertexts = verifiableShuffle.shuffledCiphertexts();
		checkArgument(shuffledCiphertexts.getGroup().equals(decryptedVotes.getGroup()),
				"The shuffled votes must have the same group as the decrypted votes.");
		checkArgument(decryptedVotes.getGroup().hasSameOrderAs(decryptionProofs.getGroup()),
				"The decrypted votes and the decryption proofs must have the same group order.");

		// Cross-dimension check
		checkArgument(shuffledCiphertexts.size() == decryptedVotes.size(),
				"The shuffled votes and the decrypted votes must have the same vector size.");
		checkArgument(shuffledCiphertexts.getElementSize() == decryptedVotes.getElementSize(),
				"The shuffled votes and the decrypted votes must have the same element size.");
		checkArgument(decryptedVotes.size() == decryptionProofs.size(),
				"The decrypted votes and the decryption proofs must have the same vector size.");

		this.verifiableShuffle = verifiableShuffle;
		this.verifiablePlaintextDecryption = new VerifiablePlaintextDecryption(decryptedVotes, decryptionProofs);
	}

	public VerifiableShuffle getVerifiableShuffle() {
		return verifiableShuffle;
	}

	public VerifiablePlaintextDecryption getVerifiablePlaintextDecryption() {
		return verifiablePlaintextDecryption;
	}
}
