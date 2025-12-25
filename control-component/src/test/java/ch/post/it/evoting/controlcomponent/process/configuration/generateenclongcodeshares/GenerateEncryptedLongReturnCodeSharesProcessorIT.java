/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.generateenclongcodeshares;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENTS_ADDRESS;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_NODE_ID;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.UUID;
import java.util.stream.IntStream;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.ArtemisSupport;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventEntity;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryEntity;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenEncLongCodeSharesAlgorithm;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenKeysCCROutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.domain.generators.SetupComponentVerificationDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@DisplayName("GenerateEncryptedLongReturnCodeSharesProcessor consuming")
class GenerateEncryptedLongReturnCodeSharesProcessorIT extends ArtemisSupport {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static final int NUMBER_OF_ELIGIBLE_VOTERS = 3;
	private static final int NUMBER_OF_VOTING_OPTIONS = 25;

	private static String electionEventId;
	private static GqGroup encryptionGroup;
	private static byte[] requestPayloadBytes;
	private static ElectionEventEntity electionEventEntity;
	private static SetupComponentVerificationDataPayload setupComponentVerificationDataPayload;
	private static SetupComponentVerificationDataPayloadGenerator setupComponentVerificationDataPayloadGenerator;

	@Autowired
	private PCCAllowListEntryService pCCAllowListEntryService;

	@MockitoSpyBean
	private GenEncLongCodeSharesAlgorithm genEncLongCodeSharesAlgorithm;

	@MockitoBean
	private SignatureKeystore<Alias> signatureKeystoreService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private VerificationCardSetService verificationCardSetService;

	@Autowired
	private BallotBoxService ballotBoxService;

	@BeforeAll
	static void setUpAll(
			@Autowired
			final ObjectMapper objectMapper,
			@Autowired
			final ElectionEventService electionEventService,
			@Autowired
			final VerificationCardSetService verificationCardSetService,
			@Autowired
			final BallotBoxService ballotBoxService,
			@Autowired
			final CcrjReturnCodesKeysService ccrjReturnCodesKeysService
	) throws IOException {

		setupComponentVerificationDataPayloadGenerator = new SetupComponentVerificationDataPayloadGenerator();
		final int chunkId = 2;
		setupComponentVerificationDataPayload = setupComponentVerificationDataPayloadGenerator.generate(chunkId, NUMBER_OF_ELIGIBLE_VOTERS,
				NUMBER_OF_VOTING_OPTIONS
		);

		requestPayloadBytes = objectMapper.writeValueAsBytes(setupComponentVerificationDataPayload);

		// Must match the group in the json.
		encryptionGroup = setupComponentVerificationDataPayload.getEncryptionGroup();

		// Save election event.
		electionEventId = setupComponentVerificationDataPayload.getElectionEventId();
		electionEventEntity = electionEventService.save(electionEventId, encryptionGroup);

		// Save verification card set.
		final String verificationCardSetId = setupComponentVerificationDataPayload.getVerificationCardSetId();
		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardSetAlias("alias-" + verificationCardSetId)
				.setVerificationCardSetDescription("Description " + verificationCardSetId)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.setElectionEventEntity(electionEventEntity)
				.build();
		verificationCardSetService.save(verificationCardSetEntity);

		// Save ballot box.
		final String ballotBoxId = uuidGenerator.generate();
		final PrimesMappingTable primesMappingTable = new PrimesMappingTableGenerator(encryptionGroup).generate(NUMBER_OF_VOTING_OPTIONS);
		ballotBoxService.save(ballotBoxId, verificationCardSetId, LocalDateTimeUtils.now().minusDays(1), LocalDateTimeUtils.now().plusDays(3), true,
				32, 900,
				primesMappingTable);

		// Save ccrjReturnCodesKeys.
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
	}

	@Test
	@DisplayName("Happy Path Generate Encrypted Long Return Code Shares Processing")
	void happyPathGenerateEncryptedLongReturnCodeSharesProcessing() throws SignatureException, IOException, JMSException {
		when(signatureKeystoreService.generateSignature(any(), any())).thenReturn(
				new ImmutableByteArray(electionEventId.getBytes(StandardCharsets.UTF_8)));
		when(signatureKeystoreService.verifySignature(eq(Alias.SDM_CONFIG), any(), any(), any())).thenReturn(true);

		final String verificationCardSetId = setupComponentVerificationDataPayload.getVerificationCardSetId();

		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, requestPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, SetupComponentVerificationDataPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		final Message responseMessage = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		assertNotNull(responseMessage);

		final byte[] response = responseMessage.getBody(byte[].class);
		final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload = objectMapper.readValue(response,
				ControlComponentCodeSharesPayload.class);

		assertEquals(correlationId, responseMessage.getJMSCorrelationID());

		verify(genEncLongCodeSharesAlgorithm, times(1)).genEncLongCodeShares(any(), any());
		verify(signatureKeystoreService, times(1)).verifySignature(eq(Alias.SDM_CONFIG), any(), any(), any());

		assertEquals(electionEventId, controlComponentCodeSharesPayload.getElectionEventId());
		assertEquals(verificationCardSetId, controlComponentCodeSharesPayload.getVerificationCardSetId());
		assertEquals(setupComponentVerificationDataPayload.getChunkId(), controlComponentCodeSharesPayload.getChunkId());
	}

