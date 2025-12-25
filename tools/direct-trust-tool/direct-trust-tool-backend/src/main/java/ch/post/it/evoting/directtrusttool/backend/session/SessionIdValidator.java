/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.session;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class SessionIdValidator {

	private SessionIdValidator() {
		// Empty constructor
	}

	public static void validateSessionId(final String sessionId) {
		checkNotNull(sessionId);
		checkState(!sessionId.isEmpty(), "SessionId cannot be empty");
		checkState(sessionId.matches("^\\w{1,32}$"), "SessionId must match the regexp '^\\w{1,32}$'");
	}
}
