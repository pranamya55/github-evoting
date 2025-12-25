/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import static ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.AUTHENTICATION_CHALLENGE_ERROR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeOutput;

@DisplayName("VotingControllerAdvice calling")
class VotingControllerAdviceTest {

	private static ObjectMapper mapper;
	private static VotingControllerAdvice votingControllerAdvice;

	@BeforeAll
	static void setupAll() {
		mapper = DomainObjectMapper.getNewInstance();
		votingControllerAdvice = new VotingControllerAdvice(mapper);
	}

	@Test
	@DisplayName("handleAuthenticationException with exception having remaining attempts returns response node with remaining attempts")
	void handleAuthenticationExceptionWhenRemainingAttemptsReturnsNumberOfRemainingAttempts() throws JsonProcessingException {
		final VerifyAuthenticationChallengeException exception = new VerifyAuthenticationChallengeException(AUTHENTICATION_CHALLENGE_ERROR, 5,
				"errorMessage");
		final ResponseEntity<String> response = assertDoesNotThrow(() -> votingControllerAdvice.handleAuthenticationException(exception));

		final HttpStatusCode statusCode = response.getStatusCode();
		final String responseBody = response.getBody();
		final JsonNode jsonNode = mapper.readTree(responseBody);

		assertEquals(exception.getRemainingAttempts().orElseThrow(), jsonNode.get("numberOfRemainingAttempts").asInt());
		assertEquals(exception.getErrorStatus().name(), jsonNode.get("errorStatus").asText());
		assertEquals(HttpStatus.UNAUTHORIZED, statusCode);
	}

	@Test
	@DisplayName("handleAuthenticationException with exception not having remaining attempts returns response node with error status VOTING_CARD_BLOCKED")
	void handleAuthenticationExceptionWhenNoRemainingAttemptsReturnsVotingCardBlocked() throws JsonProcessingException {
		final VerifyAuthenticationChallengeException exception = new VerifyAuthenticationChallengeException(AUTHENTICATION_CHALLENGE_ERROR, 0,
				"errorMessage");
		final ResponseEntity<String> response = assertDoesNotThrow(() -> votingControllerAdvice.handleAuthenticationException(exception));

		final HttpStatusCode statusCode = response.getStatusCode();
		final String responseBody = response.getBody();
		final JsonNode jsonNode = mapper.readTree(responseBody);

		assertEquals(VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.VOTING_CARD_BLOCKED.name(),
				jsonNode.get("errorStatus").asText());
		assertEquals(HttpStatus.UNAUTHORIZED, statusCode);
	}

	@Test
	@DisplayName("handleCredentialIdNotFoundException returns START_VOTING_KEY_INVALID")
	void handleCredentialIdNotFoundExceptionReturnsStartVotingKeyInvalid() throws JsonProcessingException {
		final CredentialIdNotFoundException exception = new CredentialIdNotFoundException("errorMessage");
		final ResponseEntity<String> response = assertDoesNotThrow(() -> votingControllerAdvice.handleCredentialIdNotFoundException(exception));

		final HttpStatusCode statusCode = response.getStatusCode();
		final String responseBody = response.getBody();
		final JsonNode jsonNode = mapper.readTree(responseBody);

		assertEquals("START_VOTING_KEY_INVALID", jsonNode.get("errorStatus").asText());
		assertEquals(HttpStatus.UNAUTHORIZED, statusCode);
	}

	@Test
	@DisplayName("handleConfirmationKeyInvalidException")
	void handleConfirmationKeyInvalidExceptionWhenRemainingAttemptsReturnsNumberOfRemainingAttempts() throws JsonProcessingException {
		final ConfirmationKeyInvalidException exception = new ConfirmationKeyInvalidException("Invalid confirmation key", 5);
		final ResponseEntity<String> response = assertDoesNotThrow(() -> votingControllerAdvice.handleConfirmationKeyInvalidException(exception));

		final HttpStatusCode statusCode = response.getStatusCode();
		final String responseBody = response.getBody();
		final JsonNode jsonNode = mapper.readTree(responseBody);

		assertEquals(exception.getRemainingAttempts(), jsonNode.get("numberOfRemainingAttempts").asInt());
		assertEquals("CONFIRMATION_KEY_INVALID", jsonNode.get("errorStatus").asText());
		assertEquals(HttpStatus.UNAUTHORIZED, statusCode);
	}

	@Test
	@DisplayName("handleConfirmationKeyInvalidException with exception not having remaining attempts returns response node with error status VOTING_CARD_BLOCKED")
	void handleConfirmationKeyInvalidExceptionWhenNoRemainingAttemptsReturnsVotingCardBlocked() throws JsonProcessingException {
		final ConfirmationKeyInvalidException exception = new ConfirmationKeyInvalidException("Invalid confirmation key", 0);
		final ResponseEntity<String> response = assertDoesNotThrow(() -> votingControllerAdvice.handleConfirmationKeyInvalidException(exception));

		final HttpStatusCode statusCode = response.getStatusCode();
		final String responseBody = response.getBody();
		final JsonNode jsonNode = mapper.readTree(responseBody);

		assertEquals(VerifyAuthenticationChallengeOutput.VerifyAuthenticationChallengeStatus.VOTING_CARD_BLOCKED.name(),
				jsonNode.get("errorStatus").asText());
		assertEquals(HttpStatus.UNAUTHORIZED, statusCode);
	}
}
