/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.LVCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.LongVoteCastReturnCodesAllowList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@Service
public class VerifyLVCCHashService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyLVCCHashService.class);

	private final VerifyLVCCHashAlgorithm verifyLVCCHashAlgorithm;
	private final LVCCAllowListEntryService lvccAllowListEntryService;

	@Value("${nodeID}")
	private int nodeId;

	public VerifyLVCCHashService(final VerifyLVCCHashAlgorithm verifyLVCCHashAlgorithm,
			final LVCCAllowListEntryService lvccAllowListEntryService) {
		this.verifyLVCCHashAlgorithm = verifyLVCCHashAlgorithm;
		this.lvccAllowListEntryService = lvccAllowListEntryService;
	}

	/**
	 * Invokes the VerifyLVCCHash algorithm.
	 *
	 * @param encryptionGroup               the encryption group. Must be non-null.
	 * @param controlComponenthlVCCPayloads the control component hlVCC payloads. Must be non-null.
	 * @throws NullPointerException if any parameter is null.
	 */
	public boolean verifyLVCCHash(final GqGroup encryptionGroup,
			final ImmutableList<ControlComponenthlVCCSharePayload> controlComponenthlVCCPayloads) {
		checkNotNull(encryptionGroup);
		validate(controlComponenthlVCCPayloads);
		final Map<Boolean, ImmutableList<ControlComponenthlVCCSharePayload>> controlComponenthlVCCPayloadsMap = controlComponenthlVCCPayloads.stream()
				.collect(Collectors.partitioningBy(payload -> payload.getNodeId() == nodeId, toImmutableList()));

		final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload = controlComponenthlVCCPayloadsMap.get(true).get(0);
		final ContextIds contextIds = controlComponenthlVCCSharePayload.getConfirmationKey().contextIds();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardId = contextIds.verificationCardId();
		final String verificationCardSetId = contextIds.verificationCardSetId();

		final LongVoteCastReturnCodesAllowList longVoteCastReturnCodesAllowList = longVoteCastReturnCode ->
				lvccAllowListEntryService.exists(verificationCardSetId, longVoteCastReturnCode);

		final String hashedLongVoteCastReturnCode = controlComponenthlVCCSharePayload.getHashLongVoteCastReturnCodeShare();

		final ImmutableList<String> otherCCRsHashedLongVoteCastReturnCodes = controlComponenthlVCCPayloadsMap.get(false).stream()
				.sorted(Comparator.comparingInt(ControlComponenthlVCCSharePayload::getNodeId))
				.map(ControlComponenthlVCCSharePayload::getHashLongVoteCastReturnCodeShare)
				.collect(toImmutableList());

		LOGGER.debug("This node's hashed LVCC [nodeId: {}, hashedLongVoteCastReturnCode: {}].", nodeId, hashedLongVoteCastReturnCode);
		LOGGER.debug("Other nodes's hashed LVCC [otherCCRsHashedLongVoteCastReturnCodes: {}]", otherCCRsHashedLongVoteCastReturnCodes);

		final LVCCHashContext lvccHashContext = new LVCCHashContext(encryptionGroup, nodeId, electionEventId, verificationCardSetId,
				verificationCardId);

		final VerifyLVCCHashInput verifyLVCCHashInput = new VerifyLVCCHashInput.Builder()
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList)
				.setCcrjHashedLongVoteCastReturnCode(hashedLongVoteCastReturnCode)
				.setOtherCCRsHashedLongVoteCastReturnCodes(otherCCRsHashedLongVoteCastReturnCodes)
				.build();

		LOGGER.debug("Performing VerifyLVCCHash algorithm... [contextIds: {}, nodeId: {}]", contextIds, nodeId);

		return verifyLVCCHashAlgorithm.verifyLVCCHash(lvccHashContext, verifyLVCCHashInput);
	}
}
