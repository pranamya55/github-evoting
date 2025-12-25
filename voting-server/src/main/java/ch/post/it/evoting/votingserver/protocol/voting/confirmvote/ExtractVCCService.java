/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponentlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.LongVoteCastReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTableSupplier;

@Service
public class ExtractVCCService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtractVCCService.class);

	private final ExtractVCCAlgorithm extractVCCAlgorithm;
	private final ElectionEventService electionEventService;
	private final IdentifierValidationService identifierValidationService;
	private final ReturnCodesMappingTableSupplier returnCodesMappingTableSupplier;

	public ExtractVCCService(
			final ExtractVCCAlgorithm extractVCCAlgorithm,
			final ElectionEventService electionEventService,
			final IdentifierValidationService identifierValidationService,
			final ReturnCodesMappingTableSupplier returnCodesMappingTableSupplier) {
		this.extractVCCAlgorithm = extractVCCAlgorithm;
		this.electionEventService = electionEventService;
		this.identifierValidationService = identifierValidationService;
		this.returnCodesMappingTableSupplier = returnCodesMappingTableSupplier;
	}

	/**
	 * Invokes the ExtractVCC algorithm.
	 *
	 * @param contextIds                        the context ids. Must be non-null.
	 * @param controlComponentlVCCSharePayloads the list of Control Component lVCC Share payloads. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                   <li>there is the wrong number of Control Component lVCC Share payloads.</li>
	 *                                   <li>the context ids are invalid.</li>
	 *                                  </ul>
	 */
	public ExtractVCCOutput extractVCC(final ContextIds contextIds,
			final ImmutableList<ControlComponentlVCCSharePayload> controlComponentlVCCSharePayloads) {
		checkNotNull(contextIds);
		validate(controlComponentlVCCSharePayloads);

		identifierValidationService.validateContextIds(contextIds);
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		final ExtractVCCContext extractVCCContext = new ExtractVCCContext(encryptionGroup, electionEventId, verificationCardId);

		// Retrieve short codes by combining CCR shares and looking up the CMTable.
		final GroupVector<GqElement, GqGroup> lVCCShares = controlComponentlVCCSharePayloads.stream()
				.map(ControlComponentlVCCSharePayload::getLongVoteCastReturnCodeShare)
				.map(o -> o.orElseThrow(() -> new IllegalStateException("We should not reach this state.")))
				.map(LongVoteCastReturnCodeShare::longVoteCastReturnCodeShare)
				.collect(GroupVector.toGroupVector());
		final ReturnCodesMappingTable returnCodesMappingTable = returnCodesMappingTableSupplier.get(verificationCardSetId);
		final ExtractVCCInput extractVCCInput = new ExtractVCCInput(lVCCShares, returnCodesMappingTable);

		LOGGER.debug("Performing ExtractVCC algorithm... [contextIds: {}]", contextIds);

		return extractVCCAlgorithm.extractVCC(extractVCCContext, extractVCCInput);
	}
}