	@Test
	@DisplayName("Happy Path concurrent Generate Encrypted Long Return Code Shares Processing")
	void happyPathConcurrentGenerateEncryptedLongReturnCodeSharesProcessing() throws SignatureException, IOException, JMSException {
		when(signatureKeystoreService.generateSignature(any(), any())).thenReturn(
				new ImmutableByteArray(electionEventId.getBytes(StandardCharsets.UTF_8)));
		when(signatureKeystoreService.verifySignature(eq(Alias.SDM_CONFIG), any(), any(), any())).thenReturn(true);

		final SetupComponentVerificationDataPayload payload0 = setupComponentVerificationDataPayloadGenerator.generate(electionEventId, 0,
				NUMBER_OF_ELIGIBLE_VOTERS, NUMBER_OF_VOTING_OPTIONS);
		final byte[] requestPayloadBytes0 = objectMapper.writeValueAsBytes(payload0);

		final String verificationCardSetId = payload0.getVerificationCardSetId();
		final SetupComponentVerificationDataPayload payload1 = setupComponentVerificationDataPayloadGenerator.generate(electionEventId,
				verificationCardSetId, 1, NUMBER_OF_ELIGIBLE_VOTERS, NUMBER_OF_VOTING_OPTIONS);
		final byte[] requestPayloadBytes1 = objectMapper.writeValueAsBytes(payload1);

		// Save verification card set.
		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardSetAlias("alias-" + verificationCardSetId)
				.setVerificationCardSetDescription("Description " + verificationCardSetId)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.setElectionEventEntity(electionEventEntity)
				.build();
		verificationCardSetService.save(verificationCardSetEntity);

		// Save ballot box.
		final String ballotBoxId = uuidGenerator.generate();
		final PrimesMappingTable primesMappingTable = new PrimesMappingTableGenerator(encryptionGroup).generate(NUMBER_OF_VOTING_OPTIONS);
		ballotBoxService.save(ballotBoxId, verificationCardSetId, LocalDateTimeUtils.now().minusDays(1), LocalDateTimeUtils.now().plusDays(3), true,
				32, 900, primesMappingTable);

		// Send two messages roughly at the same time to test that when two chunks are processed at the same time, one will success and one fail
		// to create the verification card set. The failing one will retry once and will be able to process the message. Depending on the thread
		// execution order, it may or may not generate a concurrent creation of the verification card set.
		final String correlationId0 = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, requestPayloadBytes0, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId0);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, SetupComponentVerificationDataPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});
		final String correlationId1 = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, requestPayloadBytes1, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId1);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, SetupComponentVerificationDataPayload.class.getName());
			jmsMessage.setStringProperty(MESSAGE_HEADER_NODE_ID, "1");
			jmsMessage.setStringProperty(MESSAGE_HEADER_TENANT_ID, contextHolder.getTenantId());
			return jmsMessage;
		});

		final Message responseMessage0 = jmsTemplate.receive(VOTING_SERVER_ADDRESS);
		final Message responseMessage1 = jmsTemplate.receive(VOTING_SERVER_ADDRESS);

		assertNotNull(responseMessage0);
		assertNotNull(responseMessage1);

		final ControlComponentCodeSharesPayload responsePayload0 = objectMapper.readValue(responseMessage0.getBody(byte[].class),
				ControlComponentCodeSharesPayload.class);
		final ControlComponentCodeSharesPayload responsePayload1 = objectMapper.readValue(responseMessage1.getBody(byte[].class),
				ControlComponentCodeSharesPayload.class);

		assertNotEquals(responsePayload0, responsePayload1);

		// In the end, two chunks must have been successfully saved, regardless of concurrency.
		final ImmutableList<PCCAllowListEntryEntity> pccAllowList = pCCAllowListEntryService.getPCCAllowListEntries(
				payload0.getVerificationCardSetId());
		final ImmutableList<Integer> chunkIds = pccAllowList.stream()
				.map(PCCAllowListEntryEntity::getChunkId)
				.distinct()
				.collect(toImmutableList());
		assertEquals(2, chunkIds.size());
	}
}
