/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponentlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.LongVoteCastReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.votingserver.messaging.InProgressMessage;
import ch.post.it.evoting.votingserver.messaging.InProgressMessageService;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionService;
import ch.post.it.evoting.votingserver.messaging.Serializer;
import ch.post.it.evoting.votingserver.process.BallotBoxRepository;
import ch.post.it.evoting.votingserver.process.BallotBoxService;
import ch.post.it.evoting.votingserver.process.ElectionEventEntity;
import ch.post.it.evoting.votingserver.process.ElectionEventRepository;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.ReturnCodesMappingTableRepository;
import ch.post.it.evoting.votingserver.process.ReturnCodesMappingTableService;
import ch.post.it.evoting.votingserver.process.VerificationCardEntity;
import ch.post.it.evoting.votingserver.process.VerificationCardRepository;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.VerificationCardSetEntity;
import ch.post.it.evoting.votingserver.process.VerificationCardSetRepository;
import ch.post.it.evoting.votingserver.process.VerificationCardSetService;
import ch.post.it.evoting.votingserver.process.VerificationCardStateEntity;
import ch.post.it.evoting.votingserver.process.VerificationCardStateRepository;
import ch.post.it.evoting.votingserver.process.VerificationCardStateService;
import ch.post.it.evoting.votingserver.process.voting.ConfirmationKeyInvalidException;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTableSupplier;
import ch.post.it.evoting.votingserver.protocol.voting.confirmvote.ExtractVCCAlgorithm;
import ch.post.it.evoting.votingserver.protocol.voting.confirmvote.ExtractVCCService;
import ch.post.it.evoting.votingserver.shelf.WorkflowShelfService;

class VoteCastReturnCodeServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
	private static final Serializer serializer = new Serializer(objectMapper);
	private static final String FIRST_CORRELATION_ID = "1234567788990";
	private static final String SECOND_CORRELATION_ID = "9876543210";
	private static VoteCastReturnCodeService voteCastReturnCodeService;
	private static SignatureKeystore<Alias> signatureKeystoreService;
	private static ElectionEventRepository electionEventRepository;
	private static VerificationCardRepository verificationCardRepository;
	private static VerificationCardService verificationCardService;
	private static WorkflowShelfService workflowShelfService;
	private static BallotBoxService ballotBoxService;
	private static ReturnCodesMappingTableRepository returnCodesMappingTableRepository;
	private static MessageHandler messageHandler;
	private static InProgressMessageService inProgressMessageService;

	private static VerificationCardStateService verificationCardStateService;
	private ContextIds contextIds;
	private GqElement confirmationKey;

	@BeforeAll
	static void setupAll() {
		electionEventRepository = mock(ElectionEventRepository.class);
		final ElectionEventService electionEventService = new ElectionEventService(electionEventRepository);
		verificationCardRepository = mock(VerificationCardRepository.class);
		final VerificationCardStateRepository verificationCardStateRepository = mock(VerificationCardStateRepository.class);
		verificationCardStateService = spy(new VerificationCardStateService(verificationCardStateRepository));
		final VerificationCardSetRepository verificationCardSetRepository = mock(VerificationCardSetRepository.class);
		final VerificationCardSetService verificationCardSetService = new VerificationCardSetService(electionEventService,
				verificationCardSetRepository);
		final BallotBoxRepository ballotBoxRepository = mock(BallotBoxRepository.class);
		ballotBoxService = spy(new BallotBoxService(objectMapper, ballotBoxRepository, electionEventService, verificationCardSetService));
		verificationCardService = new VerificationCardService(verificationCardRepository, verificationCardStateService, ballotBoxService);
		signatureKeystoreService = mock(SignatureKeystore.class);

		returnCodesMappingTableRepository = mock(ReturnCodesMappingTableRepository.class);
		final ReturnCodesMappingTableService returnCodesMappingTableService = new ReturnCodesMappingTableService(verificationCardSetService,
				returnCodesMappingTableRepository, 10);
		final ReturnCodesMappingTableSupplier returnCodesMappingTableSupplier = new ReturnCodesMappingTableSupplier(returnCodesMappingTableService);
		final IdentifierValidationService identifierValidationService = mock(IdentifierValidationService.class);
		doNothing().when(identifierValidationService).validateContextIds(any());
		final ExtractVCCAlgorithm extractVCCAlgorithm = new ExtractVCCAlgorithm(HashFactory.createHash(), SymmetricFactory.createSymmetric(),
				KeyDerivationFactory.createKeyDerivation());
		final ExtractVCCService extractVCCService = new ExtractVCCService(extractVCCAlgorithm, electionEventService,
				identifierValidationService, returnCodesMappingTableSupplier);

		messageHandler = mock(MessageHandler.class);

		inProgressMessageService = mock(InProgressMessageService.class);
		workflowShelfService = mock(WorkflowShelfService.class);
		final ResponseCompletionService responseCompletionService = mock(ResponseCompletionService.class);

		voteCastReturnCodeService = new VoteCastReturnCodeService(serializer, messageHandler, extractVCCService, electionEventService,
				workflowShelfService, verificationCardService, signatureKeystoreService, responseCompletionService);
	}

	@BeforeEach
	void setup() throws SignatureException {
		reset(electionEventRepository, verificationCardRepository, signatureKeystoreService, returnCodesMappingTableRepository);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();
		contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);

		final GqGroup encryptionGroup = GroupTestData.getGqGroup();
		final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(encryptionGroup);
		confirmationKey = gqGroupGenerator.genNonIdentityMember();

		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, encryptionGroup);
		doReturn(Optional.of(electionEventEntity)).when(electionEventRepository).findById(electionEventId);

		final VerificationCardStateEntity verificationCardStateEntity = new VerificationCardStateEntity(verificationCardId);

		final String ballotBoxId = uuidGenerator.generate();
		final String credentialId = uuidGenerator.generate();
		final String votingCardId = uuidGenerator.generate();
		final String baseAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity();
		final SetupComponentVoterAuthenticationData voterAuthenticationData = new SetupComponentVoterAuthenticationData(electionEventId,
				verificationCardSetId, ballotBoxId, verificationCardId, votingCardId, credentialId, baseAuthenticationChallenge);
		final VerificationCardEntity verificationCardEntity = new VerificationCardEntity(verificationCardId, verificationCardSetEntity, credentialId,
				votingCardId, voterAuthenticationData, verificationCardStateEntity);
		doReturn(Optional.of(verificationCardEntity)).when(verificationCardRepository).findById(verificationCardId);

		when(signatureKeystoreService.generateSignature(any(), any())).thenReturn(ImmutableByteArray.of((byte) 1, (byte) 2, (byte) 3));

		when(messageHandler.generateCorrelationId()).thenReturn(FIRST_CORRELATION_ID).thenReturn(SECOND_CORRELATION_ID);

		final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload1 = getControlComponenthlVCCPayload(1, encryptionGroup);
		final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload2 = getControlComponenthlVCCPayload(2, encryptionGroup);
		final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload3 = getControlComponenthlVCCPayload(3, encryptionGroup);
		final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload4 = getControlComponenthlVCCPayload(4, encryptionGroup);

		when(messageHandler.sendMessage(any(), eq(FIRST_CORRELATION_ID))).then(onCall -> {
			voteCastReturnCodeService.onResponseLongVoteCastReturnCodesShareHash(FIRST_CORRELATION_ID,
					ImmutableList.of(controlComponenthlVCCSharePayload1,
							controlComponenthlVCCSharePayload2,
							controlComponenthlVCCSharePayload3,
							controlComponenthlVCCSharePayload4));
			return true;
		});

		when(workflowShelfService.pullFromShelf(any(), any())).thenReturn(
				new VoteCastReturnCodeService.ShelfElement(FIRST_CORRELATION_ID, contextIds, encryptionGroup));

		when(inProgressMessageService.getAllNodesResponses(FIRST_CORRELATION_ID)).thenReturn(
				getControlComponenthlVCCInProgressMessages(controlComponenthlVCCSharePayload1, controlComponenthlVCCSharePayload2,
						controlComponenthlVCCSharePayload3,
						controlComponenthlVCCSharePayload4));
	}

	@Test
	@DisplayName("with null arguments throws a NullPointerException")
	void retrieveShortVoteCastCodeWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> voteCastReturnCodeService.retrieveShortVoteCastCode(null, confirmationKey));
		assertThrows(NullPointerException.class, () -> voteCastReturnCodeService.retrieveShortVoteCastCode(contextIds, null));
	}

	@Test
	@DisplayName("with confirmationKey from different group than encryptionGroup throws an IllegalArgumentException")
	void retrieveShortVoteCastCodeWithConfirmationKeyFromBadGroupThrows() {
		final GqGroup differentGqGroup = GroupTestData.getDifferentGqGroup(confirmationKey.getGroup());
		final GqElement badConfirmationKey = new GqGroupGenerator(differentGqGroup).genNonIdentityMember();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> voteCastReturnCodeService.retrieveShortVoteCastCode(contextIds, badConfirmationKey));
		final String expectedErrorMessage = String.format("The encryption group does not match the confirmation key's group. [contextIds: %s]",
				contextIds);
		assertEquals(expectedErrorMessage, exception.getMessage());
	}

	@Test
	@DisplayName("when retrieving ControlComponentlVCCSharePayloads with a null signature throws an IllegalStateException")
	void retrieveShortVoteCastCodeWhenRetrievingControlComponentlVCCSharePayloadWithNullSignatureThrows() throws SignatureException {
		final GqGroup encryptionGroup = confirmationKey.getGroup();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload1 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 1, encryptionGroup,
				new ConfirmationKey(contextIds, confirmationKey), false);
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload2 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 2, encryptionGroup,
				new ConfirmationKey(contextIds, confirmationKey), false);
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload3 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 3, encryptionGroup,
				new ConfirmationKey(contextIds, confirmationKey), false);
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload4 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 4, encryptionGroup,
				new ConfirmationKey(contextIds, confirmationKey), false);

		doReturn(0).when(verificationCardStateService).getNextConfirmationAttemptId(anyString());

		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(true);

		when(messageHandler.sendMessage(any(), eq(SECOND_CORRELATION_ID))).then(onCall -> {
			voteCastReturnCodeService.onResponseLongVoteCastReturnCodesShareVerify(SECOND_CORRELATION_ID, ImmutableList.of(
					controlComponentlVCCSharePayload1,
					controlComponentlVCCSharePayload2,
					controlComponentlVCCSharePayload3,
					controlComponentlVCCSharePayload4));
			return true;
		});

		when(inProgressMessageService.getAllNodesResponses(SECOND_CORRELATION_ID)).thenReturn(
				getControlComponentlVCCShareInProgressMessages(SECOND_CORRELATION_ID, controlComponentlVCCSharePayload1,
						controlComponentlVCCSharePayload2,
						controlComponentlVCCSharePayload3,
						controlComponentlVCCSharePayload4));

		final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
		verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);
		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> voteCastReturnCodeService.retrieveShortVoteCastCode(contextIds, confirmationKey));
		final String expectedErrorMessage = String.format(
				"The signature of the Control Component lVCC Share payload is null. [nodeId: %s, contextIds: %s]", 1, contextIds);
		assertEquals(expectedErrorMessage, exception.getMessage());
	}

	@Test
	@DisplayName("with invalid payload signatures throws an InvalidPayloadSignatureException")
	void retrieveShortVoteCastCodeWithInvalidSignaturesThrows() {
		final GqGroup encryptionGroup = confirmationKey.getGroup();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(encryptionGroup);
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare1 = new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId,
				verificationCardId, 1, gqGroupGenerator.genMember());
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload1 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 1, encryptionGroup, longVoteCastReturnCodeShare1,
				new ConfirmationKey(contextIds, confirmationKey), true,
				new CryptoPrimitivesSignature(ImmutableByteArray.EMPTY));
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare2 = new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId,
				verificationCardId, 2, gqGroupGenerator.genMember());
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload2 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 2, encryptionGroup, longVoteCastReturnCodeShare2,
				new ConfirmationKey(contextIds, confirmationKey), true,
				new CryptoPrimitivesSignature(ImmutableByteArray.EMPTY));
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare3 = new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId,
				verificationCardId, 3, gqGroupGenerator.genMember());
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload3 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 3, encryptionGroup, longVoteCastReturnCodeShare3,
				new ConfirmationKey(contextIds, confirmationKey), true,
				new CryptoPrimitivesSignature(ImmutableByteArray.EMPTY));
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare4 = new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId,
				verificationCardId, 4, gqGroupGenerator.genMember());
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload4 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 4, encryptionGroup, longVoteCastReturnCodeShare4,
				new ConfirmationKey(contextIds, confirmationKey), true,
				new CryptoPrimitivesSignature(ImmutableByteArray.EMPTY));

		when(messageHandler.sendMessage(any(), eq(SECOND_CORRELATION_ID))).then(onCall -> {
			voteCastReturnCodeService.onResponseLongVoteCastReturnCodesShareVerify(SECOND_CORRELATION_ID,
					ImmutableList.of(controlComponentlVCCSharePayload1,
							controlComponentlVCCSharePayload2,
							controlComponentlVCCSharePayload3,
							controlComponentlVCCSharePayload4));
			return true;
		});

		when(inProgressMessageService.getAllNodesResponses(SECOND_CORRELATION_ID)).thenReturn(
				getControlComponentlVCCShareInProgressMessages(SECOND_CORRELATION_ID, controlComponentlVCCSharePayload1,
						controlComponentlVCCSharePayload2,
						controlComponentlVCCSharePayload3,
						controlComponentlVCCSharePayload4));

		final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
		verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);

		doReturn(0).when(verificationCardStateService).getNextConfirmationAttemptId(anyString());

		final InvalidPayloadSignatureException exception = assertThrows(InvalidPayloadSignatureException.class,
				() -> voteCastReturnCodeService.retrieveShortVoteCastCode(contextIds, confirmationKey));

		final String expectedErrorMessage = String.format("Signature of payload %s is invalid. [nodeId: %s, contextIds: %s]",
				ControlComponentlVCCSharePayload.class.getSimpleName(), 1, contextIds);

		assertEquals(expectedErrorMessage, exception.getMessage());
	}

	@Test
	@DisplayName("when Encrypted short Vote Cast Return Code not found then throws an IllegalStateException")
	void retrieveShortVoteCastCodeWhenEncryptedShortVoteCastReturnCodeNotFoundThrows() throws SignatureException {
		final GqGroup encryptionGroup = confirmationKey.getGroup();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(encryptionGroup);
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare1 = new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId,
				verificationCardId, 1, gqGroupGenerator.genMember());
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload1 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 1, encryptionGroup, longVoteCastReturnCodeShare1,
				new ConfirmationKey(contextIds, confirmationKey), true,
				new CryptoPrimitivesSignature(ImmutableByteArray.EMPTY));
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare2 = new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId,
				verificationCardId, 2, gqGroupGenerator.genMember());
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload2 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 2, encryptionGroup, longVoteCastReturnCodeShare2,
				new ConfirmationKey(contextIds, confirmationKey), true,
				new CryptoPrimitivesSignature(ImmutableByteArray.EMPTY));
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare3 = new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId,
				verificationCardId, 3, gqGroupGenerator.genMember());
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload3 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 3, encryptionGroup, longVoteCastReturnCodeShare3,
				new ConfirmationKey(contextIds, confirmationKey), true,
				new CryptoPrimitivesSignature(ImmutableByteArray.EMPTY));
		final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare4 = new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId,
				verificationCardId, 4, gqGroupGenerator.genMember());
		final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload4 = new ControlComponentlVCCSharePayload(electionEventId,
				verificationCardSetId, verificationCardId, 4, encryptionGroup, longVoteCastReturnCodeShare4,
				new ConfirmationKey(contextIds, confirmationKey), true,
				new CryptoPrimitivesSignature(ImmutableByteArray.EMPTY));

		when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(true);

		doReturn(Optional.empty()).when(returnCodesMappingTableRepository).findByHashedLongReturnCode(eq(verificationCardSetId), anyString());

		when(messageHandler.sendMessage(any(), eq(SECOND_CORRELATION_ID))).then(onCall -> {
			voteCastReturnCodeService.onResponseLongVoteCastReturnCodesShareVerify(SECOND_CORRELATION_ID, ImmutableList.of(
					controlComponentlVCCSharePayload1,
					controlComponentlVCCSharePayload2,
					controlComponentlVCCSharePayload3,
					controlComponentlVCCSharePayload4
			));
			return true;
		});

		when(inProgressMessageService.getAllNodesResponses(SECOND_CORRELATION_ID)).thenReturn(
				getControlComponentlVCCShareInProgressMessages(FIRST_CORRELATION_ID, controlComponentlVCCSharePayload1,
						controlComponentlVCCSharePayload2,
						controlComponentlVCCSharePayload3,
						controlComponentlVCCSharePayload4));

		final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
		verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);

		doReturn(0).when(verificationCardStateService).getNextConfirmationAttemptId(anyString());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> voteCastReturnCodeService.retrieveShortVoteCastCode(contextIds, confirmationKey));
		final String expectedErrorMessage = String.format(
				"Encrypted short Vote Cast Return Code not found in CMtable. [electionEventId: %s, verificationCardId: %s]", electionEventId,
				verificationCardId);
		assertEquals(expectedErrorMessage, exception.getMessage());
	}

	@Test
	void testExceptionSerializationDeserialization() throws IOException {
		final ConfirmationKeyInvalidException exception = new ConfirmationKeyInvalidException("blabla", 23);

		final byte[] bytes = objectMapper.writeValueAsBytes(exception);
		assertNotNull(bytes);

		final ConfirmationKeyInvalidException exception2 = objectMapper.readValue(bytes, ConfirmationKeyInvalidException.class);
		assertNotNull(exception2);
	}

	private ControlComponenthlVCCSharePayload getControlComponenthlVCCPayload(final int nodeId, final GqGroup encryptionGroup) {
		final int confirmationAttemptId = 0;
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(ImmutableByteArray.of((byte) 1, (byte) 2, (byte) 3));
		final ConfirmationKey confKey = new ConfirmationKey(contextIds, confirmationKey);
		return new ControlComponenthlVCCSharePayload(encryptionGroup, nodeId, "1234567890", confKey, confirmationAttemptId, signature);
	}

	private static ImmutableList<InProgressMessage> getControlComponentlVCCShareInProgressMessages(
			final String firstCorrelationId,
			final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload1,
			final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload2,
			final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload3,
			final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload4) {
		return ImmutableList.of(
				new InProgressMessage(firstCorrelationId, 1, serializer.serialize(controlComponentlVCCSharePayload1)),
				new InProgressMessage(firstCorrelationId, 2, serializer.serialize(controlComponentlVCCSharePayload2)),
				new InProgressMessage(firstCorrelationId, 3, serializer.serialize(controlComponentlVCCSharePayload3)),
				new InProgressMessage(firstCorrelationId, 4, serializer.serialize(controlComponentlVCCSharePayload4)));

	}

	private static ImmutableList<InProgressMessage> getControlComponenthlVCCInProgressMessages(
			final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload1,
			final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload2,
			final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload3,
			final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload4) {
		return ImmutableList.of(
				new InProgressMessage(VoteCastReturnCodeServiceTest.FIRST_CORRELATION_ID, 1,
						serializer.serialize(controlComponenthlVCCSharePayload1)),
				new InProgressMessage(VoteCastReturnCodeServiceTest.FIRST_CORRELATION_ID, 2,
						serializer.serialize(controlComponenthlVCCSharePayload2)),
				new InProgressMessage(VoteCastReturnCodeServiceTest.FIRST_CORRELATION_ID, 3,
						serializer.serialize(controlComponenthlVCCSharePayload3)),
				new InProgressMessage(VoteCastReturnCodeServiceTest.FIRST_CORRELATION_ID, 4,
						serializer.serialize(controlComponenthlVCCSharePayload4)));
	}
}
