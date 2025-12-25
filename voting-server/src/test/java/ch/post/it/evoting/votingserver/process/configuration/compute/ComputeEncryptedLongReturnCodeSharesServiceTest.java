/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.compute;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.internal.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.domain.configuration.setupvoting.ComputingStatus;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.idempotence.IdempotentExecutionRepository;
import ch.post.it.evoting.votingserver.messaging.MessageHandler;
import ch.post.it.evoting.votingserver.messaging.Serializer;
import ch.post.it.evoting.votingserver.process.configuration.EncLongCodeShareRepository;

@DisplayName("ComputeEncryptedLongReturnCodeSharesService calling")
class ComputeEncryptedLongReturnCodeSharesServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final IdempotentExecutionRepository IDEMPOTENT_EXECUTION_REPOSITORY = mock(IdempotentExecutionRepository.class);

	private static MessageHandler messageHandler;
	private static EncLongCodeShareRepository encLongCodeShareRepository;
	private static ComputeEncryptedLongReturnCodeSharesService computeEncryptedLongReturnCodeSharesService;

	private String electionEventId;
	private String verificationCardSetId;
	private int chunkCount;
	private SetupComponentVerificationDataPayload setupComponentVerificationDataPayload;

	@BeforeAll
	static void setupAll() {
		messageHandler = mock(MessageHandler.class);
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final IdempotenceService<IdempotenceContext> idempotenceService = new IdempotenceService<>(HashService.getInstance(),
				IDEMPOTENT_EXECUTION_REPOSITORY
		);
		encLongCodeShareRepository = mock(EncLongCodeShareRepository.class);
		final QueuedComputeChunkIdsService queuedComputeChunkIdsService = Mockito.mock(QueuedComputeChunkIdsService.class);
		computeEncryptedLongReturnCodeSharesService = new ComputeEncryptedLongReturnCodeSharesService(messageHandler, idempotenceService,
				encLongCodeShareRepository, queuedComputeChunkIdsService, new Serializer(objectMapper));
	}

	@BeforeEach
	void setup() {
		reset(encLongCodeShareRepository);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		final int chunkId = 0;
		chunkCount = 5;
		final ImmutableList<String> partialChoiceReturnCodes = Stream.generate(
						() -> random.genRandomString(SHORT_CHOICE_RETURN_CODE_LENGTH, base64Alphabet))
				.limit(5)
				.collect(toImmutableList());
		final GqGroup gqGroup = GroupTestData.getGqGroup();
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final String verificationCardId = uuidGenerator.generate();
		final ElGamalMultiRecipientCiphertext encryptedHashedSquaredConfirmationKey = elGamalGenerator.genRandomCiphertext(1);
		final ElGamalMultiRecipientCiphertext encryptedHashedSquaredPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(5);
		final ElGamalMultiRecipientPublicKey verificationCardPublicKey = elGamalGenerator.genRandomPublicKey(1);
		final SetupComponentVerificationData setupComponentVerificationData = new SetupComponentVerificationData(verificationCardId,
				verificationCardPublicKey, encryptedHashedSquaredPartialChoiceReturnCodes, encryptedHashedSquaredConfirmationKey);
		setupComponentVerificationDataPayload = new SetupComponentVerificationDataPayload(gqGroup, electionEventId, verificationCardSetId, chunkId,
				partialChoiceReturnCodes, ImmutableList.of(setupComponentVerificationData));
	}

	@DisplayName("onRequest with null arguments throws a NullPointerException")
	@Test
	void onRequestWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> computeEncryptedLongReturnCodeSharesService.onRequest(null));
	}

	@DisplayName("onRequest with valid arguments does not throw")
	@Test
	void computeGenEncLongCodeSharesWithValidArgumentsDoesNotThrow() {
		assertDoesNotThrow(() -> computeEncryptedLongReturnCodeSharesService.onRequest(setupComponentVerificationDataPayload));
		verify(messageHandler, times(1)).sendMessage(any());
	}

	@DisplayName("getComputingStatus with null arguments throws a NullPointerException")
	@Test
	void getEncLongCodeSharesComputingStatusWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> computeEncryptedLongReturnCodeSharesService.getComputingStatus(null, verificationCardSetId, chunkCount));
		assertThrows(NullPointerException.class,
				() -> computeEncryptedLongReturnCodeSharesService.getComputingStatus(electionEventId, null, chunkCount));
	}

	@DisplayName("getComputingStatus with non UUID arguments throws a FailedValidationException")
	@Test
	void getEncLongCodeSharesComputingStatusWithNonUuidArgumentsThrows() {
		assertThrows(FailedValidationException.class,
				() -> computeEncryptedLongReturnCodeSharesService.getComputingStatus("nonUUID", verificationCardSetId, chunkCount));
		assertThrows(FailedValidationException.class,
				() -> computeEncryptedLongReturnCodeSharesService.getComputingStatus(electionEventId, "nonUUID", chunkCount));
	}

	@DisplayName("getComputingStatus with chunk id smaller than zero throws an IllegalArgumentException")
	@Test
	void getEncLongCodeSharesComputingStatusWithChunkCountSmallerZeroThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> computeEncryptedLongReturnCodeSharesService.getComputingStatus(electionEventId, verificationCardSetId, -1));
	}

	@DisplayName("getComputingStatus when actual count is smaller than expected count returns COMPUTING")
	@Test
	void getEncLongCodeSharesComputingStatusWhenActualCountSmallerThanExpectedCountReturnsComputing() {
		doReturn(19L).when(encLongCodeShareRepository).countByVerificationCardSetId(verificationCardSetId);
		final ComputingStatus computingStatus = assertDoesNotThrow(
				() -> computeEncryptedLongReturnCodeSharesService.getComputingStatus(electionEventId, verificationCardSetId, chunkCount));
		assertEquals(ComputingStatus.COMPUTING, computingStatus);
	}

	@DisplayName("getComputingStatus when actual count is equal to expected count returns COMPUTED")
	@Test
	void getEncLongCodeSharesComputingStatusWhenActualCountEqualToExpectedCountReturnsComputed() {
		doReturn(20L).when(encLongCodeShareRepository).countByVerificationCardSetId(verificationCardSetId);
		final ComputingStatus computingStatus = assertDoesNotThrow(
				() -> computeEncryptedLongReturnCodeSharesService.getComputingStatus(electionEventId, verificationCardSetId, chunkCount));
		assertEquals(ComputingStatus.COMPUTED, computingStatus);
	}

	@DisplayName("getComputingStatus when actual count is greater than expected count returns COMPUTING_ERROR")
	@Test
	void getEncLongCodeSharesComputingStatusWhenActualCountGreaterThanExpectedCountReturnsComputingError() {
		doReturn(21L).when(encLongCodeShareRepository).countByVerificationCardSetId(verificationCardSetId);
		final ComputingStatus computingStatus = assertDoesNotThrow(
				() -> computeEncryptedLongReturnCodeSharesService.getComputingStatus(electionEventId, verificationCardSetId, chunkCount));
		assertEquals(ComputingStatus.COMPUTING_ERROR, computingStatus);
	}
}
