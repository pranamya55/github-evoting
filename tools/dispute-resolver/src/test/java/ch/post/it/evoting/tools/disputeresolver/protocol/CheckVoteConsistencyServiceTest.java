/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedVerificationCardGenerator;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInput;

@DisplayName("CheckVoteConsistencyService with")
class CheckVoteConsistencyServiceTest {

	private static ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads;
	private static ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads;
	private static CheckVoteConsistencyService checkVoteConsistencyService;

	@BeforeAll
	static void setUpAll() {
		final ControlComponentExtractedVerificationCardsPayloadGenerator controlComponentExtractedVerificationCardsPayloadGenerator = new ControlComponentExtractedVerificationCardsPayloadGenerator();

		controlComponentExtractedVerificationCardsPayloads = controlComponentExtractedVerificationCardsPayloadGenerator.generate();
		final String electionEventId = controlComponentExtractedVerificationCardsPayloads.getFirst().getElectionEventId();
		final ControlComponentExtractedElectionEventPayloadGenerator controlComponentExtractedElectionEventPayloadGenerator = new ControlComponentExtractedElectionEventPayloadGenerator();
		controlComponentExtractedElectionEventPayloads = ControlComponentNode.ids().stream()
				.map(nodeId -> controlComponentExtractedElectionEventPayloadGenerator.generate(electionEventId, nodeId))
				.collect(toImmutableList());

		final CheckVoteConsistencyAlgorithm checkVoteConsistencyAlgorithm = new CheckVoteConsistencyAlgorithm(BaseEncodingFactory.createBase64(),
				HashFactory.createHash());
		checkVoteConsistencyService = new CheckVoteConsistencyService(checkVoteConsistencyAlgorithm);
	}

	@Test
	@DisplayName("null input throws NullPointerException.")
	void nullInputThrows() {
		assertThrows(NullPointerException.class, () -> checkVoteConsistencyService.checkVoteConsistency(null));
	}

	@Test
	@DisplayName("inconsistent encrypted votes returns false.")
	void invalidReturnsFalse() {
		final ExtractedVerificationCard otherExtractedVerificationCard = new ExtractedVerificationCardGenerator().generate();
		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> inconsistentVote = controlComponentExtractedVerificationCardsPayloads.stream()
				.map(payload -> payload.getNodeId() == 3 ?
						new ControlComponentExtractedVerificationCardsPayload(payload.getEncryptionGroup(), payload.getElectionEventId(),
								payload.getNodeId(), ImmutableList.of(otherExtractedVerificationCard)) :
						payload
				).collect(toImmutableList());

		final DisputeResolverInput disputeResolverInput = new DisputeResolverInput(
				controlComponentExtractedElectionEventPayloads,
				inconsistentVote);

		assertFalse(() -> checkVoteConsistencyService.checkVoteConsistency(disputeResolverInput));
	}

	@Test
	@DisplayName("consistent encrypted votes returns true.")
	void validReturnsTrue() {
		final DisputeResolverInput disputeResolverInput = new DisputeResolverInput(
				controlComponentExtractedElectionEventPayloads,
				controlComponentExtractedVerificationCardsPayloads);
		assertTrue(() -> checkVoteConsistencyService.checkVoteConsistency(disputeResolverInput));
	}
}
