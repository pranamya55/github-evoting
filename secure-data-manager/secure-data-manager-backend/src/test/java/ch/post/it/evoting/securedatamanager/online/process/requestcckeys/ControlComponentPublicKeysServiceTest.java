/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.requestcckeys;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.KEY_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.online.process.OnlinePathResolver;

@DisplayName("A ControlComponentPublicKeysService")
class ControlComponentPublicKeysServiceTest {

	private static final String ELECTION_EVENT_ID = "314BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String WRONG_ELECTION_EVENT_ID = "414BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String NOT_ENOUGH_ELECTION_EVENT_ID = "614BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String INVALID_ID = "invalidId";
	private static final int NODE_ID = 1;

	private static GqGroup gqGroup;
	private static ObjectMapper objectMapper;
	private static ElGamalGenerator elGamalGenerator;
	private static ControlComponentPublicKeysService controlComponentPublicKeysService;

	@BeforeAll
	static void setUpAll() throws URISyntaxException, IOException {
		gqGroup = SerializationUtils.getGqGroup();
		elGamalGenerator = new ElGamalGenerator(gqGroup);
		objectMapper = DomainObjectMapper.getNewInstance();
		final Path path = Paths.get(
				Objects.requireNonNull(ControlComponentPublicKeysServiceTest.class.getResource("/controlComponentPublicKeysTest/")).toURI());
		final OnlinePathResolver pathResolver = new OnlinePathResolver(path, Path.of(""), Path.of(""));

		final ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepository = new ControlComponentPublicKeysPayloadFileRepository(
				objectMapper, pathResolver);

		controlComponentPublicKeysService = new ControlComponentPublicKeysService(controlComponentPublicKeysPayloadFileRepository);
	}

	@Nested
	@DisplayName("saving")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SaveTest {

		private ControlComponentPublicKeysService controlComponentPublicKeysServiceTemp;

		private ControlComponentPublicKeysPayload controlComponentPublicKeysPayload;

		@BeforeAll
		void setUpAll(
				@TempDir
				final Path tempDir) throws IOException {

			final OnlinePathResolver pathResolver = new OnlinePathResolver(tempDir, Path.of(""), Path.of(""));
			final ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepositoryTemp = new ControlComponentPublicKeysPayloadFileRepository(
					objectMapper, pathResolver);

			controlComponentPublicKeysServiceTemp = new ControlComponentPublicKeysService(controlComponentPublicKeysPayloadFileRepositoryTemp);
		}

		@BeforeEach
		void setUp() {
			// Create keys.
			final ElGamalMultiRecipientPublicKey ccrChoiceReturnCodesEncryptionPublicKey = elGamalGenerator.genRandomPublicKey(KEY_LENGTH);
			final ElGamalMultiRecipientPublicKey ccmElectionPublicKey = elGamalGenerator.genRandomPublicKey(KEY_LENGTH);
			final GroupVector<SchnorrProof, ZqGroup> schnorrProofs = SerializationUtils.getSchnorrProofs(KEY_LENGTH);
			final ControlComponentPublicKeys controlComponentPublicKeys = new ControlComponentPublicKeys(NODE_ID,
					ccrChoiceReturnCodesEncryptionPublicKey, schnorrProofs, ccmElectionPublicKey, schnorrProofs);

			// Create payload.
			controlComponentPublicKeysPayload = new ControlComponentPublicKeysPayload(gqGroup, ELECTION_EVENT_ID, controlComponentPublicKeys);
		}

		@Test
		@DisplayName("a valid payload does not throw")
		void saveValidPayload() {
			assertDoesNotThrow(() -> controlComponentPublicKeysServiceTemp.save(controlComponentPublicKeysPayload));
		}

		@Test
		@DisplayName("a null payload throws NullPointerException")
		void saveNullPayload() {
			assertThrows(NullPointerException.class, () -> controlComponentPublicKeysServiceTemp.save(null));
		}

	}

	@Nested
	@DisplayName("calling exist")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ExistTest {

		@Test
		@DisplayName("for valid election event returns true")
		void existValidElectionEvent() {
			assertTrue(controlComponentPublicKeysService.exist(ELECTION_EVENT_ID));
		}

		@Test
		@DisplayName("for invalid election event id throws FailedValidationException")
		void existInvalidElectionEvent() {
			assertThrows(FailedValidationException.class, () -> controlComponentPublicKeysService.exist(INVALID_ID));
		}

		@Test
		@DisplayName("for non existing election event returns false")
		void existNonExistingElectionEvent() {
			assertFalse(controlComponentPublicKeysService.exist(WRONG_ELECTION_EVENT_ID));
		}

		@Test
		@DisplayName("for missing payloads return false")
		void existMissingPayloads() {
			assertFalse(controlComponentPublicKeysService.exist(NOT_ENOUGH_ELECTION_EVENT_ID));
		}

	}
}
