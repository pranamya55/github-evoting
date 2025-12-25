/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.BallotBoxEntity;
import ch.post.it.evoting.votingserver.process.SetupComponentVoterAuthenticationDataPayloadService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.VerificationCardStateValidator;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;
import ch.post.it.evoting.votingserver.process.voting.VerifyAuthenticationChallengeException;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus;

@Service
public class VerifyAuthenticationChallengeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerifyAuthenticationChallengeService.class);

	private final VerificationCardService verificationCardService;
	private final SetupComponentVoterAuthenticationDataPayloadService setupComponentVoterAuthenticationDataPayloadService;
	private final VerifyAuthenticationChallengeAlgorithm verifyAuthenticationChallengeAlgorithm;

	public VerifyAuthenticationChallengeService(
			final VerificationCardService verificationCardService,
			final SetupComponentVoterAuthenticationDataPayloadService setupComponentVoterAuthenticationDataPayloadService,
			final VerifyAuthenticationChallengeAlgorithm verifyAuthenticationChallengeAlgorithm) {
		this.verificationCardService = verificationCardService;
		this.setupComponentVoterAuthenticationDataPayloadService = setupComponentVoterAuthenticationDataPayloadService;
		this.verifyAuthenticationChallengeAlgorithm = verifyAuthenticationChallengeAlgorithm;
	}

	/**
	 * Verifies the authentication challenge for the given {@code credentialId}. Invokes the algorithm VerifyAuthenticationChallenge.
	 *
	 * @param electionEventId         the election event id.
	 * @param authenticationStep      the {@link AuthenticationStep} for which to verify the challenge.
	 * @param authenticationChallenge the {@link AuthenticationChallenge} containing the credential id and derived authentication challenge.
	 * @throws NullPointerException                   if any parameter is null.
	 * @throws FailedValidationException              if {@code electionEventId} is invalid.
	 * @throws VerifyAuthenticationChallengeException if the verification of the authentication challenge is unsuccessful for the given
	 *                                                {@code authenticationChallenge}.
	 */
	public void verifyAuthenticationChallenge(final String electionEventId, final AuthenticationStep authenticationStep,
			final AuthenticationChallenge authenticationChallenge) {

		validateUUID(electionEventId);
		checkNotNull(authenticationStep);
		checkNotNull(authenticationChallenge);

		final String credentialId = authenticationChallenge.derivedVoterIdentifier();

		// Check that verification card is not blocked or in an incoherent state with respect to the current authenticationStep.
		final VerificationCardState verificationCardState = verificationCardService.getVerificationCardState(credentialId);
		VerificationCardStateValidator.validateVerificationCardState(authenticationStep, verificationCardState);

		// Check that the ballot box is still opened.
		validateBallotBoxTime(authenticationStep, credentialId);

		// Retrieve base authentication challenge.
		final SetupComponentVoterAuthenticationData voterAuthenticationData = setupComponentVoterAuthenticationDataPayloadService.load(
				electionEventId, credentialId);
		final String baseAuthenticationChallenge = voterAuthenticationData.baseAuthenticationChallenge();

		// Verify authentication challenge.
		final String derivedAuthenticationChallenge = authenticationChallenge.derivedAuthenticationChallenge();
		final BigInteger authenticationNonce = authenticationChallenge.authenticationNonce();

		final VerifyAuthenticationChallengeContext context = new VerifyAuthenticationChallengeContext(electionEventId, credentialId);
		final VerifyAuthenticationChallengeInput input = new VerifyAuthenticationChallengeInput(authenticationStep, derivedAuthenticationChallenge,
				baseAuthenticationChallenge, authenticationNonce);

		LOGGER.debug("Performing VerifyAuthenticationChallenge algorithm... [electionEventId: {}, credentialId: {}, authenticationStep: {}]",
				electionEventId, credentialId, authenticationStep);

		final VerifyAuthenticationChallengeOutput output = verifyAuthenticationChallengeAlgorithm.verifyAuthenticationChallenge(context, input);

		// The verificationCardStateEntity has been accessed at the start of this method, and modified in a new transaction (propagation=REQUIRES_NEW)
		// in the verifyAuthenticationChallengeAlgorithm method. Here we manually refresh it to ensure any subsequent operation needing the
		// verificationCardStateEntity will work with the updated version.
		verificationCardService.refreshVerificationCardStateEntity(credentialId);

		final VerifyAuthenticationChallengeStatus status = output.getStatus();

		if (!VerifyAuthenticationChallengeStatus.SUCCESS.equals(status)) {
			final String errorMessage = String.format(
					"Verification of authentication challenge failed. [electionEventId: %s, credentialId: %s, authenticationStep: %s, reason: %s]",
					electionEventId, credentialId, authenticationStep, output.getErrorMessage());
			throw new VerifyAuthenticationChallengeException(status, output.getAttemptsLeft(), errorMessage);
		}

		LOGGER.info("Successfully verified authentication challenge. [electionEventId: {}, credentialId: {}, authenticationStep: {}]",
				electionEventId, credentialId, authenticationStep);
	}

	private void validateBallotBoxTime(final AuthenticationStep authenticationStep, final String credentialId) {
		final BallotBoxEntity ballotBoxEntity = verificationCardService
				.getVerificationCardSetEntity(credentialId)
				.getBallotBox();

		final LocalDateTime now = LocalDateTimeUtils.now();
		final LocalDateTime ballotBoxStartTime = ballotBoxEntity.getBallotBoxStartTime();
		// The voter can still vote and confirm during the grace period window after the ballot box is closed.
		LocalDateTime ballotBoxFinishTime = ballotBoxEntity.getBallotBoxFinishTime();
		if (AuthenticationStep.SEND_VOTE.equals(authenticationStep) || AuthenticationStep.CONFIRM_VOTE.equals(authenticationStep)) {
			ballotBoxFinishTime = ballotBoxFinishTime.plusSeconds(ballotBoxEntity.getGracePeriod());
		}

		if (now.isBefore(ballotBoxStartTime)) {
			final String errorMessage = String.format("The ballot box is not open yet. [step: %s, credentialId: %s, ballotBoxId: %s]",
					authenticationStep, credentialId, ballotBoxEntity.getBallotBoxId());
			throw new VerifyAuthenticationChallengeException(VerifyAuthenticationChallengeStatus.BALLOT_BOX_NOT_STARTED, errorMessage);
		} else if (now.isAfter(ballotBoxFinishTime)) {
			final String errorMessage = String.format("The ballot box is closed. [step: %s, credentialId: %s, ballotBoxId: %s]", authenticationStep,
					credentialId, ballotBoxEntity.getBallotBoxId());
			throw new VerifyAuthenticationChallengeException(VerifyAuthenticationChallengeStatus.BALLOT_BOX_ENDED, errorMessage);
		}
	}

}
