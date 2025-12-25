/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.post.it.evoting.votingserver.process.voting.authenticatevoter.AuthenticateVoterController;
import ch.post.it.evoting.votingserver.process.voting.confirmvote.ConfirmVoteController;
import ch.post.it.evoting.votingserver.process.voting.sendvote.SendVoteController;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus;

@ControllerAdvice(assignableTypes = { AuthenticateVoterController.class, SendVoteController.class, ConfirmVoteController.class })
public class VotingControllerAdvice {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingControllerAdvice.class);
	private static final String ERROR_STATUS = "errorStatus";
	private static final String START_VOTING_KEY_INVALID = "START_VOTING_KEY_INVALID";
	private static final String CONFIRMATION_KEY_INVALID = "CONFIRMATION_KEY_INVALID";

	private final ObjectMapper objectMapper;

	public VotingControllerAdvice(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@ExceptionHandler(VerifyAuthenticationChallengeException.class)
	protected ResponseEntity<String> handleAuthenticationException(final VerifyAuthenticationChallengeException e) {

		final VerifyAuthenticationChallengeStatus errorStatus = e.getErrorStatus();
		if (errorStatus == VerifyAuthenticationChallengeStatus.EXTENDED_FACTOR_INVALID
				|| errorStatus == VerifyAuthenticationChallengeStatus.AUTHENTICATION_ATTEMPTS_EXCEEDED) {
			LOGGER.info("Authentication failed. {}", e.getErrorMessage());
		} else {
			LOGGER.warn("Authentication failed. {}", e.getErrorMessage());
		}

		final ObjectNode responseNode = objectMapper.createObjectNode();
		final String name = errorStatus.name();
		responseNode.put(ERROR_STATUS, name);

		// Add the number of attempts left in case of invalid extended authentication factor error.
		e.getRemainingAttempts().ifPresent(attemptsLeft -> {
			if (attemptsLeft == 0) {
				// When attempts are exhausted, return a blocked status instead of 0 remaining attempts.
				LOGGER.warn("Voting card blocked. Exhausted the extended authentication factor attempts.");
				responseNode.put(ERROR_STATUS, VerifyAuthenticationChallengeStatus.VOTING_CARD_BLOCKED.name());
			} else {
				responseNode.put("numberOfRemainingAttempts", attemptsLeft);
				responseNode.put("timestamp", Instant.now().getEpochSecond());
			}
		});

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.APPLICATION_JSON)
				.body(responseNode.toString());
	}

	@ExceptionHandler(CredentialIdNotFoundException.class)
	protected ResponseEntity<String> handleCredentialIdNotFoundException(final CredentialIdNotFoundException e) {

		LOGGER.info("Invalid start voting key. {}", e.getMessage());

		final ObjectNode responseNode = objectMapper.createObjectNode();
		responseNode.put(ERROR_STATUS, START_VOTING_KEY_INVALID);

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.APPLICATION_JSON)
				.body(responseNode.toString());
	}

	@ExceptionHandler(ConfirmationKeyInvalidException.class)
	protected ResponseEntity<String> handleConfirmationKeyInvalidException(final ConfirmationKeyInvalidException e) {

		LOGGER.info("Invalid confirmation key. {}", e.getMessage());

		final ObjectNode responseNode = objectMapper.createObjectNode();
		responseNode.put(ERROR_STATUS, CONFIRMATION_KEY_INVALID);

		// Add the number of attempts left in case of invalid confirmation key error.
		final int remainingAttempts = e.getRemainingAttempts();
		if (remainingAttempts == 0) {
			// When attempts are exhausted, return a blocked status instead of 0 remaining attempts.
			LOGGER.warn("Voting card blocked. Exhausted the confirmation key attempts.");
			responseNode.put(ERROR_STATUS, VerifyAuthenticationChallengeStatus.VOTING_CARD_BLOCKED.name());
		} else {
			responseNode.put("numberOfRemainingAttempts", remainingAttempts);
		}

		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.APPLICATION_JSON)
				.body(responseNode.toString());
	}

}
