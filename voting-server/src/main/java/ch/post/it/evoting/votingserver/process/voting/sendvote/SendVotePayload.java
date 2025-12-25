/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.sendvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;

/**
 * Send vote request payload sent by the voting-client.
 *
 * @param contextIds              the context ids. Must be non-null.
 * @param encryptionGroup         the encryption group used to deserialize the {@code encryptedVerifiableVote}. Must be non-null.
 * @param encryptedVerifiableVote the encrypted verifiable vote. Must be non-null.
 * @param authenticationChallenge the authentication challenge. Must be non-null.
 */
@JsonDeserialize(using = SendVotePayloadDeserializer.class)
public record SendVotePayload(ContextIds contextIds,
					   GqGroup encryptionGroup,
					   EncryptedVerifiableVote encryptedVerifiableVote,
					   AuthenticationChallenge authenticationChallenge) {

	/**
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if {@code encryptionGroup} does not match the group of {@code encryptedVerifiableVote}.
	 */
	public SendVotePayload {
		checkNotNull(contextIds);
		checkNotNull(encryptionGroup);
		checkNotNull(encryptedVerifiableVote);
		checkNotNull(authenticationChallenge);

		checkArgument(encryptionGroup.equals(encryptedVerifiableVote.encryptedVote().getGroup()),
				"The encryption group must match the encrypted verifiable vote's group.");
	}

}
