/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_FILENAME_PATH;
import static ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer.KEYSTORE_PASSWORD_FILENAME_PATH;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement.PrimeGqElementFactory.getSmallPrimeGroupMembers;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static ch.post.it.evoting.domain.SharedQueue.CONTROL_COMPONENTS_ADDRESS;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_MESSAGE_TYPE;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_NODE_ID;
import static ch.post.it.evoting.domain.SharedQueue.MESSAGE_HEADER_TENANT_ID;
import static ch.post.it.evoting.domain.SharedQueue.VOTING_SERVER_ADDRESS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;

import ch.post.it.evoting.controlcomponent.ArtemisSupport;
import ch.post.it.evoting.controlcomponent.TestDatabaseCleanUpService;
import ch.post.it.evoting.controlcomponent.TestSigner;
import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.process.EncryptedVerifiableVoteService;
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
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenKeysCCROutput;
import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.GetHashExtractedElectionEventService;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.DecryptPCCContext;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.DecryptPCCService;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.PartialDecryptPCCAlgorithm;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.PartialDecryptPCCInput;
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.PartialDecryptPCCOutput;
import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupElement;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.utils.Conversions;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.domain.voting.sendvote.CombinedControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

/**
 * This test evaluates the functionality of the LCCShareProcessor by examining both valid and invalid scenarios. Initially, the test focuses on the
 * ideal case where a voting client submits the correct quantity of pCC along with a valid combination of voting options. Subsequently, the test
 * scrutinizes various error conditions: submitting either fewer or more pCC than required and supplying a valid pCC count but with an improper voting
 * option combination.
 */
@DisplayName("LCCShareProcessor consuming")
class LongChoiceReturnCodeShareProcessorIT extends ArtemisSupport {

	private static final Random random = RandomFactory.createRandom();
	private static final ElGamal elGamal = ElGamalFactory.createElGamal();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final int NUMBER_OF_VOTING_OPTIONS = 10;
	private static final int NUMBER_OF_VOTERS = 5;
	private static final int DELTA_SUP = MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1;

