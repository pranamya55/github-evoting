/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.domain.configuration.VerificationCardKeystore;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationCardKeystoresPayloadFileRepository;

@DisplayName("A SetupComponentVerificationCardKeystoresPayloadService")
class SetupComponentVerificationCardKeystoresPayloadServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final Hash hash = HashFactory.createHash();

	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String MISSING_ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();
	private static final String INVALID_ID = "invalidId";

	private static SetupComponentVerificationCardKeystoresPayloadService setupComponentVerificationCardKeystoresPayloadService;

	@BeforeAll
	static void setUpAll(
			@TempDir
			final Path tempDir) throws IOException {

		final PathResolver pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final SetupComponentVerificationCardKeystoresPayloadFileRepository setupComponentVerificationCardKeystoresPayloadFileRepository =
				new SetupComponentVerificationCardKeystoresPayloadFileRepository(DomainObjectMapper.getNewInstance(), pathResolver);

		final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload = validSetupComponentVerificationCardKeystoresPayload(
				ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);
		setupComponentVerificationCardKeystoresPayloadFileRepository.save(setupComponentVerificationCardKeystoresPayload);

		final UploadSetupComponentVerificationCardKeystoresRepository uploadSetupComponentVerificationCardKeystoresPayloadRepositoryMock =
				mock(UploadSetupComponentVerificationCardKeystoresRepository.class);
		setupComponentVerificationCardKeystoresPayloadService = new SetupComponentVerificationCardKeystoresPayloadService(
				setupComponentVerificationCardKeystoresPayloadFileRepository, uploadSetupComponentVerificationCardKeystoresPayloadRepositoryMock);
	}

	private static SetupComponentVerificationCardKeystoresPayload validSetupComponentVerificationCardKeystoresPayload(final String electionEventId,
			final String verificationCardSetId) {

		// Create payload.
		final ImmutableList<VerificationCardKeystore> verificationCardKeystores = ImmutableList.of(
				new VerificationCardKeystore(uuidGenerator.generate(),
						"%s=".formatted(random.genRandomString(571, base64Alphabet))),
				new VerificationCardKeystore(uuidGenerator.generate(),
						"%s=".formatted(random.genRandomString(571, base64Alphabet))),
				new VerificationCardKeystore(uuidGenerator.generate(),
						"%s=".formatted(random.genRandomString(571, base64Alphabet)))
		);

		final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload = new SetupComponentVerificationCardKeystoresPayload(
				electionEventId, verificationCardSetId,
				verificationCardKeystores);

		final ImmutableByteArray payloadHash = hash.recursiveHash(setupComponentVerificationCardKeystoresPayload);
		final CryptoPrimitivesSignature signature = new CryptoPrimitivesSignature(payloadHash);
		setupComponentVerificationCardKeystoresPayload.setSignature(signature);

		return setupComponentVerificationCardKeystoresPayload;
	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_METHOD)
	class LoadTest {

		@Test
		@DisplayName("existing election event and verification card set returns expected setup component verification card keystores payload")
		void loadExistingElectionEventValidSignature() {
			final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload = setupComponentVerificationCardKeystoresPayloadService.load(
					ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);

			assertEquals(ELECTION_EVENT_ID, setupComponentVerificationCardKeystoresPayload.getElectionEventId());
		}

		@Test
		@DisplayName("null input throws NullPointerException")
		void loadNullInput() {
			assertThrows(NullPointerException.class,
					() -> setupComponentVerificationCardKeystoresPayloadService.load(null, VERIFICATION_CARD_SET_ID));
			assertThrows(NullPointerException.class, () -> setupComponentVerificationCardKeystoresPayloadService.load(ELECTION_EVENT_ID, null));
		}

		@Test
		@DisplayName("invalid input throws FailedValidationException")
		void loadInvalidInput() {
			assertThrows(FailedValidationException.class,
					() -> setupComponentVerificationCardKeystoresPayloadService.load(INVALID_ID, VERIFICATION_CARD_SET_ID));
			assertThrows(FailedValidationException.class,
					() -> setupComponentVerificationCardKeystoresPayloadService.load(ELECTION_EVENT_ID, INVALID_ID));
		}

		@Test
		@DisplayName("existing election event and verification card set but with missing payload throws IllegalStateException")
		void loadMissingPayload() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> setupComponentVerificationCardKeystoresPayloadService.load(MISSING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID));

			final String errorMessage = String.format(
					"Requested setup component verification card keystores payload is not present. [electionEventId: %s, verificationCardSetId: %s]",
					MISSING_ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

	}

}
