/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.generators.ControlComponentCodeSharesPayloadGenerator;
import ch.post.it.evoting.domain.generators.SetupComponentVerificationDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("EncryptedNodeLongReturnCodeSharesChunk")
class EncryptedNodeLongReturnCodeSharesChunkTest {

	private static ControlComponentCodeSharesPayloadGenerator controlComponentCodeSharesPayloadGenerator;
	private static int numberOfEligibleVoters;
	private static int numberOfVotingOptions;
	private static String electionEventId;
	private static String verificationCardSetId;
	private static int chunkId;
	private static ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> controlComponentCodeSharesChunks;
	private static ImmutableList<SetupComponentVerificationData> setupComponentVerificationData;

	@BeforeAll
	static void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();

		final Random random = RandomFactory.createRandom();
		chunkId = random.genRandomInteger(10);

		numberOfEligibleVoters = 2;
		numberOfVotingOptions = 3;
		final ImmutableList<String> verificationCardIds = Stream.generate(uuidGenerator::generate)
				.limit(numberOfEligibleVoters).collect(ImmutableList.toImmutableList());
		controlComponentCodeSharesPayloadGenerator = new ControlComponentCodeSharesPayloadGenerator();
		final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayload = controlComponentCodeSharesPayloadGenerator.generate(
				electionEventId, verificationCardSetId, chunkId, verificationCardIds, numberOfVotingOptions);
		controlComponentCodeSharesChunks = controlComponentCodeSharesPayload.stream().map(EncryptedSingleNodeLongReturnCodeSharesChunk::new)
				.collect(ImmutableList.toImmutableList());

