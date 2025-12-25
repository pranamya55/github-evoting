/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Base64;
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

@JsonPropertyOrder({ "electionEventId", "verificationCardSetId", "longVoteCastReturnCodesAllowList", "signature" })
public final class SetupComponentLVCCAllowListPayload implements SignedPayload {

	@JsonProperty
	private final String electionEventId;

	@JsonProperty
	private final String verificationCardSetId;

	@JsonProperty
	private final ImmutableList<String> longVoteCastReturnCodesAllowList;

	@JsonProperty
	private CryptoPrimitivesSignature signature;

	@JsonCreator
	public SetupComponentLVCCAllowListPayload(
			@JsonProperty("electionEventId")
			final String electionEventId,

			@JsonProperty("verificationCardSetId")
			final String verificationCardSetId,

			@JsonProperty("longVoteCastReturnCodesAllowList")
			final ImmutableList<String> longVoteCastReturnCodesAllowList,

			@JsonProperty("signature")
			final CryptoPrimitivesSignature signature) {

		this(electionEventId, verificationCardSetId, longVoteCastReturnCodesAllowList);
		this.signature = checkNotNull(signature);
	}

	public SetupComponentLVCCAllowListPayload(final String electionEventId, final String verificationCardSetId,
			final ImmutableList<String> longVoteCastReturnCodesAllowList) {
		this.electionEventId = validateUUID(electionEventId);
		this.verificationCardSetId = validateUUID(verificationCardSetId);
		this.longVoteCastReturnCodesAllowList = checkNotNull(longVoteCastReturnCodesAllowList);

		checkArgument(!this.longVoteCastReturnCodesAllowList.isEmpty(), "The long Vote Cast Return Codes Allow List must not be empty.");
		checkArgument(allEqual(this.longVoteCastReturnCodesAllowList.stream(), String::length),
				"The length of all long Vote Cast Return Codes allow list entries must be equal.");
		checkArgument(this.longVoteCastReturnCodesAllowList.stream().allMatch(Objects::nonNull),
				"The long Vote Cast Return Codes Allow List must not contain null elements.");
		checkArgument(this.longVoteCastReturnCodesAllowList.stream().noneMatch(String::isBlank),
				"The long Vote Cast Return Codes Allow List must not contain empty or whitespace strings.");
		// The long Vote Cast Return Codes Allow List must only contain Base64 strings.
		this.longVoteCastReturnCodesAllowList.forEach(Base64.getDecoder()::decode);
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public ImmutableList<String> getLongVoteCastReturnCodesAllowList() {
		return longVoteCastReturnCodesAllowList;
	}

	public CryptoPrimitivesSignature getSignature() {
		return signature;
	}

	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = checkNotNull(signature);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		final ImmutableList<HashableString> hashableLongVoteCastReturnCodesAllowList = longVoteCastReturnCodesAllowList.stream()
				.map(HashableString::from)
				.collect(toImmutableList());

		return ImmutableList.of(
				HashableString.from(electionEventId),
				HashableString.from(verificationCardSetId),
				HashableList.from(hashableLongVoteCastReturnCodesAllowList));
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final SetupComponentLVCCAllowListPayload that = (SetupComponentLVCCAllowListPayload) o;
		return electionEventId.equals(that.electionEventId) &&
				verificationCardSetId.equals(that.verificationCardSetId) &&
				longVoteCastReturnCodesAllowList.equals(that.longVoteCastReturnCodesAllowList)
				&& Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(electionEventId, verificationCardSetId, longVoteCastReturnCodesAllowList, signature);
	}
}
