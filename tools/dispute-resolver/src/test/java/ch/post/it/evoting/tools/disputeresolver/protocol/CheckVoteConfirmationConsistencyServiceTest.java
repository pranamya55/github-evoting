/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.protocol;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedElectionEventPayloadGenerator;
import ch.post.it.evoting.domain.generators.ControlComponentExtractedVerificationCardsPayloadGenerator;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.disputeresolver.ConfirmVoteAgreementAlgorithm;
import ch.post.it.evoting.tools.disputeresolver.process.input.DisputeResolverInput;

@DisplayName("CheckVoteConfirmationConsistencyService with")
class CheckVoteConfirmationConsistencyServiceTest {

	private static ImmutableList<ControlComponentExtractedVerificationCardsPayload> controlComponentExtractedVerificationCardsPayloads;
	private static ImmutableList<ControlComponentExtractedElectionEventPayload> controlComponentExtractedElectionEventPayloads;
	private static CheckVoteConfirmationConsistencyService checkVoteConfirmationConsistencyService;

	@BeforeAll
	static void setUpAll() {
		final ControlComponentExtractedElectionEventPayloadGenerator controlComponentExtractedElectionEventPayloadGenerator =
				new ControlComponentExtractedElectionEventPayloadGenerator();
		controlComponentExtractedElectionEventPayloads = controlComponentExtractedElectionEventPayloadGenerator.generate();
		final ControlComponentExtractedVerificationCardsPayloadGenerator controlComponentExtractedVerificationCardsPayloadGenerator =
				new ControlComponentExtractedVerificationCardsPayloadGenerator();
		controlComponentExtractedVerificationCardsPayloads =
				controlComponentExtractedVerificationCardsPayloadGenerator.generate(controlComponentExtractedElectionEventPayloads.getFirst());

		final ConfirmVoteAgreementAlgorithm confirmVoteAgreementAlgorithm = new ConfirmVoteAgreementAlgorithm(
				HashFactory.createHash(),
				BaseEncodingFactory.createBase64());
		final CheckVoteConfirmationConsistencyAlgorithm checkVoteConfirmationConsistencyAlgorithm = new CheckVoteConfirmationConsistencyAlgorithm(
				confirmVoteAgreementAlgorithm);
		checkVoteConfirmationConsistencyService = new CheckVoteConfirmationConsistencyService(checkVoteConfirmationConsistencyAlgorithm);
	}

	@Test
	@DisplayName("null parameter throws a NullPointerException.")
	void nullParameterThrows() {
		assertThrows(NullPointerException.class, () -> checkVoteConfirmationConsistencyService.checkVoteConfirmationConsistency(null));
	}

	@Test
	@DisplayName("unknown verification card set ids throws IllegalArgumentException.")
	void unknownVerificationCardSetIdsThrows() {
		final ImmutableList<ControlComponentExtractedVerificationCardsPayload> unknownVerificationCardSetIdsPayloads = controlComponentExtractedVerificationCardsPayloads.stream()
				.map(payload -> new ControlComponentExtractedVerificationCardsPayload(payload.getEncryptionGroup(), payload.getElectionEventId(),
						payload.getNodeId(),
						payload.getExtractedVerificationCards().stream()
								.map(extractedVerificationCard -> new ExtractedVerificationCard(
										extractedVerificationCard.verificationCardId(),
										controlComponentExtractedVerificationCardsPayloads.getFirst().getElectionEventId(),
										extractedVerificationCard.encryptedVote(),
										extractedVerificationCard.hashedLongVoteCastReturnCodeShares())
								).collect(toImmutableList()))
				).collect(toImmutableList());

		final DisputeResolverInput disputeResolverInput = new DisputeResolverInput(controlComponentExtractedElectionEventPayloads,
				unknownVerificationCardSetIdsPayloads);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> checkVoteConfirmationConsistencyService.checkVoteConfirmationConsistency(disputeResolverInput)
		);

		assertEquals(
				"All verification card set ids in the control component extracted verification cards payloads must be in the control component extracted election event payloads.",
				Throwables.getRootCause(exception).getMessage());
	}
}
