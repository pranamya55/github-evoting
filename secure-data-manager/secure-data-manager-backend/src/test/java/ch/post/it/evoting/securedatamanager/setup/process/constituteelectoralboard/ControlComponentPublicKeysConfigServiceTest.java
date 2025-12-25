/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SignatureException;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;

@DisplayName("A ControlComponentPublicKeysConfigService")
class ControlComponentPublicKeysConfigServiceTest {

	private static final String ELECTION_EVENT_ID = "314BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String NOT_ENOUGH_ELECTION_EVENT_ID = "614BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String TOO_MANY_ELECTION_EVENT_ID = "714BD34DCF6E4DE4B771A92FA3849D3D";
	private static final String INVALID_ID = "invalidId";

	private static ControlComponentPublicKeysConfigService controlComponentPublicKeysConfigService;
	private static SignatureKeystore<Alias> signatureKeystore;

	@BeforeAll
	static void setUpAll() throws URISyntaxException, IOException {
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final Path path = Paths.get(
				Objects.requireNonNull(ControlComponentPublicKeysConfigServiceTest.class.getResource("/controlComponentPublicKeysTest/")).toURI());
		final SetupPathResolver pathResolver = new SetupPathResolver(path, Path.of(""), Path.of(""), Path.of(""), Path.of(""));

		final ControlComponentPublicKeysPayloadFileRepository controlComponentPublicKeysPayloadFileRepository = new ControlComponentPublicKeysPayloadFileRepository(
				objectMapper, pathResolver);

		signatureKeystore = mock(SignatureKeystore.class);

		controlComponentPublicKeysConfigService = new ControlComponentPublicKeysConfigService(controlComponentPublicKeysPayloadFileRepository,
				signatureKeystore);
	}

	@Nested
	@DisplayName("loading")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class LoadTest {

		@BeforeEach
		void setUp() throws SignatureException {
			when(signatureKeystore.verifySignature(any(), any(), any(), any())).thenReturn(true);
		}

		@Test
		@DisplayName("existing election event returns all payloads")
		void loadExistingElectionEvent() {
			final ImmutableList<ControlComponentPublicKeys> controlComponentPublicKeys = controlComponentPublicKeysConfigService.loadOrderByNodeId(
					ELECTION_EVENT_ID);

			final ImmutableList<Integer> payloadsNodeIds = controlComponentPublicKeys.stream()
					.map(ControlComponentPublicKeys::nodeId)
					.collect(toImmutableList());

			assertEquals(ControlComponentNode.ids().size(), controlComponentPublicKeys.size());
			assertTrue(payloadsNodeIds.containsAll(ControlComponentNode.ids()));
		}

		@Test
		@DisplayName("invalid election event id throws FailedValidationException")
		void loadInvalidElectionEventId() {
			assertThrows(FailedValidationException.class, () -> controlComponentPublicKeysConfigService.loadOrderByNodeId(INVALID_ID));
		}

		@Test
		@DisplayName("existing election event with missing payloads throws IllegalStateException")
		void loadMissingPayloads() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> controlComponentPublicKeysConfigService.loadOrderByNodeId(NOT_ENOUGH_ELECTION_EVENT_ID));

			final String errorMessage = String.format("Wrong number of control component public keys payloads. [required node ids: %s, found: %s]",
					ControlComponentNode.ids(), ImmutableList.of(1));
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("existing election event with too many payloads throws IllegalStateException")
		void loadTooManyPayloads() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> controlComponentPublicKeysConfigService.loadOrderByNodeId(TOO_MANY_ELECTION_EVENT_ID));

			final String errorMessage = String.format("Wrong number of control component public keys payloads. [required node ids: %s, found: %s]",
					ControlComponentNode.ids(), ImmutableList.of(1, 2, 3, 4, 4));
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}

		@Test
		@DisplayName("payload with invalid signature throws IllegalStateException")
		void loadInvalidPayloadSignature() throws SignatureException {
			when(signatureKeystore.verifySignature(any(), any(), any(), any())).thenReturn(false);

			final InvalidPayloadSignatureException exception = assertThrows(InvalidPayloadSignatureException.class,
					() -> controlComponentPublicKeysConfigService.loadOrderByNodeId(ELECTION_EVENT_ID));

			final String errorMessage = String.format(
					"Signature of payload %s is invalid. [electionEventId: %s, nodeId: %s]",
					ControlComponentPublicKeysPayload.class.getSimpleName(), ELECTION_EVENT_ID, 1);
			assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
		}
	}
}
