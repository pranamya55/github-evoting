/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Service to manage the election event.
 */
@Service
public class ElectionEventService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventService.class);

	private final ElectionEventRepository electionEventRepository;
	private final ElectionEventStateRepository electionEventStateRepository;

	public ElectionEventService(
			final ElectionEventRepository electionEventRepository,
			final ElectionEventStateRepository electionEventStateRepository) {
		this.electionEventRepository = electionEventRepository;
		this.electionEventStateRepository = electionEventStateRepository;
	}

	/**
	 * Saves the election event and its associated initial state ({@link ElectionEventState#INITIAL}).
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @param encryptionGroup the encryption group of the election event. Must be non-null.
	 * @return the newly saved election event entity.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 */
	@Transactional
	public ElectionEventEntity save(final String electionEventId, final GqGroup encryptionGroup) {
		validateUUID(electionEventId);
		checkNotNull(encryptionGroup);

		final ElectionEventEntity entityToSave = new ElectionEventEntity(electionEventId, encryptionGroup);
		electionEventRepository.save(entityToSave);
		LOGGER.debug("Saved election event. [electionEventId: {}]", electionEventId);

		final ElectionEventStateEntity electionEventStateEntity = new ElectionEventStateEntity(entityToSave);
		electionEventStateRepository.save(electionEventStateEntity);
		LOGGER.debug("Saved election event initial state. [electionEventId: {}]", electionEventId);

		return entityToSave;
	}

	/**
	 * Checks the existence of the election event for the given {@code electionEventId}.
	 *
	 * @param electionEventId the election event id to check. Must be non-null and a valid UUID.
	 * @return {@code true} if the election event exists, {@code false} otherwise.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 */
	public boolean exists(final String electionEventId) {
		validateUUID(electionEventId);

		final boolean exists = electionEventRepository.existsById(electionEventId);
		LOGGER.debug("Checked election event existence. [electionEventId: {}]", electionEventId);

		return exists;
	}

	/**
	 * Gets the election event entity for the given {@code electionEventId}.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the election event entity.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 */
	public ElectionEventEntity getElectionEventEntity(final String electionEventId) {
		validateUUID(electionEventId);

		final ElectionEventEntity electionEventEntity = electionEventRepository.findById(electionEventId)
				.orElseThrow(() -> new IllegalStateException(String.format("Election event not found. [electionEventId: %s]", electionEventId)));
		LOGGER.debug("Retrieved election event entity. [electionEventId: {}]", electionEventId);

		return electionEventEntity;
	}

	/**
	 * Gets the encryption group of the election event for the given {@code electionEventId}. The result of this method is cached.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the encryption group of the election event.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 */
	@Cacheable("gqGroups")
	public GqGroup getEncryptionGroup(final String electionEventId) {
		validateUUID(electionEventId);

		return getElectionEventEntity(electionEventId).getEncryptionGroup();
	}

}
