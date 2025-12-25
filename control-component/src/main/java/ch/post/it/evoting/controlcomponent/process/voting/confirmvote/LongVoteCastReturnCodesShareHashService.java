/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote.CreateLVCCShareOutput;
import ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote.CreateLVCCShareService;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@Service
public class LongVoteCastReturnCodesShareHashService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LongVoteCastReturnCodesShareHashService.class);

	private final ElectionEventService electionEventService;
	private final CreateLVCCShareService createLVCCShareService;
	private final ElectionEventStateService electionEventStateService;
	private final LVCCShareService lvccShareService;

	@Value("${nodeID}")
	private int nodeId;

	LongVoteCastReturnCodesShareHashService(
			final ElectionEventService electionEventService,
			final CreateLVCCShareService createLVCCShareService,
			final ElectionEventStateService electionEventStateService,
			final LVCCShareService lvccShareService) {
		this.electionEventService = electionEventService;
		this.createLVCCShareService = createLVCCShareService;
		this.electionEventStateService = electionEventStateService;
		this.lvccShareService = lvccShareService;
	}

	@Transactional
	public CreateLVCCShareOutput performCreateLVCCShare(final ConfirmationKey confirmationKey) {
		checkNotNull(confirmationKey);

		final ContextIds contextIds = confirmationKey.contextIds();
		final String electionEventId = contextIds.electionEventId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		// Validate election event state.
		final ElectionEventState expectedState = ElectionEventState.CONFIGURED;
		final ElectionEventState electionEventState = electionEventStateService.getElectionEventState(contextIds.electionEventId());
		checkState(expectedState.equals(electionEventState),
				"The election event is not in the expected state. [electionEventId: %s, nodeId: %s, expected: %s, actual: %s]", electionEventId,
				nodeId, expectedState, electionEventState);

		final CreateLVCCShareOutput createLVCCShareOutput = createLVCCShareService.createLVCCShare(encryptionGroup, confirmationKey);
		LOGGER.info(
				"CreateLVCCShare algorithm successfully performed. Successfully generated the Long Vote Cast Return Codes Share. [contextIds: {}]",
				contextIds);

		lvccShareService.save(confirmationKey, createLVCCShareOutput);
		LOGGER.debug("Saved hashed Long Vote Cast Return Codes share. [contextIds: {}]", contextIds);

		return createLVCCShareOutput;
	}
}
