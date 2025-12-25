/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.process.HashedLVCCSharesService;
import ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote.VerifyLVCCHashService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@Service
public class LongVoteCastReturnCodesShareVerifyService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LongVoteCastReturnCodesShareVerifyService.class);

	private final VerifyLVCCHashService verifyLVCCHashService;
	private final HashedLVCCSharesService hashedLVCCSharesService;
	private final ElectionEventStateService electionEventStateService;

	@Value("${nodeID}")
	private int nodeId;

	LongVoteCastReturnCodesShareVerifyService(
			final VerifyLVCCHashService verifyLVCCHashService,
			final HashedLVCCSharesService hashedLVCCSharesService,
			final ElectionEventStateService electionEventStateService) {
		this.verifyLVCCHashService = verifyLVCCHashService;
		this.hashedLVCCSharesService = hashedLVCCSharesService;
		this.electionEventStateService = electionEventStateService;
	}

	@Transactional
	public boolean performVerifyLVCCHashService(final GqGroup encryptionGroup,
			final ImmutableList<ControlComponenthlVCCSharePayload> controlComponenthlVCCPayloads) {
		checkNotNull(encryptionGroup);
		validate(controlComponenthlVCCPayloads);
		checkArgument(controlComponenthlVCCPayloads.get(0).getEncryptionGroup().equals(encryptionGroup));

		final ContextIds contextIds = controlComponenthlVCCPayloads.get(0)
				.getConfirmationKey()
				.contextIds();
		final String electionEventId = contextIds.electionEventId();

		// Validate election event state.
		final ElectionEventState expectedState = ElectionEventState.CONFIGURED;
		final ElectionEventState electionEventState = electionEventStateService.getElectionEventState(electionEventId);
		checkState(expectedState.equals(electionEventState),
				"The election event is not in the expected state. [electionEventId: %s, nodeId: %s, expected: %s, actual: %s]", electionEventId,
				nodeId, expectedState, electionEventState);

		final boolean isVerified = verifyLVCCHashService.verifyLVCCHash(encryptionGroup, controlComponenthlVCCPayloads);
		LOGGER.info("VerifyLVCCHash algorithm successfully performed. [electionEventId: {}]", electionEventId);

		final String verificationCardId = contextIds.verificationCardId();
		hashedLVCCSharesService.save(verificationCardId, controlComponenthlVCCPayloads, isVerified);
		LOGGER.debug("Saved the hashed Long Vote Cast Return Codes share from all nodes. [contextIds: {}]", contextIds);

		return isVerified;
	}
}
