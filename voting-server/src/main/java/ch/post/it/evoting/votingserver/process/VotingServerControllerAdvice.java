/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@ControllerAdvice
public class VotingServerControllerAdvice {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingServerControllerAdvice.class);

	@ExceptionHandler(value = { IllegalStateException.class, FailedValidationException.class, IllegalArgumentException.class,
			InvalidPayloadSignatureException.class })
	public ResponseEntity<Void> handleIllegalStateException(final ServerHttpRequest serverHttpRequest, final Exception exception) {
		logInfo(serverHttpRequest, exception);

		return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
	}

	@ExceptionHandler(value = { CompletionException.class })
	public ResponseEntity<Void> handleCompletionException(final ServerHttpRequest serverHttpRequest, final CompletionException exception) {
		logWarn(serverHttpRequest, exception);

		return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
	}

	private static void logWarn(final ServerHttpRequest serverHttpRequest, final Exception exception) {
		final String parameters = extractParameters(serverHttpRequest);
		if (parameters.isEmpty()) {
			LOGGER.warn("Failed to process request. [request: {}, reason: {}]", serverHttpRequest.getURI(), exception.getMessage());
		} else {
			LOGGER.warn("Failed to process request. [request: {}, parameters: {}, reason: {}]", serverHttpRequest.getURI(), parameters,
					exception.getMessage());
		}
	}

	private static void logInfo(final ServerHttpRequest serverHttpRequest, final Exception exception) {
		final String parameters = extractParameters(serverHttpRequest);
		if (parameters.isEmpty()) {
			LOGGER.info("Failed to process request. [request: {}, reason: {}]", serverHttpRequest.getURI(), exception.getMessage());
		} else {
			LOGGER.info("Failed to process request. [request: {}, parameters: {}, reason: {}]", serverHttpRequest.getURI(), parameters,
					exception.getMessage());
		}
	}

	private static String extractParameters(final ServerHttpRequest serverHttpRequest) {
		return serverHttpRequest.getQueryParams().entrySet().stream()
				.map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
				.reduce((a, b) -> a + "&" + b)
				.orElse("");
	}
}
