/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import org.springframework.stereotype.Component;

import ch.post.it.evoting.directtrusttool.backend.session.SessionService;

import picocli.CommandLine;

@Component
@CommandLine.Command(
		name = "clear",
		description = "Remove a workspace and all its generated keystores.",
		mixinStandardHelpOptions = true)
public class ClearCommand implements Runnable {

	@CommandLine.Option(
			names = { "--session-id" },
			description = "The UUID of the wanted session.",
			defaultValue = "00000000000000000000000000000000"
	)
	private String sessionId;

	private final SessionService sessionService;

	public ClearCommand(final SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@Override
	public void run() {
		sessionService.deleteSession(sessionId);
	}
}

