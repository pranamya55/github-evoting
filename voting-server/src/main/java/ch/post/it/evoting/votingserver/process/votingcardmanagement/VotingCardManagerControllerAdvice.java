/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.votingcardmanagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@ControllerAdvice(assignableTypes = { VotingCardManagerController.class })
public class VotingCardManagerControllerAdvice {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingCardManagerControllerAdvice.class);

	@ExceptionHandler(value = { VerificationCardNotFoundException.class })
	public ResponseEntity<VotingCardDto> handleVerificationCardNotFoundException(final ServerHttpRequest serverRequest,
			final VerificationCardNotFoundException ex) {
		logWarn(serverRequest, ex);
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(value = { InvalidVerificationCardStateException.class })
	public ResponseEntity<VotingCardDto> handleInvalidVerificationCardStateException(final ServerHttpRequest serverRequest,
			final InvalidVerificationCardStateException ex) {
		logWarn(serverRequest, ex);
		return new ResponseEntity<>(HttpStatus.CONFLICT);
	}

	@ExceptionHandler(value = { IllegalStateException.class, FailedValidationException.class })
	public ResponseEntity<VotingCardDto> handleOtherExceptions(final ServerHttpRequest serverRequest,
			final IllegalStateException ex) {
		logWarn(serverRequest, ex);
		return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
	}

	private static void logWarn(final ServerHttpRequest request, final Exception ex) {
		final String parameters = request.getQueryParams().entrySet().stream()
				.map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
				.reduce((a, b) -> a + "&" + b)
				.orElse("");
		if (parameters.isEmpty()) {
			LOGGER.warn("Failed to process request. [request: {}, reason: {}]", request.getURI(), ex.getMessage());
		} else {
			LOGGER.warn("Failed to process request. [request: {}, parameters: {}, reason: {}]", request.getURI(), parameters,
					ex.getMessage());
		}
	}
}
