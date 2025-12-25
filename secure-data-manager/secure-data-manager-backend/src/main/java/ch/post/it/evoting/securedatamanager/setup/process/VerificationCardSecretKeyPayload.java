/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;

@JsonDeserialize(using = VerificationCardSecretKeyPayloadDeserializer.class)
public record VerificationCardSecretKeyPayload(GqGroup encryptionGroup,
											   String electionEventId,
											   String verificationCardSetId,
											   ImmutableList<VerificationCardSecretKey> verificationCardSecretKeys
) {
	public VerificationCardSecretKeyPayload(final GqGroup encryptionGroup,
			final String electionEventId,
			final String verificationCardSetId,
			final ImmutableList<VerificationCardSecretKey> verificationCardSecretKeys) {
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.electionEventId = validateUUID(electionEventId);
		this.verificationCardSetId = validateUUID(verificationCardSetId);
		this.verificationCardSecretKeys = checkNotNull(verificationCardSecretKeys);

		checkArgument(!this.verificationCardSecretKeys.isEmpty(), "There must be at least one verification card secret key.");

		final GroupVector<ElGamalMultiRecipientPrivateKey, ZqGroup> secretKeys = this.verificationCardSecretKeys.stream()
				.map(VerificationCardSecretKey::privateKey)
				.collect(GroupVector.toGroupVector());
		checkArgument(secretKeys.getGroup().hasSameOrderAs(encryptionGroup),
				"The verification card secret keys' group must be of same order as the encryption group.");

		final ImmutableList<String> verificationCardIds = this.verificationCardSecretKeys.stream()
				.map(VerificationCardSecretKey::verificationCardId)
				.collect(toImmutableList());
		checkArgument(hasNoDuplicates(verificationCardIds), "There must not be any duplicate verification card ids.");
	}
}
