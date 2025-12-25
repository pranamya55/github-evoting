/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_VERIFICATION_CARD_SECRET_KEY_PAYLOAD;
import static ch.post.it.evoting.securedatamanager.shared.Constants.VERIFICATION_CARD_SETS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("A VerificationCardSecretKeyService")
class VerificationCardSecretKeyPayloadServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String MISSING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_ID = uuidGenerator.generate();
	private static final String INVALID_ID = "invalidId";

	private static ObjectMapper objectMapper;
	private static SetupPathResolver pathResolver;
	private static VerificationCardSecretKeyPayloadService verificationCardSecretKeyPayloadService;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {
		objectMapper = DomainObjectMapper.getNewInstance();

		final SetupPathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final VerificationCardSecretKeyPayloadFileRepository verificationCardSecretKeyPayloadFileRepository =
				new VerificationCardSecretKeyPayloadFileRepository(objectMapper, pathResolver);

		final VerificationCardSecretKeyPayload verificationCardSecretKeyPayload = validVerificationCardSecretKeyPayload(ELECTION_EVENT_ID,
				VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_ID);
		verificationCardSecretKeyPayloadFileRepository.save(verificationCardSecretKeyPayload);

		verificationCardSecretKeyPayloadService = new VerificationCardSecretKeyPayloadService(verificationCardSecretKeyPayloadFileRepository);
	}

	private static VerificationCardSecretKeyPayload validVerificationCardSecretKeyPayload(final String electionEventId,
			final String verificationCardSetId, final String verificationCardId) {
		final ImmutableList<VerificationCardSecretKey> verificationCardSecretKeys = ImmutableList.of(
				new VerificationCardSecretKey(verificationCardId, SerializationUtils.getPrivateKey()));
		return new VerificationCardSecretKeyPayload(SerializationUtils.getGqGroup(), electionEventId, verificationCardSetId,
				verificationCardSecretKeys);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private VerificationCardSecretKeyPayload verificationCardSecretKeyPayload;

		private VerificationCardSecretKeyPayloadService verificationCardSecretKeyPayloadServiceTemp;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

			final VerificationCardSecretKeyPayloadFileRepository verificationCardSecretKeyPayloadFileRepositoryTemp =
					new VerificationCardSecretKeyPayloadFileRepository(objectMapper, pathResolver);

			verificationCardSecretKeyPayloadServiceTemp = new VerificationCardSecretKeyPayloadService(
					verificationCardSecretKeyPayloadFileRepositoryTemp);
		}

		@BeforeEach
		void setUp() {
			verificationCardSecretKeyPayload = validVerificationCardSecretKeyPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID,
					VERIFICATION_CARD_ID);
		}

		@Test
		@DisplayName("a valid payload does not throw")
		void saveValidPayload() {
			assertDoesNotThrow(() -> verificationCardSecretKeyPayloadServiceTemp.save(verificationCardSecretKeyPayload));

			assertTrue(Files.exists(pathResolver.resolveElectionEventPath(ELECTION_EVENT_ID)
					.resolve(VERIFICATION_CARD_SETS).resolve(VERIFICATION_CARD_SET_ID)
					.resolve(CONFIG_FILE_NAME_VERIFICATION_CARD_SECRET_KEY_PAYLOAD)));
		}

		@Test
		@DisplayName("a null verification card secret key throws")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> verificationCardSecretKeyPayloadServiceTemp.save(null));
		}
	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_METHOD)
	class LoadTest {

		@Test
		@DisplayName("existing election event and verification card set returns expected verification card secret key")
		void loadExistingElectionEventValidSignature() {
			final VerificationCardSecretKeyPayload verificationCardSecretKeyPayload = verificationCardSecretKeyPayloadService.load(ELECTION_EVENT_ID,
					VERIFICATION_CARD_SET_ID);

			assertEquals(ELECTION_EVENT_ID, verificationCardSecretKeyPayload.electionEventId());
		}

		@Test
		@DisplayName("null input throws NullPointerException")
		void loadNullInput() {
			assertThrows(NullPointerException.class,
					() -> verificationCardSecretKeyPayloadService.load(null, VERIFICATION_CARD_SET_ID));
			assertThrows(NullPointerException.class,
					() -> verificationCardSecretKeyPayloadService.load(ELECTION_EVENT_ID, null));
		}

		@Test
		@DisplayName("invalid input throws FailedValidationException")
		void loadInvalidInput() {
			assertThrows(FailedValidationException.class,
					() -> verificationCardSecretKeyPayloadService.load(INVALID_ID, VERIFICATION_CARD_SET_ID));
			assertThrows(FailedValidationException.class,
					() -> verificationCardSecretKeyPayloadService.load(ELECTION_EVENT_ID, INVALID_ID));
		}

		@Test
		@DisplayName("existing election event and verification card set but with missing payload throws IllegalStateException")
		void loadMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> verificationCardSecretKeyPayloadService.load(MISSING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));

			final String errorMessage = String.format(
					"Requested verification card secret key payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
					MISSING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

	}

}
