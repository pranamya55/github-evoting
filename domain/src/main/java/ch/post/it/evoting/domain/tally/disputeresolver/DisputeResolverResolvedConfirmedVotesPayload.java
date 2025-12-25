/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;

@JsonDeserialize(using = DisputeResolverResolvedConfirmedVotesPayloadDeserializer.class)
public class DisputeResolverResolvedConfirmedVotesPayload implements SignedPayload {

	private final String electionEventId;
	private final ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes;

	private CryptoPrimitivesSignature signature;

	@JsonCreator
	public DisputeResolverResolvedConfirmedVotesPayload(
			@JsonProperty("electionEventId")
			final String electionEventId,
			@JsonProperty("resolvedConfirmedVotes")
			final ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes,
			@JsonProperty("signature")
			final CryptoPrimitivesSignature signature) {

		this(electionEventId, resolvedConfirmedVotes);
		this.signature = checkNotNull(signature);
	}

	public DisputeResolverResolvedConfirmedVotesPayload(final String electionEventId,
			final ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes) {
		this.electionEventId = validateUUID(electionEventId);
		this.resolvedConfirmedVotes = checkNotNull(resolvedConfirmedVotes).stream()
				.sorted(Comparator.comparing(ResolvedConfirmedVote::verificationCardId))
				.collect(toImmutableList());

		checkArgument(hasNoDuplicates(resolvedConfirmedVotes.stream().map(ResolvedConfirmedVote::verificationCardId).collect(toImmutableList())),
				"The verification card ids must be unique.");
	}

	public String getElectionEventId() {
		return electionEventId;
	}

	public ImmutableList<ResolvedConfirmedVote> getResolvedConfirmedVotes() {
		return resolvedConfirmedVotes;
	}

	@Override
	public CryptoPrimitivesSignature getSignature() {
		return this.signature;
	}

	@Override
	public void setSignature(final CryptoPrimitivesSignature signature) {
		this.signature = checkNotNull(signature);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(electionEventId),
				HashableList.from(resolvedConfirmedVotes)
		);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final DisputeResolverResolvedConfirmedVotesPayload that = (DisputeResolverResolvedConfirmedVotesPayload) o;
		return Objects.equals(electionEventId, that.electionEventId) && Objects.equals(resolvedConfirmedVotes,
				that.resolvedConfirmedVotes) && Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		return Objects.hash(electionEventId, resolvedConfirmedVotes, signature);
	}
}
