/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENTS_ADDRESS;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_NODE_ID;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.security.SignatureException;
import java.util.UUID;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.process.CcmjElectionKeysService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.protocol.tally.mixonline.VerifyMixDecOnlineService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRequestPayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineResponsePayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(2)
@TestPropertySource(properties = "nodeID=2")
@DisplayName("MixDecryptProcessor on node 2 consuming")
class MixDecryptProcessorCC2ITCase extends MixDecryptProcessorTestBase {

	private static final int NODE_ID_2 = 2;
	private static final String ELECTION_EVENT_ID = electionEventContext.electionEventId();

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ElectionEventService electionEventService;

	@MockitoSpyBean
	private MixDecryptService mixDecryptService;

	@MockitoSpyBean
	private MixDecryptProcessor mixDecryptProcessor;

	@MockitoSpyBean
	private SignatureKeystore<Alias> signatureKeystoreService;

	@MockitoSpyBean
	private VerifyMixDecOnlineService verifyMixDecOnlineService;

	@BeforeAll
	static void setUpAll(
			@Autowired
			final CcmjElectionKeysService ccmjElectionKeysService) {

		// Each node needs to save its ccmj election key pair.
		ccmjElectionKeysService.save(ELECTION_EVENT_ID, getSetupTallyCCMOutput(NODE_ID_2));
	}

	@Test
	@DisplayName("a mixDecryptOnlineRequestPayload correctly mixes")
	void request() throws IOException, SignatureException, JMSException {
		doReturn(true).when(signatureKeystoreService).verifySignature(any(), any(), any(), any());

		final MixDecryptResponse mixDecryptResponseNode1 = getResponseMessage(1);
		final String payloadElectionEventId = mixDecryptResponseNode1.getElectionEventId();
		final String ballotBoxIdToMix = mixDecryptResponseNode1.getBallotBoxId();
		final ImmutableByteArray responseMessageNode1 = mixDecryptResponseNode1.getMessage();
		final GqGroup gqGroup = electionEventService.getEncryptionGroup(payloadElectionEventId);

		final MixDecryptOnlineResponsePayload mixDecryptOnlineResponsePayload = objectMapper.reader()
				.withAttribute("group", gqGroup)
				.readValue(responseMessageNode1.elements(), MixDecryptOnlineResponsePayload.class);

		// Send to node 2.
		final MixDecryptOnlineRequestPayload request = new MixDecryptOnlineRequestPayload(payloadElectionEventId, ballotBoxIdToMix, NODE_ID_2,
				controlComponentVotesHashPayloads, ImmutableList.of(mixDecryptOnlineResponsePayload.controlComponentShufflePayload()));
		final byte[] requestBytes = objectMapper.writeValueAsBytes(request);

		// Sends a request to the processor.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, requestBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, MixDecryptOnlineRequestPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "2");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		// Collects the response of the processor.
		final Message responseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);

		assertNotNull(responseMessage);
		assertEquals(correlationId, responseMessage.getJMSCorrelationID());

		verify(mixDecryptProcessor, after(5000).times(1)).onRequest(any());
		verify(mixDecryptService).performMixDecOnline(any(), any(), any(), any());
		verify(verifyMixDecOnlineService).verifyMixDecOnline(any(), any(), anyInt(), any(), any(), any());
		verify(signatureKeystoreService, times(5)).verifySignature(any(), any(), any(), any());
		verify(signatureKeystoreService, times(2)).generateSignature(any(), any());

		// Saves the response so the next nodes can use it.
		addResponseMessage(NODE_ID_2, payloadElectionEventId, ballotBoxIdToMix, new ImmutableByteArray(responseMessage.getBody(byte[].class)));
	}

}
