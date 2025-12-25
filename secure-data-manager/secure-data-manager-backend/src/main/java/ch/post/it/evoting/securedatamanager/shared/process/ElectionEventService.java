/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

/**
 * This is a service for handling election event entities.
 */
@Service
public class ElectionEventService {

	private final ElectionEventRepository electionEventRepository;

	public ElectionEventService(final ElectionEventRepository electionEventRepository) {
		this.electionEventRepository = electionEventRepository;
	}

	public boolean exists(final String electionEventId) {
		validateUUID(electionEventId);

		return electionEventRepository.existsById(electionEventId);
	}

	@Cacheable(value = "electionEvent", unless="#result == null")
	public String findElectionEventId() {

		final ImmutableList<ElectionEventEntity> electionEventEntities = ImmutableList.from(electionEventRepository.findAll());

		if (electionEventEntities.isEmpty()) {
			return null;
		}
		if (electionEventEntities.size() > 1) {
			throw new IllegalStateException("More than one election event is available in the database.");
		}

		final String electionEventId = electionEventEntities.get(0).getElectionEventId();
		return validateUUID(electionEventId);
	}

}
