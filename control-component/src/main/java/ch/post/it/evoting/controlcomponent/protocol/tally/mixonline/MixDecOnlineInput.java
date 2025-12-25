/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

/**
 * Regroups the input values needed by the MixDecOnline algorithm.
 *
 * <ul>
 *     <li>c<sub>dec,j-1</sub>, the partially decrypted votes. Not null.</li>
 *     <li>EL<sub>sk,j</sub>, the CCM<sub>j</sub> election secret key. Not null.</li>
 *     <li>hvc<sub>j</sub>, the CCM<sub>j</sub> hash of the encrypted, confirmed votes. Not null.</li>
 *     <li>hvc=(hvc<sub>1</sub>,hvc<sub>2</sub>,hvc<sub>3</sub>,hvc<sub>4</sub>), the CCM hashes of the encrypted, confirmed votes. Not null, of size 4 and all entries equal to hvc<sub>j</sub>.</li>
 * </ul>
 */
@SuppressWarnings("java:S1068") // Fields encryptionConfirmedVotesHash and encryptedConfirmedVotesHashes are aligned to specification.
public class MixDecOnlineInput {

	private final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes;
	private final ElGamalMultiRecipientPrivateKey ccmjElectionSecretKey;
	private final String encryptedConfirmedVotesHash; // Aligned to specification.
	private final ImmutableList<String> encryptedConfirmedVotesHashes; // Aligned to specification.

	private MixDecOnlineInput(
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes,
			final ElGamalMultiRecipientPrivateKey ccmjElectionSecretKey,
			final String encryptedConfirmedVotesHash,
			final ImmutableList<String> encryptedConfirmedVotesHashes) {
		this.partiallyDecryptedVotes = partiallyDecryptedVotes;
		this.ccmjElectionSecretKey = ccmjElectionSecretKey;
		this.encryptedConfirmedVotesHash = encryptedConfirmedVotesHash;
		this.encryptedConfirmedVotesHashes = encryptedConfirmedVotesHashes;
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getPartiallyDecryptedVotes() {
		return partiallyDecryptedVotes;
	}

	public ElGamalMultiRecipientPrivateKey getCcmjElectionSecretKey() {
		return ccmjElectionSecretKey;
	}

	/**
	 * Builder performing input validations before constructing a {@link MixDecOnlineInput}.
	 */
	public static class Builder {

		private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes;
		private ElGamalMultiRecipientPrivateKey ccmjElectionSecretKey;
		private String encryptedConfirmedVotesHash;
		private ImmutableList<String> encryptedConfirmedVotesHashes;

		public Builder setPartiallyDecryptedVotes(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> partiallyDecryptedVotes) {
			this.partiallyDecryptedVotes = partiallyDecryptedVotes;
			return this;
		}

		public Builder setCcmjElectionSecretKey(final ElGamalMultiRecipientPrivateKey ccmjElectionSecretKey) {
			this.ccmjElectionSecretKey = ccmjElectionSecretKey;
			return this;
		}

		public Builder setEncryptedConfirmedVotesHash(final String encryptedConfirmedVotesHash) {
			this.encryptedConfirmedVotesHash = encryptedConfirmedVotesHash;
			return this;
		}

		public Builder setEncryptedConfirmedVotesHashes(final ImmutableList<String> encryptedConfirmedVotesHashes) {
			this.encryptedConfirmedVotesHashes = encryptedConfirmedVotesHashes;
			return this;
		}

		/**
		 * Creates a MixDecryptInput object.
		 *
		 * @throws NullPointerException     if any of the fields are null.
		 * @throws IllegalArgumentException if
		 *                                  <ul>
		 *                                      <li>the ciphertexts and the public keys do not have the same group.</li>
		 *                                      <li>the CCM hashes of the encrypted, confirmed votes are not of size 4.</li>
		 *                                      <li>the CCM hashes of the encrypted, confirmed votes are not all equal to the CCM<sub>j</sub> hash of the encrypted, confirmed votes.</li>
		 *                                  </ul>
		 */
		@SuppressWarnings("java:S117")
		public MixDecOnlineInput build() {
			checkNotNull(partiallyDecryptedVotes);
			checkNotNull(ccmjElectionSecretKey);
			checkNotNull(encryptedConfirmedVotesHash);
			checkNotNull(encryptedConfirmedVotesHashes);

			// Size checks.
			checkArgument(encryptedConfirmedVotesHashes.size() == ControlComponentNode.ids().size(),
					"The must be exactly %s encrypted confirmed votes hashes.",
					ControlComponentNode.ids().size());

			// Cross-group checks.
			checkArgument(partiallyDecryptedVotes.getGroup().hasSameOrderAs(ccmjElectionSecretKey.getGroup()),
					"The partially decrypted votes must have the same group order as the CCM_j election secret key.");

			// Require.
			checkArgument(Stream.concat(Stream.of(encryptedConfirmedVotesHash), encryptedConfirmedVotesHashes.stream())
							.map(hvc -> {
								checkArgument(hvc.length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
										"The hash of the encrypted, confirmed votes must be of size %s.", BASE64_ENCODED_HASH_OUTPUT_LENGTH);
								return validateBase64Encoded(hvc);
							})
							.allMatch(hvc_j -> hvc_j.equals(encryptedConfirmedVotesHash)),
					"The view of the initial ciphertexts must be the same for all CCs before mixing begins.");

			return new MixDecOnlineInput(partiallyDecryptedVotes, ccmjElectionSecretKey,
					encryptedConfirmedVotesHash, encryptedConfirmedVotesHashes);
		}
	}
}
