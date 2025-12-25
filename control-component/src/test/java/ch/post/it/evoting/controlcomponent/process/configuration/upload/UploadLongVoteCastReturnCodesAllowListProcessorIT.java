/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.upload;

import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENTS_ADDRESS;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_NODE_ID;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.security.SignatureException;
import java.util.UUID;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.ArtemisSupport;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystoreFactory;
import ch.post.it.evoting.domain.configuration.setupvoting.LongVoteCastReturnCodesAllowListResponsePayload;
import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.domain.generators.SetupComponentLVCCAllowListPayloadGenerator;
import ch.post.it.evoting.domain.multitenancy.TenantConstants;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.KeystoreRepository;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;

@DisplayName("A UploadLongVoteCastReturnCodesAllowListProcessor consuming")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UploadLongVoteCastReturnCodesAllowListProcessorIT extends ArtemisSupport {

	private static ElectionEventContext electionEventContext;
	private static SetupComponentLVCCAllowListPayload signedSetupComponentLVCCAllowListPayload;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoSpyBean
	private UploadLongVoteCastReturnCodesAllowListProcessor uploadLongVoteCastReturnCodesAllowListProcessor;

	@Autowired
	private VerificationCardSetService verificationCardSetService;

	@BeforeAll
	static void setUpAll(
			@Autowired
			final ElectionEventService electionEventService,
			@Autowired
			final TenantService tenantService,
			@Autowired
			final ElectionEventContextService electionEventContextService) throws SignatureException, IOException {

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		final GqGroup gqGroup = electionEventContext.encryptionGroup();
		final String electionEventId = electionEventContext.electionEventId();

		// Save election event.
		electionEventService.save(electionEventId, gqGroup);

		// Save election event context.
		electionEventContextService.save(electionEventContext);

		// Request payload.
		final String verificationCardSetId = electionEventContext.verificationCardSetContexts().get(0).getVerificationCardSetId();
		final SetupComponentLVCCAllowListPayloadGenerator setupComponentLVCCAllowListPayloadGenerator = new SetupComponentLVCCAllowListPayloadGenerator();
		signedSetupComponentLVCCAllowListPayload = setupComponentLVCCAllowListPayloadGenerator.generate(electionEventId, verificationCardSetId);

		final KeystoreRepository repository = tenantService.getTenant(TenantConstants.TEST_TENANT_ID).keystoreRepository();
		final SignatureKeystore<Alias> sdmSignatureKeystoreService = SignatureKeystoreFactory.createSignatureKeystore(repository.getKeyStore(),
				"PKCS12", repository.getKeystorePassword(), keystore -> true, Alias.SDM_CONFIG);
		final ImmutableByteArray signature = sdmSignatureKeystoreService.generateSignature(signedSetupComponentLVCCAllowListPayload,
				ChannelSecurityContextData.setupComponentLVCCAllowList(electionEventId, verificationCardSetId));
		signedSetupComponentLVCCAllowListPayload.setSignature(new CryptoPrimitivesSignature(signature));
	}

	@Test
	@DisplayName("a longVoteCastReturnCodesAllowListPayload saves the lists in the database and returns a response.")
	void request() throws IOException, JMSException {
		final byte[] longVoteCastReturnCodesAllowListPayloadBytes = objectMapper.writeValueAsBytes(signedSetupComponentLVCCAllowListPayload);

		// Sends a request to the processor.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, longVoteCastReturnCodesAllowListPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, SetupComponentLVCCAllowListPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		// Collects the response of the processor.
		final Message responseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		assertNotNull(responseMessage);
		assertEquals(correlationId, responseMessage.getJMSCorrelationID());

		verify(uploadLongVoteCastReturnCodesAllowListProcessor, after(5000).times(1)).onRequest(any());

		final String verificationCardSetId = electionEventContext.verificationCardSetContexts().get(0).getVerificationCardSetId();
		assertEquals(signedSetupComponentLVCCAllowListPayload.getLongVoteCastReturnCodesAllowList(),
				verificationCardSetService.getLongVoteCastReturnCodesAllowList(verificationCardSetId));

		final LongVoteCastReturnCodesAllowListResponsePayload longVoteCastReturnCodesAllowListResponsePayload =
				objectMapper.readValue(responseMessage.getBody(byte[].class), LongVoteCastReturnCodesAllowListResponsePayload.class);

		assertEquals(1, longVoteCastReturnCodesAllowListResponsePayload.nodeId());

		final String electionEventId = electionEventContext.electionEventId();
		assertEquals(electionEventId, longVoteCastReturnCodesAllowListResponsePayload.electionEventId());
	}

}
