/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;

import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;

public record VotingCardDto(String electionEventId,
							String verificationCardSetId,
							String verificationCardId,
							String votingCardId,
							VerificationCardState verificationCardState,
							LocalDateTime votingCardStateDate) {

	public VotingCardDto {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		validateUUID(verificationCardId);
		validateUUID(votingCardId);
		checkNotNull(verificationCardState);
		checkNotNull(votingCardStateDate);
	}

}
