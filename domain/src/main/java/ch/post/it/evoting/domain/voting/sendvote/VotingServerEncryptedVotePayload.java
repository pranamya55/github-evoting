/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.sendvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;

@JsonDeserialize(using = VotingServerEncryptedVotePayloadDeserializer.class)
@JsonPropertyOrder({ "encryptionGroup", "encryptedVerifiableVote", "signature" })
public class VotingServerEncryptedVotePayload implements SignedPayload {

	@JsonProperty
	private final GqGroup encryptionGroup;

	@JsonProperty
	private final EncryptedVerifiableVote encryptedVerifiableVote;

	@JsonProperty
	private CryptoPrimitivesSignature signature;

	@JsonCreator
	public VotingServerEncryptedVotePayload(

			@JsonProperty("encryptionGroup")
			final GqGroup encryptionGroup,

			@JsonProperty("encryptedVerifiableVote")
			final EncryptedVerifiableVote encryptedVerifiableVote,

			@JsonProperty("signature")
			final CryptoPrimitivesSignature signature) {
		this(encryptionGroup, encryptedVerifiableVote);
		this.signature = checkNotNull(signature);
	}

	/**
	 * Creates an unsigned payload.
	 */
	public VotingServerEncryptedVotePayload(final GqGroup encryptionGroup, final EncryptedVerifiableVote encryptedVerifiableVote) {
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.encryptedVerifiableVote = checkNotNull(encryptedVerifiableVote);
		checkArgument(encryptionGroup.equals(encryptedVerifiableVote.encryptedVote().getGroup()),
				"The groups of the voting server encrypted vote payload and the encrypted vote of the encrypted verifiable vote must be equal.");
	}

	public GqGroup getEncryptionGroup() {
		return encryptionGroup;
	}

	public EncryptedVerifiableVote getEncryptedVerifiableVote() {
		return encryptedVerifiableVote;
	}

	public CryptoPrimitivesSignature getSignature() {
		return signature;
	}

	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = checkNotNull(signature);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				encryptionGroup,
				encryptedVerifiableVote);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final VotingServerEncryptedVotePayload that = (VotingServerEncryptedVotePayload) o;
		return encryptionGroup.equals(that.encryptionGroup) &&
				encryptedVerifiableVote.equals(that.encryptedVerifiableVote) &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(encryptionGroup, encryptedVerifiableVote, signature);
	}
}
