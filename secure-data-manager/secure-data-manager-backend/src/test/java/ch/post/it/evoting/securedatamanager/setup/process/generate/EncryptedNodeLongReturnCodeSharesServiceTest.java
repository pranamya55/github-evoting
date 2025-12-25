/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.securedatamanager.setup.process.generate.ControlComponentCodeSharesPayloadService.ControlComponentCodeSharesPayloadsChunk;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;

@DisplayName("EncryptedNodeLongCodeSharesService")
class EncryptedNodeLongReturnCodeSharesServiceTest {

	private static final String INVALID_UUID = "0123456789ABCDEF0123456789ABCDEF";
	private static final String ELECTION_EVENT_ID = "7E6225DF3A10F4A5D63D76EA7E0E7916";
	private static final String VERIFICATION_CARD_SET_ID = "5B78F34995FAE5EA69DBD3A7608F5397";

	private Path testPath;
	private ObjectMapper objectMapper;
	private ControlComponentCodeSharesPayloadsChunk controlComponentCodeSharesPayloadsChunk;
	private ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloadList;
	private EncryptedNodeLongReturnCodeSharesService encryptedNodeLongReturnCodeSharesService;

	private SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;

	@BeforeEach
	void setup() throws URISyntaxException, IOException {
		prepare("valid");
	}

	private void prepare(final String dataset) throws URISyntaxException, IOException {
		testPath = Paths.get(EncryptedNodeLongReturnCodeSharesService.class.getResource("/encryptedNodeLongCodeSharesServiceTest").toURI());
		objectMapper = DomainObjectMapper.getNewInstance();

		controlComponentCodeSharesPayloadList = objectMapper.readValue(
				testPath.resolve(dataset).resolve("controlComponentCodeSharesPayload.0.json").toFile(), new TypeReference<>() {
				});

		controlComponentCodeSharesPayloadsChunk = new ControlComponentCodeSharesPayloadsChunk(controlComponentCodeSharesPayloadList, 0);

		setupComponentVerificationDataPayloadFileRepository = mock(SetupComponentVerificationDataPayloadFileRepository.class);
		final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload = objectMapper.readValue(
				testPath.resolve("setupComponentVerificationDataPayload.0.json").toFile(), SetupComponentVerificationDataPayload.class);
		when(setupComponentVerificationDataPayloadFileRepository.retrieve(anyString(), anyString(), anyInt()))
				.thenReturn(setupComponentVerificationDataPayload);

		encryptedNodeLongReturnCodeSharesService = new EncryptedNodeLongReturnCodeSharesService(setupComponentVerificationDataPayloadFileRepository);
	}

	@Nested
	class ConvertNodeContributionsChunkCall {
		@Test
		@DisplayName("Convert NodeContributionsChunk")
		void convertNodeContributionsChunk() {
			final int expectedNodeSize = 4;
			final int expectedListSize = 3;

			final EncryptedNodeLongReturnCodeSharesChunk encryptedNodeLongReturnCodeSharesChunk = encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(
					ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, controlComponentCodeSharesPayloadsChunk);

			assertAll(
					() -> assertEquals(ELECTION_EVENT_ID, encryptedNodeLongReturnCodeSharesChunk.getElectionEventId()),
					() -> assertEquals(VERIFICATION_CARD_SET_ID, encryptedNodeLongReturnCodeSharesChunk.getVerificationCardSetId()),
					() -> assertEquals(expectedListSize, encryptedNodeLongReturnCodeSharesChunk.getVerificationCardIds().size(),
							"Verification card ids size"),
					() -> assertEquals(expectedNodeSize,
							encryptedNodeLongReturnCodeSharesChunk.getControlComponentCodeSharesChunks().size(),
							"Node return codes values size"),
					() -> assertEquals(expectedListSize,
							encryptedNodeLongReturnCodeSharesChunk.getControlComponentCodeSharesChunks().get(0)
									.getExponentiatedEncryptedHashedConfirmationKey()
									.size(),
							"Exponentiated confirmation key size"),
					() -> assertEquals(expectedListSize,
							encryptedNodeLongReturnCodeSharesChunk.getControlComponentCodeSharesChunks().get(0)
									.getExponentiatedEncryptedHashedPartialChoiceReturnCodes()
									.size(),
							"Exponentiated partial choice return codes size")
			);
		}

