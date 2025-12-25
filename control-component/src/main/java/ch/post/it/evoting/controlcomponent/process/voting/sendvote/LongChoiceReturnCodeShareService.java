/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.CombinedPartiallyDecryptedPCCService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.IdentifierValidationService;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.CreateLCCShareOutput;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.CreateLCCShareService;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.DecryptPCCService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.LongChoiceReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;

/**
 * Decrypts the partially decrypted encrypted Choice Return Codes and creates the CCR_j long Choice Return Code shares.
 */
@Service
public class LongChoiceReturnCodeShareService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LongChoiceReturnCodeShareService.class);

	private final DecryptPCCService decryptPCCService;
	private final CreateLCCShareService createLCCShareService;
	private final ElectionEventService electionEventService;
	private final IdentifierValidationService identifierValidationService;
	private final BallotBoxService ballotBoxService;
	private final LCCShareService lccShareService;
	private final CombinedPartiallyDecryptedPCCService combinedPartiallyDecryptedPCCService;

	@Value("${nodeID}")
	private int nodeId;

	public LongChoiceReturnCodeShareService(
			final DecryptPCCService decryptPCCService,
			final CreateLCCShareService createLCCShareService,
			final ElectionEventService electionEventService,
			final IdentifierValidationService identifierValidationService,
			final BallotBoxService ballotBoxService,
			final LCCShareService lccShareService,
			final CombinedPartiallyDecryptedPCCService combinedPartiallyDecryptedPCCService) {
		this.decryptPCCService = decryptPCCService;
		this.createLCCShareService = createLCCShareService;
		this.electionEventService = electionEventService;
		this.identifierValidationService = identifierValidationService;
		this.ballotBoxService = ballotBoxService;
		this.lccShareService = lccShareService;
		this.combinedPartiallyDecryptedPCCService = combinedPartiallyDecryptedPCCService;
	}

	/**
	 * Decrypts the partially decrypted encrypted Choice Return Codes with the DecryptPCC algorithm and computes the CCR_j long Choice Return Code
	 * shares with the CreateLCCShare algorithm.
	 *
	 * @param controlComponentPartialDecryptPayloads the partially decrypted encrypted node contributions.
	 * @return the Long Choice Return Code shares.
	 */
	@Transactional
	public LongChoiceReturnCodeShare performCreateLCCShare(
			final ImmutableList<ControlComponentPartialDecryptPayload> controlComponentPartialDecryptPayloads) {

		checkNotNull(controlComponentPartialDecryptPayloads);
		final ContextIds contextIds = controlComponentPartialDecryptPayloads.get(0).getPartiallyDecryptedEncryptedPCC().contextIds();

		identifierValidationService.validateContextIds(contextIds);
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		checkArgument(controlComponentPartialDecryptPayloads.get(0).getEncryptionGroup().equals(encryptionGroup),
				"The control component partial decrypt payloads do not have the expected encryption group.");

		LOGGER.debug("Starting decryption of the partially decrypted encrypted Choice Return Codes. [contextIds: {}]", contextIds);

		final PrimesMappingTable primesMappingTable = ballotBoxService.getPrimesMappingTableByVerificationCardSetId(verificationCardSetId);

		final GroupVector<GqElement, GqGroup> decryptedPartialChoiceReturnCodes = decryptPCCService.decryptPCC(encryptionGroup, primesMappingTable,
				controlComponentPartialDecryptPayloads);
		LOGGER.info("DecryptPCC algorithm successfully performed. [electionEventId: {}]", electionEventId);

		final CreateLCCShareOutput createLCCShareOutput = createLCCShareService.createLCCShare(encryptionGroup, contextIds, primesMappingTable,
				decryptedPartialChoiceReturnCodes);
		LOGGER.info("CreateLCCShare algorithm successfully performed. [electionEventId: {}]", electionEventId);

		lccShareService.save(contextIds, createLCCShareOutput);
		LOGGER.debug("Long Choice Return Code share saved. [contextIds: {}]", contextIds);

		combinedPartiallyDecryptedPCCService.save(verificationCardId, controlComponentPartialDecryptPayloads);
		LOGGER.debug("Combined partially decrypted PCCs saved. [contextIds: {}]", contextIds);

		return new LongChoiceReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, nodeId,
				createLCCShareOutput.longChoiceReturnCodeShare());
	}
}
