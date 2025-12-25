/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@Service
@ConditionalOnProperty("role.isTally")
public class IdentifierValidationService {

	private final BallotBoxService ballotBoxService;
	private final ElectionEventService electionEventService;

	public IdentifierValidationService(
			final BallotBoxService ballotBoxService,
			final ElectionEventService electionEventService) {
		this.electionEventService = electionEventService;
		this.ballotBoxService = ballotBoxService;
	}

	/**
	 * Validates that the given election event ID exists and that the given ballot box ID exists for the given election event ID.
	 *
	 * @param electionEventId the identifier of the election event. Must be a valid UUID.
	 * @param ballotBoxId     the identifier of the ballot box. Must be a valid UUID.
	 * @throws NullPointerException      if any of the IDs is null.
	 * @throws FailedValidationException if any of the IDs is not a valid UUID.
	 * @throws IllegalArgumentException  if
	 *                                   <ul>
	 *                                       <li>the election event ID does not exist in the data base</li>
	 *                                       <li>the ballot box ID does not exist in the database for the given election event ID</li>
	 *                                   </ul>
	 */
	public void validateBallotBoxRelatedIds(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkArgument(electionEventService.exists(electionEventId), "The given election event ID does not exist. [electionEventId: %s]",
				electionEventId);
		checkArgument(ballotBoxService.getBallotBoxIds(electionEventId).contains(ballotBoxId),
				"The given ballot box ID does not belong to the given election event ID. [ballotBoxId: %s, electionEventId: %s]", ballotBoxId,
				electionEventId);
	}

}
