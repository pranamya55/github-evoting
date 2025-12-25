/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import static ch.post.it.evoting.evotinglibraries.domain.JarHashLogger.getFingerprintLogMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableCaching
@EntityScan("ch.post.it.evoting")
@EnableJpaRepositories("ch.post.it.evoting")
@EnableScheduling
@SpringBootApplication(scanBasePackages = { "ch.post.it.evoting" })
public class ControlComponentsApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentsApplication.class);

	public static void main(final String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		SpringApplication.run(ControlComponentsApplication.class);
		logJarHash();
	}

	@Scheduled(cron = "${heartbeat.cron}")
	public void heartbeat() {
		LOGGER.info("Control Component is running.");
		logJarHash();
	}

	private static void logJarHash() {
		final Path jarPath = new ApplicationHome(ControlComponentsApplication.class).getSource().toPath();

		try {
			final String fingerprintLogMessage = getFingerprintLogMessage(jarPath);
			LOGGER.info(fingerprintLogMessage);
		} catch (final IOException | NoSuchAlgorithmException e) {
			LOGGER.error("Failed to compute JAR Fingerprint.", e);
		}
	}
}

