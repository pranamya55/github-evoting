/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.votingserver.process.voting.CredentialIdNotFoundException;

@Service
public class IdentifierValidationService {

	private final VerificationCardService verificationCardService;

	public IdentifierValidationService(final VerificationCardService verificationCardService) {
		this.verificationCardService = verificationCardService;
	}

	/**
	 * Validates that the given {@code credentialId} corresponds to an existing election event identifier by {@code electionEventId}.
	 *
	 * @param electionEventId the election event id.
	 * @param credentialId    the credential id.
	 */
	public void validateCredentialId(final String electionEventId, final String credentialId) {
		validateUUID(electionEventId);
		validateUUID(credentialId);

		// If the verification card is not found, it means the start voting key (from which the credentialId is derived) given by the voter is wrong.
		final VerificationCardEntity verificationCardEntity;
		try {
			verificationCardEntity = verificationCardService.getVerificationCardEntityByCredentialId(credentialId);
		} catch (final IllegalStateException e) {
			throw new CredentialIdNotFoundException(
					String.format("No verification card found for given credentialId. [electionEventId: %s, credentialId: %s]", electionEventId,
							credentialId));
		}

		final ElectionEventEntity electionEventEntity = verificationCardEntity.getVerificationCardSetEntity().getElectionEventEntity();

		checkArgument(electionEventEntity.getElectionEventId().equals(electionEventId),
				"Election event and credential id are not consistent. [electionEventId: %s, credentialId: %s", electionEventId, credentialId);
	}

	/**
	 * Validates that the given context ids are stored in the database and that the credential id corresponds to te verification card.
	 *
	 * @param contextIds   the context ids to be validated. Must be non-null.
	 * @param credentialId the credential id to validate against the verification card. Must be a valid uuid.
	 */
	public void validateContextIdsAndCredentialId(final ContextIds contextIds, final String credentialId) {
		validateUUID(credentialId);
		validateContextIds(contextIds);

		final String verificationCardId = contextIds.verificationCardId();
		final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntity(verificationCardId);

		checkArgument(verificationCardEntity.getCredentialId().equals(credentialId),
				"Verification card id and credential id are not consistent. [verificationCardId: %s, credentialId: %s]", verificationCardId,
				credentialId);
	}

	/**
	 * Validates that the given context ids are stored in the database
	 *
	 * @param contextIds the context ids to be validated. Must be non-null.
	 */
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
}
