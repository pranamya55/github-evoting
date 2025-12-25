/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_SETUP_COMPONENT_VERIFICATION_CARD_KEYSTORES_PAYLOAD;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationCardKeystoresPayloadFileRepository;

@DisplayName("A SetupComponentVerificationCardKeystoresPayloadService")
class SetupComponentVerificationCardKeystoresPayloadServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final Hash hash = HashFactory.createHash();

	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final String VERIFICATION_CARD_SET_ID = uuidGenerator.generate();

	private static final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
	private static PathResolver pathResolver;

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
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload;

		private SetupComponentVerificationCardKeystoresPayloadService setupComponentVerificationCardKeystoresPayloadServiceTemp;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			pathResolver = new SetupPathResolver(tempDir, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

			final SetupComponentVerificationCardKeystoresPayloadFileRepository setupComponentVerificationCardKeystoresPayloadFileRepositoryTemp =
					new SetupComponentVerificationCardKeystoresPayloadFileRepository(objectMapper, pathResolver);

			setupComponentVerificationCardKeystoresPayloadServiceTemp = new SetupComponentVerificationCardKeystoresPayloadService(
					setupComponentVerificationCardKeystoresPayloadFileRepositoryTemp);
		}

		@BeforeEach
		void setUp() {
			setupComponentVerificationCardKeystoresPayload = validSetupComponentVerificationCardKeystoresPayload(ELECTION_EVENT_ID,
					VERIFICATION_CARD_SET_ID);
		}

		@Test
		@DisplayName("a valid payload does not throw")
		void saveValidPayload() {
			assertDoesNotThrow(() -> setupComponentVerificationCardKeystoresPayloadServiceTemp.save(setupComponentVerificationCardKeystoresPayload));

			assertTrue(Files.exists(pathResolver.resolveVerificationCardSetPath(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID)
					.resolve(CONFIG_FILE_NAME_SETUP_COMPONENT_VERIFICATION_CARD_KEYSTORES_PAYLOAD)));
		}

		@Test
		@DisplayName("a null setup component verification card keystores payload throws")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> setupComponentVerificationCardKeystoresPayloadServiceTemp.save(null));
		}
	}

}
