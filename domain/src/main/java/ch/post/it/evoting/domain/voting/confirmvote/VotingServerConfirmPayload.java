/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;

@JsonPropertyOrder({ "encryptionGroup", "confirmationKey", "confirmationAttemptId", "signature" })
@JsonDeserialize(using = VotingServerConfirmPayloadDeserializer.class)
public class VotingServerConfirmPayload implements SignedPayload {

	@JsonProperty
	private final GqGroup encryptionGroup;

	@JsonProperty
	private final ConfirmationKey confirmationKey;

	@JsonProperty
	private final int confirmationAttemptId;

	@JsonProperty
	private CryptoPrimitivesSignature signature;

	@JsonCreator
	public VotingServerConfirmPayload(

			@JsonProperty("encryptionGroup")
			final GqGroup encryptionGroup,

			@JsonProperty("confirmationKey")
			final ConfirmationKey confirmationKey,

			@JsonProperty("confirmationAttemptId")
			final int confirmationAttemptId,

			@JsonProperty("signature")
			final CryptoPrimitivesSignature signature) {

		this(encryptionGroup, confirmationKey, confirmationAttemptId);
		this.signature = checkNotNull(signature);
	}

	public VotingServerConfirmPayload(final GqGroup encryptionGroup, final ConfirmationKey confirmationKey,
			final int confirmationAttemptId) {
		this.encryptionGroup = checkNotNull(encryptionGroup);

		checkArgument(encryptionGroup.equals(confirmationKey.element().getGroup()), "The confirmation key must be in the encryption group");
		this.confirmationKey = checkNotNull(confirmationKey);

		checkArgument(confirmationAttemptId >= 0 && confirmationAttemptId < MAX_CONFIRMATION_ATTEMPTS,
				"The confirmation attempt id must be in range [0,%s).", MAX_CONFIRMATION_ATTEMPTS);
		this.confirmationAttemptId = confirmationAttemptId;
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public ConfirmationKey getConfirmationKey() {
		return confirmationKey;
	}

	public int getConfirmationAttemptId() {
		return confirmationAttemptId;
	}

	public CryptoPrimitivesSignature getSignature() {
		return signature;
	}

	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = checkNotNull(signature);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final VotingServerConfirmPayload that = (VotingServerConfirmPayload) o;
		return encryptionGroup.equals(that.encryptionGroup) &&
				confirmationKey.equals(that.confirmationKey) &&
				confirmationAttemptId == that.confirmationAttemptId &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, confirmationKey, confirmationAttemptId, signature);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				encryptionGroup,
				confirmationKey,
				HashableBigInteger.from(confirmationAttemptId));
	}
}
