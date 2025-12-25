/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_FILENAME_PATH;
import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_PASSWORD_FILENAME_PATH;
import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENTS_ADDRESS;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_NODE_ID;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.SignatureException;
import java.util.UUID;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.ArtemisSupport;
import ch.post.it.evoting.controlcomponent.TestDatabaseCleanUpService;
import ch.post.it.evoting.controlcomponent.TestSigner;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.process.VerificationCard;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.domain.generators.ControlComponentBallotBoxPayloadGenerator;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.domain.voting.sendvote.VotingServerEncryptedVotePayload;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@DisplayName("PartialDecryptProcessor consuming")
class PartialDecryptProcessorIT extends ArtemisSupport {

	private byte[] encryptedVotePayloadBytes;
	private ContextIds contextIds;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ElectionEventService electionEventService;

	@Autowired
	private VerificationCardService verificationCardService;

	@Autowired
	private ElectionEventContextService electionEventContextService;

	@Autowired
	private ElectionEventStateService electionEventStateService;

	@Autowired
	private TestDatabaseCleanUpService testDatabaseCleanUpService;

	@MockitoSpyBean
	private SignatureKeystore<Alias> signatureKeystoreService;

	@MockitoBean
	private PartialDecryptService partialDecryptService;

	@BeforeEach
	void setUp() throws IOException, SignatureException {
		reset(signatureKeystoreService, partialDecryptService);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContext electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();
		final GqGroup gqGroup = electionEventContext.encryptionGroup();

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(gqGroup);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(ZqGroup.sameOrderAs(gqGroup));

		// Save election event. The election must be in the CONFIGURED state for this processor.
		electionEventService.save(electionEventId, gqGroup);
		electionEventStateService.updateElectionEventState(electionEventId, ElectionEventState.CONFIGURED);

		// Save election event context.
		electionEventContextService.save(electionEventContext);

		// Create verification card.
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String verificationCardId = uuidGenerator.generate();
		final String verificationCardSetId = electionEventContext.verificationCardSetContexts().get(0).getVerificationCardSetId();
		final ElGamalMultiRecipientPublicKey publicKey = elGamalGenerator.genRandomPublicKey(1);
		verificationCardService.save(new VerificationCard(verificationCardId, verificationCardSetId, publicKey));

		// Request payload.
		contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		final ControlComponentBallotBoxPayloadGenerator controlComponentBallotBoxPayloadGenerator = new ControlComponentBallotBoxPayloadGenerator(
				gqGroup);
		final EncryptedVerifiableVote encryptedVerifiableVote = controlComponentBallotBoxPayloadGenerator.generateEncryptedVerifiableVote(contextIds,
				1, 1);

		final VotingServerEncryptedVotePayload votingServerEncryptedVotePayload = new VotingServerEncryptedVotePayload(gqGroup,
				encryptedVerifiableVote);

		final TestSigner votingServerSigner = new TestSigner(KEYSTORE_FILENAME_PATH, KEYSTORE_PASSWORD_FILENAME_PATH, Alias.VOTING_SERVER);
		votingServerSigner.sign(votingServerEncryptedVotePayload,
				ChannelSecurityContextData.votingServerEncryptedVote(electionEventId, verificationCardSetId, verificationCardId));

		encryptedVotePayloadBytes = objectMapper.writeValueAsBytes(votingServerEncryptedVotePayload);

		// For now still mock the PartialDecrypt response.
		final ExponentiationProof exponentiationProof = new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember());
		final GroupVector<GqElement, GqGroup> exponentiatedGammas = gqGroupGenerator.genRandomGqElementVector(1);
		final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC = new PartiallyDecryptedEncryptedPCC(contextIds, 1, exponentiatedGammas,
				GroupVector.of(exponentiationProof));
		when(partialDecryptService.performPartialDecrypt(any())).thenReturn(partiallyDecryptedEncryptedPCC);
	}

	@AfterEach
	void cleanUp() {
		testDatabaseCleanUpService.cleanUp();
	}

	@Test
	@DisplayName("a request for the first time perform calculation")
	void firstTimeCommand() throws JMSException {
		// Send to request queue the VotingServerEncryptedVotePayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, encryptedVotePayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, VotingServerEncryptedVotePayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		// Verifications.
		final Message responseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		assertNotNull(responseMessage);
		assertEquals(correlationId, responseMessage.getJMSCorrelationID());
	}

	@Test
	@DisplayName("problem verifying VotingServerEncryptedVotePayload signature sends null message")
	void problemVerifyingPayloadSignatureSendsNullMessage() throws SignatureException, JMSException {
		doThrow(SignatureException.class).when(signatureKeystoreService).verifySignature(any(), any(), any(), any());

		// Send to request queue the VotingServerEncryptedVotePayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, encryptedVotePayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, VotingServerEncryptedVotePayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		// There should not be any response.
		assertNull(jmsTemplate.receive(VOTING_SERVER_ADDRESS));
		final Message dlqResponseMessage = dlqListenerJmsTemplate.receive(DEAD_LETTER_QUEUE);
		assertNotNull(dlqResponseMessage);
		assertEquals(correlationId, dlqResponseMessage.getJMSCorrelationID());

		assertExceptionMessage(
				String.format("Could not verify the signature of the voting server encrypted vote payload. [contextIds: %s]", contextIds.toString()));
	}

	@Test
	@DisplayName("invalid VotingServerEncryptedVotePayload signature sends null message")
	void invalidPayloadSignatureSendsNullMessage() throws SignatureException, JMSException {
		doReturn(false).when(signatureKeystoreService).verifySignature(any(), any(), any(), any());

		// Send to request queue the VotingServerEncryptedVotePayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, encryptedVotePayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, VotingServerEncryptedVotePayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		// There should not be any response.
		assertNull(jmsTemplate.receive(VOTING_SERVER_ADDRESS));
		final Message dlqResponseMessage = dlqListenerJmsTemplate.receive(DEAD_LETTER_QUEUE);
		assertNotNull(dlqResponseMessage);
		assertEquals(correlationId, dlqResponseMessage.getJMSCorrelationID());

		final String requestMessageType = checkNotNull(dlqResponseMessage.getStringProperty(MESSAGE_HEADER_MESSAGE_TYPE));
		final String expectedErrorMessage = String.format("The signature is not valid. [requestMessageType: %s, correlationId: %s, nodeId: %s]",
				requestMessageType, correlationId, 1);
		assertExceptionMessage(expectedErrorMessage);
	}
}
