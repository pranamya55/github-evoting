/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.votingserver.process.voting.VerifyAuthenticationChallengeException;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.InvalidVerificationCardStateException;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.VerificationCardNotFoundException;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.VotingCardSearchDto;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput;

@DisplayName("VerificationCardService calling")
class VerificationCardServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private static VerificationCardRepository verificationCardRepository;
	private static VerificationCardStateRepository verificationCardStateRepository;
	private static VerificationCardService verificationCardService;

	private String verificationCardId;
	private String credentialId;
	private String votingCardId;
	private String partialVotingCardId;

	@BeforeAll
	static void setupAll() {
		verificationCardRepository = mock(VerificationCardRepository.class);
		final VerificationCardSetService verificationCardSetService = mock(VerificationCardSetService.class);
		verificationCardStateRepository = mock(VerificationCardStateRepository.class);
		final VerificationCardStateService verificationCardStateService = new VerificationCardStateService(verificationCardStateRepository);
		final ElectionEventRepository electionEventRepository = mock(ElectionEventRepository.class);
		final ElectionEventService electionEventService = new ElectionEventService(electionEventRepository);
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final BallotBoxRepository ballotBoxRepository = mock(BallotBoxRepository.class);
		final BallotBoxService ballotBoxService = spy(
				new BallotBoxService(objectMapper, ballotBoxRepository, electionEventService, verificationCardSetService));

		verificationCardService = new VerificationCardService(verificationCardRepository, verificationCardStateService,
				ballotBoxService);
	}

	@BeforeEach
	void setup() {
		verificationCardId = uuidGenerator.generate();
		final VerificationCardStateEntity verificationCardStateEntity = new VerificationCardStateEntity(verificationCardId);
		doReturn(Optional.of(verificationCardStateEntity)).when(verificationCardStateRepository).findById(verificationCardId);

		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();
		credentialId = uuidGenerator.generate();
		votingCardId = uuidGenerator.generate();
		partialVotingCardId = votingCardId.substring(0, 3);
		final String baseAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final VerificationCardSetEntity verificationCardSetEntity = Mockito.mock(VerificationCardSetEntity.class);

		Mockito.when(verificationCardSetEntity.getVerificationCardSetId()).thenReturn(verificationCardSetId);

		final SetupComponentVoterAuthenticationData voterAuthenticationData = new SetupComponentVoterAuthenticationData(electionEventId,
				verificationCardSetId, ballotBoxId, verificationCardId, votingCardId, credentialId, baseAuthenticationChallenge);
		final VerificationCardEntity verificationCardEntity = new VerificationCardEntity(verificationCardId, verificationCardSetEntity, credentialId,
				votingCardId, voterAuthenticationData, verificationCardStateEntity);

		doReturn(Optional.of(verificationCardEntity)).when(verificationCardRepository).findById(verificationCardId);
		doReturn(Optional.of(verificationCardEntity)).when(verificationCardRepository).findByVotingCardId(votingCardId);
		doReturn(Optional.of(verificationCardEntity)).when(verificationCardRepository).findByCredentialId(credentialId);
		doReturn(List.of(verificationCardEntity)).when(verificationCardRepository).findTop5ByVotingCardIdStartsWithOrderByVotingCardIdAsc(partialVotingCardId);
	}

	@Nested
	@DisplayName("incrementConfirmationAttempts")
	class IncrementConfirmationAttemptsTest {

		@Test
		@DisplayName("when voting card state is AUTHENTICATION_ATTEMPTS_EXCEEDED then throws IllegalStateException")
		void incrementConfirmationAttemptsWhenStateAuthenticationAtemptsExceededThrows() {
			verificationCardService.incrementAuthenticationAttempts(credentialId);
			verificationCardService.incrementAuthenticationAttempts(credentialId);
			verificationCardService.incrementAuthenticationAttempts(credentialId);
			verificationCardService.incrementAuthenticationAttempts(credentialId);
			verificationCardService.incrementAuthenticationAttempts(credentialId);

			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> verificationCardService.incrementConfirmationAttempts(verificationCardId));
			final String expectedErrorMessage = String.format(
					"The current state does not allow to increment confirmation attempts. [verificationCardId: %s, verificationCardState: %s]",
					verificationCardId, VerificationCardState.AUTHENTICATION_ATTEMPTS_EXCEEDED);
			assertEquals(expectedErrorMessage, exception.getMessage());
		}

		@Test
		@DisplayName("when voting card state is INITIAL then throws IllegalStateException")
		void incrementConfirmationAttemptsWhenStateInitialThrows() {
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> verificationCardService.incrementConfirmationAttempts(verificationCardId));
			final String expectedErrorMessage = String.format(
					"The current state does not allow to increment confirmation attempts. [verificationCardId: %s, verificationCardState: %s]",
					verificationCardId, VerificationCardState.INITIAL);
			assertEquals(expectedErrorMessage, exception.getMessage());
		}

		@Test
		@DisplayName("when voting card state is SENT then throws IllegalStateException")
		void incrementConfirmationAttemptsWhenStateSentThrows() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> verificationCardService.incrementConfirmationAttempts(verificationCardId));
			final String expectedErrorMessage = String.format(
					"The current state does not allow to increment confirmation attempts. [verificationCardId: %s, verificationCardState: %s]",
					verificationCardId, VerificationCardState.SENT);
			assertEquals(expectedErrorMessage, exception.getMessage());
		}

		@Test
		@DisplayName("when voting card state is CONFIRMING then increments confirmation attempts by 1")
		void incrementConfirmationAttemptsWhenStateConfirmingIncrements() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);
			verificationCardService.saveConfirmingState(verificationCardId);
			final Integer confirmationAttempts = assertDoesNotThrow(() -> verificationCardService.incrementConfirmationAttempts(verificationCardId));
			assertEquals(1, confirmationAttempts);
		}

		@Test
		@DisplayName("when voting card state is CONFIRMED then throws IllegalStateException")
		void incrementConfirmationAttemptsWhenStateConfirmedThrows() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);
			verificationCardService.saveConfirmingState(verificationCardId);
			final String shortVoteCastReturnCode = random.genUniqueDecimalStrings(8, 1).get(0);
			verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode);
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> verificationCardService.incrementConfirmationAttempts(verificationCardId));
			final String expectedErrorMessage = String.format(
					"The current state does not allow to increment confirmation attempts. [verificationCardId: %s, verificationCardState: %s]",
					verificationCardId, VerificationCardState.CONFIRMED);
			assertEquals(expectedErrorMessage, exception.getMessage());
		}
	}

	@Nested
	@DisplayName("saveConfirmedState")
	class SaveConfirmedStateTest {

		@Test
		@DisplayName("with state AUTHENTICATION_ATTEMPTS_EXCEEDED throws a VerifyAuthenticationChallengeException")
		void saveConfirmedStateWhenCurrentStateIsAuthenticationAttemptsExceededThrows() {
			verificationCardService.incrementAuthenticationAttempts(credentialId);
			verificationCardService.incrementAuthenticationAttempts(credentialId);
			verificationCardService.incrementAuthenticationAttempts(credentialId);
			verificationCardService.incrementAuthenticationAttempts(credentialId);
			verificationCardService.incrementAuthenticationAttempts(credentialId);

			final String shortVoteCastReturnCode = random.genUniqueDecimalStrings(8, 1).get(0);
			final VerifyAuthenticationChallengeException exception = assertThrows(
					VerifyAuthenticationChallengeException.class,
					() -> verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode));
			assertEquals(VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.VOTING_CARD_BLOCKED, exception.getErrorStatus());

			final VerificationCardState verificationCardState = verificationCardService.getVerificationCardEntity(verificationCardId)
					.getVerificationCardStateEntity().getState();
			assertEquals(VerificationCardState.AUTHENTICATION_ATTEMPTS_EXCEEDED, verificationCardState);
		}

		@Test
		@DisplayName("with state SENT saves CONFIRMED state")
		void saveConfirmedStateWhenCurrentStateIsSentChangesStateToConfirming() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);

			final String shortVoteCastReturnCode = random.genUniqueDecimalStrings(8, 1).get(0);
			assertDoesNotThrow(() -> verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode));

			final VerificationCardState verificationCardState = verificationCardService.getVerificationCardEntity(verificationCardId)
					.getVerificationCardStateEntity().getState();
			assertEquals(VerificationCardState.CONFIRMED, verificationCardState);
		}

		@Test
		@DisplayName("with state CONFIRMING saves CONFIRMED state")
		void saveConfirmedStateWhenCurrentStateIsConfirmingChangesStateToConfirmed() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);
			verificationCardService.saveConfirmingState(verificationCardId);

			final String shortVoteCastReturnCode = random.genUniqueDecimalStrings(8, 1).get(0);
			assertDoesNotThrow(() -> verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode));

			final VerificationCardState verificationCardState = verificationCardService.getVerificationCardEntity(verificationCardId)
					.getVerificationCardStateEntity().getState();
			assertEquals(VerificationCardState.CONFIRMED, verificationCardState);
		}

		@Test
		@DisplayName("with state CONFIRMED throws a VerifyAuthenticationChallengeException")
		void saveConfirmedStateWhenCurrentStateIsConfirmedThrows() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);
			verificationCardService.saveConfirmingState(verificationCardId);

			final String shortVoteCastReturnCode = random.genUniqueDecimalStrings(8, 1).get(0);
			verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode);

			final VerifyAuthenticationChallengeException exception = assertThrows(VerifyAuthenticationChallengeException.class,
					() -> verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode));
			assertEquals(VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.AUTHENTICATION_CHALLENGE_ERROR,
					exception.getErrorStatus());

			final VerificationCardState verificationCardState = verificationCardService.getVerificationCardEntity(verificationCardId)
					.getVerificationCardStateEntity().getState();
			assertEquals(VerificationCardState.CONFIRMED, verificationCardState);
		}

		@Test
		@DisplayName("with state BLOCKED throws a VerifyAuthenticationChallengeException")
		void saveConfirmedStateWhenCurrentStateIsBlockedThrows() {
			verificationCardService.blockVotingCard(votingCardId);

			final String shortVoteCastReturnCode = random.genUniqueDecimalStrings(8, 1).get(0);
			final VerifyAuthenticationChallengeException exception = assertThrows(VerifyAuthenticationChallengeException.class,
					() -> verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode));
			assertEquals(VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.VOTING_CARD_BLOCKED, exception.getErrorStatus());

			final VerificationCardState verificationCardState = verificationCardService.getVerificationCardEntity(verificationCardId)
					.getVerificationCardStateEntity().getState();
			assertEquals(VerificationCardState.BLOCKED, verificationCardState);
		}

		@Test
		@DisplayName("with state CONFIRMATION_ATTEMPTS_EXCEEDED throws a VerifyAuthenticationChallengeException")
		void saveConfirmedStateWhenCurrentStateIsConfirmationAttemptsExceededThrows() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);

			for (int i = 0; i < 5; i++) {
				verificationCardService.saveConfirmingState(verificationCardId);
				verificationCardService.incrementConfirmationAttempts(verificationCardId);
			}

			final String shortVoteCastReturnCode = random.genUniqueDecimalStrings(8, 1).get(0);
			final VerifyAuthenticationChallengeException exception = assertThrows(VerifyAuthenticationChallengeException.class,
					() -> verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode));
			assertEquals(VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.VOTING_CARD_BLOCKED, exception.getErrorStatus());

			final VerificationCardState verificationCardState = verificationCardService.getVerificationCardEntity(verificationCardId)
					.getVerificationCardStateEntity().getState();
			assertEquals(VerificationCardState.CONFIRMATION_ATTEMPTS_EXCEEDED, verificationCardState);
		}
	}

	@Nested
	@DisplayName("blockVotingCard")
	class BlockVotingCardTest {

		@Test
		@DisplayName("when VerificationCardEntity not found throws a VerificationCardNotFoundException ")
		void blockVotingCardWhenVerificationCardEntityNotFindThrows() {
			final String nonExistingVotingCardId = uuidGenerator.generate();
			final VerificationCardNotFoundException exception = assertThrows(VerificationCardNotFoundException.class,
					() -> verificationCardService.blockVotingCard(nonExistingVotingCardId));
			final String expectedErrorMessage = String.format("Verification card not found. [votingCardIdSearched: %s]", nonExistingVotingCardId);
			assertEquals(expectedErrorMessage, exception.getMessage());
		}

		@Test
		@DisplayName("with state already BLOCKED throws an InvalidVerificationCardStateException")
		void blockVotingCardWhenStateIsBlockedThrows() {
			assertDoesNotThrow(() -> verificationCardService.blockVotingCard(votingCardId));

			final InvalidVerificationCardStateException exception = assertThrows(InvalidVerificationCardStateException.class,
					() -> verificationCardService.blockVotingCard(votingCardId));
			final String expectedErrorMessage = String.format(
					"Verification card not blocked. The current state does not allow to block it. [votingCardId: %s, verificationCardState: %s]",
					votingCardId, VerificationCardState.BLOCKED);
			assertEquals(expectedErrorMessage, exception.getMessage());
		}

		@Test
		@DisplayName("with state CONFIRMING throws an InvalidVerificationCardStateException")
		void blockVotingCardWhenStateIsConfirmingThrows() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);
			verificationCardService.saveConfirmingState(verificationCardId);

			final InvalidVerificationCardStateException exception = assertThrows(InvalidVerificationCardStateException.class,
					() -> verificationCardService.blockVotingCard(votingCardId));
			final String expectedErrorMessage = String.format(
					"Verification card not blocked. The current state does not allow to block it. [votingCardId: %s, verificationCardState: %s]",
					votingCardId, VerificationCardState.CONFIRMING);
			assertEquals(expectedErrorMessage, exception.getMessage());
		}

		@Test
		@DisplayName("with state CONFIRMED throws an InvalidVerificationCardStateException")
		void blockVotingCardWhenStateIsConfirmedThrows() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);
			final String shortVoteCastReturnCode = random.genUniqueDecimalStrings(8, 1).get(0);
			verificationCardService.saveConfirmedState(verificationCardId, shortVoteCastReturnCode);

			final InvalidVerificationCardStateException exception = assertThrows(InvalidVerificationCardStateException.class,
					() -> verificationCardService.blockVotingCard(votingCardId));
			final String expectedErrorMessage = String.format(
					"Verification card not blocked. The current state does not allow to block it. [votingCardId: %s, verificationCardState: %s]",
					votingCardId, VerificationCardState.CONFIRMED);
			assertEquals(expectedErrorMessage, exception.getMessage());
		}

		@Test
		@DisplayName("with state INITIAL blocks the voting card")
		void blockVotingCardWhenStateIsInitialBlocksTheVotingCard() {
			assertDoesNotThrow(() -> verificationCardService.blockVotingCard(votingCardId));
		}

		@Test
		@DisplayName("with state SENT blocks the voting card")
		void blockVotingCardWhenStateIsSentBlocksTheVotingCard() {
			final ImmutableList<String> shortChoiceReturnCodes = random.genUniqueDecimalStrings(4, 5);
			verificationCardService.saveSentState(verificationCardId, shortChoiceReturnCodes);

			assertDoesNotThrow(() -> verificationCardService.blockVotingCard(votingCardId));
		}
	}

	@Nested
	@DisplayName("searchVotingCard")
	class SearchVotingCard {

		private final long votingCardLimit = 5;

		@Test
		@DisplayName("when more matching verification cards than voting card limit throws a TooManyVerificationCardsException")
		void searchVotingCardWhenMoreMatchingThanLimitReturns() {
			final long totalCount = 6L;

			doReturn(totalCount).when(verificationCardRepository).countAllByVotingCardIdStartsWith(partialVotingCardId);

			final VotingCardSearchDto votingCardSearchDto = assertDoesNotThrow(() -> verificationCardService.searchVotingCard(partialVotingCardId));

			assertFalse(votingCardSearchDto.votingCards().isEmpty());
			assertEquals(votingCardLimit, votingCardSearchDto.metadata().limit());
			assertEquals(totalCount, votingCardSearchDto.metadata().totalCount());
		}

		@Test
		@DisplayName("with valid argument returns list of matching voting cards")
		void searchVotingCardWithValidArgumentReturns() {
			final long totalCount = 1L;
			doReturn(totalCount).when(verificationCardRepository).countAllByVotingCardIdStartsWith(partialVotingCardId);

			final VotingCardSearchDto votingCardSearchDto = assertDoesNotThrow(() -> verificationCardService.searchVotingCard(partialVotingCardId));

			assertEquals(1, votingCardSearchDto.votingCards().size());
			assertEquals(votingCardId, votingCardSearchDto.votingCards().get(0).votingCardId());
			assertEquals(votingCardLimit, votingCardSearchDto.metadata().limit());
			assertEquals(totalCount, votingCardSearchDto.metadata().totalCount());
		}
	}
}
