/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponent.process.EncryptedVerifiableVoteService;
import ch.post.it.evoting.controlcomponent.process.HashedLVCCSharesService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.generators.ControlComponentBallotBoxPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("An ExtractVerificationCardsAlgorithm with")
class ExtractVerificationCardsAlgorithmTest {

	private static final VerificationCardStateService verificationCardStateService = mock(VerificationCardStateService.class);
	private static final HashedLVCCSharesService hashedLVCCSharesService = mock(HashedLVCCSharesService.class);
	private static final EncryptedVerifiableVoteService encryptedVerifiableVoteService = mock(EncryptedVerifiableVoteService.class);

	private static String electionEventId;
	private static ExtractVerificationCardsAlgorithm extractVerificationCardsAlgorithm;

	@BeforeAll
	static void setUpAll() {
		extractVerificationCardsAlgorithm = new ExtractVerificationCardsAlgorithm(verificationCardStateService, hashedLVCCSharesService,
				encryptedVerifiableVoteService);

		electionEventId = UUIDGenerator.getInstance().generate();

		mockDB();
	}

	@Test
	@DisplayName("valid parameter does not throw")
	void validParamDoesNotThrow() {
		assertDoesNotThrow(() -> extractVerificationCardsAlgorithm.extractVerificationCards(electionEventId));
	}

	@Test
	@DisplayName("null election event id throws NullPointerException")
	void nullElectionEventIdThrows() {
		assertThrows(NullPointerException.class, () -> extractVerificationCardsAlgorithm.extractVerificationCards(null));
	}

	@Test
	@DisplayName("non UUID election event id throws FailedValidationException")
	void nonUUIDElectionEventIdThrows() {
		assertThrows(FailedValidationException.class, () -> extractVerificationCardsAlgorithm.extractVerificationCards("non-UUID"));
	}

	private static void mockDB() {
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContext electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();

		final Random random = RandomFactory.createRandom();
		final Base64Alphabet base64Alphabet = Base64Alphabet.getInstance();
		final ControlComponentBallotBoxPayloadGenerator controlComponentBallotBoxPayloadGenerator = new ControlComponentBallotBoxPayloadGenerator(
				electionEventContext.encryptionGroup());

		electionEventContext.verificationCardSetContexts().stream()
				.map(VerificationCardSetContext::getVerificationCardSetId)
				.forEach(verificationCardSetId -> {

					// Create encrypted votes.
					final ControlComponentBallotBoxPayload controlComponentBallotBoxPayload = controlComponentBallotBoxPayloadGenerator.generate(
									electionEventId, verificationCardSetId, electionEventContext.maximumNumberOfSelections(),
									electionEventContext.maximumNumberOfWriteInsPlusOne())
							.getFirst();
					when(encryptedVerifiableVoteService.getSentVotes(electionEventId))
							.thenReturn(controlComponentBallotBoxPayload.getConfirmedEncryptedVotes());
					when(verificationCardStateService.isConfirmedVote(verificationCardSetId))
							.thenReturn(random.genRandomInteger(2) == 0);

					// Create hashed LVCC Shares.
					controlComponentBallotBoxPayload.getConfirmedEncryptedVotes()
							.stream()
							.map(EncryptedVerifiableVote::contextIds)
							.map(ContextIds::verificationCardId)
							.forEach(verificationCardId -> {
										final ImmutableList<String> hashedLongVoteCastReturnCodeShares = ControlComponentNode.ids().stream()
												.map(j -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
												.collect(ImmutableList.toImmutableList());
										when(hashedLVCCSharesService.getHashedLVCCShares(verificationCardId))
												.thenReturn(hashedLongVoteCastReturnCodeShares);
									}
							);
				});
	}
}