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
import static org.mockito.Mockito.never;
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
import ch.post.it.evoting.controlcomponent.protocol.tally.mixonline.VerifyMixDecOnlineService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRequestPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(1)
@TestPropertySource(properties = "nodeID=1")
@DisplayName("MixDecryptProcessor on node 1 consuming")
class MixDecryptProcessorCC1ITCase extends MixDecryptProcessorTestBase {

	private static final int NODE_ID_1 = 1;
	private static final String ELECTION_EVENT_ID = electionEventContext.electionEventId();

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoSpyBean
	private MixDecryptProcessor mixDecryptProcessor;

	@MockitoSpyBean
	private MixDecryptService mixDecryptService;

	@MockitoSpyBean
	private VerifyMixDecOnlineService verifyMixDecOnlineService;

	@MockitoSpyBean
	private SignatureKeystore<Alias> signatureKeystoreService;

	@BeforeAll
	static void setUpAll(
			@Autowired
			final CcmjElectionKeysService ccmjElectionKeysService) {

		// Each node needs to save its ccmj election key pair.
		ccmjElectionKeysService.save(ELECTION_EVENT_ID, getSetupTallyCCMOutput(NODE_ID_1));
	}

	@Test
	@DisplayName("a mixDecryptOnlineRequestPayload correctly mixes")
	void request() throws IOException, SignatureException, JMSException {
		// Request payload.
		final MixDecryptOnlineRequestPayload mixDecryptOnlineRequestPayload = new MixDecryptOnlineRequestPayload(ELECTION_EVENT_ID, ballotBoxIdToMix,
				NODE_ID_1, controlComponentVotesHashPayloads, ImmutableList.emptyList());

		final byte[] request = objectMapper.writeValueAsBytes(mixDecryptOnlineRequestPayload);

		// Sends a request to the processor.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, request, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, MixDecryptOnlineRequestPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		// Collects the response of the processor.
		final Message responseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);

		assertNotNull(responseMessage);
		assertEquals(correlationId, responseMessage.getJMSCorrelationID());

		verify(mixDecryptProcessor, after(5000).times(1)).onRequest(any());
		verify(mixDecryptService).performMixDecOnline(any(), any(), any(), any());
		verify(verifyMixDecOnlineService, never()).verifyMixDecOnline(any(), any(), anyInt(), any(), any(), any());
		verify(signatureKeystoreService, times(2)).generateSignature(any(), any());

		// Saves the response so the next nodes can use it.
		addResponseMessage(NODE_ID_1, ELECTION_EVENT_ID, ballotBoxIdToMix, new ImmutableByteArray(responseMessage.getBody(byte[].class)));
	}

}
