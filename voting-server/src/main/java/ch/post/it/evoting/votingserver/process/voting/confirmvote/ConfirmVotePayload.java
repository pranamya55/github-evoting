/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.confirmvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;

/**
 * Confirm vote payload sent by the voting-client.
 *
 * @param contextIds              the context ids. Must be non-null.
 * @param encryptionGroup         the encryption group use to deserialize {@code confirmationKey}. Must be non-null.
 * @param confirmationKey         the confirmation key as a {@link GqElement}. Must be non-null.
 * @param authenticationChallenge the authentication challenge. Must be non-null.
 */
@JsonDeserialize(using = ConfirmVotePayloadDeserializer.class)
public record ConfirmVotePayload(ContextIds contextIds,
								 GqGroup encryptionGroup,
								 GqElement confirmationKey,
								 AuthenticationChallenge authenticationChallenge) implements HashableList {

	/**
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if {@code encryptionGroup} does not match the group of {@code confirmationKey}.
	 */
	public ConfirmVotePayload {
		checkNotNull(contextIds);
		checkNotNull(encryptionGroup);
		checkNotNull(confirmationKey);
		checkNotNull(authenticationChallenge);

		checkArgument(encryptionGroup.equals(confirmationKey.getGroup()), "The encryption group must match the confirmation key's group.");
	}

	/**
	 * Intentionally ignore the {@code authenticationChallenge} in order to allow the idempotency service to ignore the nonce part which changes at
	 * each new request (outside of time window).
	 */
	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				contextIds,
				encryptionGroup,
				confirmationKey
		);
	}
}
