/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_SETUP_COMPONENT_TALLY_DATA_PAYLOAD;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.domain.generators.SetupComponentTallyDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("A SetupComponentTallyDataPayloadService")
class SetupComponentTallyDataPayloadServiceTest {
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final SetupComponentTallyDataPayloadGenerator generator = new SetupComponentTallyDataPayloadGenerator();

	private static final String MISSING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String EXISTING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String INVALID_ID = "invalidId";

	private static ObjectMapper objectMapper;
	private static PathResolver pathResolver;
	private static SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService;
	private static String electionEventId;
	private static String verificationCardSetId;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		objectMapper = DomainObjectMapper.getNewInstance();

		final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final SetupComponentTallyDataPayloadFileRepository setupComponentTallyDataPayloadFileRepository =
				new SetupComponentTallyDataPayloadFileRepository(objectMapper, pathResolver);

		final SetupComponentTallyDataPayload setupComponentTallyDataPayload = generator.generate();
		electionEventId = setupComponentTallyDataPayload.getElectionEventId();
		verificationCardSetId = setupComponentTallyDataPayload.getVerificationCardSetId();
		setupComponentTallyDataPayloadFileRepository.save(setupComponentTallyDataPayload);

		setupComponentTallyDataPayloadService = new SetupComponentTallyDataPayloadService(setupComponentTallyDataPayloadFileRepository);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private SetupComponentTallyDataPayload setupComponentTallyDataPayload;

		private SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadServiceTemp;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

			final SetupComponentTallyDataPayloadFileRepository setupComponentTallyDataPayloadFileRepositoryTemp =
					new SetupComponentTallyDataPayloadFileRepository(objectMapper, pathResolver);

			setupComponentTallyDataPayloadServiceTemp = new SetupComponentTallyDataPayloadService(setupComponentTallyDataPayloadFileRepositoryTemp);
		}

		@BeforeEach
		void setUp() {
			final int numberOfEligibleVoters = 3;
			setupComponentTallyDataPayload = generator.generate(electionEventId, verificationCardSetId, numberOfEligibleVoters);
		}

		@Test
		@DisplayName("a valid payload does not throw")
		void saveValidPayload() {
			assertDoesNotThrow(() -> setupComponentTallyDataPayloadServiceTemp.save(setupComponentTallyDataPayload));

			assertTrue(Files.exists(pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId)
					.resolve(CONFIG_FILE_NAME_SETUP_COMPONENT_TALLY_DATA_PAYLOAD)));
		}

		@Test
		@DisplayName("a null setup component tally data payload throws")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> setupComponentTallyDataPayloadServiceTemp.save(null));
		}
	}

	@Nested
	@DisplayName("calling exist")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistTest {

		@Test
		@DisplayName("for valid election event and verification card set returns true")
		void existValidElectionEvent() {
			assertTrue(setupComponentTallyDataPayloadService.exist(electionEventId, verificationCardSetId));
		}

		@Test
		@DisplayName("for null input throws NullPointerException")
		void existNullInput() {
			assertThrows(NullPointerException.class, () -> setupComponentTallyDataPayloadService.exist(null, verificationCardSetId));
			assertThrows(NullPointerException.class, () -> setupComponentTallyDataPayloadService.exist(EXISTING_ELECTION_EVENT_ID, null));
		}

		@Test
		@DisplayName("for invalid input throws FailedValidationException")
		void existInvalidInput() {
			assertThrows(FailedValidationException.class, () -> setupComponentTallyDataPayloadService.exist(INVALID_ID, verificationCardSetId));
			assertThrows(FailedValidationException.class, () -> setupComponentTallyDataPayloadService.exist(EXISTING_ELECTION_EVENT_ID, INVALID_ID));
		}

		@Test
		@DisplayName("for non existing election event returns false")
		void existNonExistingElectionEvent() {
			assertFalse(setupComponentTallyDataPayloadService.exist(MISSING_ELECTION_EVENT_ID, verificationCardSetId));
		}

	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_METHOD)
	class LoadTest {

		@Test
		@DisplayName("existing election event and verification card set returns expected setup component tally data payload")
		void loadExistingElectionEventValidSignature() {
			final SetupComponentTallyDataPayload setupComponentTallyDataPayload = setupComponentTallyDataPayloadService.load(electionEventId,
					verificationCardSetId);

			assertEquals(electionEventId, setupComponentTallyDataPayload.getElectionEventId());
		}

		@Test
		@DisplayName("null input throws NullPointerException")
		void loadNullInput() {
			assertThrows(NullPointerException.class, () -> setupComponentTallyDataPayloadService.load(null, verificationCardSetId));
			assertThrows(NullPointerException.class, () -> setupComponentTallyDataPayloadService.load(electionEventId, null));
		}

		@Test
		@DisplayName("invalid input throws FailedValidationException")
		void loadInvalidInput() {
			assertThrows(FailedValidationException.class, () -> setupComponentTallyDataPayloadService.load(INVALID_ID, verificationCardSetId));
			assertThrows(FailedValidationException.class, () -> setupComponentTallyDataPayloadService.load(electionEventId, INVALID_ID));
		}

		@Test
		@DisplayName("existing election event and verification card set but with missing payload throws IllegalStateException")
		void loadMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> setupComponentTallyDataPayloadService.load(MISSING_ELECTION_EVENT_ID, verificationCardSetId));

			final String errorMessage = String.format(
					"Requested setup component tally data payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
					MISSING_ELECTION_EVENT_ID, verificationCardSetId);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

	}

}
