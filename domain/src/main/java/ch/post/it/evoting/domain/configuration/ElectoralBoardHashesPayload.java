/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashableList.toHashableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Base64;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

@JsonPropertyOrder({ "electionEventId", "electoralBoardHashes", "signature" })
public class ElectoralBoardHashesPayload implements SignedPayload {

	private final String electionEventId;

	private final ImmutableList<String> electoralBoardHashes;

	private CryptoPrimitivesSignature signature;

	@JsonCreator
	public ElectoralBoardHashesPayload(
			@JsonProperty("electionEventId")
			final String electionEventId,
			@JsonProperty("electoralBoardHashes")
			final ImmutableList<String> electoralBoardHashes,
			@JsonProperty("signature")
			final CryptoPrimitivesSignature signature
	) {
		this.electionEventId = validateUUID(electionEventId);
		this.electoralBoardHashes = checkNotNull(electoralBoardHashes);
		checkArgument(this.electoralBoardHashes.size() >= 2);
		this.electoralBoardHashes.forEach(Validations::validateBase64Encoded);
		this.signature = checkNotNull(signature);
	}

	public ElectoralBoardHashesPayload(final String electionEventId, final ImmutableList<ImmutableByteArray> electoralBoardHashes) {
		this.electionEventId = validateUUID(electionEventId);
		checkNotNull(electoralBoardHashes);
		checkArgument(electoralBoardHashes.size() >= 2);
		this.electoralBoardHashes = electoralBoardHashes.stream().parallel()
				.map(ImmutableByteArray::elements)
				.map(hash -> Base64.getEncoder().encodeToString(hash))
				.collect(toImmutableList());
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public ImmutableList<ImmutableByteArray> getElectoralBoardHashes() {
		return electoralBoardHashes.stream()
				.map(hash -> Base64.getDecoder().decode(hash))
				.map(ImmutableByteArray::new)
				.collect(toImmutableList());
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
				HashableString.from(electionEventId),
				electoralBoardHashes.stream().map(HashableString::from).collect(toHashableList()));
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ElectoralBoardHashesPayload that = (ElectoralBoardHashesPayload) o;
		return electionEventId.equals(that.electionEventId) &&
				electoralBoardHashes.equals(that.electoralBoardHashes) &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(electionEventId, electoralBoardHashes, signature);
	}
}