		final SetupComponentVerificationDataPayloadGenerator setupComponentVerificationDataPayloadGenerator = new SetupComponentVerificationDataPayloadGenerator(
				controlComponentCodeSharesPayload.getFirst().getEncryptionGroup());
		setupComponentVerificationData = setupComponentVerificationDataPayloadGenerator.generate(electionEventId, verificationCardSetId, chunkId,
				verificationCardIds, numberOfVotingOptions).getSetupComponentVerificationData();
	}

	private static Stream<Arguments> provideNullInputsForEncryptedSingleNodeLongReturnCodeSharesChunk() {
		return Stream.of(Arguments.of(null, verificationCardSetId, chunkId, controlComponentCodeSharesChunks, setupComponentVerificationData),
				Arguments.of(electionEventId, null, chunkId, controlComponentCodeSharesChunks, setupComponentVerificationData),
				Arguments.of(electionEventId, verificationCardSetId, chunkId, null, controlComponentCodeSharesChunks, setupComponentVerificationData),
				Arguments.of(electionEventId, verificationCardSetId, chunkId, null, setupComponentVerificationData),
				Arguments.of(electionEventId, verificationCardSetId, chunkId, controlComponentCodeSharesChunks, null));
	}

	@ParameterizedTest
	@MethodSource("provideNullInputsForEncryptedSingleNodeLongReturnCodeSharesChunk")
	@DisplayName("calling builder with null values throws NullPointerException")
	void buildWithInvalidValuesThrows(final String electionEventId, final String verificationCardSetId, final int chunkId,
			final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> encryptedSingleNodeLongReturnCodeSharesChunks,
			final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData) {
		final EncryptedNodeLongReturnCodeSharesChunk.Builder builder = new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setControlComponentCodeSharesChunks(encryptedSingleNodeLongReturnCodeSharesChunks)
				.setSetupComponentVerificationData(setupComponentVerificationData);
		assertThrows(NullPointerException.class, builder::build);
	}

	private static Stream<Arguments> provideInvalidUUIDInputsForEncryptedSingleNodeLongReturnCodeSharesChunk() {
		return Stream.of(Arguments.of("invalid UUID", verificationCardSetId), Arguments.of(electionEventId, "invalid UUID"));
	}

	@ParameterizedTest
	@MethodSource("provideInvalidUUIDInputsForEncryptedSingleNodeLongReturnCodeSharesChunk")
	@DisplayName("calling builder with null values throws FailedValidationException")
	void buildWithInvalidUUIDValuesThrows(final String electionEventId, final String verificationCardSetId) {
		final EncryptedNodeLongReturnCodeSharesChunk.Builder builder = new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setControlComponentCodeSharesChunks(controlComponentCodeSharesChunks)
				.setSetupComponentVerificationData(setupComponentVerificationData);
		assertThrows(FailedValidationException.class, builder::build);
	}

	@Test
	@DisplayName("calling builder with invalid node id values throws IllegalArgumentException")
	void buildWithInvalidNodeIdValues() {
		final EncryptedNodeLongReturnCodeSharesChunk.Builder builder = new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setSetupComponentVerificationData(setupComponentVerificationData);

		// Wrong size node return codes values -> node ids (1, 2, 3)
		final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> wrongSizeNodes = controlComponentCodeSharesChunks.stream()
				.filter(chunk -> chunk.getNodeId() != ControlComponentNode.FOUR.id()).collect(ImmutableList.toImmutableList());
		final EncryptedNodeLongReturnCodeSharesChunk.Builder wrongSizeBuilder = builder.setControlComponentCodeSharesChunks(wrongSizeNodes);

		// Missing node id return codes values -> node ids (1, 2, 3, 3)
		final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> missingNodeId = IntStream.range(0, controlComponentCodeSharesChunks.size())
				.mapToObj(
						i -> controlComponentCodeSharesChunks.get(i == ControlComponentNode.FOUR.id() - 1 ? ControlComponentNode.THREE.id() - 1 : i))
				.collect(ImmutableList.toImmutableList());
		final EncryptedNodeLongReturnCodeSharesChunk.Builder missingNodeIdBuilder = builder.setControlComponentCodeSharesChunks(missingNodeId);

		assertAll(() -> assertThrows(IllegalArgumentException.class, wrongSizeBuilder::build),
				() -> assertThrows(IllegalArgumentException.class, missingNodeIdBuilder::build));

	}

	@Test
	@DisplayName("calling builder with negative chunk id values throws IllegalArgumentException")
	void buildWithNegativeChunkIdValues() {
		final EncryptedNodeLongReturnCodeSharesChunk.Builder negativeChunkIdBuilder = new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(-1)
				.setControlComponentCodeSharesChunks(controlComponentCodeSharesChunks)
				.setSetupComponentVerificationData(setupComponentVerificationData);

		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, negativeChunkIdBuilder::build);
		assertTrue(illegalArgumentException.getMessage().startsWith("The chunk id must be positive."));

	}

	@Test
	@DisplayName("calling builder with different verification card ids count throw IllegalArgumentException")
	void buildWithDifferentCount() {
		final ControlComponentCodeSharesPayload wrongNumberOfEligibleVotersPayload = controlComponentCodeSharesPayloadGenerator.generate(
						electionEventId, verificationCardSetId, chunkId, numberOfEligibleVoters + 1, numberOfVotingOptions).stream()
				.filter(chunk -> ControlComponentNode.ONE.id() == chunk.getNodeId())
				.findFirst()
				.orElseThrow();
		final EncryptedSingleNodeLongReturnCodeSharesChunk wrongNumberOfEligibleVoters = new EncryptedSingleNodeLongReturnCodeSharesChunk(
				new ControlComponentCodeSharesPayload(wrongNumberOfEligibleVotersPayload.getElectionEventId(),
						wrongNumberOfEligibleVotersPayload.getVerificationCardSetId(), wrongNumberOfEligibleVotersPayload.getChunkId(),
						wrongNumberOfEligibleVotersPayload.getEncryptionGroup(), wrongNumberOfEligibleVotersPayload.getControlComponentCodeShares(),
						ControlComponentNode.FOUR.id()));
		final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> differentNumberOfVerificationCardIds = IntStream.range(0,
						controlComponentCodeSharesChunks.size())
				.mapToObj(i -> i == ControlComponentNode.FOUR.id() - 1 ? wrongNumberOfEligibleVoters : controlComponentCodeSharesChunks.get(i))
				.collect(ImmutableList.toImmutableList());

		final EncryptedNodeLongReturnCodeSharesChunk.Builder builder = new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setControlComponentCodeSharesChunks(differentNumberOfVerificationCardIds)
				.setSetupComponentVerificationData(setupComponentVerificationData);
		final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
		assertTrue(ex.getMessage().startsWith("All nodes must return the same verification card ids."));
	}

	@Test
	@DisplayName("calling builder with different verification card ids within nodes throw IllegalArgumentException")
	void buildWithDifferentValuesWithinNodes() {
		final ControlComponentCodeSharesPayload differentVerificationCardIdsPayload = controlComponentCodeSharesPayloadGenerator.generate(
						electionEventId, verificationCardSetId, chunkId, numberOfEligibleVoters, numberOfVotingOptions).stream()
				.filter(chunk -> ControlComponentNode.ONE.id() == chunk.getNodeId()).findFirst()
				.orElseThrow();
		final EncryptedSingleNodeLongReturnCodeSharesChunk differentVerificationCardIds = new EncryptedSingleNodeLongReturnCodeSharesChunk(
				new ControlComponentCodeSharesPayload(differentVerificationCardIdsPayload.getElectionEventId(),
						differentVerificationCardIdsPayload.getVerificationCardSetId(), differentVerificationCardIdsPayload.getChunkId(),
						differentVerificationCardIdsPayload.getEncryptionGroup(), differentVerificationCardIdsPayload.getControlComponentCodeShares(),
						ControlComponentNode.FOUR.id()));
		final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> differentVerificationCardIdsChunks = IntStream.range(0,
						controlComponentCodeSharesChunks.size())
				.mapToObj(i -> i == ControlComponentNode.FOUR.id() - 1 ? differentVerificationCardIds : controlComponentCodeSharesChunks.get(i))
				.collect(ImmutableList.toImmutableList());

		final EncryptedNodeLongReturnCodeSharesChunk.Builder builder = new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setControlComponentCodeSharesChunks(differentVerificationCardIdsChunks)
				.setSetupComponentVerificationData(setupComponentVerificationData);
		final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
		assertTrue(ex.getMessage().startsWith("All nodes must return the same verification card ids."));
	}

	@Test
	@DisplayName("calling builder with different verification card set ids throw IllegalArgumentException")
	void buildWithDifferentValues() {
		final ImmutableList<ControlComponentCodeSharesPayload> differentVerificationCardIdsPayload = controlComponentCodeSharesPayloadGenerator.generate(
				electionEventId, verificationCardSetId, chunkId, numberOfEligibleVoters, numberOfVotingOptions);
		final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> differentVerificationCardIdsChunks = differentVerificationCardIdsPayload.stream()
				.map(EncryptedSingleNodeLongReturnCodeSharesChunk::new).collect(ImmutableList.toImmutableList());
		final EncryptedNodeLongReturnCodeSharesChunk.Builder builder = new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setControlComponentCodeSharesChunks(differentVerificationCardIdsChunks)
				.setSetupComponentVerificationData(setupComponentVerificationData);
		final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);
		assertTrue(ex.getMessage().startsWith(
				"The setup component verification data verification card ids must match the control component code shares verification card ids."));
	}

	@Test
	@DisplayName("calling builder with valid values does not throw")
	void buildWithValidValues() {
		final EncryptedNodeLongReturnCodeSharesChunk.Builder builder = new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setControlComponentCodeSharesChunks(controlComponentCodeSharesChunks)
				.setSetupComponentVerificationData(setupComponentVerificationData);
		assertDoesNotThrow(builder::build);
	}
}