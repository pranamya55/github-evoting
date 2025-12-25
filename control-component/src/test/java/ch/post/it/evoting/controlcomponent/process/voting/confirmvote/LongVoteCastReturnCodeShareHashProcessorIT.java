/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_FILENAME_PATH;
import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_PASSWORD_FILENAME_PATH;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENTS_ADDRESS;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_NODE_ID;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.security.SignatureException;
import java.util.UUID;
import java.util.stream.IntStream;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.ArtemisSupport;
import ch.post.it.evoting.controlcomponent.TestSigner;
import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.process.VerificationCard;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenKeysCCROutput;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.VotingServerConfirmPayload;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

class LongVoteCastReturnCodeShareHashProcessorIT extends ArtemisSupport {

	private static ContextIds contextIds;
	private static VotingServerConfirmPayload votingServerConfirmPayload;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeAll
	static void setUpAll(
			@Autowired
			final ElectionEventStateService electionEventStateService,
			@Autowired
			final ElectionEventContextService electionEventContextService,
			@Autowired
			final ElectionEventService electionEventService,
			@Autowired
			final VerificationCardService verificationCardService,
			@Autowired
			final CcrjReturnCodesKeysService ccrjReturnCodesKeysService,
			@Autowired
			final VerificationCardStateService verificationCardStateService) throws IOException, SignatureException {

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContext electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();
		final GqGroup encryptionGroup = electionEventContext.encryptionGroup();

		// Save election event. The election must be in the CONFIGURED state for this processor.
		electionEventService.save(electionEventId, encryptionGroup);
		electionEventStateService.updateElectionEventState(electionEventId, ElectionEventState.CONFIGURED);

		// Save election event context.
		electionEventContextService.save(electionEventContext);

		// Create CCRj election keys.
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(ZqGroup.sameOrderAs(encryptionGroup));
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		final ZqElement ccrjReturnCodesGenerationSecretKey = zqGroupGenerator.genRandomZqElementMember();
		final ElGamalMultiRecipientKeyPair ccrjChoiceReturnCodesEncryptionKeyPair = elGamalGenerator.genRandomKeyPair(10);
		final GroupVector<SchnorrProof, ZqGroup> schnorrProofs = IntStream.range(0, ccrjChoiceReturnCodesEncryptionKeyPair.size())
				.mapToObj(i -> new SchnorrProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.collect(toGroupVector());
		final GenKeysCCROutput genKeysCCROutput = new GenKeysCCROutput(ccrjChoiceReturnCodesEncryptionKeyPair, ccrjReturnCodesGenerationSecretKey,
				schnorrProofs);

		ccrjReturnCodesKeysService.save(electionEventId, genKeysCCROutput);

		// Create verification card.
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String verificationCardId = uuidGenerator.generate();
		final String verificationCardSetId = electionEventContext.verificationCardSetContexts().get(0).getVerificationCardSetId();
		final ElGamalMultiRecipientPublicKey publicKey = elGamalGenerator.genRandomPublicKey(1);
		verificationCardService.save(new VerificationCard(verificationCardId, verificationCardSetId, publicKey));

		contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);

		//Mark card as LCC share created
		verificationCardStateService.setSentVote(verificationCardId);

		// Generate request payload.
		final GqElement confirmationKeyElement = new GqGroupGenerator(encryptionGroup).genMember();
		final ConfirmationKey confirmationKey = new ConfirmationKey(contextIds, confirmationKeyElement);
		final int confirmationAttemptId = 0;

		votingServerConfirmPayload = new VotingServerConfirmPayload(encryptionGroup, confirmationKey, confirmationAttemptId);

		final TestSigner votingServerSigner = new TestSigner(KEYSTORE_FILENAME_PATH, KEYSTORE_PASSWORD_FILENAME_PATH, Alias.VOTING_SERVER);
		votingServerSigner.sign(votingServerConfirmPayload,
				ChannelSecurityContextData.votingServerConfirm(electionEventId, verificationCardSetId, verificationCardId));
	}

	@Test
	void happyPath() throws IOException, JMSException {
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, objectMapper.writeValueAsBytes(votingServerConfirmPayload), jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, votingServerConfirmPayload.getClass().getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		final Message responseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		assertNotNull(responseMessage);

		final ControlComponenthlVCCSharePayload responsePayload = objectMapper.readValue(responseMessage.getBody(byte[].class),
				ControlComponenthlVCCSharePayload.class);

		assertEquals(contextIds, responsePayload.getConfirmationKey().contextIds());
		assertNotNull(responsePayload.getHashLongVoteCastReturnCodeShare());
	}
}

