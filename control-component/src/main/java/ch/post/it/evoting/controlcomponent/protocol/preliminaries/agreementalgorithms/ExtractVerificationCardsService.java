/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
public class ExtractVerificationCardsService {

	private final ExtractVerificationCardsAlgorithm extractVerificationCardsAlgorithm;

	public ExtractVerificationCardsService(final ExtractVerificationCardsAlgorithm extractVerificationCardsAlgorithm) {
		this.extractVerificationCardsAlgorithm = extractVerificationCardsAlgorithm;
	}

	/**
	 * Call the ExtractVerificationCards algorithm.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the output of the ExtractVerificationCards algorithm.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 */
	public ImmutableList<ExtractedVerificationCard> extractVerificationCards(final String electionEventId) {
		validateUUID(electionEventId);

		return extractVerificationCardsAlgorithm.extractVerificationCards(electionEventId);
	}

}
