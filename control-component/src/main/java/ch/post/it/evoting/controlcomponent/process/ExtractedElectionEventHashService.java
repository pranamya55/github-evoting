/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.ExtractElectionEventAlgorithm;
import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.ExtractElectionEventService;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashExtractedElectionEventAlgorithm;

@Service
public class ExtractedElectionEventHashService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtractedElectionEventHashService.class);

	private final ElectionEventService electionEventService;
	private final ExtractElectionEventService extractElectionEventService;
	private final GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm;
	private final ExtractedElectionEventHashRepository extractedElectionEventHashRepository;

	public ExtractedElectionEventHashService(final ElectionEventService electionEventService,
			final ExtractElectionEventService extractElectionEventService,
			final GetHashExtractedElectionEventAlgorithm getHashExtractedElectionEventAlgorithm,
			final ExtractedElectionEventHashRepository extractedElectionEventHashRepository) {
		this.electionEventService = electionEventService;
		this.extractElectionEventService = extractElectionEventService;
		this.getHashExtractedElectionEventAlgorithm = getHashExtractedElectionEventAlgorithm;
		this.extractedElectionEventHashRepository = extractedElectionEventHashRepository;
	}

	/**
	 * Computes the hash of the extracted election event for the given election event id and saves it in the database.
	 * <p>
	 * Performs the algorithms {@link ExtractElectionEventAlgorithm} and {@link GetHashExtractedElectionEventAlgorithm}.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 */
	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void computeAndSave(final String electionEventId) {
		validateUUID(electionEventId);

		final ExtractedElectionEvent extractedElectionEvent = extractElectionEventService.extractElectionEvent(electionEventId);
		LOGGER.info("Performed the algorithm ExtractElectionEvent. [electionEventId: {}]", electionEventId);

		final String extractedElectionEventHash = getHashExtractedElectionEventAlgorithm.getHashExtractedElectionEvent(extractedElectionEvent);
		LOGGER.info("Performed the algorithm GetHashExtractedElectionEvent. [electionEventId: {}]", electionEventId);

		final ElectionEventEntity electionEventEntity = electionEventService.getElectionEventEntity(electionEventId);
		final ExtractedElectionEventHashEntity extractedElectionEventHashEntity = new ExtractedElectionEventHashEntity(electionEventEntity,
				extractedElectionEventHash);
		extractedElectionEventHashRepository.save(extractedElectionEventHashEntity);
		LOGGER.info("Saved the result of the algorithm GetHashExtractedElectionEvent. [electionEventId: {}]", electionEventId);
	}

	public String getHashExtractedElectionEvent(final String electionEventId) {
		validateUUID(electionEventId);

		return extractedElectionEventHashRepository.findById(electionEventId)
				.map(ExtractedElectionEventHashEntity::getExtractedElectionEventHash)
				.orElseThrow(
						() -> new IllegalStateException(
								String.format("Extracted election event hash entity not found. [electionEventId: %s]", electionEventId)));
	}
}