	private final GqGroup gqGroup = GroupTestData.getLargeGqGroup();
	private final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(gqGroup);
	private final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(ZqGroup.sameOrderAs(gqGroup));
	private final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);

	private VoteData voteData;

	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private ElectionEventStateService electionEventStateService;
	@Autowired
	private PartiallyDecryptedPCCService partiallyDecryptedPCCService;
	@MockitoSpyBean
	private DecryptPCCService decryptPCCService;
	@MockitoSpyBean
	private SignatureKeystore<Alias> signatureKeystoreService;
	@Autowired
	private ElectionEventService electionEventService;
	@Autowired
	private VerificationCardSetService verificationCardSetService;
	@Autowired
	private VerificationCardService verificationCardService;
	@Autowired
	private VerificationCardStateService verificationCardStateService;
	@Autowired
	private ElectionEventContextService electionEventContextService;
	@Autowired
	private EncryptedVerifiableVoteService encryptedVerifiableVoteService;
	@Autowired
	private SetupComponentPublicKeysService setupComponentPublicKeysService;
	@Autowired
	private CcrjReturnCodesKeysService ccrjReturnCodesKeysService;
	@Autowired
	private PCCAllowListEntryService pccAllowListEntryService;
	@Autowired
	private TestDatabaseCleanUpService testDatabaseCleanUpService;
	@Autowired
	private PrimesMappingTableAlgorithms primesMappingTableAlgorithms;
	@Autowired
	private GetHashExtractedElectionEventService getHashExtractedElectionEventService;
	@Autowired
	private LVCCAllowListEntryService lvccAllowListEntryService;
	@Autowired
	private ExtractedElectionEventHashService extractedElectionEventHashService;

	@BeforeEach
	void setUp() {
		reset(signatureKeystoreService, decryptPCCService);
		voteData = prepareVote(NUMBER_OF_VOTERS);
	}

	@AfterEach
	void cleanUp() {
		testDatabaseCleanUpService.cleanUp();
	}

	@Test
	@DisplayName("a request with a valid number and combination of voting options executes correctly")
	void firstTimeCommand() throws JsonProcessingException, JMSException {
		// Selected voting options: Q1_YES, Q2_NO, C
		final byte[] combinedCCPartialDecryptPayloadBytes = prepareRequestPayload(voteData, 0, 4, 8).elements();

		// Send to request queue the CombinedControlComponentPartialDecryptPayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, combinedCCPartialDecryptPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, CombinedControlComponentPartialDecryptPayload.class.getName());
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
	@DisplayName("a request with duplicated partial choice return codes writes to dead letter queue")
	void duplicatePartialChoiceReturnCodes() throws IOException, JMSException {
		final GroupVector<GqElement, GqGroup> partialChoiceReturnCodes = gqGroupGenerator.genRandomGqElementVector(
				NUMBER_OF_VOTING_OPTIONS - 1);
		final GroupVector<GqElement, GqGroup> pCC = partialChoiceReturnCodes.append(partialChoiceReturnCodes.getFirst());
		doReturn(pCC).when(decryptPCCService).decryptPCC(any(), any(), any());
		// Selected voting options: Q1_NO, Q2_YES, B
		final byte[] combinedCCPartialDecryptPayloadBytes = prepareRequestPayload(voteData, 1, 3, 7).elements();

		// Send to request queue the CombinedControlComponentPartialDecryptPayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, combinedCCPartialDecryptPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, CombinedControlComponentPartialDecryptPayload.class.getName());
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

		assertExceptionMessage("All pCC must be distinct.");
	}

	@Test
	@DisplayName("a request with too few partial choice return codes writes to dead letter queue")
	void tooFewPartialChoiceReturnCodes() throws IOException, JMSException {
		final GroupVector<GqElement, GqGroup> pCC = gqGroupGenerator.genRandomGqElementVector(
				NUMBER_OF_VOTING_OPTIONS - 1);
		doReturn(pCC).when(decryptPCCService).decryptPCC(any(), any(), any());
		// Selected voting options: Q1_EMPTY, Q2_YES, A
		final byte[] combinedCCPartialDecryptPayloadBytes = prepareRequestPayload(voteData, 2, 3, 6).elements();

		// Send to request queue the CombinedControlComponentPartialDecryptPayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, combinedCCPartialDecryptPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, CombinedControlComponentPartialDecryptPayload.class.getName());
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

		final int psi = primesMappingTableAlgorithms.getBlankCorrectnessInformation(voteData.primesMappingTable()).size();
		assertExceptionMessage(String.format("The number of partial choice return codes must be equal to psi. [psi: %s]", psi));
	}

	@Test
	@DisplayName("a request with too many partial choice return codes writes to dead letter queue")
	void tooManyPartialChoiceReturnCodes() throws IOException, JMSException {
		final GroupVector<GqElement, GqGroup> pCC = gqGroupGenerator.genRandomGqElementVector(
				NUMBER_OF_VOTING_OPTIONS + 1);
		doReturn(pCC).when(decryptPCCService).decryptPCC(any(), any(), any());
		// Selected voting options: Q1_YES, Q2_EMPTY, A
		final byte[] combinedCCPartialDecryptPayloadBytes = prepareRequestPayload(voteData, 0, 5, 6).elements();

		// Send to request queue the CombinedControlComponentPartialDecryptPayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, combinedCCPartialDecryptPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, CombinedControlComponentPartialDecryptPayload.class.getName());
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

		final int psi = primesMappingTableAlgorithms.getBlankCorrectnessInformation(voteData.primesMappingTable()).size();
		assertExceptionMessage(String.format("The number of partial choice return codes must be equal to psi. [psi: %s]", psi));
	}

	@Test
	@DisplayName("a request with an invalid combination of voting options (two voting options from the same question) writes to the dead letter queue")
	void twoPccForSameQuestion() throws IOException, JMSException {
		// Selected voting options: Q1_YES, Q1_NO, D
		final byte[] combinedControlComponentPartialDecryptPayloadBytes = prepareRequestPayload(voteData, 0, 1, 9).elements();

		// Send to request queue the CombinedControlComponentPartialDecryptPayload.
		final String correlationId = UUID.randomUUID().toString();
		multicastJmsTemplate.convertAndSend(CONTROL_COMPONENTS_ADDRESS, combinedControlComponentPartialDecryptPayloadBytes, jmsMessage -> {
			jmsMessage.setJMSCorrelationID(correlationId);
			jmsMessage.setStringProperty(MESSAGE_HEADER_MESSAGE_TYPE, CombinedControlComponentPartialDecryptPayload.class.getName());
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

		assertExceptionMessage("The partial Choice Return Codes allow list does not contain the partial Choice Return Code.");
	}

	/**
	 * Generates and saves to the database all objects that are needed to be able to vote.
	 * <p>
	 * Algorithms needed to prepare the vote:
	 *     <ol>
	 *         <li>GenKeysCCR</li>
	 *         <li>GenVerDat</li>
	 *         <li>GenVerCardSetKeys</li>
	 *     </ol>
	 * </p>
	 *
	 * @param numberOfVoters the number of voters for which to prepare the vote
	 * @return the vote data, containing context ids, primes mapping table, encoded voting options, ccr encryption key pairs, and verification data
	 */
	private VoteData prepareVote(final int numberOfVoters) {
		// GenVerDat
		final String question1 = "question1";
		final String question2 = "question2";
		final String election1 = "election1";
		final ImmutableList<String> correctnessInformation = ImmutableList.of(question1, question1, question1, question2, question2, question2,
				election1, election1, election1, election1);
		final ImmutableList<String> actualVotingOptions = ImmutableList.of("Q1|YES", "Q1|NO", "Q1|EMPTY", "Q2|YES", "Q2|NO", "Q2|EMPTY", "E1|A",
				"E2|B", "E1|C", "E1|D");
		final ImmutableList<String> semanticInformation = ImmutableList.of("NON_BLANK|Q1|YES", "NON_BLANK|Q1|NO", "BLANK|Q1|EMPTY",
				"NON_BLANK|Q2|YES", "NON_BLANK|Q2|NO", "BLANK|Q2|EMPTY", "NON_BLANK|A", "NON_BLANK|B", "NON_BLANK|C", "BLANK|D");
		final GroupVector<PrimeGqElement, GqGroup> encodedVotingOptions = getSmallPrimeGroupMembers(gqGroup, correctnessInformation.size());
		final PrimesMappingTable primesMappingTable = new PrimesMappingTableGenerator(gqGroup).generate(actualVotingOptions, encodedVotingOptions,
				semanticInformation, correctnessInformation);

		final ElectionEventContext electionEventContext = new ElectionEventContextPayloadGenerator(gqGroup).generate(primesMappingTable,
				numberOfVoters).getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();

		// GenKeysCCR
		final int psiMax = electionEventContext.maximumNumberOfSelections();
		final ImmutableList<ElGamalMultiRecipientKeyPair> ccrEncryptionKeyPairs = Stream.generate(() -> elGamalGenerator.genRandomKeyPair(psiMax))
				.limit(ControlComponentNode.ids().size())
				.collect(toImmutableList());

		// Save election event. The election must be in the CONFIGURED state for this processor.
		electionEventService.save(electionEventId, gqGroup);
		electionEventStateService.updateElectionEventState(electionEventId, ElectionEventState.CONFIGURED);

		// Save election event context.
		electionEventContextService.save(electionEventContext);

		// Save LVCC allow list.
		electionEventContext.verificationCardSetContexts().forEach(verificationCardSetContext -> {
			final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSet(
					verificationCardSetContext.getVerificationCardSetId());
			final int numberOfEligibleVoters = verificationCardSetContext.getNumberOfEligibleVoters();
			final ImmutableList<LVCCAllowListEntryEntity> lvccAllowListEntryEntities = IntStream.range(0, numberOfEligibleVoters)
					.mapToObj(i -> new LVCCAllowListEntryEntity(verificationCardSetEntity,
							random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet)))
					.collect(toImmutableList());
			lvccAllowListEntryService.saveAll(lvccAllowListEntryEntities);
		});

		final ImmutableMap<String, VerificationData> verificationDataMap = electionEventContext.verificationCardSetContexts().stream()
				.collect(toImmutableMap(
						VerificationCardSetContext::getVerificationCardSetId,
						verificationCardSetContext -> genPcc(random, numberOfVoters, encodedVotingOptions, primesMappingTable, electionEventId)
				));

		// Save pcc allow list, which is normally done in the GenEncLongCodeSharesProcessor
		final ImmutableList<PCCAllowListEntryEntity> partialChoiceReturnCodeAllowList = verificationDataMap.entrySet().stream()
				.flatMap(verificationDataEntry -> verificationDataEntry.value().pccAllowList.stream()
						.map(partialChoiceCode -> new PCCAllowListEntryEntity(
								verificationCardSetService.getVerificationCardSet(verificationDataEntry.key()),
								partialChoiceCode, 0)))
				.collect(toImmutableList());
		pccAllowListEntryService.saveAll(partialChoiceReturnCodeAllowList);

		final String verificationCardSetId = electionEventContext.verificationCardSetContexts().getFirst().getVerificationCardSetId();
		final VerificationData verificationData = verificationDataMap.get(verificationCardSetId);
		final String verificationCardId = verificationData.verificationCardIds().get(0);
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		final ElGamalMultiRecipientPrivateKey verificationCardSecretKey = new ElGamalMultiRecipientPrivateKey(
				GroupVector.of(verificationData.verificationCardSecretKeys().getFirst()));
		final ElGamalMultiRecipientPublicKey verificationCardPublicKey = ElGamalMultiRecipientKeyPair.from(verificationCardSecretKey,
						gqGroup.getGenerator())
				.getPublicKey();
		verificationCardService.save(new VerificationCard(verificationCardId, verificationCardSetId, verificationCardPublicKey));

		return new VoteData(contextIds, primesMappingTable, encodedVotingOptions, ccrEncryptionKeyPairs, verificationData);
	}

	/**
	 * Prepares the request to the LCCShareProcessor.
	 * <p>
	 * The algorithms that need to be executed are:
	 *     <ol>
	 *         <li>CreateVote</li>
	 *         <li>PartialDecryptPCC</li>
	 *     </ol>
	 * </p>
	 *
	 * @param voteData   the vote data to be processed.
	 * @param selections the indexes of the voting options to be selected.
	 * @return the request message as a byte array
	 * @throws JsonProcessingException if the request message cannot be written as bytes
	 */
	private ImmutableByteArray prepareRequestPayload(final VoteData voteData, final int... selections) throws JsonProcessingException {
		final ContextIds contextIds = voteData.contextIds;

		// CreateVote
		final PrimesMappingTable primesMappingTable = voteData.primesMappingTable;
		final GroupVector<PrimeGqElement, GqGroup> encodedVotingOptions = voteData.encodedVotingOptions;
		final ImmutableList<PrimeGqElement> selectedEncodedVotingOptionsList = Arrays.stream(selections)
				.mapToObj(encodedVotingOptions::get)
				.collect(toImmutableList());
		final GroupVector<PrimeGqElement, GqGroup> selectedEncodedVotingOptions = GroupVector.from(selectedEncodedVotingOptionsList);
		final ImmutableList<ElGamalMultiRecipientKeyPair> ccrEncryptionKeyPairs = voteData.ccrEncryptionKeyPairs;
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccrEncryptionPublicKeys = ccrEncryptionKeyPairs.stream()
				.map(ElGamalMultiRecipientKeyPair::getPublicKey)
				.collect(GroupVector.toGroupVector());
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys = Stream.generate(
						() -> elGamalGenerator.genRandomKeyPair(DELTA_SUP))
				.limit(ControlComponentNode.ids().size())
				.map(ElGamalMultiRecipientKeyPair::getPublicKey)
				.collect(GroupVector.toGroupVector());
		final SetupComponentPublicKeys setupComponentPublicKeys = new SetupComponentPublicKeysPayloadGenerator(gqGroup).generate(
						ccrEncryptionPublicKeys, ccmElectionPublicKeys)
				.getSetupComponentPublicKeys();
		final ElGamalMultiRecipientPublicKey electionPublicKey = setupComponentPublicKeys.electionPublicKey();
		final VerificationData verificationData = voteData.verificationData;
		final EncryptedVerifiableVote vote = createVote(contextIds, selectedEncodedVotingOptions, electionPublicKey, ccrEncryptionKeyPairs,
				verificationData.verificationCardSecretKeys.getFirst(), primesMappingTable);
		encryptedVerifiableVoteService.save(vote);

		// Save SetupComponentPublicKeys.
		final String electionEventId = contextIds.electionEventId();
		setupComponentPublicKeysService.save(electionEventId, setupComponentPublicKeys);
		final GroupVector<SchnorrProof, ZqGroup> schnorrProofs = IntStream.range(0, ccrEncryptionKeyPairs.get(0).size())
				.mapToObj(i -> new SchnorrProof(zqGroupGenerator.genRandomZqElementMember(), zqGroupGenerator.genRandomZqElementMember()))
				.collect(toGroupVector());
		final GenKeysCCROutput genKeysCCROutput = new GenKeysCCROutput(ccrEncryptionKeyPairs.get(0), zqGroupGenerator.genRandomZqElementMember(),
				schnorrProofs);
		ccrjReturnCodesKeysService.save(electionEventId, genKeysCCROutput);

		// Save extracted election event hash, which is normally done in the UploadSetupComponentPublicKeysProcessor
		extractedElectionEventHashService.computeAndSave(electionEventId);

		// PartialDecryptPCC
		final ImmutableList<ControlComponentPartialDecryptPayload> controlComponentPartialDecryptPayloads = genControlComponentPartialDecryptPayloads(
				contextIds, primesMappingTable, ccrEncryptionKeyPairs, vote);

		final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC1 = controlComponentPartialDecryptPayloads.get(0)
				.getPartiallyDecryptedEncryptedPCC();
		partiallyDecryptedPCCService.save(partiallyDecryptedEncryptedPCC1);

		// Combine partial decrypt payloads to prepare request
		final CombinedControlComponentPartialDecryptPayload combinedControlComponentPartialDecryptPayload = new CombinedControlComponentPartialDecryptPayload(
				controlComponentPartialDecryptPayloads);

		return new ImmutableByteArray(objectMapper.writeValueAsBytes(combinedControlComponentPartialDecryptPayload));
	}

	/**
	 * Generates the partial choice return codes.
	 * <p>
	 * This is a reduced implementation of the GenVerDat algorithm which omits the parts not needed in this context.
	 * </p>
	 *
	 * @param random             the {@link Random} instance to be used.
	 * @param N_E                the number of voters.
	 * @param p_tilde            the encoded voting options.
	 * @param primesMappingTable the primes mapping table.
	 * @param ee                 the election event id.
	 * @return the verification data, containing verification card ids, partial choice return codes, pcc allow list, and verification card secret keys
	 */
	@SuppressWarnings("java:S117")
	private VerificationData genPcc(final Random random, final int N_E, final GroupVector<PrimeGqElement, GqGroup> p_tilde,
			final PrimesMappingTable primesMappingTable, final String ee) {
		final Hash hash = HashFactory.createHash();
		final Base64 base64 = BaseEncodingFactory.createBase64();

		final int n = p_tilde.size();

		final ImmutableList<String> tau = primesMappingTableAlgorithms.getCorrectnessInformation(primesMappingTable, ImmutableList.emptyList());

		// Algorithm.
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final ImmutableList<VerificationData> verificationData = IntStream.range(0, N_E).parallel()
				.mapToObj(id -> {
					final String vc_id = uuidGenerator.generate();
					final ElGamalMultiRecipientKeyPair K_id_k_id = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, 1, random);

					// Compute hpCC_id.
					final List<GqElement> pCC_id_elements = new ArrayList<>();
					final ZqElement k_id = K_id_k_id.getPrivateKey().get(0);
					final List<String> L_pCC_id = new ArrayList<>();
					for (int k = 0; k < n; k++) {
						final PrimeGqElement p_k_tilde = p_tilde.get(k);
						final GqElement pCC_id_k = p_k_tilde.exponentiate(k_id);
						final GqElement hpCC_id_k = hash.hashAndSquare(pCC_id_k.getValue(), gqGroup);
						final String ci = tau.get(k);
						final ImmutableByteArray lpCC_id_k = hash.recursiveHash(hpCC_id_k, HashableString.from(vc_id), HashableString.from(ee),
								HashableString.from(ci));
						L_pCC_id.add(base64.base64Encode(lpCC_id_k));

						pCC_id_elements.add(pCC_id_k);
					}

					return new VerificationData(ImmutableList.of(vc_id), GroupVector.from(pCC_id_elements), ImmutableList.from(L_pCC_id),
							GroupVector.of(k_id));
				})
				.collect(toImmutableList());

		final ImmutableList<String> vc_ids = verificationData.stream()
				.flatMap(veDa -> veDa.verificationCardIds.stream())
				.collect(toImmutableList());
		final GroupVector<GqElement, GqGroup> pCC_elements = verificationData.stream().flatMap(veDa -> veDa.partialChoiceReturnCodes.stream())
				.collect(GroupVector.toGroupVector());
		final ImmutableList<String> L_pCC = verificationData.stream()
				.flatMap(v -> v.pccAllowList.stream())
				.sorted()
				.collect(toImmutableList());
		final GroupVector<ZqElement, ZqGroup> k = verificationData.stream().flatMap(veDa -> veDa.verificationCardSecretKeys.stream())
				.collect(GroupVector.toGroupVector());

		return new VerificationData(vc_ids, pCC_elements, L_pCC, k);
	}

	/**
	 * Generates the encrypted verifiable vote from the selected voting options.
	 *
	 * @param contextIds                   the context ids (ee, vcs, vc_id).
	 * @param selectedVotingOptions        (p_hat_0, ..., p_hat_psi_minus_one), the selected voting options.
	 * @param ccrEncryptionKeyPairs        (pk_CCR_i, sk_CCR_i), the choice return codes encryption key pairs.
	 * @param verificationCardSetSecretKey the verification card set secret key.
	 * @param primesMappingTable           pTable, the primes mapping table.
	 * @return the {@link EncryptedVerifiableVote}
	 */
	@SuppressWarnings("java:S117")
	private EncryptedVerifiableVote createVote(final ContextIds contextIds,
			final GroupVector<PrimeGqElement, GqGroup> selectedVotingOptions, final ElGamalMultiRecipientPublicKey electionPublicKey,
			final ImmutableList<ElGamalMultiRecipientKeyPair> ccrEncryptionKeyPairs, final ZqElement verificationCardSetSecretKey,
			final PrimesMappingTable primesMappingTable) {
		final ZeroKnowledgeProof zkp = ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
		final ZqGroup zqGroup = ZqGroup.sameOrderAs(gqGroup);
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();
		final GqElement rho = selectedVotingOptions.stream().map(p -> GqElement.GqElementFactory.fromValue(p.getValue(), gqGroup))
				.reduce(gqGroup.getIdentity(),
						GqElement::multiply);
		final ZqElement r = ZqElement.create(random.genRandomInteger(gqGroup.getQ()), zqGroup);
		final ElGamalMultiRecipientMessage m = new ElGamalMultiRecipientMessage(GroupVector.of(rho));

		final ElGamalMultiRecipientCiphertext e1 = elGamal.getCiphertext(m, r, electionPublicKey);
		final GroupVector<GqElement, GqGroup> pccId = selectedVotingOptions.stream().map(p -> p.exponentiate(verificationCardSetSecretKey))
				.collect(GroupVector.toGroupVector());
		final ZqElement rPrime = ZqElement.create(random.genRandomInteger(gqGroup.getQ()), zqGroup);
		final ElGamalMultiRecipientPublicKey ccrEncryptionPublicKey = elGamal.combinePublicKeys(
				ccrEncryptionKeyPairs.stream().map(ElGamalMultiRecipientKeyPair::getPublicKey).collect(GroupVector.toGroupVector()));
		final ElGamalMultiRecipientCiphertext e2 = elGamal.getCiphertext(new ElGamalMultiRecipientMessage(pccId), rPrime,
				ccrEncryptionPublicKey);

		final ElGamalMultiRecipientCiphertext e1Tilde = ElGamalMultiRecipientCiphertext.create(e1.getGamma(), GroupVector.of(e1.getPhis().getFirst()))
				.getCiphertextExponentiation(verificationCardSetSecretKey);
		final GqElement e2TildePhi = e2.getPhis().stream().reduce(gqGroup.getIdentity(), GqElement::multiply);
		final ElGamalMultiRecipientCiphertext e2Tilde = ElGamalMultiRecipientCiphertext.create(e2.getGamma(), GroupVector.of(e2TildePhi));

		final GqElement publicKId = gqGroup.getGenerator().exponentiate(verificationCardSetSecretKey);

		final GroupVector<PrimesMappingTableEntry, GqGroup> pTable = primesMappingTable.pTable();
		final ImmutableList<String> sigma = pTable.stream()
				.map(PrimesMappingTableEntry::semanticInformation)
				.collect(toImmutableList());
		final ImmutableList<String> v_tilde = pTable.stream()
				.map(PrimesMappingTableEntry::actualVotingOption)
				.collect(toImmutableList());
		final GroupVector<PrimeGqElement, GqGroup> p_tilde = pTable.stream()
				.map(PrimesMappingTableEntry::encodedVotingOption)
				.collect(GroupVector.toGroupVector());

		final AuxiliaryInformation iAux = AuxiliaryInformation.from(Streams.concat(
				Stream.of("CreateVote"),
				Stream.of(electionEventId),
				Stream.of(verificationCardId),
				electionPublicKey.stream().map(GroupElement::getValue).map(Conversions::integerToString),
				e1.getPhis().stream().map(GroupElement::getValue).map(Conversions::integerToString),
				Stream.of("EncodedVotingOptions"),
				p_tilde.stream().map(p_tilde_i -> integerToString(p_tilde_i.getValue())),
				Stream.of("ActualVotingOptions"),
				v_tilde.stream(),
				Stream.of("SemanticInformation"),
				sigma.stream()
		).collect(toImmutableList()));

		final ExponentiationProof piExp = zkp.genExponentiationProof(
				GroupVector.from(ImmutableList.of(gqGroup.getGenerator(), e1.getGamma(), e1.getPhis().getFirst())),
				verificationCardSetSecretKey,
				GroupVector.from(ImmutableList.of(publicKId, e1Tilde.getGamma(), e1Tilde.getPhis().getFirst())),
				iAux);

		final GqElement pkCCRTilde = ccrEncryptionPublicKey.stream().limit(selectedVotingOptions.size())
				.reduce(gqGroup.getIdentity(), GqElement::multiply);

		final PlaintextEqualityProof piEqEnc = zkp.genPlaintextEqualityProof(e1Tilde, e2Tilde, electionPublicKey.get(0), pkCCRTilde,
				GroupVector.from(ImmutableList.of(r.multiply(verificationCardSetSecretKey), rPrime)), iAux);

		return new EncryptedVerifiableVote(new ContextIds(electionEventId, verificationCardSetId, verificationCardId), e1, e1Tilde, e2, piExp,
				piEqEnc);
	}

	/**
	 * Generates the list of partial decrypt payloads.
	 * <p>
	 * The payload containing all four partial decrypt payloads represents the message sent to the LCCShareProcessor, which will call the
	 * {@code decryptPCC} and {@code createLCCShare} algorithms.
	 * </p>
	 *
	 * @param contextIds                              the context ids (ee, vcs, vc_id)
	 * @param primesMappingTable                      pTable, the primes mapping table.
	 * @param ccrjChoiceReturnCodesEncryptionKeyPairs (pk_CCR, sk_CCR), the choice return codes encryption key pairs of the four CCs.
	 * @param vote                                    an {@link EncryptedVerifiableVote}
	 * @return the list of {@link ControlComponentPartialDecryptPayload}s
	 */
	private ImmutableList<ControlComponentPartialDecryptPayload> genControlComponentPartialDecryptPayloads(final ContextIds contextIds,
			final PrimesMappingTable primesMappingTable, final ImmutableList<ElGamalMultiRecipientKeyPair> ccrjChoiceReturnCodesEncryptionKeyPairs,
			final EncryptedVerifiableVote vote) {
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();
		final ElGamalMultiRecipientCiphertext encryptedVote = vote.encryptedVote();
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote = vote.exponentiatedEncryptedVote();
		final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes = vote.encryptedPartialChoiceReturnCodes();
		verificationCardStateService.setPartiallyDecrypted(verificationCardId);

		final int psi = primesMappingTableAlgorithms.getPsi(primesMappingTable);
		final int delta = primesMappingTableAlgorithms.getDelta(primesMappingTable);

		return IntStream.range(1, 5)
				.mapToObj(nodeId -> {
					final VerificationCardStateService verificationCardStateServiceMock = mock(VerificationCardStateService.class);
					when(verificationCardStateServiceMock.isNotPartiallyDecrypted(verificationCardId)).thenReturn(true);
					final PartialDecryptPCCAlgorithm partialDecryptPCCAlgorithm = new PartialDecryptPCCAlgorithm(
							ZeroKnowledgeProofFactory.createZeroKnowledgeProof(), verificationCardStateServiceMock,
							getHashExtractedElectionEventService);

					final DecryptPCCContext partialDecryptPccContext = new DecryptPCCContext.Builder()
							.setVerificationCardId(verificationCardId)
							.setNodeId(nodeId)
							.setNumberOfSelections(psi)
							.setNumberOfWriteInsPlusOne(delta)
							.setEncryptionGroup(gqGroup)
							.setElectionEventId(electionEventId)
							.build();

					final ElGamalMultiRecipientKeyPair ccrjChoiceReturnCodesEncryptionKeyPair = ccrjChoiceReturnCodesEncryptionKeyPairs.get(
							nodeId - 1);
					final ElGamalMultiRecipientPublicKey ccrjChoiceReturnCodesEncryptionPublicKey = ccrjChoiceReturnCodesEncryptionKeyPair.getPublicKey();
					final ElGamalMultiRecipientPrivateKey ccrjChoiceReturnCodesEncryptionPrivateKey = ccrjChoiceReturnCodesEncryptionKeyPair.getPrivateKey();

					final PartialDecryptPCCInput partialDecryptPCCInput = new PartialDecryptPCCInput.Builder()
							.setEncryptedVote(encryptedVote)
							.setExponentiatedEncryptedVote(exponentiatedEncryptedVote)
							.setEncryptedPartialChoiceReturnCodes(encryptedPartialChoiceReturnCodes)
							.setCcrjChoiceReturnCodesEncryptionPublicKey(ccrjChoiceReturnCodesEncryptionPublicKey)
							.setCcrjChoiceReturnCodesEncryptionSecretKey(ccrjChoiceReturnCodesEncryptionPrivateKey)
							.build();

					final PartialDecryptPCCOutput partialDecryptPCCOutput = partialDecryptPCCAlgorithm.partialDecryptPCC(partialDecryptPccContext,
							partialDecryptPCCInput);
					return new PartiallyDecryptedEncryptedPCC(contextIds, nodeId, partialDecryptPCCOutput.exponentiatedGammas(),
							partialDecryptPCCOutput.exponentiationProofs());
				})
				.map(partiallyDecryptedEncryptedPCC -> {
					final ControlComponentPartialDecryptPayload partialDecryptPayload = new ControlComponentPartialDecryptPayload(gqGroup,
							partiallyDecryptedEncryptedPCC);
					try {
						final int nodeId = partialDecryptPayload.getPartiallyDecryptedEncryptedPCC().nodeId();
						final TestSigner ccSigner = new TestSigner(KEYSTORE_FILENAME_PATH, KEYSTORE_PASSWORD_FILENAME_PATH,
								Alias.getControlComponentByNodeId(nodeId));
						ccSigner.sign(partialDecryptPayload,
								ChannelSecurityContextData.controlComponentPartialDecrypt(nodeId, electionEventId, verificationCardSetId,
										verificationCardId));
						return partialDecryptPayload;
					} catch (final IOException | SignatureException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(toImmutableList());
	}

	record VoteData(ContextIds contextIds,
					PrimesMappingTable primesMappingTable,
					GroupVector<PrimeGqElement, GqGroup> encodedVotingOptions,
					ImmutableList<ElGamalMultiRecipientKeyPair> ccrEncryptionKeyPairs,
					VerificationData verificationData) {

	}

	record VerificationData(ImmutableList<String> verificationCardIds, GroupVector<GqElement, GqGroup> partialChoiceReturnCodes,
							ImmutableList<String> pccAllowList,
							GroupVector<ZqElement, ZqGroup> verificationCardSecretKeys) {

	}

}
