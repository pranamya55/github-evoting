/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmationKeyInvalidException extends RuntimeException {

	private final int remainingAttempts;

	@JsonCreator
	public ConfirmationKeyInvalidException(
			@JsonProperty("message")
			final String message,
			@JsonProperty("remainingAttempts")
			final int remainingAttempts) {
		super(message);
		this.remainingAttempts = remainingAttempts;
	}

	public int getRemainingAttempts() {
		return remainingAttempts;
	}

}
