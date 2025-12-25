/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.disputeresolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.Constants;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementAlgorithm;

@DisplayName("Calling updateConfirmedVotingCards with")
class UpdateConfirmedVotingCardsAlgorithmTest {

	private static final UUIDGenerator UUID_GENERATOR = UUIDGenerator.getInstance();
	private static final Random RANDOM = RandomFactory.createRandom();
	private static final Hash HASH = HashFactory.createHash();
	private static final Base64 BASE_64 = BaseEncodingFactory.createBase64();

	private static VerificationCardStateService verificationCardStateService;
	private static UpdateConfirmedVotingCardsAlgorithm updateConfirmedVotingCardsAlgorithm;

	private UpdateConfirmedVotingCardsContext context;
	private UpdateConfirmedVotingCardsInput input;

	@BeforeAll
	static void setUpAll() {
		verificationCardStateService = Mockito.mock(VerificationCardStateService.class);
		final ConfirmVoteAgreementAlgorithm confirmVoteAgreementAlgorithm = new ConfirmVoteAgreementAlgorithm(HASH, BASE_64);
		updateConfirmedVotingCardsAlgorithm = new UpdateConfirmedVotingCardsAlgorithm(verificationCardStateService, confirmVoteAgreementAlgorithm);
	}

	@BeforeEach
	void setUp() {
		final String electionEventId = UUID_GENERATOR.generate();
		final ImmutableList<ResolvedConfirmedVote> resolvedConfirmedVotes = Stream.generate(UUID_GENERATOR::generate)
				.limit(3)
				.map(vcId -> {
					final String vcs = UUID_GENERATOR.generate();
					final ImmutableList<String> hashedLongVoteCastReturnCodeShares = Stream.generate(
									() -> RANDOM.genRandomString(Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance()))
							.limit(4)
							.collect(ImmutableList.toImmutableList());
					return new ResolvedConfirmedVote(vcId, vcs, hashedLongVoteCastReturnCodeShares);
				})
				.collect(ImmutableList.toImmutableList());
		final ImmutableMap<String, ImmutableList<String>> longVoteCastReturnCodesAllowLists = resolvedConfirmedVotes.stream()
				.map(resolvedConfirmedVote -> {
					final String verificationCardSetId = resolvedConfirmedVote.verificationCardSetId();
					final String verificationCardId = resolvedConfirmedVote.verificationCardId();
					final ImmutableList<String> hlVCCShares = resolvedConfirmedVote.hashedLongVoteCastReturnCodeShares();
					final ImmutableList<String> lVCCAllowList = ImmutableList.of(
							getValidLongVoteCastCodeHash(electionEventId, verificationCardSetId, verificationCardId, hlVCCShares));
					return new ImmutableMap.Entry<>(verificationCardSetId, lVCCAllowList);
				})
				.collect(ImmutableMap.toImmutableMap());
		context = new UpdateConfirmedVotingCardsContext(1, electionEventId, longVoteCastReturnCodesAllowLists);
		input = new UpdateConfirmedVotingCardsInput(resolvedConfirmedVotes);
	}

	@AfterEach
	void tearDown() {
		Mockito.reset(verificationCardStateService);
	}

	@Test
	@DisplayName("null arguments throws a NulPointerException")
	void updateConfirmedVotingCardsWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> updateConfirmedVotingCardsAlgorithm.updateConfirmedVotingCards(null, input));
		assertThrows(NullPointerException.class, () -> updateConfirmedVotingCardsAlgorithm.updateConfirmedVotingCards(context, null));
	}

	@Test
	@DisplayName("list with all elements already confirmed returns true")
	void updateConfirmedVotingCardsWithAllElementsAlreadyConfirmedReturnsTrue() {
		when(verificationCardStateService.isNotSentVote(anyString())).thenReturn(false);
		when(verificationCardStateService.isNotConfirmedVote(anyString())).thenReturn(false);

		assertTrue(updateConfirmedVotingCardsAlgorithm.updateConfirmedVotingCards(context, input));
	}

	@Test
	@DisplayName("list with all elements not confirmed but sent returns true")
	void updateConfirmedVotingCardsWithAllElementsNotConfirmedButSentReturnsTrue() {
		when(verificationCardStateService.isNotSentVote(anyString())).thenReturn(false);
		when(verificationCardStateService.isNotConfirmedVote(anyString())).thenReturn(true);

		assertTrue(updateConfirmedVotingCardsAlgorithm.updateConfirmedVotingCards(context, input));
	}

	@Test
	@DisplayName("list with one element not sent returns false")
	void updateConfirmedVotingCardsWithOneElementNotSentReturnsFalse() {
		when(verificationCardStateService.isNotSentVote(anyString())).thenReturn(false, false, true);
		when(verificationCardStateService.isNotConfirmedVote(anyString())).thenReturn(true, false, true);

		assertFalse(updateConfirmedVotingCardsAlgorithm.updateConfirmedVotingCards(context, input));
	}

	@SuppressWarnings("java:S117")
	private String getValidLongVoteCastCodeHash(final String electionEventId, final String votingCardSetId, final String votingCardId,
			final ImmutableList<String> hashedLongVoteCastReturnCodeShares) {
		final AuxiliaryInformation i_aux = AuxiliaryInformation.of("VerifyLVCCHash", electionEventId, votingCardSetId, votingCardId);
		final HashableString hlVCC_id_1 = HashableString.from(hashedLongVoteCastReturnCodeShares.get(0));
		final HashableString hlVCC_id_2 = HashableString.from(hashedLongVoteCastReturnCodeShares.get(1));
		final HashableString hlVCC_id_3 = HashableString.from(hashedLongVoteCastReturnCodeShares.get(2));
		final HashableString hlVCC_id_4 = HashableString.from(hashedLongVoteCastReturnCodeShares.get(3));
		return BASE_64.base64Encode(HASH.recursiveHash(i_aux, hlVCC_id_1, hlVCC_id_2, hlVCC_id_3, hlVCC_id_4));
	}
}