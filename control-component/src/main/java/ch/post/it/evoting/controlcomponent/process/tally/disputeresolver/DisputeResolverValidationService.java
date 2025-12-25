/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.disputeresolver;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkState;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
@Profile("dispute-resolution")
public class DisputeResolverValidationService {

	private final ElectionEventContextService electionEventContextService;

	public DisputeResolverValidationService(final ElectionEventContextService electionEventContextService) {
		this.electionEventContextService = electionEventContextService;
	}

	/**
	 * Validates if the dispute resolution process is allowed for the given election event.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalStateException     if the dispute resolution process is not allowed.
	 */
	public void validate(final String electionEventId) {
		validateUUID(electionEventId);

		final ElectionEventContext electionEventContext = electionEventContextService.getElectionEventContext(electionEventId);
		final LocalDateTime electionEventFinishTime = electionEventContext.finishTime();
		final int maxGracePeriod = electionEventContext.verificationCardSetContexts().stream()
				.map(VerificationCardSetContext::getGracePeriod)
				.max(Integer::compareTo)
				.orElseThrow();
		final boolean isTestBallotBoxes = electionEventContext.verificationCardSetContexts().stream()
				.allMatch(VerificationCardSetContext::isTestBallotBox);
		checkState(isTestBallotBoxes || LocalDateTimeUtils.now().isAfter(electionEventFinishTime.plusSeconds(maxGracePeriod)),
				"The election event has not ended yet. [finishTime: %s, electionEventId: %s]", electionEventFinishTime,
				electionEventContext.electionEventId());
	}
}
