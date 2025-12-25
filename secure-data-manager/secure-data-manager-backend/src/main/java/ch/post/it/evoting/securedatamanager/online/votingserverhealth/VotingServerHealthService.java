/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.votingserverhealth;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ch.post.it.evoting.securedatamanager.online.VotingServerProperties;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;

import reactor.util.retry.Retry;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class VotingServerHealthService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingServerHealthService.class);

	private final List<Consumer<VotingServerHealth>> listeners = new LinkedList<>();
	private final WebClientFactory webClientFactory;
	private final String votingServerHost;
	private final int maxAttempts;
	private final long readTimeout;

	public VotingServerHealthService(
			final WebClientFactory webClientFactory,
			final VotingServerProperties votingServerProperties,
			@Value("${spring.webflux.retry.backoff.max-attempts}")
			final int maxAttempts) {
		this.webClientFactory = webClientFactory;
		this.votingServerHost = votingServerProperties.host();
		this.maxAttempts = maxAttempts;
		this.readTimeout = votingServerProperties.healthCheck().readTimeout();
	}

	public void addListener(final Consumer<VotingServerHealth> consumer) {
		listeners.add(consumer);

		// force the execution to get an immediate result for new listeners.
		run();
	}

	public void removeListener(final Consumer<VotingServerHealth> listener) {
		listeners.remove(listener);
	}

	@Scheduled(fixedRateString = "${voting-server.health-check.fixed-rate}")
	public void run() {
		if (!listeners.isEmpty()) {
			final VotingServerHealth health = getHealth();
			LOGGER.debug("Voting Server health retrieved. [status: {}]", health != null ? health.status() : "");
			listeners.forEach(consumer -> consumer.accept(health));
		}
	}

	private VotingServerHealth getHealth() {
		try {
			return webClientFactory.getWebClient()
					.get()
					.uri(builder -> builder.path("actuator/health").build())
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.bodyToMono(Health.class)
					.map(Health::isUp)
					.map(isUp -> new VotingServerHealth(isUp, votingServerHost))
					.timeout(Duration.ofMillis(readTimeout))
					.retryWhen(Retry.max(maxAttempts))
					.block();
		} catch (final Exception e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Unable to get the voting server healthiness. Assuming it's not healthy.", e);
			}
		}
		return new VotingServerHealth(false, votingServerHost);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Health(String status) {
		public boolean isUp() {
			final boolean isUp = "UP".equalsIgnoreCase(status);
			if (!isUp) {
				LOGGER.warn("Voting Server is not up. [status: {}]", status);
			}
			return isUp;
		}
	}
}
