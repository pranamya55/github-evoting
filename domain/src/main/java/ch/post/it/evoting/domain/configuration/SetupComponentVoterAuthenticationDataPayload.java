/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
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

@JsonPropertyOrder({ "electionEventId", "verificationCardSetId", "setupComponentVoterAuthenticationData", "signature" })
public final class SetupComponentVoterAuthenticationDataPayload implements SignedPayload {

	@JsonProperty
	private final String electionEventId;

	@JsonProperty
	private final String verificationCardSetId;

	@JsonProperty
	private final ImmutableList<SetupComponentVoterAuthenticationData> setupComponentVoterAuthenticationData;

	@JsonProperty
	private CryptoPrimitivesSignature signature;

	@JsonCreator
	public SetupComponentVoterAuthenticationDataPayload(
			@JsonProperty("electionEventId")
			final String electionEventId,
			@JsonProperty("verificationCardSetId")
			final String verificationCardSetId,
			@JsonProperty("setupComponentVoterAuthenticationData")
			final ImmutableList<SetupComponentVoterAuthenticationData> setupComponentVoterAuthenticationData,
			@JsonProperty("signature")
			final CryptoPrimitivesSignature signature) {

		this(electionEventId, verificationCardSetId, setupComponentVoterAuthenticationData);
		this.signature = checkNotNull(signature);
	}

	public SetupComponentVoterAuthenticationDataPayload(final String electionEventId, final String verificationCardSetId,
			final ImmutableList<SetupComponentVoterAuthenticationData> setupComponentVoterAuthenticationData) {

		this.electionEventId = validateUUID(electionEventId);
		this.verificationCardSetId = validateUUID(verificationCardSetId);

		checkNotNull(setupComponentVoterAuthenticationData);
		checkArgument(!setupComponentVoterAuthenticationData.isEmpty(), "The setup component voter authentications must not be empty.");
		checkArgument(setupComponentVoterAuthenticationData.stream()
						.allMatch(s -> s.electionEventId().equals(electionEventId) && s.verificationCardSetId().equals(verificationCardSetId)),
				"All voter authentications must have the same election event id and verification card set id.");
		checkArgument(allEqual(setupComponentVoterAuthenticationData.stream(), SetupComponentVoterAuthenticationData::ballotBoxId),
				"All voter authentication data must have the same ballot box ids.");
		checkArgument(hasNoDuplicates(setupComponentVoterAuthenticationData.stream()
				.map(SetupComponentVoterAuthenticationData::verificationCardId)
				.collect(toImmutableList())), "The list of voter authentication data must not contain any duplicate verification card ids.");
		checkArgument(hasNoDuplicates(setupComponentVoterAuthenticationData.stream()
				.map(SetupComponentVoterAuthenticationData::votingCardId)
				.collect(toImmutableList())), "The list of voter authentication data must not contain any duplicate voting card ids.");
		checkArgument(hasNoDuplicates(setupComponentVoterAuthenticationData.stream()
				.map(SetupComponentVoterAuthenticationData::credentialId)
				.collect(toImmutableList())), "The list of voter authentication data must not contain any duplicate credential ids.");

		this.setupComponentVoterAuthenticationData = setupComponentVoterAuthenticationData;
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public String getVerificationCardSetId() {
		return verificationCardSetId;
	}

	public ImmutableList<SetupComponentVoterAuthenticationData> getSetupComponentVoterAuthenticationData() {
		return setupComponentVoterAuthenticationData;
	}

	@Override
	public CryptoPrimitivesSignature getSignature() {
		return signature;
	}

	@Override
	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = signature;
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(electionEventId),
				HashableString.from(verificationCardSetId),
				HashableList.from(setupComponentVoterAuthenticationData));
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final SetupComponentVoterAuthenticationDataPayload that = (SetupComponentVoterAuthenticationDataPayload) o;
		return electionEventId.equals(that.electionEventId) && verificationCardSetId.equals(that.verificationCardSetId)
				&& setupComponentVoterAuthenticationData.equals(that.setupComponentVoterAuthenticationData) && Objects.equals(signature,
				that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(electionEventId, verificationCardSetId, setupComponentVoterAuthenticationData, signature);
	}
}
