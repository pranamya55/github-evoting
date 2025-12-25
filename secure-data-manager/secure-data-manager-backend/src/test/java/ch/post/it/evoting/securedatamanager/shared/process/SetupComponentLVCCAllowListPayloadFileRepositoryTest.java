/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.securedatamanager.shared.process.SetupComponentLVCCAllowListPayloadFileRepository.PAYLOAD_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.domain.generators.SetupComponentLVCCAllowListPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("A SetupComponentLVCCAllowListPayloadFileRepository")
class SetupComponentLVCCAllowListPayloadFileRepositoryTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String CORRUPTED_VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String NON_EXISTING_VERIFICATION_CARD_SET_ID = uuidGenerator.generate();

	private static PathResolver pathResolver;
	private static ObjectMapper objectMapper;
	private static String electionEventId;
	private static String verificationCardSetId;
	private static SetupComponentLVCCAllowListPayload validSetupComponentLVCCAAllowListPayload;
	private static SetupComponentLVCCAllowListPayloadFileRepository setupComponentLVCCAllowListPayloadFileRepository;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		objectMapper = DomainObjectMapper.getNewInstance();

		pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		setupComponentLVCCAllowListPayloadFileRepository = new SetupComponentLVCCAllowListPayloadFileRepository(objectMapper, pathResolver);

		final SetupComponentLVCCAllowListPayloadFileRepository repository =
				new SetupComponentLVCCAllowListPayloadFileRepository(objectMapper, pathResolver);

		final SetupComponentLVCCAllowListPayloadGenerator setupComponentLVCCAllowListPayloadGenerator = new SetupComponentLVCCAllowListPayloadGenerator();
		validSetupComponentLVCCAAllowListPayload = setupComponentLVCCAllowListPayloadGenerator.generate();
		electionEventId = validSetupComponentLVCCAAllowListPayload.getElectionEventId();
		verificationCardSetId = validSetupComponentLVCCAAllowListPayload.getVerificationCardSetId();

		final SetupComponentLVCCAllowListPayload unsignedSetupComponentLVCCAAllowListPayload = new SetupComponentLVCCAllowListPayload(electionEventId,
				CORRUPTED_VERIFICATION_CARD_SET_ID, validSetupComponentLVCCAAllowListPayload.getLongVoteCastReturnCodesAllowList());

		repository.save(validSetupComponentLVCCAAllowListPayload);
		repository.save(unsignedSetupComponentLVCCAAllowListPayload);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private SetupComponentLVCCAllowListPayloadFileRepository setupComponentLVCCAllowListPayloadFileRepositoryTemp;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final PathResolver setupPathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));
			setupComponentLVCCAllowListPayloadFileRepositoryTemp = new SetupComponentLVCCAllowListPayloadFileRepository(objectMapper,
					setupPathResolver);
		}

		@Test
		@DisplayName("valid vote cast return codes allow list payload creates file")
		void save() {
			final Path savedPath = setupComponentLVCCAllowListPayloadFileRepositoryTemp.save(validSetupComponentLVCCAAllowListPayload);

			assertTrue(Files.exists(savedPath));
		}

		@Test
		@DisplayName("null vote cast return codes allow list payload throws NullPointerException")
		void saveNullLongVoteCastReturnCodesAllowList() {
			assertThrows(NullPointerException.class, () -> setupComponentLVCCAllowListPayloadFileRepositoryTemp.save(null));
		}

	}

	@Nested
	@DisplayName("calling existsByElectionEventIdAndVerificationCardSetId")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistsByElectionEventIdAndVerificationCardSetIdTest {

		@Test
		@DisplayName("for existing vote cast return codes allow list payload returns true")
		void existingLongVoteCastReturnCodesAllowList() {
			assertTrue(setupComponentLVCCAllowListPayloadFileRepository.existsByElectionEventIdAndVerificationCardSetId(electionEventId,
					verificationCardSetId));
		}

		@Test
		@DisplayName("with invalid id throws an exception")
		void invalidIdThrows() {
			assertAll(
					() -> assertThrows(NullPointerException.class,
							() -> setupComponentLVCCAllowListPayloadFileRepository.existsByElectionEventIdAndVerificationCardSetId(
									null, verificationCardSetId)),
					() -> assertThrows(FailedValidationException.class,
							() -> setupComponentLVCCAllowListPayloadFileRepository.existsByElectionEventIdAndVerificationCardSetId(
									"invalidElectionEventId", verificationCardSetId)),
					() -> assertThrows(NullPointerException.class,
							() -> setupComponentLVCCAllowListPayloadFileRepository.existsByElectionEventIdAndVerificationCardSetId(
									electionEventId, null)),
					() -> assertThrows(FailedValidationException.class,
							() -> setupComponentLVCCAllowListPayloadFileRepository.existsByElectionEventIdAndVerificationCardSetId(
									electionEventId, "invalidVerificationCardSetId")));
		}

		@Test
		@DisplayName("for non existing vote cast return codes allow list payload returns false")
		void nonExistingLongVoteCastReturnCodesAllowList() {
			assertFalse(setupComponentLVCCAllowListPayloadFileRepository.existsByElectionEventIdAndVerificationCardSetId(electionEventId,
					NON_EXISTING_VERIFICATION_CARD_SET_ID));
		}

	}

	@Nested
	@DisplayName("calling findByElectionEventIdAndVerificationCardSetId")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class FindByElectionEventIdAndVerificationCardSetIdTest {

		@Test
		@DisplayName("for existing vote cast return codes allow list payload returns it")
		void existingLongVoteCastReturnCodesAllowList() {
			assertTrue(setupComponentLVCCAllowListPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(electionEventId,
					verificationCardSetId).isPresent());
		}

		@Test
		@DisplayName("for non existing vote cast return codes allow list payload return empty optional")
		void nonExistingLongVoteCastReturnCodesAllowList() {
			assertFalse(setupComponentLVCCAllowListPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(electionEventId,
							NON_EXISTING_VERIFICATION_CARD_SET_ID)
					.isPresent());
		}

		@Test
		@DisplayName("for corrupted vote cast return codes allow list payload throws UncheckedIOException")
		void corruptedLongVoteCastReturnCodesAllowList() {
			final UncheckedIOException exception = assertThrows(UncheckedIOException.class,
					() -> setupComponentLVCCAllowListPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(electionEventId,
							CORRUPTED_VERIFICATION_CARD_SET_ID));

			final Path verificationCardSetPath = pathResolver.resolveVerificationCardSetPath(electionEventId, CORRUPTED_VERIFICATION_CARD_SET_ID);
			final Path longVoteCastReturnCodesAllowListPath = verificationCardSetPath.resolve(PAYLOAD_FILE_NAME);
			final String errorMessage = String.format(
					"Failed to deserialize setup component LVCC allow list payload. [electionEventId: %s, verificationCardSetId: %s, path: %s]",
					electionEventId, CORRUPTED_VERIFICATION_CARD_SET_ID, longVoteCastReturnCodesAllowListPath);

			assertEquals(errorMessage, exception.getMessage());
		}

		@Test
		@DisplayName("with invalid id throws an exception")
		void invalidIdThrows() {
			assertAll(
					() -> assertThrows(NullPointerException.class,
							() -> setupComponentLVCCAllowListPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(
									null, verificationCardSetId)),
					() -> assertThrows(FailedValidationException.class,
							() -> setupComponentLVCCAllowListPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(
									"invalidElectionEventId", verificationCardSetId)),
					() -> assertThrows(NullPointerException.class,
							() -> setupComponentLVCCAllowListPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(
									electionEventId, null)),
					() -> assertThrows(FailedValidationException.class,
							() -> setupComponentLVCCAllowListPayloadFileRepository.findByElectionEventIdAndVerificationCardSetId(
									electionEventId, "invalidVerificationCardSetId")));
		}

	}

}
