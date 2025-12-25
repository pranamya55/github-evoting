/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.configuration.VerificationCardKeystore;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;

@DisplayName("SetupComponentVerificationCardKeystoreService")
@ExtendWith(MockitoExtension.class)
class SetupComponentVerificationCardKeystoreServiceTest {
	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
	private static final String DUMMY_KEYSTORE = "%s=".formatted(random.genRandomString(571, base64Alphabet));

	private SetupComponentVerificationCardKeystoreRepository setupComponentVerificationCardKeystoreRepository;
	private ElectionEventContextService electionEventContextService;
	private SetupComponentVerificationCardKeystoreService setupComponentVerificationCardKeystoreService;

	@BeforeEach
	void setUp() {
		setupComponentVerificationCardKeystoreRepository = Mockito.mock(SetupComponentVerificationCardKeystoreRepository.class);
		electionEventContextService = Mockito.mock(ElectionEventContextService.class);
		final ElectionEventService electionEventService = Mockito.mock(ElectionEventService.class);
		final VerificationCardService verificationCardSetService = mock(VerificationCardService.class);
		final IdentifierValidationService identifierValidationService = mock(IdentifierValidationService.class);

		doNothing().when(identifierValidationService).validateContextIds(any());

		setupComponentVerificationCardKeystoreService = new SetupComponentVerificationCardKeystoreService(objectMapper,
				verificationCardSetService, setupComponentVerificationCardKeystoreRepository, electionEventContextService, electionEventService,
				identifierValidationService);
	}

	@ParameterizedTest
	@MethodSource("loadVerificationCardKeystoreDoesNotThrowIfInValidPeriodProvider")
	void loadVerificationCardKeystoreDoesNotThrowIfInValidPeriod(final LocalDateTime now, final LocalDateTime start, final LocalDateTime finish)
			throws JsonProcessingException {
		setupCommonMocks(start, finish);
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();

		final var setupComponentVerificationCardKeystoreEntity = new SetupComponentVerificationCardKeystoreEntity(
				new VerificationCardEntity(),
				new ImmutableByteArray(objectMapper.writeValueAsBytes(new VerificationCardKeystore(verificationCardId, DUMMY_KEYSTORE))));
		when(setupComponentVerificationCardKeystoreRepository.findById(any())).thenReturn(Optional.of(setupComponentVerificationCardKeystoreEntity));

		assertDoesNotThrow(
				() -> setupComponentVerificationCardKeystoreService.loadVerificationCardKeystore(electionEventId, verificationCardSetId,
						verificationCardId, () -> now));
	}

	public static Stream<Arguments> loadVerificationCardKeystoreDoesNotThrowIfInValidPeriodProvider() {
		final LocalDateTime now = LocalDateTime.of(2022, 1, 1, 15, 0);
		return Stream.of(
				Arguments.of(now, now.minusDays(5), now.plusDays(5)),
				Arguments.of(now, now, now)
		);
	}

	@ParameterizedTest
	@MethodSource("loadVerificationCardKeystoreThrowsIfInInvalidPeriodProvider")
	void loadVerificationCardKeystoreThrowsIfInInvalidPeriod(final LocalDateTime now, final LocalDateTime start, final LocalDateTime finish) {
		setupCommonMocks(start, finish);
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> setupComponentVerificationCardKeystoreService.loadVerificationCardKeystore(electionEventId, verificationCardSetId,
						verificationCardId, () -> now));

		final String errorMessage = String.format(
				"Cannot load verification card keystore outside the opened election time window. [electionEventId: %s, verificationCardSetId: %s, verificationCardId: %s, startTime: %s, finishTime: %s]",
				electionEventId, verificationCardSetId, verificationCardId, start, finish);
		assertEquals(errorMessage, Throwables.getRootCause(illegalStateException).getMessage());
	}

	public static Stream<Arguments> loadVerificationCardKeystoreThrowsIfInInvalidPeriodProvider() {
		final LocalDateTime now = LocalDateTime.of(2022, 1, 1, 15, 0);
		return Stream.of(
				Arguments.of(now, now.minusDays(10), now.minusDays(5)),
				Arguments.of(now, now.plusSeconds(1), now.plusDays(10)),
				Arguments.of(now, now.minusDays(10), now.minusSeconds(1))
		);
	}

	private void setupCommonMocks(final LocalDateTime start, final LocalDateTime finish) {
		final var electionEventContextEntityMock = mock(ElectionEventContextEntity.class);
		when(electionEventContextEntityMock.getStartTime()).thenReturn(start);
		when(electionEventContextEntityMock.getFinishTime()).thenReturn(finish);
		when(electionEventContextService.getElectionEventContextEntity(any())).thenReturn(electionEventContextEntityMock);
	}
}
