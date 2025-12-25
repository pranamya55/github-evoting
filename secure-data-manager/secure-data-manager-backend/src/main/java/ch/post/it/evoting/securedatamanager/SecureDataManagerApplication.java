/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager;

import static ch.post.it.evoting.evotinglibraries.domain.JarHashLogger.getFingerprintLogMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@EnableScheduling
@EnableCaching
@SpringBootApplication
@ConfigurationPropertiesScan
public class SecureDataManagerApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecureDataManagerApplication.class);

	public static void main(final String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		final ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(SecureDataManagerApplication.class).run(args);
		applicationContext.registerShutdownHook();
		logJarHash();
	}

	private static void logJarHash() {
		final Path jarPath = new ApplicationHome(SecureDataManagerApplication.class).getSource().toPath();

		try {
			final String fingerprintLogMessage = getFingerprintLogMessage(jarPath);
			LOGGER.info(fingerprintLogMessage);
		} catch (final IOException | NoSuchAlgorithmException e) {
			LOGGER.error("Failed to compute JAR Fingerprint.", e);
		}
	}

}
