/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
public class GenKeysCCRService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenKeysCCRService.class);

	private final GenKeysCCRAlgorithm genKeysCCRAlgorithm;

	@Value("${nodeID}")
	private int nodeId;

	public GenKeysCCRService(final GenKeysCCRAlgorithm genKeysCCRAlgorithm) {
		this.genKeysCCRAlgorithm = genKeysCCRAlgorithm;
	}

	/**
	 * Invokes the GenKeysCCR algorithm.
	 *
	 * @param encryptionGroup      the encryption group. Must be non-null.
	 * @param electionEventId      the election event id. Must be non-null and a valid UUID.
	 * @param electionEventContext the election event context. Must be non-null.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 * @throws IllegalArgumentException  if the election event context does not correspond to the given election event id.
	 */
	public GenKeysCCROutput genKeysCCR(final GqGroup encryptionGroup, final String electionEventId,
			final ElectionEventContext electionEventContext) {
		validateUUID(electionEventId);
		checkNotNull(encryptionGroup);
		checkNotNull(electionEventContext);
		checkArgument(electionEventContext.electionEventId().equals(electionEventId),
				"The election event context does not correspond to the given election event id.");

		final GenKeysCCRContext genKeysCCRContext = new GenKeysCCRContext(encryptionGroup, nodeId, electionEventId,
				electionEventContext.maximumNumberOfSelections());

		LOGGER.debug("Performing Gen Keys CCR algorithm... [electionEventId: {}, nodeId: {}]", electionEventId, nodeId);

		return genKeysCCRAlgorithm.genKeysCCR(genKeysCCRContext);
	}
}
