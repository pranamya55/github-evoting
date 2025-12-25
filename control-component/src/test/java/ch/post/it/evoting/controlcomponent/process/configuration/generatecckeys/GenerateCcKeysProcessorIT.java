/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.generatecckeys;

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

import java.io.IOException;
import java.security.SignatureException;
import java.util.UUID;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.ArtemisSupport;
import ch.post.it.evoting.controlcomponent.TestDatabaseCleanUpService;
import ch.post.it.evoting.controlcomponent.TestSigner;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@DisplayName("GenerateCcKeysProcessor consuming")
class GenerateCcKeysProcessorIT extends ArtemisSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateCcKeysProcessorIT.class);

	private ElectionEventContextPayload electionEventContextPayload;
	private byte[] electionContextPayloadBytes;
	private byte[] modifiedElectionContextPayloadBytes;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TestDatabaseCleanUpService testDatabaseCleanUpService;

	@BeforeEach
	void setUp() throws IOException, SignatureException {
		// Request payload.
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();

		final TestSigner sdmSigner = new TestSigner(KEYSTORE_FILENAME_PATH, KEYSTORE_PASSWORD_FILENAME_PATH, Alias.SDM_CONFIG);
		sdmSigner.sign(electionEventContextPayload, ChannelSecurityContextData.electionEventContext(electionEventId));

		electionContextPayloadBytes = objectMapper.writeValueAsBytes(electionEventContextPayload);

		// Modified request payload.
		final String modifiedSeed = "NE_20271124_TT05";
		final ElectionEventContextPayload modifiedElectionEventContextPayload = new ElectionEventContextPayload(
				electionEventContextPayload.getEncryptionGroup(), modifiedSeed, electionEventContextPayload.getSmallPrimes(),
				electionEventContextPayload.getElectionEventContext(),
				electionEventContextPayload.getTenantId());
		sdmSigner.sign(modifiedElectionEventContextPayload, ChannelSecurityContextData.electionEventContext(electionEventId));
		modifiedElectionContextPayloadBytes = objectMapper.writeValueAsBytes(modifiedElectionEventContextPayload);
	}

	@AfterEach
	void cleanUp() {
		testDatabaseCleanUpService.cleanUp();
	}

	@Test
	@DisplayName("a request for the first time saves ElectionEventContext")
	void firstTimeCommand() throws JMSException {
		// Send to request queue the ElectionContextPayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, electionContextPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, ElectionEventContextPayload.class.getName());
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
	@DisplayName("an identical command sent twice sends previously computed response")
	void identicalCommandTwice() throws JMSException, IOException {
		// Send to request queue the ElectionContextPayload for the first time.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, electionContextPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, ElectionEventContextPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});
		final Message firstResponseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);

		// Send to request queue the ElectionContextPayload for the second time.
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, electionContextPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, ElectionEventContextPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});
		final Message secondResponseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);

		// Verifications.
		assertNotNull(firstResponseMessage);
		assertNotNull(secondResponseMessage);
		assertEquals(correlationId, firstResponseMessage.getJMSCorrelationID());
		assertEquals(correlationId, secondResponseMessage.getJMSCorrelationID());

		final ControlComponentPublicKeysPayload firstResponse = objectMapper.reader()
				.withAttribute("group", electionEventContextPayload.getEncryptionGroup())
				.readValue(firstResponseMessage.getBody(byte[].class), ControlComponentPublicKeysPayload.class);
		final ControlComponentPublicKeysPayload secondResponse = objectMapper.reader()
				.withAttribute("group", electionEventContextPayload.getEncryptionGroup())
				.readValue(secondResponseMessage.getBody(byte[].class), ControlComponentPublicKeysPayload.class);

		// It's expected that the signature of the payloads differ as the signature contains the timestamp.
		// This is why we check the hash equality and not the message body equality.
		final Hash hash = HashFactory.createHash();
		assertEquals(hash.recursiveHash(secondResponse), hash.recursiveHash(firstResponse));
	}

	@Test
	@DisplayName("an identical command with different payload is rejected")
	void identicalCommandDifferentPayload() throws JMSException {
		// Send to request queue the ElectionContextPayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, electionContextPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, ElectionEventContextPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});
		LOGGER.info("waiting for response");
		final Message firstResponseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		LOGGER.info("response received");

		assertNotNull(firstResponseMessage);
		assertEquals(correlationId, firstResponseMessage.getJMSCorrelationID());

		// Send to request queue the modified ElectionContextPayload.
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, modifiedElectionContextPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, ElectionEventContextPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});
		final Message secondResponseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		final Message dlqResponseMessage = dlqListenerJmsTemplate.receive(DEAD_LETTER_QUEUE);

		// Verifications.
		assertNull(secondResponseMessage);
		assertNotNull(dlqResponseMessage);
		assertEquals(correlationId, dlqResponseMessage.getJMSCorrelationID());

		final String expectedErrorMessage = String.format(
				"Similar request previously treated but for different request payload. [correlationId: %s, contextId: %s, context: %s, nodeId: %s]",
				correlationId, electionEventContextPayload.getElectionEventContext().electionEventId(), "CONFIGURATION_RETURN_CODES_GEN_KEYS_CCR", 1);
		assertExceptionMessage(expectedErrorMessage);
	}

	@Test
	@DisplayName("a payload with invalid signature sends null response and does not save ElectionEventContext")
	void sendNullResponseWithInvalidSignature() throws IOException, SignatureException, JMSException {
		// Resign with wrong alias.
		final TestSigner wrongSigner = new TestSigner(KEYSTORE_FILENAME_PATH, KEYSTORE_PASSWORD_FILENAME_PATH, Alias.SDM_TALLY);
		wrongSigner.sign(electionEventContextPayload,
				ChannelSecurityContextData.electionEventContext(electionEventContextPayload.getElectionEventContext().electionEventId()));
		final byte[] wrongSignatureElectionContextPayloadBytes = objectMapper.writeValueAsBytes(electionEventContextPayload);

		// Send to request queue the ElectionContextPayload.
		final String correlationId = UUID.randomUUID().toString();

		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, wrongSignatureElectionContextPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, ElectionEventContextPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		final Message responseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		final Message dlqResponseMessage = dlqListenerJmsTemplate.receive(DEAD_LETTER_QUEUE);

		// Verifications.
		assertNull(responseMessage);
		assertNotNull(dlqResponseMessage);
		assertEquals(correlationId, dlqResponseMessage.getJMSCorrelationID());

		final String requestMessageType = checkNotNull(dlqResponseMessage.getStringProperty(MESSAGE_HEADER_MESSAGE_TYPE));
		final String expectedErrorMessage = String.format("The signature is not valid. [requestMessageType: %s, correlationId: %s, nodeId: %s]",
				requestMessageType, correlationId, 1);
		assertExceptionMessage(expectedErrorMessage);
	}

}
