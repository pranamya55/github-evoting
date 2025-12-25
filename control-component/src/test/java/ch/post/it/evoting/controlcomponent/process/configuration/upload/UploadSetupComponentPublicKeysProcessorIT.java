/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.upload;

import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_FILENAME_PATH;
import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_PASSWORD_FILENAME_PATH;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENTS_ADDRESS;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_NODE_ID;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.security.SignatureException;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import ch.post.it.evoting.controlcomponent.process.ExtractedElectionEventHashService;
import ch.post.it.evoting.controlcomponent.process.LVCCAllowListEntryEntity;
import ch.post.it.evoting.controlcomponent.process.LVCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryEntity;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.controlcomponent.process.VerificationCard;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.domain.configuration.SetupComponentPublicKeysResponsePayload;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@DisplayName("A UploadSetupComponentPublicKeysProcessor consuming")
class UploadSetupComponentPublicKeysProcessorIT extends ArtemisSupport {

	private static ElectionEventContext electionEventContext;
	private static SetupComponentPublicKeysPayload setupComponentPublicKeysPayload;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoSpyBean
	private UploadSetupComponentPublicKeysProcessor uploadSetupComponentPublicKeysProcessor;

	@Autowired
	private SetupComponentPublicKeysService setupComponentPublicKeysService;

	@Autowired
	private ExtractedElectionEventHashService extractedElectionEventHashService;

	@MockitoBean
	private VerifySetupComponentPublicKeysService verifySetupComponentPublicKeysService;

	@BeforeAll
	static void setUpAll(
			@Autowired
			final ElectionEventService electionEventService,
			@Autowired
			final VerificationCardSetService verificationCardSetService,
			@Autowired
			final VerificationCardService verificationCardService,
			@Autowired
			final ElectionEventContextService electionEventContextService,
			@Autowired
			final PCCAllowListEntryService pccAllowListEntryService,
			@Autowired
			final LVCCAllowListEntryService lvccAllowListEntryService) throws SignatureException, IOException {

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		final GqGroup gqGroup = electionEventContext.encryptionGroup();
		final String electionEventId = electionEventContext.electionEventId();

		// Save election event.
		electionEventService.save(electionEventId, gqGroup);

		// Save election event context.
		electionEventContextService.save(electionEventContext);

		// Save verification cards.
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		verificationCardService.saveAll(electionEventContext.verificationCardSetContexts().stream()
				.flatMap(verificationCardSetContext ->
						IntStream.range(0, verificationCardSetContext.getNumberOfEligibleVoters())
								.mapToObj(i -> new VerificationCard(
										uuidGenerator.generate(),
										verificationCardSetContext.getVerificationCardSetId(),
										elGamalGenerator.genRandomPublicKey(10)))
				).collect(toImmutableList())
		);

		// Save lVCC and pCC allow list.
		final Random random = RandomFactory.createRandom();
		final Base64Alphabet base64Alphabet = Base64Alphabet.getInstance();
		electionEventContext.verificationCardSetContexts().forEach(
				verificationCardSetContext -> {
					final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSet(
							verificationCardSetContext.getVerificationCardSetId());
					final int numberOfEligibleVoters = verificationCardSetContext.getNumberOfEligibleVoters();

					final ImmutableList<PCCAllowListEntryEntity> partialChoiceReturnCodesAllowList = Stream
							.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
							.limit(numberOfEligibleVoters)
							.map(partialChoiceReturnCode -> new PCCAllowListEntryEntity(verificationCardSetEntity, partialChoiceReturnCode, 1))
							.collect(toImmutableList());
					pccAllowListEntryService.saveAll(partialChoiceReturnCodesAllowList);

					final ImmutableList<LVCCAllowListEntryEntity> longVoteCastReturnCodesAllowList = Stream
							.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
							.limit(numberOfEligibleVoters)
							.map(longVoteCastReturnCode -> new LVCCAllowListEntryEntity(verificationCardSetEntity, longVoteCastReturnCode))
							.collect(toImmutableList());
					lvccAllowListEntryService.saveAll(longVoteCastReturnCodesAllowList);
				}
		);

		// Request payload.
		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator(
				gqGroup);
		setupComponentPublicKeysPayload = new SetupComponentPublicKeysPayload(gqGroup, electionEventId,
				setupComponentPublicKeysPayloadGenerator.generate().getSetupComponentPublicKeys());

		final TestSigner sdmSigner = new TestSigner(KEYSTORE_FILENAME_PATH, KEYSTORE_PASSWORD_FILENAME_PATH, Alias.SDM_CONFIG);
		sdmSigner.sign(setupComponentPublicKeysPayload,
				ChannelSecurityContextData.setupComponentPublicKeys(setupComponentPublicKeysPayload.getElectionEventId()));
	}

	@AfterAll
	static void cleanUpAll(
			@Autowired
			final TestDatabaseCleanUpService testDatabaseCleanUpService
	) {
		testDatabaseCleanUpService.cleanUp();
	}

	@Test
	@DisplayName("a setupComponentPublicKeysPayload saves the public keys in the database and returns a response.")
	void request() throws IOException, JMSException {
		final byte[] setupComponentPublicKeysPayloadBytes = objectMapper.writeValueAsBytes(setupComponentPublicKeysPayload);

		// Sends a request to the processor.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, setupComponentPublicKeysPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, SetupComponentPublicKeysPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		// Collects the response of the processor.
		final Message responseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		assertNotNull(responseMessage);
		assertEquals(correlationId, responseMessage.getJMSCorrelationID());

		verify(uploadSetupComponentPublicKeysProcessor, after(5000).times(1)).onRequest(any());

		// Verify saved public keys.
		final String electionEventId = electionEventContext.electionEventId();
		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayload.getSetupComponentPublicKeys();
		assertAll(
				() -> assertEquals(setupComponentPublicKeys.electionPublicKey(),
						setupComponentPublicKeysService.getElectionPublicKey(electionEventId)),
				() -> assertEquals(setupComponentPublicKeys.combinedControlComponentPublicKeys(),
						setupComponentPublicKeysService.getCombinedControlComponentPublicKeys(electionEventId)),
				() -> assertEquals(setupComponentPublicKeys.electoralBoardPublicKey(),
						setupComponentPublicKeysService.getElectoralBoardPublicKey(electionEventId)),
				() -> assertEquals(setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey(),
						setupComponentPublicKeysService.getChoiceReturnCodesEncryptionPublicKey(electionEventId))
		);

		// Verify response.
		final SetupComponentPublicKeysResponsePayload setupComponentPublicKeysResponsePayload =
				objectMapper.readValue(responseMessage.getBody(byte[].class), SetupComponentPublicKeysResponsePayload.class);
		assertEquals(1, setupComponentPublicKeysResponsePayload.nodeId());
		assertEquals(electionEventId, setupComponentPublicKeysResponsePayload.electionEventId());
	}

}
