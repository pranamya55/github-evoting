/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_VOTER_INITIAL_CODES_PAYLOAD;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodes;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodesPayload;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A VoterInitialCodesPayloadService")
class VoterInitialCodesPayloadServiceTest {
	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static final String NON_EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String EXISTING_VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String INVALID_ID = "invalidId";
	private static final int NUMBER_OF_VOTER_INITIAL_CODES = 3;

	private static ObjectMapper objectMapper;
	private static SetupPathResolver pathResolver;
	private static VoterInitialCodesPayloadService voterInitialCodesPayloadService;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		objectMapper = DomainObjectMapper.getNewInstance();

		final SetupPathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final VoterInitialCodesPayloadFileRepository voterInitialCodesPayloadFileRepository =
				new VoterInitialCodesPayloadFileRepository(objectMapper, pathResolver);

		final VoterInitialCodesPayload voterInitialCodesPayload = validVoterInitialCodesPayload();
		voterInitialCodesPayloadFileRepository.save(voterInitialCodesPayload, EXISTING_VERIFICATION_CARD_SET_ID);

		voterInitialCodesPayloadService = new VoterInitialCodesPayloadService(voterInitialCodesPayloadFileRepository);
	}

	@SuppressWarnings("java:S117")
	private static VoterInitialCodesPayload validVoterInitialCodesPayload() {
		final ImmutableList<String> voterIdentifications = random.genUniqueDecimalStrings(8, NUMBER_OF_VOTER_INITIAL_CODES);
		final ImmutableList<String> UUIDs = ImmutableList.of(
				uuidGenerator.generate(),
				uuidGenerator.generate(),
				uuidGenerator.generate());
		final ImmutableList<String> SVKs = ImmutableList.of(
				ElectionSetupUtils.genStartVotingKey(),
				ElectionSetupUtils.genStartVotingKey(),
				ElectionSetupUtils.genStartVotingKey());
		final ImmutableList<String> extendedAuthenticationFactor = random.genUniqueDecimalStrings(8, NUMBER_OF_VOTER_INITIAL_CODES);
		final ImmutableList<String> BCKs = random.genUniqueDecimalStrings(9, NUMBER_OF_VOTER_INITIAL_CODES);

		final ImmutableList<VoterInitialCodes> voterInitialCodes = IntStream.range(0, NUMBER_OF_VOTER_INITIAL_CODES)
				.mapToObj(i -> new VoterInitialCodes(
						voterIdentifications.get(i), UUIDs.get(i), UUIDs.get(i), SVKs.get(i), extendedAuthenticationFactor.get(i), BCKs.get(i))
				).collect(toImmutableList());

		return new VoterInitialCodesPayload(EXISTING_ELECTION_EVENT_ID, EXISTING_VERIFICATION_CARD_SET_ID, voterInitialCodes);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private VoterInitialCodesPayload voterInitialCodesPayload;

		private VoterInitialCodesPayloadService voterInitialCodesPayloadServiceTemp;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

			final VoterInitialCodesPayloadFileRepository voterInitialCodesPayloadFileRepositoryTemp =
					new VoterInitialCodesPayloadFileRepository(objectMapper, pathResolver);

			voterInitialCodesPayloadServiceTemp = new VoterInitialCodesPayloadService(voterInitialCodesPayloadFileRepositoryTemp);
		}

		@BeforeEach
		void setUp() {
			voterInitialCodesPayload = validVoterInitialCodesPayload();
		}

		@Test
		@DisplayName("a valid payload does not throw")
		void saveValidPayload() {
			assertDoesNotThrow(() -> voterInitialCodesPayloadServiceTemp.save(voterInitialCodesPayload, EXISTING_VERIFICATION_CARD_SET_ID));

			assertTrue(Files.exists(pathResolver.resolveVerificationCardSetPath(EXISTING_ELECTION_EVENT_ID, EXISTING_VERIFICATION_CARD_SET_ID)
					.resolve(CONFIG_FILE_NAME_VOTER_INITIAL_CODES_PAYLOAD)));
		}

		@Test
		@DisplayName("a null parameters throws")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> voterInitialCodesPayloadServiceTemp.save(null, EXISTING_VERIFICATION_CARD_SET_ID));
			assertThrows(NullPointerException.class, () -> voterInitialCodesPayloadServiceTemp.save(voterInitialCodesPayload, null));
		}
	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_METHOD)
	class LoadTest {

		@Test
		@DisplayName("existing election event and verification card set returns expected voter initial codes payload")
		void loadExistingElectionEventValidSignature() {
			final VoterInitialCodesPayload voterInitialCodesPayload = voterInitialCodesPayloadService.load(EXISTING_ELECTION_EVENT_ID,
					EXISTING_VERIFICATION_CARD_SET_ID);

			assertEquals(EXISTING_ELECTION_EVENT_ID, voterInitialCodesPayload.electionEventId());
		}

		@Test
		@DisplayName("null input throws NullPointerException")
		void loadNullInput() {
			assertThrows(NullPointerException.class, () -> voterInitialCodesPayloadService.load(null, EXISTING_VERIFICATION_CARD_SET_ID));
			assertThrows(NullPointerException.class, () -> voterInitialCodesPayloadService.load(EXISTING_ELECTION_EVENT_ID, null));
		}

		@Test
		@DisplayName("invalid input throws FailedValidationException")
		void loadInvalidInput() {
			assertThrows(FailedValidationException.class, () -> voterInitialCodesPayloadService.load(INVALID_ID, EXISTING_VERIFICATION_CARD_SET_ID));
			assertThrows(FailedValidationException.class, () -> voterInitialCodesPayloadService.load(EXISTING_ELECTION_EVENT_ID, INVALID_ID));
		}

		@Test
		@DisplayName("existing election event and verification card set but with missing payload throws IllegalStateException")
		void loadMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> voterInitialCodesPayloadService.load(NON_EXISTING_ELECTION_EVENT_ID, EXISTING_VERIFICATION_CARD_SET_ID));

			final String errorMessage = String.format(
					"Requested voter initial codes payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
					NON_EXISTING_ELECTION_EVENT_ID, EXISTING_VERIFICATION_CARD_SET_ID);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

	}

}
