/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.generatecckeys;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponent.process.CcmjElectionKeysService;
import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally.SetupTallyCCMOutput;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally.SetupTallyCCMService;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenKeysCCROutput;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenKeysCCRService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;

@Service
class GenerateCcKeysService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateCcKeysService.class);

	private final GenKeysCCRService genKeysCCRService;
	private final SetupTallyCCMService setupTallyCCMService;
	private final CcmjElectionKeysService ccmjElectionKeysService;
	private final CcrjReturnCodesKeysService ccrjReturnCodesKeysService;

	@Value("${nodeID}")
	private int nodeId;

	GenerateCcKeysService(
			final GenKeysCCRService genKeysCCRService,
			final SetupTallyCCMService setupTallyCCMService,
			final CcmjElectionKeysService ccmjElectionKeysService,
			final CcrjReturnCodesKeysService ccrjReturnCodesKeysService) {
		this.genKeysCCRService = genKeysCCRService;
		this.setupTallyCCMService = setupTallyCCMService;
		this.ccmjElectionKeysService = ccmjElectionKeysService;
		this.ccrjReturnCodesKeysService = ccrjReturnCodesKeysService;
	}

	@Transactional
	public ControlComponentPublicKeys performGenKeysCCR(final GqGroup encryptionGroup, final String electionEventId,
			final ElectionEventContext electionEventContext) {
		validateUUID(electionEventId);
		checkNotNull(encryptionGroup);
		checkNotNull(electionEventContext);

		// Generate ccrj keys and save them.
		final GenKeysCCROutput genKeysCCROutput = genKeysCCRService.genKeysCCR(encryptionGroup, electionEventId, electionEventContext);
		LOGGER.info("Gen Keys CCR algorithm successfully performed. [electionEventId: {}]", electionEventId);
		ccrjReturnCodesKeysService.save(electionEventId, genKeysCCROutput);
		LOGGER.info("Gen Keys CCR algorithm output successfully saved. [electionEventId: {}]", electionEventId);

		// Generate ccm election key pair and save it.
		final SetupTallyCCMOutput setupTallyCCMOutput = setupTallyCCMService.setupTallyCCM(encryptionGroup, electionEventContext);
		LOGGER.info("Setup Tally CCM algorithm successfully performed. [electionEventId: {}]", electionEventId);
		ccmjElectionKeysService.save(electionEventId, setupTallyCCMOutput);
		LOGGER.info("Setup Tally CCM algorithm output successfully saved. [electionEventId: {}]", electionEventId);

		return new ControlComponentPublicKeys(nodeId, genKeysCCROutput.ccrjChoiceReturnCodesEncryptionKeyPair().getPublicKey(),
				genKeysCCROutput.ccrjSchnorrProofs(), setupTallyCCMOutput.getCcmjElectionKeyPair().getPublicKey(),
				setupTallyCCMOutput.getSchnorrProofs());
	}

	@Transactional
	public ControlComponentPublicKeys getCcKeys(final String electionEventId) {
		validateUUID(electionEventId);

		final ElGamalMultiRecipientKeyPair ccrjChoiceReturnCodesEncryptionKeyPair = ccrjReturnCodesKeysService.getCcrjChoiceReturnCodesEncryptionKeyPair(
				electionEventId);
		final GroupVector<SchnorrProof, ZqGroup> ccrjSchnorrProofs = ccrjReturnCodesKeysService.getCcrjSchnorrProofs(electionEventId);

		final ElGamalMultiRecipientKeyPair ccmjElectionKeyPair = ccmjElectionKeysService.getCcmjElectionKeyPair(electionEventId);
		final GroupVector<SchnorrProof, ZqGroup> ccmjSchnorrProofs = ccmjElectionKeysService.getCcmjSchnorrProofs(electionEventId);

		return new ControlComponentPublicKeys(nodeId, ccrjChoiceReturnCodesEncryptionKeyPair.getPublicKey(), ccrjSchnorrProofs,
				ccmjElectionKeyPair.getPublicKey(), ccmjSchnorrProofs);
	}

}
