/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver;

import static ch.post.it.evoting.evotinglibraries.domain.JarHashLogger.getFingerprintLogMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.event.EventListener;

import ch.post.it.evoting.tools.disputeresolver.process.DisputeResolverService;

@SpringBootApplication
public class DisputeResolverApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(DisputeResolverApplication.class);

	private final DisputeResolverService disputeResolverService;

	public static void main(final String[] args) {
		SpringApplication.run(DisputeResolverApplication.class);
	}

	public DisputeResolverApplication(final DisputeResolverService disputeResolverService) {
		this.disputeResolverService = disputeResolverService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReadyEvent() {
		// Log the JAR fingerprint.
		logJarHash();

		// Start the dispute resolution process on application ready event.
		disputeResolverService.run();
	}

	private static void logJarHash() {
		final Path jarPath = new ApplicationHome(DisputeResolverApplication.class).getSource().toPath();

		try {
			final String fingerprintLogMessage = getFingerprintLogMessage(jarPath);
			LOGGER.info(fingerprintLogMessage);
		} catch (final IOException | NoSuchAlgorithmException e) {
			LOGGER.error("Failed to compute JAR Fingerprint.", e);
		}
	}

}
