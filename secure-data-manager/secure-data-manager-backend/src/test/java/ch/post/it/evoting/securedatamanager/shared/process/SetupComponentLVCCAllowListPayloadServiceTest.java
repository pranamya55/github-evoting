/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.securedatamanager.shared.process.SetupComponentLVCCAllowListPayloadFileRepository.PAYLOAD_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.domain.generators.SetupComponentLVCCAllowListPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("A SetupComponentLVCCAllowListPayloadService")
class SetupComponentLVCCAllowListPayloadServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String NON_EXISTING_VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

	private static PathResolver pathResolver;
	private static String electionEventId;
	private static String verificationCardSetId;
	private static SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload;
	private static SetupComponentLVCCAllowListPayloadService setupComponentLVCCAllowListPayloadService;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {

		final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final SetupComponentLVCCAllowListPayloadFileRepository setupComponentLVCCAllowListPayloadFileRepository =
				new SetupComponentLVCCAllowListPayloadFileRepository(objectMapper, pathResolver);

		final SetupComponentLVCCAllowListPayloadGenerator setupComponentLVCCAllowListPayloadGenerator = new SetupComponentLVCCAllowListPayloadGenerator();
		setupComponentLVCCAllowListPayload = setupComponentLVCCAllowListPayloadGenerator.generate();
		electionEventId = setupComponentLVCCAllowListPayload.getElectionEventId();
		verificationCardSetId = setupComponentLVCCAllowListPayload.getVerificationCardSetId();

		setupComponentLVCCAllowListPayloadFileRepository.save(setupComponentLVCCAllowListPayload);
		setupComponentLVCCAllowListPayloadService = new SetupComponentLVCCAllowListPayloadService(
				setupComponentLVCCAllowListPayloadFileRepository);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private SetupComponentLVCCAllowListPayloadService setupComponentLVCCAllowListPayloadServiceTemp;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			final SetupComponentLVCCAllowListPayloadFileRepository setupComponentLVCCAllowListPayloadFileRepositoryTemp =
					new SetupComponentLVCCAllowListPayloadFileRepository(objectMapper, pathResolver);

			setupComponentLVCCAllowListPayloadServiceTemp = new SetupComponentLVCCAllowListPayloadService(
					setupComponentLVCCAllowListPayloadFileRepositoryTemp);
		}

		@Test
		@DisplayName("a valid payload does not throw")
		void saveValidPayload() {
			assertDoesNotThrow(() -> setupComponentLVCCAllowListPayloadServiceTemp.save(setupComponentLVCCAllowListPayload));

			assertTrue(Files.exists(
					pathResolver.resolveVerificationCardSetPath(electionEventId, verificationCardSetId).resolve(PAYLOAD_FILE_NAME)));
		}

		@Test
		@DisplayName("a null payload throws NullPointerException")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> setupComponentLVCCAllowListPayloadServiceTemp.save(null));
		}
	}

	@Nested
	@DisplayName("calling exist")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistTest {

		@Test
		@DisplayName("for valid verification card set returns true")
		void existValid() {
			assertTrue(setupComponentLVCCAllowListPayloadService.exist(electionEventId, verificationCardSetId));
		}

		@Test
		@DisplayName("with invalid ids throws an exception")
		void existInvalidIdThrows() {
			assertAll(
					() -> assertThrows(NullPointerException.class,
							() -> setupComponentLVCCAllowListPayloadService.exist(null, verificationCardSetId)),
					() -> assertThrows(FailedValidationException.class,
							() -> setupComponentLVCCAllowListPayloadService.exist("invalidElectionEventId", verificationCardSetId)),
					() -> assertThrows(NullPointerException.class,
							() -> setupComponentLVCCAllowListPayloadService.exist(electionEventId, null)),
					() -> assertThrows(FailedValidationException.class,
							() -> setupComponentLVCCAllowListPayloadService.exist(electionEventId, "invalidVerificationCardSetId"))
			);
		}

		@Test
		@DisplayName("for non existing verification card set returns false")
		void existNonExistingVerificationCardSet() {
			assertFalse(setupComponentLVCCAllowListPayloadService.exist(electionEventId, NON_EXISTING_VERIFICATION_CARD_SET_ID));
		}

	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LoadTest {

		@Test
		@DisplayName("existing verification card set returns expected setup component LVCC allow list payload")
		void loadExistingVerificationCardSet() {
			assertNotNull(setupComponentLVCCAllowListPayloadService.load(electionEventId, verificationCardSetId));
		}

		@Test
		@DisplayName("with invalid ids throws an exception")
		void loadInvalidIdThrows() {
			assertAll(
					() -> assertThrows(NullPointerException.class,
							() -> setupComponentLVCCAllowListPayloadService.load(null, verificationCardSetId)),
					() -> assertThrows(FailedValidationException.class,
							() -> setupComponentLVCCAllowListPayloadService.load("invalidElectionEventId", verificationCardSetId)),
					() -> assertThrows(NullPointerException.class,
							() -> setupComponentLVCCAllowListPayloadService.load(electionEventId, null)),
					() -> assertThrows(FailedValidationException.class,
							() -> setupComponentLVCCAllowListPayloadService.load(electionEventId, "invalidVerificationCardSetId"))
			);
		}

		@Test
		@DisplayName("existing verification card set with missing payload throws")
		void loadMissingPayload() {

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () ->
					setupComponentLVCCAllowListPayloadService.load(electionEventId, NON_EXISTING_VERIFICATION_CARD_SET_ID));

			assertEquals(String.format(
					"Requested setup component LVCC allow list payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
					electionEventId, NON_EXISTING_VERIFICATION_CARD_SET_ID), Throwables.getRootCause(illegalStateException).getMessage());
		}

	}

}
