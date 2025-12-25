/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@Service
public class IdentifierValidationService {

	private final VerificationCardService verificationCardService;
	private final VerificationCardSetService verificationCardSetService;

	public IdentifierValidationService(final VerificationCardService verificationCardService,
			final VerificationCardSetService verificationCardSetService) {
		this.verificationCardService = verificationCardService;
		this.verificationCardSetService = verificationCardSetService;
	}

	/**
	 * Validates that the given context ids are stored in the database
	 *
	 * @param contextIds the context ids to be validated. Must be non-null.
	 */
	@Transactional // Required due to the lazy loading of entities.
	public void validateContextIds(final ContextIds contextIds) {
		checkNotNull(contextIds);

		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		// Check for the existence of the electionEventId, verificationCardSetId and verificationCardId
		final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntity(verificationCardId);
		final VerificationCardSetEntity verificationCardSetEntity = verificationCardEntity.getVerificationCardSetEntity();
		final ElectionEventEntity electionEventEntity = verificationCardSetEntity.getElectionEventEntity();

		checkArgument(electionEventEntity.getElectionEventId().equals(electionEventId),
				"Verification card set and election event are not consistent. [verificationCardSetId: %s, electionEventId: %s]",
				verificationCardSetId, electionEventId);
		checkArgument(verificationCardSetEntity.getVerificationCardSetId().equals(verificationCardSetId),
				"Verification card and verification card set are not consistent. [verificationCardId: %s, verificationCardSetId: %s]",
				verificationCardId, verificationCardSetId);
	}

	/**
	 * Validates that the given election event id and verification card set id are stored in the database
	 *
	 * @param electionEventId       the election event id to be validated. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id to be validated. Must be non-null and a valid UUID.
	 */
	@Transactional // Required due to the lazy loading of entities.
	public void validateIds(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		// Check for the existence of the electionEventId and verificationCardSetId
		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSet(verificationCardSetId);
		final ElectionEventEntity electionEventEntity = verificationCardSetEntity.getElectionEventEntity();

		checkArgument(electionEventEntity.getElectionEventId().equals(electionEventId),
				"Verification card set and election event are not consistent. [verificationCardSetId: %s, electionEventId: %s]",
				verificationCardSetId, electionEventId);
	}
}
