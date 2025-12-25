/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;

@Service
public class SetupTallyCCMService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupTallyCCMService.class);

	private final int nodeId;
	private final SetupTallyCCMAlgorithm setupTallyCCMAlgorithm;

	public SetupTallyCCMService(
			@Value("${nodeID}")
			final int nodeId,
			final SetupTallyCCMAlgorithm setupTallyCCMAlgorithm) {
		this.nodeId = nodeId;
		this.setupTallyCCMAlgorithm = setupTallyCCMAlgorithm;
	}

	/**
	 * Invokes the SetupTallyCCM algorithm.
	 *
	 * @param encryptionGroup      the encryption group. Must be non-null.
	 * @param electionEventContext the election event context. Must be non-null.
	 * @throws NullPointerException if any parameter is null.
	 */
	public SetupTallyCCMOutput setupTallyCCM(final GqGroup encryptionGroup, final ElectionEventContext electionEventContext) {
		checkNotNull(encryptionGroup);
		checkNotNull(electionEventContext);

		final String electionEventId = electionEventContext.electionEventId();

		final SetupTallyCCMContext context = new SetupTallyCCMContext(nodeId, electionEventContext);

		LOGGER.debug("Performing Setup Tally CCM algorithm... [electionEventId: {}]", electionEventId);

		return setupTallyCCMAlgorithm.setupTallyCCM(context);
	}
}
