/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.votingserverhealth;

import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController()
@RequestMapping("/sdm-online/voting-server-health")
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class VotingServerHealthController {
	private final VotingServerHealthService votingServerHealthService;
	private SseEmitter sseEmitter;
	private final Consumer<VotingServerHealth> listener = votingServerHealth -> {
		try {
			sseEmitter.send(votingServerHealth);
		} catch (final Exception e) {
			sseEmitter = null;
			removeListener();
		}
	};

	public VotingServerHealthController(final VotingServerHealthService votingServerHealthService) {
		this.votingServerHealthService = votingServerHealthService;
	}

	@GetMapping("subscribe")
	public SseEmitter subscribe() {
		if (sseEmitter == null) {
			sseEmitter = new SseEmitter();
			votingServerHealthService.addListener(listener);
		}
		return sseEmitter;
	}

	private void removeListener() {
		votingServerHealthService.removeListener(listener);
	}

}