		@Test
		@DisplayName("Convert a NodeContributionsChunk with null inputs throws")
		void convertNodeContributionsChunkWithNullInputs() {
			assertAll(
					() -> assertThrows(NullPointerException.class,
							() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(null,
									VERIFICATION_CARD_SET_ID,
									controlComponentCodeSharesPayloadsChunk)),
					() -> assertThrows(NullPointerException.class,
							() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID, null,
									controlComponentCodeSharesPayloadsChunk)),
					() -> assertThrows(NullPointerException.class,
							() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID,
									VERIFICATION_CARD_SET_ID,
									null))
			);
		}

		@Test
		@DisplayName("Convert a NodeContributionsChunk with invalid UUID inputs throws")
		void convertNodeContributionsChunkWithInvalidIds() {
			assertAll(
					() -> assertThrows(IllegalStateException.class,
							() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(INVALID_UUID,
									VERIFICATION_CARD_SET_ID,
									controlComponentCodeSharesPayloadsChunk)),
					() -> assertThrows(IllegalStateException.class,
							() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID,
									INVALID_UUID,
									controlComponentCodeSharesPayloadsChunk))
			);
		}
	}

	@Nested
	class NodeContributionsChunkConsistencyCheck {

		@Test
		@DisplayName("Convert a NodeContributionsChunk with empty nodeContributions throws")
		void convertNodeContributionsChunkWithEmptyNodeContributions() {
			final ControlComponentCodeSharesPayloadsChunk nodeContributionsChunkWithEmptyControlComponentCodeSharesPayload = new ControlComponentCodeSharesPayloadsChunk(
					ImmutableList.emptyList(), 0);

			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID,
							VERIFICATION_CARD_SET_ID,
							nodeContributionsChunkWithEmptyControlComponentCodeSharesPayload));

			assertTrue(exception.getMessage().startsWith("No node payloads responses."));
		}

		@Test
		@DisplayName("Convert a NodeContributionsChunk with missing node throws")
		void convertNodeContributionsChunkWithMissingNode() {
			final ControlComponentCodeSharesPayloadsChunk nodeContributionsChunkWithEmptyControlComponentCodeSharesPayload = new ControlComponentCodeSharesPayloadsChunk(
					ImmutableList.of(controlComponentCodeSharesPayloadList.get(0),
							controlComponentCodeSharesPayloadList.get(1),
							controlComponentCodeSharesPayloadList.get(3)), 0);

			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID,
							VERIFICATION_CARD_SET_ID,
							nodeContributionsChunkWithEmptyControlComponentCodeSharesPayload));

			assertTrue(exception.getMessage().startsWith("The node ID sequence is incomplete."));
		}

		@Test
		@DisplayName("Convert a NodeContributionsChunk with node in wrong order throws")
		void convertNodeContributionsChunkWithWrongNodeOrder() {
			final ControlComponentCodeSharesPayloadsChunk nodeContributionsChunkWithWrongNodeOrder = new ControlComponentCodeSharesPayloadsChunk(
					ImmutableList.of(controlComponentCodeSharesPayloadList.get(0),
							controlComponentCodeSharesPayloadList.get(1),
							controlComponentCodeSharesPayloadList.get(3),
							controlComponentCodeSharesPayloadList.get(2)), 0);

			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID,
							VERIFICATION_CARD_SET_ID,
							nodeContributionsChunkWithWrongNodeOrder));

			assertTrue(exception.getMessage().startsWith("The node ID sequence is not in the correct order."));
		}

		@Test
		@DisplayName("Convert a NodeContributionsChunk with duplicated node throws")
		void convertNodeContributionsChunkWithDuplicateNode() {
			final ControlComponentCodeSharesPayloadsChunk nodeContributionsChunkWithDuplicateNode = new ControlComponentCodeSharesPayloadsChunk(
					ImmutableList.of(controlComponentCodeSharesPayloadList.get(0),
							controlComponentCodeSharesPayloadList.get(1),
							controlComponentCodeSharesPayloadList.get(3),
							controlComponentCodeSharesPayloadList.get(3)), 0);

			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID,
							VERIFICATION_CARD_SET_ID,
							nodeContributionsChunkWithDuplicateNode));

			assertTrue(exception.getMessage().startsWith("The node ID sequence is not in the correct order."));
		}

		@ParameterizedTest
		@DisplayName("Convert a NodeContributionsChunk with incorrect input throws")
		@ValueSource(strings = { "inconsistent-eeid", "inconsistent-vcsid", "inconsistent-chunkid" })
		void convertNodeContributionsChunkWithInconsistentInput(final String dataset) throws URISyntaxException, IOException {
			prepare(dataset);
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID,
							VERIFICATION_CARD_SET_ID,
							controlComponentCodeSharesPayloadsChunk));

			assertTrue(exception.getMessage().startsWith("All return code generation response payloads must be related to the correct "
					+ "election event id, verification card set id and chunkId."));
		}

		@Test
		@DisplayName("Convert a NodeContributionsChunk with different verification card list length between ControlComponentCodeShares and SetupComponentVerificationData payloads throws")
		void convertNodeContributionsChunkWithDifferentVcListLength() {

			final GqGroup encryptionGroup = controlComponentCodeSharesPayloadList.get(0).getEncryptionGroup();
			final ImmutableList<ControlComponentCodeShare> differentChunkCountControlComponentCodeShareList = ImmutableList.of(
					controlComponentCodeSharesPayloadList.get(0).getControlComponentCodeShares().get(0),
					controlComponentCodeSharesPayloadList.get(0).getControlComponentCodeShares().get(1)
			);

			final ImmutableList<ControlComponentCodeSharesPayload> differentVcListLengthPayloads = ControlComponentNode.ids().stream()
					.map(nodeId -> new ControlComponentCodeSharesPayload(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, 0, encryptionGroup,
							differentChunkCountControlComponentCodeShareList, nodeId))
					.collect(toImmutableList());

			final ControlComponentCodeSharesPayloadsChunk controlComponentCodeSharesPayloadsChunkWithDifferentChunkCount = new ControlComponentCodeSharesPayloadsChunk(
					differentVcListLengthPayloads, 0);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID,
							VERIFICATION_CARD_SET_ID,
							controlComponentCodeSharesPayloadsChunkWithDifferentChunkCount));
			assertTrue(illegalStateException.getMessage().startsWith(
					"The ControlComponentCodeSharesPayload does not have the same verification card ids as the SetupComponentVerificationDataPayload."));
		}

	}

	@Nested
	class SetupComponentVerificationDataPayloadContentConsistencyCheck {
		@ParameterizedTest
		@DisplayName("get a SetupComponentVerificationDataPayloadContent with inconsistent values throws")
		@MethodSource("inconsistentSetupComponentVerificationDataPayloadContent")
		void getSetupComponentWithInconsistentValues(final String jsonPath, final String exceptionMessage) throws IOException {

			final SetupComponentVerificationDataPayload setupComponentVerificationDataPayloadWrongChunk = objectMapper.readValue(
					testPath.resolve(jsonPath).toFile(),
					SetupComponentVerificationDataPayload.class);
			when(setupComponentVerificationDataPayloadFileRepository.retrieve(anyString(), anyString(), anyInt()))
					.thenReturn(setupComponentVerificationDataPayloadWrongChunk);

			final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
					() -> encryptedNodeLongReturnCodeSharesService.convertControlComponentCodeSharesPayloadsChunk(ELECTION_EVENT_ID,
							VERIFICATION_CARD_SET_ID,
							controlComponentCodeSharesPayloadsChunk));
			assertTrue(illegalStateException.getMessage().startsWith(exceptionMessage));
		}

		static Stream<Arguments> inconsistentSetupComponentVerificationDataPayloadContent() {
			return Stream.of(
					Arguments.of("setupComponentVerificationDataPayload.wrong-chunk.0.json",
							"The chunkId in SetupComponentVerificationDataPayload is not correct."),
					Arguments.of("setupComponentVerificationDataPayload.wrong-eeid.0.json",
							"The electionEventId in SetupComponentVerificationDataPayload is not correct."),
					Arguments.of("setupComponentVerificationDataPayload.wrong-vcsid.0.json",
							"The verificationCardSetId in SetupComponentVerificationDataPayload is not correct.")
			);
		}

	}
}
