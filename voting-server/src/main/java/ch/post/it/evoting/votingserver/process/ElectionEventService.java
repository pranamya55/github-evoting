/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
public class ElectionEventService {

	private final ElectionEventRepository electionEventRepository;

	public ElectionEventService(final ElectionEventRepository electionEventRepository) {
		this.electionEventRepository = electionEventRepository;
	}

	/**
	 * Saves the given election event.
	 *
	 * @param electionEventId the election event identifier. Must be non-null.
	 * @param encryptionGroup the G<sub>q</sub> group. Must be non-null.
	 * @return the election event encryption parameters entity that was saved.
	 * @throws NullPointerException      if any input parameter is null.
	 * @throws FailedValidationException if the {@code electionEventId} is invalid.
	 */
	public ElectionEventEntity save(final String electionEventId, final GqGroup encryptionGroup) {
		validateUUID(electionEventId);
		checkNotNull(encryptionGroup);

		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, encryptionGroup);
		return electionEventRepository.save(electionEventEntity);
	}

	/**
	 * Retrieves the election event entity for a given election event id.
	 *
	 * @param electionEventId the election event id.
	 * @return the election event entity.
	 * @throws NullPointerException      if the {@code electionEventId} is null.
	 * @throws FailedValidationException if the {@code electionEventId} is invalid.
	 * @throws IllegalStateException     if no election event is found for the given election event id.
	 */
	public ElectionEventEntity retrieveElectionEventEntity(final String electionEventId) {
		validateUUID(electionEventId);

		return electionEventRepository.findById(electionEventId)
				.orElseThrow(() -> new IllegalStateException(String.format("Election event not found. [electionEventId: %s]", electionEventId)));
	}

	/**
	 * @param electionEventId the election event id of the election event entity to look for. Must be non-null and a valid UUID.
	 * @return true of the corresponding election event entity exists, false otherwise.
	 */
	public boolean exists(final String electionEventId) {
		validateUUID(electionEventId);

		return electionEventRepository.existsById(electionEventId);
	}

	/**
	 * Gets the encryption group for the given {@code electionEventId}. The result of this method is cached.
	 *
	 * @param electionEventId the election event id for which to get the encryption group.
	 * @return the encryption group corresponding to {@code electionEventId}.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 * @throws IllegalStateException     if the encryption group is not found for this {@code electionEventId}.
	 */
	@Cacheable("gqGroups")
	public GqGroup getEncryptionGroup(final String electionEventId) {
		validateUUID(electionEventId);

		final Optional<ElectionEventEntity> electionEventEntity = electionEventRepository.findById(electionEventId);

		return electionEventEntity
				.orElseThrow(() -> new IllegalStateException(String.format("Election event not found. [electionEventId: %s]", electionEventId)))
				.getEncryptionGroup();
	}

}
