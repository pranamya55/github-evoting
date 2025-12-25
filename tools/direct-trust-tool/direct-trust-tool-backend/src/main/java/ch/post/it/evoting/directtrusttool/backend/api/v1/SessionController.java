/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.api.v1;

import static ch.post.it.evoting.directtrusttool.backend.api.v1.RouteConstants.BASE_PATH;
import static ch.post.it.evoting.directtrusttool.backend.session.SessionIdValidator.validateSessionId;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.directtrusttool.backend.session.Phase;
import ch.post.it.evoting.directtrusttool.backend.session.SessionService;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@RestController
@RequestMapping(BASE_PATH + "/session")
public class SessionController {

	private final SessionService sessionService;

	public SessionController(final SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@PostMapping(value = "")
	public String createSession() {
		return sessionService.createNewSession();
	}

	@GetMapping(value = "{sessionId}", produces = "application/json")
	public Phase sessionPhase(
			@PathVariable
			final String sessionId) {
		validateSessionId(sessionId);
		return sessionService.getSessionPhase(sessionId);
	}

	@DeleteMapping(value = "{sessionId}")
	public void deleteSession(
			@PathVariable
			final String sessionId) {
		validateSessionId(sessionId);
		sessionService.deleteSession(sessionId);
	}

	@GetMapping(value = "{sessionId}/selected", produces = "application/json")
	public ImmutableSet<Alias> getSelectedKeystore(
			@PathVariable
			final String sessionId) {
		validateSessionId(sessionId);
		return sessionService.getCurrentComponents(sessionId);
	}

	@GetMapping(value = "{sessionId}/key/{key}", produces = "application/json")
	public String getSessionKeyValue(
			@PathVariable
			final String sessionId,
			@PathVariable
			final String key) {
		validateSessionId(sessionId);
		checkNotNull(key);
		return sessionService.getSessionKey(sessionId, key);
	}
}
