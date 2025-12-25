/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;

@JsonPropertyOrder({ "electionEventId", "verificationCardSetId", "verificationCardKeystores", "signature" })
public final class SetupComponentVerificationCardKeystoresPayload implements SignedPayload {
	@JsonProperty
	private final String electionEventId;
	@JsonProperty
	private final String verificationCardSetId;
	@JsonProperty
	private final ImmutableList<VerificationCardKeystore> verificationCardKeystores;
	@JsonProperty
	private CryptoPrimitivesSignature signature;

	@JsonCreator
	public SetupComponentVerificationCardKeystoresPayload(
			@JsonProperty("electionEventId")
			final String electionEventId,

			@JsonProperty("verificationCardSetId")
			final String verificationCardSetId,

			@JsonProperty("verificationCardKeystores")
			final ImmutableList<VerificationCardKeystore> verificationCardKeystores,

			@JsonProperty("signature")
			final CryptoPrimitivesSignature signature) {
		this(electionEventId, verificationCardSetId, verificationCardKeystores);
		this.signature = checkNotNull(signature);
	}

	public SetupComponentVerificationCardKeystoresPayload(final String electionEventId, final String verificationCardSetId,
			final ImmutableList<VerificationCardKeystore> verificationCardKeystores) {
		this.electionEventId = validateUUID(electionEventId);
		this.verificationCardSetId = validateUUID(verificationCardSetId);
		this.verificationCardKeystores = checkNotNull(verificationCardKeystores);

		checkArgument(!this.verificationCardKeystores.isEmpty(), "The list of verificationCardKeystores must not be empty.");
		checkArgument(hasNoDuplicates(this.verificationCardKeystores.stream()
				.map(VerificationCardKeystore::verificationCardId)
				.collect(toImmutableList())), "The verification card keystores must not contain any duplicate verification card ids.");
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public ImmutableList<VerificationCardKeystore> getVerificationCardKeystores() {
		return verificationCardKeystores;
	}

	@Override
	public CryptoPrimitivesSignature getSignature() {
		return signature;
	}

	@Override
	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = checkNotNull(signature);

	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(HashableString.from(electionEventId),
				HashableString.from(verificationCardSetId),
				HashableList.from(verificationCardKeystores));
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final SetupComponentVerificationCardKeystoresPayload that = (SetupComponentVerificationCardKeystoresPayload) o;
		return electionEventId.equals(that.electionEventId) &&
				verificationCardSetId.equals(that.verificationCardSetId) &&
				verificationCardKeystores.equals(that.verificationCardKeystores) &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(electionEventId, verificationCardSetId, verificationCardKeystores, signature);
	}
}
