/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInput;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInputService;
import ch.post.it.evoting.tools.disputeresolver.process.output.DisputeResolverOutputService;
import ch.post.it.evoting.tools.disputeresolver.protocol.CheckExtractedElectionEventConsistencyService;
import ch.post.it.evoting.tools.disputeresolver.protocol.CheckVoteConfirmationConsistencyService;
import ch.post.it.evoting.tools.disputeresolver.protocol.CheckVoteConsistencyService;

@DisplayName("DisputeResolverService calling run")
class DisputeResolverServiceTest {

	private final DisputeResolverInputService disputeResolverInputService = mock(DisputeResolverInputService.class);
	private final DisputeResolverOutputService disputeResolverOutputService = mock(DisputeResolverOutputService.class);
	private final CheckVoteConsistencyService checkVoteConsistencyService = mock(CheckVoteConsistencyService.class);
	private final CheckVoteConfirmationConsistencyService checkVoteConfirmationConsistencyService = mock(
			CheckVoteConfirmationConsistencyService.class);
	private final CheckExtractedElectionEventConsistencyService checkExtractedElectionEventConsistencyService = mock(
			CheckExtractedElectionEventConsistencyService.class);

	private final ControlComponentExtractedElectionEventPayloadGenerator controlComponentExtractedElectionEventPayloadGenerator = new ControlComponentExtractedElectionEventPayloadGenerator();
	private final ControlComponentExtractedVerificationCardsPayloadGenerator controlComponentExtractedVerificationCardsPayloadGenerator = new ControlComponentExtractedVerificationCardsPayloadGenerator();
	private final DisputeResolverService disputeResolverService = new DisputeResolverService(
			disputeResolverInputService,
			checkVoteConsistencyService,
			disputeResolverOutputService,
			checkVoteConfirmationConsistencyService,
			checkExtractedElectionEventConsistencyService);
	private ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes;
	private long numberOfExtractedVerificationCards;
	private DisputeResolverInput disputeResolverInput;
	private String electionEventId;

	@BeforeEach
	void setUp() {
		final ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads = controlComponentExtractedElectionEventPayloadGenerator.generate();

		// Ensure that we have at least one confirmed vote to resolve the dispute.
		ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads;
		ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotesList;
		do {
			controlComponentExtractedVerificationCardsPayloads = controlComponentExtractedVerificationCardsPayloadGenerator.generate(
					controlComponentExtractedElectionEventPayloads.getFirst());
			resolvedConfirmedVotesList = controlComponentExtractedVerificationCardsPayloads.stream()
					.map(ControlComponentExtractedVerificationCardsPayload::getExtractedVerificationCards)
					.flatMap(ImmutableList::stream)
					.filter(ExtractedVerificationCard::isConfirmed)
					.map(extractedVerificationCard -> new ResolvedConfirmedVote(extractedVerificationCard.verificationCardId(),
							extractedVerificationCard.verificationCardSetId(), extractedVerificationCard.hashedLongVoteCastReturnCodeShares()))
					.distinct()
					.sorted(Comparator.comparing(ResolvedConfirmedVote::verificationCardId))
					.collect(toImmutableList());
		} while (resolvedConfirmedVotesList.isEmpty());

		disputeResolverInput = new DisputeResolverInput(
				controlComponentExtractedElectionEventPayloads,
				controlComponentExtractedVerificationCardsPayloads
		);

		electionEventId = disputeResolverInput.electionEventId();
		resolvedConfirmedVotes = resolvedConfirmedVotesList;
		numberOfExtractedVerificationCards = controlComponentExtractedVerificationCardsPayloads.stream()
				.map(ControlComponentExtractedVerificationCardsPayload::getExtractedVerificationCards)
				.flatMap(ImmutableList::stream)
				.count();
	}

	@Test
	@DisplayName("behaves as expected.")
	void runHappyPath() {
		when(disputeResolverInputService.read()).thenReturn(disputeResolverInput);
		when(checkExtractedElectionEventConsistencyService.checkExtractedElectionEventConsistency(disputeResolverInput)).thenReturn(true);
		when(checkVoteConsistencyService.checkVoteConsistency(disputeResolverInput)).thenReturn(true);
		when(checkVoteConfirmationConsistencyService.checkVoteConfirmationConsistency(any())).thenReturn(resolvedConfirmedVotes);
		doNothing().when(disputeResolverOutputService).save(any());

		assertDoesNotThrow(disputeResolverService::run);
	}

	@Test
	@DisplayName("throws an IllegalStateException when CheckExtractedElectionEventConsistency does not succeed.")
	void runThrowsWhenCheckExtractedElectionEventConsistencyDoesNotSucceed() {
		when(disputeResolverInputService.read()).thenReturn(disputeResolverInput);
		when(checkExtractedElectionEventConsistencyService.checkExtractedElectionEventConsistency(disputeResolverInput)).thenReturn(false);

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, disputeResolverService::run);

		assertEquals(String.format("The extracted election events are not consistent. [electionEventId: %s]", electionEventId),
				illegalStateException.getMessage());
	}

	@Test
	@DisplayName("throws an IllegalStateException when CheckVoteConsistency does not succeed.")
	void runThrowsWhenCheckVoteConsistencyDoesNotSucceed() {
		when(disputeResolverInputService.read()).thenReturn(disputeResolverInput);
		when(checkExtractedElectionEventConsistencyService.checkExtractedElectionEventConsistency(disputeResolverInput)).thenReturn(true);
		when(checkVoteConsistencyService.checkVoteConsistency(disputeResolverInput)).thenReturn(false);

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, disputeResolverService::run);

		assertEquals(String.format("The votes are not consistent. [electionEventId: %s]", electionEventId), illegalStateException.getMessage());
	}

	@Test
	@DisplayName("throws an IllegalStateException when CheckVoteConfirmationConsistency returns more resolved confirmed votes than extracted verification cards.")
	void runThrowsWhenCheckVoteConfirmationConsistencyReturnsMoreResolvedConfirmedVotesThanExtractedVerificationCards() {
		when(disputeResolverInputService.read()).thenReturn(disputeResolverInput);
		when(checkExtractedElectionEventConsistencyService.checkExtractedElectionEventConsistency(disputeResolverInput)).thenReturn(true);
		when(checkVoteConsistencyService.checkVoteConsistency(disputeResolverInput)).thenReturn(true);

		final ImmutableList<String> hashedLongVoteCastReturnCodeShares = resolvedConfirmedVotes.getFirst().hashedLongVoteCastReturnCodeShares();
		when(checkVoteConfirmationConsistencyService.checkVoteConfirmationConsistency(any())).thenReturn(
				IntStream.rangeClosed(0, (int) numberOfExtractedVerificationCards)
						.mapToObj(i -> new ResolvedConfirmedVote(
								UUIDGenerator.getInstance().generate(),
								UUIDGenerator.getInstance().generate(),
								hashedLongVoteCastReturnCodeShares))
						.collect(toImmutableList())
		);

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, disputeResolverService::run);

		assertEquals(String.format("The resolved confirmed votes must not be larger than the extracted verification cards. [electionEventId: %s]",
				electionEventId), illegalStateException.getMessage());
	}

}
