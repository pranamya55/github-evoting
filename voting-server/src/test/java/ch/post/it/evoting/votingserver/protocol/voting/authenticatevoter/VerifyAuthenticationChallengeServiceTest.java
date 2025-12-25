/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.votingserver.process.Constants.TWO_POW_256;
import static ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.AUTHENTICATION_CHALLENGE_ERROR;
import static ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.EXTENDED_FACTOR_INVALID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.BallotBoxEntity;
import ch.post.it.evoting.votingserver.process.SetupComponentVoterAuthenticationDataPayloadService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.VerificationCardSetEntity;
import ch.post.it.evoting.votingserver.process.VerificationCardStateValidator;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;
import ch.post.it.evoting.votingserver.process.voting.VerifyAuthenticationChallengeException;

@DisplayName("VerifyAuthenticationChallengeService calling")
class VerifyAuthenticationChallengeServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private final VerificationCardService verificationCardService = mock(VerificationCardService.class, Mockito.RETURNS_DEEP_STUBS);
	private final SetupComponentVoterAuthenticationDataPayloadService setupComponentVoterAuthenticationDataPayloadService = mock(
			SetupComponentVoterAuthenticationDataPayloadService.class);
	private final VerifyAuthenticationChallengeAlgorithm verifyAuthenticationChallengeAlgorithm = mock(VerifyAuthenticationChallengeAlgorithm.class);
	private final VerifyAuthenticationChallengeService verifyAuthenticationChallengeService = new VerifyAuthenticationChallengeService(
			verificationCardService, setupComponentVoterAuthenticationDataPayloadService, verifyAuthenticationChallengeAlgorithm);

	@AfterEach
	void tearDown() {
		reset(verificationCardService, setupComponentVoterAuthenticationDataPayloadService, verifyAuthenticationChallengeAlgorithm);
	}

	private static Stream<Arguments> provideNullParameters() {
		final String electionEventId = uuidGenerator.generate();
		final AuthenticationStep authenticationStep = AuthenticationStep.AUTHENTICATE_VOTER;
		final AuthenticationChallenge authenticationChallenge = mock(AuthenticationChallenge.class);
		return Stream.of(
				Arguments.of(null, authenticationStep, authenticationChallenge),
				Arguments.of(electionEventId, null, authenticationChallenge),
				Arguments.of(electionEventId, authenticationStep, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void verifyAuthenticationChallengeWithNullParametersThrows(final String electionEventId, final AuthenticationStep authenticationStep,
			final AuthenticationChallenge authenticationChallenge) {
		assertThrows(NullPointerException.class,
				() -> verifyAuthenticationChallengeService.verifyAuthenticationChallenge(electionEventId, authenticationStep,
						authenticationChallenge));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void verifyAuthenticationChallengeWithInvalidElectionEventIdThrows() {
		// given
		final AuthenticationStep authenticationStep = AuthenticationStep.AUTHENTICATE_VOTER;
		final AuthenticationChallenge authenticationChallenge = mock(AuthenticationChallenge.class);

		// when / then
		assertThrows(FailedValidationException.class,
				() -> verifyAuthenticationChallengeService.verifyAuthenticationChallenge("invalidId", authenticationStep, authenticationChallenge));
	}

	private BallotBoxEntity genBallotBoxEntity() {
		return genBallotBoxEntity(LocalDateTimeUtils.now());
	}

	private BallotBoxEntity genBallotBoxEntity(final LocalDateTime now) {
		return new BallotBoxEntity.Builder()
				.setTestBallotBox(true)
				.setBallotBoxStartTime(now.minusDays(1))
				.setBallotBoxFinishTime(now.plusDays(1))
				.setBallotBoxId(uuidGenerator.generate())
				.setPrimesMappingTable(ImmutableByteArray.EMPTY)
				.setVerificationCardSetEntity(mock(VerificationCardSetEntity.class))
				.setGracePeriod(1)
				.build();
	}

	private static AuthenticationChallenge genAuthenticationChallenge() {
		return new AuthenticationChallenge(
				uuidGenerator.generate(),
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance()),
				random.genRandomInteger(TWO_POW_256)
		);
	}

	private static SetupComponentVoterAuthenticationData genSetupComponentVoterAuthenticationData() {
		final String id = uuidGenerator.generate();
		return new SetupComponentVoterAuthenticationData(id, id, id, id, id, id,
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance())
		);
	}

	@Nested
	class WithValidateVerificationCardState {

		private static MockedStatic<VerificationCardStateValidator> verificationCardStateValidator;

		@BeforeAll
		static void beforeAll() {
			verificationCardStateValidator = mockStatic(VerificationCardStateValidator.class);
			verificationCardStateValidator.when(() -> VerificationCardStateValidator.validateVerificationCardState(any(), any()))
					.thenAnswer(invocationOnMock -> null);
		}

		@AfterAll
		static void afterAll() {
			verificationCardStateValidator.close();
		}

		@Test
		@DisplayName("ballot box not open yet throws VerifyAuthenticationChallengeException")
		void verifyAuthenticationChallengeWithBallotBoxNotOpenYetThrows() {
			// given
			final String randomId = uuidGenerator.generate();
			final AuthenticationStep authenticationStep = AuthenticationStep.AUTHENTICATE_VOTER;
			final AuthenticationChallenge authenticationChallenge = genAuthenticationChallenge();
			when(verifyAuthenticationChallengeAlgorithm.verifyAuthenticationChallenge(Mockito.any(), Mockito.any()))
					.thenReturn(VerifyAuthenticationChallengeOutput.success());
			final BallotBoxEntity ballotBoxEntity = genBallotBoxEntity(LocalDateTimeUtils.now().plusDays(5));
			when(verificationCardService.getVerificationCardSetEntity(anyString()).getBallotBox()).thenReturn(ballotBoxEntity);

			// when
			final VerifyAuthenticationChallengeException exception = assertThrows(VerifyAuthenticationChallengeException.class,
					() -> verifyAuthenticationChallengeService.verifyAuthenticationChallenge(randomId, authenticationStep,
							authenticationChallenge));

			// then
			final String expected = String.format("The ballot box is not open yet. [step: %s, credentialId: %s, ballotBoxId: %s]", authenticationStep,
					authenticationChallenge.derivedVoterIdentifier(), ballotBoxEntity.getBallotBoxId());
			assertEquals(expected, exception.getErrorMessage());
			assertEquals(VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.BALLOT_BOX_NOT_STARTED, exception.getErrorStatus());
		}

		@Test
		@DisplayName("ballot box closed throws VerifyAuthenticationChallengeException")
		void verifyAuthenticationChallengeWithBallotBoxClosedThrows() {
			// given
			final String randomId = uuidGenerator.generate();
			final AuthenticationStep authenticationStep = AuthenticationStep.AUTHENTICATE_VOTER;
			final AuthenticationChallenge authenticationChallenge = genAuthenticationChallenge();
			when(verifyAuthenticationChallengeAlgorithm.verifyAuthenticationChallenge(Mockito.any(), Mockito.any()))
					.thenReturn(VerifyAuthenticationChallengeOutput.success());
			final BallotBoxEntity ballotBoxEntity = genBallotBoxEntity(LocalDateTimeUtils.now().minusDays(5));
			when(verificationCardService.getVerificationCardSetEntity(anyString()).getBallotBox()).thenReturn(ballotBoxEntity);

			// when
			final VerifyAuthenticationChallengeException exception = assertThrows(VerifyAuthenticationChallengeException.class,
					() -> verifyAuthenticationChallengeService.verifyAuthenticationChallenge(randomId, authenticationStep,
							authenticationChallenge));

			// then
			final String expected = String.format("The ballot box is closed. [step: %s, credentialId: %s, ballotBoxId: %s]", authenticationStep,
					authenticationChallenge.derivedVoterIdentifier(), ballotBoxEntity.getBallotBoxId());
			assertEquals(expected, exception.getErrorMessage());
			assertEquals(VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.BALLOT_BOX_ENDED, exception.getErrorStatus());
		}

		@Test
		@DisplayName("valid parameters does not throw")
		void verifyAuthenticationChallengeWithValidAuthenticationChallengeDoesNotThrow() {
			// given
			final String randomId = uuidGenerator.generate();
			final AuthenticationStep authenticationStep = AuthenticationStep.AUTHENTICATE_VOTER;
			final AuthenticationChallenge authenticationChallenge = genAuthenticationChallenge();
			when(verifyAuthenticationChallengeAlgorithm.verifyAuthenticationChallenge(Mockito.any(), Mockito.any()))
					.thenReturn(VerifyAuthenticationChallengeOutput.success());
			when(verificationCardService.getVerificationCardSetEntity(anyString()).getBallotBox()).thenReturn(genBallotBoxEntity());
			when(setupComponentVoterAuthenticationDataPayloadService.load(anyString(), anyString())).thenReturn(
					genSetupComponentVoterAuthenticationData());

			// when / then
			assertDoesNotThrow(() -> verifyAuthenticationChallengeService.verifyAuthenticationChallenge(randomId, authenticationStep,
					authenticationChallenge));
		}

		@Test
		@DisplayName("authentication failed throws VerifyAuthenticationChallengeException")
		void verifyAuthenticationChallengeWithInvalidAuthenticationChallengeThrows() {
			// given
			final String randomId = uuidGenerator.generate();
			final AuthenticationStep authenticationStep = AuthenticationStep.AUTHENTICATE_VOTER;
			final AuthenticationChallenge authenticationChallenge = genAuthenticationChallenge();
			final String errorMessage = "expected error message";
			when(verifyAuthenticationChallengeAlgorithm.verifyAuthenticationChallenge(Mockito.any(), Mockito.any()))
					.thenReturn(VerifyAuthenticationChallengeOutput.authenticationChallengeError(errorMessage));
			when(verificationCardService.getVerificationCardSetEntity(anyString()).getBallotBox()).thenReturn(genBallotBoxEntity());
			when(setupComponentVoterAuthenticationDataPayloadService.load(anyString(), anyString())).thenReturn(
					genSetupComponentVoterAuthenticationData());

			// when
			final VerifyAuthenticationChallengeException exception = assertThrows(VerifyAuthenticationChallengeException.class,
					() -> verifyAuthenticationChallengeService.verifyAuthenticationChallenge(randomId, authenticationStep,
							authenticationChallenge));

			// then
			assertEquals(AUTHENTICATION_CHALLENGE_ERROR, exception.getErrorStatus());
			assertTrue(exception.getErrorMessage().contains(errorMessage));
		}

		@Test
		@DisplayName("invalid extended factor throws a VerifyAuthenticationChallengeException")
		void verifyAuthenticationChallengeWithInvalidExtendedFactorThrows() {
			// given
			final String randomId = uuidGenerator.generate();
			final AuthenticationStep authenticationStep = AuthenticationStep.AUTHENTICATE_VOTER;
			final AuthenticationChallenge authenticationChallenge = genAuthenticationChallenge();
			final String errorMessage = "expected error message";
			final int expectedAttemptLeft = 2;
			when(verifyAuthenticationChallengeAlgorithm.verifyAuthenticationChallenge(Mockito.any(), Mockito.any()))
					.thenReturn(VerifyAuthenticationChallengeOutput.invalidExtendedFactor(errorMessage, expectedAttemptLeft));
			when(verificationCardService.getVerificationCardSetEntity(anyString()).getBallotBox()).thenReturn(genBallotBoxEntity());
			when(setupComponentVoterAuthenticationDataPayloadService.load(anyString(), anyString())).thenReturn(
					genSetupComponentVoterAuthenticationData());

			// when
			final VerifyAuthenticationChallengeException exception = assertThrows(VerifyAuthenticationChallengeException.class,
					() -> verifyAuthenticationChallengeService.verifyAuthenticationChallenge(randomId, authenticationStep,
							authenticationChallenge));

			// then
			assertEquals(EXTENDED_FACTOR_INVALID, exception.getErrorStatus());
			assertTrue(exception.getErrorMessage().contains(errorMessage));
			assertEquals(expectedAttemptLeft, exception.getRemainingAttempts().orElseThrow());
		}
	}
}
