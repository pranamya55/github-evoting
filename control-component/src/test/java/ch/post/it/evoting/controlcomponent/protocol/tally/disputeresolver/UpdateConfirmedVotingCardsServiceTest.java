/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.disputeresolver;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.generators.DisputeResolverResolvedConfirmedVotesPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.domain.common.Constants;

@DisplayName("Calling updateConfirmedVotingCards with")
class UpdateConfirmedVotingCardsServiceTest {

	private static final Random RANDOM = RandomFactory.createRandom();
	private static final Base64Alphabet BASE_64_ALPHABET = Base64Alphabet.getInstance();

	private static UpdateConfirmedVotingCardsAlgorithm updateConfirmedVotingCardsAlgorithm;
	private static UpdateConfirmedVotingCardsService updateConfirmedVotingCardsService;

	private String electionEventId;
	private ImmutableMap<String, ImmutableList<String>> longVoteCastReturnCodesAllowLists;
	private DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload;

	@BeforeAll
	static void setUpAll() {
		final int nodeId = 3;
		updateConfirmedVotingCardsAlgorithm = mock(UpdateConfirmedVotingCardsAlgorithm.class);
		updateConfirmedVotingCardsService = new UpdateConfirmedVotingCardsService(nodeId, updateConfirmedVotingCardsAlgorithm);
	}

	@BeforeEach
	void setUp() {
		final DisputeResolverResolvedConfirmedVotesPayloadGenerator generator = new DisputeResolverResolvedConfirmedVotesPayloadGenerator();
		disputeResolverResolvedConfirmedVotesPayload = generator.generate();
		electionEventId = disputeResolverResolvedConfirmedVotesPayload.getElectionEventId();

		longVoteCastReturnCodesAllowLists = disputeResolverResolvedConfirmedVotesPayload.getResolvedConfirmedVotes().stream()
				.map(ResolvedConfirmedVote::verificationCardSetId)
				.distinct()
				.collect(toImmutableMap(
						Function.identity(),
						ignored -> Stream.generate(() -> RANDOM.genRandomString(Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH, BASE_64_ALPHABET))
								.limit(4)
								.collect(toImmutableList()))
				);
	}

	@AfterEach
	void tearDown() {
		Mockito.reset(updateConfirmedVotingCardsAlgorithm);
	}

	@Test
	@DisplayName("null arguments throws a NulPointerException")
	void updateConfirmedVotingCardsWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> updateConfirmedVotingCardsService.updateConfirmedVotingCards(null, longVoteCastReturnCodesAllowLists,
						disputeResolverResolvedConfirmedVotesPayload));
		assertThrows(NullPointerException.class,
				() -> updateConfirmedVotingCardsService.updateConfirmedVotingCards(electionEventId, null,
						disputeResolverResolvedConfirmedVotesPayload));
		assertThrows(NullPointerException.class,
				() -> updateConfirmedVotingCardsService.updateConfirmedVotingCards(electionEventId, longVoteCastReturnCodesAllowLists,
						null));
	}

	@Test
	@DisplayName("happy path returns true")
	void updateConfirmedVotingCardsWithAllElementsAlreadyConfirmedReturnsTrue() {
		when(updateConfirmedVotingCardsAlgorithm.updateConfirmedVotingCards(any(), any())).thenReturn(true);

		assertTrue(updateConfirmedVotingCardsService.updateConfirmedVotingCards(electionEventId, longVoteCastReturnCodesAllowLists,
				disputeResolverResolvedConfirmedVotesPayload));
	}
}