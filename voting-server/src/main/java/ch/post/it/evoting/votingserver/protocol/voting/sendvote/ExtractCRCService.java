/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentPayloadListValidation.validate;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentlCCSharePayload;
import ch.post.it.evoting.domain.voting.sendvote.LongChoiceReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTableSupplier;

@Service
public class ExtractCRCService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtractCRCService.class);

	private final ExtractCRCAlgorithm extractCRCAlgorithm;
	private final ElectionEventService electionEventService;
	private final VerificationCardService verificationCardService;
	private final IdentifierValidationService identifierValidationService;
	private final PrimesMappingTableAlgorithms primesMappingTableAlgorithms;
	private final ReturnCodesMappingTableSupplier returnCodesMappingTableSupplier;

	ExtractCRCService(
			final ExtractCRCAlgorithm extractCRCAlgorithm,
			final ElectionEventService electionEventService,
			final VerificationCardService verificationCardService,
			final IdentifierValidationService identifierValidationService,
			final PrimesMappingTableAlgorithms primesMappingTableAlgorithms,
			final ReturnCodesMappingTableSupplier returnCodesMappingTableSupplier) {
		this.extractCRCAlgorithm = extractCRCAlgorithm;
		this.electionEventService = electionEventService;
		this.verificationCardService = verificationCardService;
		this.identifierValidationService = identifierValidationService;
		this.primesMappingTableAlgorithms = primesMappingTableAlgorithms;
		this.returnCodesMappingTableSupplier = returnCodesMappingTableSupplier;
	}

	/**
	 * Invokes the ExtractCRC algorithm.
	 *
	 * @param contextIds                       the context ids. Must be non-null.
	 * @param controlComponentLCCSharePayloads the list of Control Component LCC Share payloads. Must be non-null.
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if
	 *                                  <ul>
	 *                                   <li>there is the wrong number of Control Component LCC Share payloads.</li>
	 *                                   <li>the context ids are invalid.</li>
	 *                                  </ul>
	 */
	public ExtractCRCOutput extractCRC(final ContextIds contextIds,
			final ImmutableList<ControlComponentlCCSharePayload> controlComponentLCCSharePayloads) {
		checkNotNull(contextIds);
		validate(controlComponentLCCSharePayloads);

		identifierValidationService.validateContextIds(contextIds);
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		// Retrieve short Choice Return Codes by combining CCR shares and looking up the Return Codes Mapping Table.
		final PrimesMappingTable primesMappingTable = verificationCardService.getPrimesMappingTable(verificationCardId);
		final ImmutableList<String> blankCorrectnessInformation = primesMappingTableAlgorithms.getBlankCorrectnessInformation(primesMappingTable);

		final ImmutableList<GroupVector<GqElement, GqGroup>> lCCShares = controlComponentLCCSharePayloads.stream()
				.map(ControlComponentlCCSharePayload::getLongChoiceReturnCodeShare)
				.map(LongChoiceReturnCodeShare::longChoiceReturnCodeShare)
				.collect(toImmutableList());

		final ReturnCodesMappingTable returnCodesMappingTable = returnCodesMappingTableSupplier.get(verificationCardSetId);

		final ExtractCRCContext extractCRCContext = new ExtractCRCContext(encryptionGroup, electionEventId, verificationCardId,
				blankCorrectnessInformation);
		final ExtractCRCInput extractCRCInput = new ExtractCRCInput(lCCShares, returnCodesMappingTable);

		LOGGER.debug("Performing ExtractCRC algorithm... [contextIds: {}]", contextIds);

		return extractCRCAlgorithm.extractCRC(extractCRCContext, extractCRCInput);
	}
}
