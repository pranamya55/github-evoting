/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ResolvedConfirmedVote;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCardSet;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.generators.ExtractedElectionEventGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementAlgorithm;

@DisplayName("CheckVoteConfirmationConsistencyAlgorithm with")
class CheckVoteConfirmationConsistencyAlgorithmTest {

	private static final Base64 base64 = BaseEncodingFactory.createBase64();
	private static final Hash hash = HashFactory.createHash();

	private static ExtractedElectionEvent context;
	private static ImmutableList<ImmutableList<ExtractedVerificationCard>> input;
	private static CheckVoteConfirmationConsistencyAlgorithm checkVoteConfirmationConsistencyAlgorithm;

	@BeforeAll
	static void setUpAll() {
		final ExtractedElectionEventGenerator extractedElectionEventGenerator = new ExtractedElectionEventGenerator();
		final ExtractedElectionEvent extractedElectionEvent = extractedElectionEventGenerator.generate();
		final String electionEventId = extractedElectionEvent.electionEventId();
		final ImmutableList<String> verificationCardSetIds = extractedElectionEvent.extractedVerificationCardSets().stream()
				.map(ExtractedVerificationCardSet::verificationCardSetId)
				.collect(toImmutableList());

		final ControlComponentExtractedVerificationCardsPayloadGenerator payloadsGenerator = new ControlComponentExtractedVerificationCardsPayloadGenerator();
		input = payloadsGenerator.generate(electionEventId, verificationCardSetIds).stream()
				.map(ControlComponentExtractedVerificationCardsPayload::getExtractedVerificationCards)
				.collect(toImmutableList());

		final Map<String, ImmutableList<String>> lvccAllowListMap = getValidLVCCAllowList(electionEventId, input);

		context = getValidExtractedElectionEvent(extractedElectionEvent, lvccAllowListMap);

		final ConfirmVoteAgreementAlgorithm confirmVoteAgreementAlgorithm = new ConfirmVoteAgreementAlgorithm(hash, base64);
		checkVoteConfirmationConsistencyAlgorithm = new CheckVoteConfirmationConsistencyAlgorithm(confirmVoteAgreementAlgorithm);
	}

	@Test
	@DisplayName("null parameters throws NullPointerException.")
	void nullParametersThrows() {
		assertThrows(NullPointerException.class, () -> checkVoteConfirmationConsistencyAlgorithm.checkVoteConfirmationConsistency(null, input));
		assertThrows(NullPointerException.class, () -> checkVoteConfirmationConsistencyAlgorithm.checkVoteConfirmationConsistency(context, null));
	}

	@Test
	@DisplayName("not enough extracted verification cards throws IllegalArgumentException.")
	void notEnoughExtractedVerificationCardsThrows() {
		final ImmutableList<ImmutableList<ExtractedVerificationCard>> notEnoughExtractedVerificationCard = input.subList(0, 3);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> checkVoteConfirmationConsistencyAlgorithm.checkVoteConfirmationConsistency(context, notEnoughExtractedVerificationCard));

		final String expected = "There must be as many CCR's extracted verification cards as node ids.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("too many extracted verification cards throws IllegalArgumentException.")
	void tooManyExtractedVerificationCardsThrows() {
		final ImmutableList<ImmutableList<ExtractedVerificationCard>> tooManyExtractedVerificationCard = input.append(input.getFirst());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> checkVoteConfirmationConsistencyAlgorithm.checkVoteConfirmationConsistency(context, tooManyExtractedVerificationCard));

		final String expected = "There must be as many CCR's extracted verification cards as node ids.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("different encryption group throws IllegalArgumentException.")
	void differentEncryptionGroupThrows() {
		final GqGroup otherGroup = GroupTestData.getGroupP59();
		final ExtractedElectionEventGenerator otherGroupGenerator = new ExtractedElectionEventGenerator(otherGroup);
		final ExtractedElectionEvent otherGroupContext = otherGroupGenerator.generate();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> checkVoteConfirmationConsistencyAlgorithm.checkVoteConfirmationConsistency(otherGroupContext, input));

		final String expected = "The context and input must have the same encryption group.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("consistent vote confirmations returns expected resolved confirmed votes.")
	void validReturnsExpected() {
		final ImmutableList<ResolvedConfirmedVote> expected = input.stream()
				.flatMap(ImmutableList::stream)
				.filter(ExtractedVerificationCard::isConfirmed)
				.map(extractedVerificationCard -> new ResolvedConfirmedVote(extractedVerificationCard.verificationCardId(),
						extractedVerificationCard.verificationCardSetId(), extractedVerificationCard.hashedLongVoteCastReturnCodeShares()))
				.distinct()
				.sorted(Comparator.comparing(ResolvedConfirmedVote::verificationCardId))
				.collect(toImmutableList());

		final ImmutableList<ResolvedConfirmedVote> actual = checkVoteConfirmationConsistencyAlgorithm.checkVoteConfirmationConsistency(context,
				input);

		assertEquals(expected, actual);
	}

	private static Map<String, ImmutableList<String>> getValidLVCCAllowList(final String electionEventId,
			final ImmutableList<ImmutableList<ExtractedVerificationCard>> extractedVerificationCardSets) {
		return extractedVerificationCardSets.stream()
				.flatMap(ImmutableList::stream)
				.filter(ExtractedVerificationCard::isConfirmed)
				.collect(Collectors.toMap(
						ExtractedVerificationCard::verificationCardSetId,
						extractedVerificationCard -> ImmutableList.of(base64.base64Encode(hash.recursiveHash(
								AuxiliaryInformation.of("VerifyLVCCHash", electionEventId,
										extractedVerificationCard.verificationCardSetId(),
										extractedVerificationCard.verificationCardId()),
								HashableString.from(extractedVerificationCard.hashedLongVoteCastReturnCodeShares().get(0)),
								HashableString.from(extractedVerificationCard.hashedLongVoteCastReturnCodeShares().get(1)),
								HashableString.from(extractedVerificationCard.hashedLongVoteCastReturnCodeShares().get(2)),
								HashableString.from(extractedVerificationCard.hashedLongVoteCastReturnCodeShares().get(3))))),
						(evc1, evc2) -> !evc1.containsAll(evc2) ? evc1.append(evc2) : evc1)
				);
	}

	private static ExtractedElectionEvent getValidExtractedElectionEvent(final ExtractedElectionEvent extractedElectionEvent,
			final Map<String, ImmutableList<String>> lvccAllowListMap) {
		return new ExtractedElectionEvent(
				extractedElectionEvent.hashElectionEventContext(),
				extractedElectionEvent.encryptionGroup(),
				extractedElectionEvent.electionEventId(),
				extractedElectionEvent.extractedVerificationCardSets().stream()
						.map(extractedVerificationCardSet -> {
									final ImmutableList<String> lvccAllowList = lvccAllowListMap.containsKey(
											extractedVerificationCardSet.verificationCardSetId()) ?
											lvccAllowListMap.get(extractedVerificationCardSet.verificationCardSetId()) :
											extractedVerificationCardSet.longVoteCastReturnCodesAllowList();

									return new ExtractedVerificationCardSet(
											extractedVerificationCardSet.hashContext(),
											extractedVerificationCardSet.verificationCardSetId(),
											extractedVerificationCardSet.partialChoiceReturnCodesAllowList().subList(0, lvccAllowList.size()),
											lvccAllowList);
								}
						).collect(toImmutableList())
		);
	}
}