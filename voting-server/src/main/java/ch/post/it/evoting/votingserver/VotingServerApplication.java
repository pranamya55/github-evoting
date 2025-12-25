/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver;

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
import org.springframework.boot.system.ApplicationHome;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableCaching
@EnableScheduling
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class VotingServerApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingServerApplication.class);

	public static void main(final String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		SpringApplication.run(VotingServerApplication.class);
		logJarHash();
	}

	@Scheduled(cron = "${heartbeat.cron}")
	public void heartbeat() {
		LOGGER.info("Voting Server is running.");
		logJarHash();
	}

	private static void logJarHash() {
		final Path jarPath = new ApplicationHome(VotingServerApplication.class).getSource().toPath();

		try {
			final String fingerprintLogMessage = getFingerprintLogMessage(jarPath);
			LOGGER.info(fingerprintLogMessage);
		} catch (final IOException | NoSuchAlgorithmException e) {
			LOGGER.error("Failed to compute JAR Fingerprint.", e);
		}
	}
}
