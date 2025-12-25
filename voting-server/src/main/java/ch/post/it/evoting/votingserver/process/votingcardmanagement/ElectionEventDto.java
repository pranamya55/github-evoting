/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;

public record ElectionEventDto(String electionEventId,
							   String electionEventAlias,
							   String electionEventDescription,
							   LocalDateTime startTime,
							   LocalDateTime finishTime) {

	public ElectionEventDto {
		validateUUID(electionEventId);
		validateNonBlankUCS(electionEventAlias);
		validateNonBlankUCS(electionEventDescription);
		checkNotNull(startTime);
		checkNotNull(finishTime);
		checkArgument(startTime.isBefore(finishTime) || startTime.equals(finishTime), "The start time must not be after the finish time.");
	}
}
