/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

import ch.post.it.evoting.securedatamanager.shared.Constants;

@ConfigurationProperties(prefix = "voting-server")
public record VotingServerProperties(String host, String url, ConnectionProperties connection, HealthCheckProperties healthCheck,
									 ResponseProperties response, RSocketProperties rsocket) {

	public VotingServerProperties {
		validateHost(host);
	}

	private void validateHost(final String host) {
		final URI votingServerURI;
		try {
			votingServerURI = new URI(host);
		} catch (final URISyntaxException e) {
			throw new IllegalArgumentException("Failed to parse voting server host.", e);
		}

		checkNotNull(votingServerURI.getScheme());
		checkArgument(Objects.isNull(votingServerURI.getQuery()) && Objects.isNull(votingServerURI.getFragment()) &&
				votingServerURI.getPath().isEmpty(), "The voting server host is not valid.");
	}

	public record ConnectionProperties(int timeout, int maxMessageSize, boolean keepAlive, String clientKeystoreLocation,
									   String clientKeystorePasswordLocation) {
		public ConnectionProperties {

			if (!clientKeystoreLocation.isBlank()) {
				// SSL is enabled.
				validateKeystoreLocations(clientKeystoreLocation, clientKeystorePasswordLocation);
			}
		}

		private static void validateKeystoreLocations(final String keystoreLocation, final String keystorePasswordLocation) {
			checkNotNull(keystoreLocation, "The Two-Way SSL keystore location is null.");
			checkNotNull(keystorePasswordLocation, "The Two-Way SSL keystore password location is null.");

			checkArgument(!keystoreLocation.isBlank(), "The Two-Way SSL keystore location is empty.");
			checkArgument(!keystorePasswordLocation.isBlank(), "The Two-Way SSL keystore password location is empty.");

			final Path keystoreLocationPath;
			try {
				keystoreLocationPath = Paths.get(keystoreLocation).toRealPath(LinkOption.NOFOLLOW_LINKS);
			} catch (final IOException e) {
				throw new UncheckedIOException("The file does not exist.", e);
			}
			checkArgument(Files.isRegularFile(keystoreLocationPath), "The given Two-Way SSL keystore location is not a file. [path: %s]",
					keystoreLocationPath);
			checkArgument(keystoreLocation.endsWith(Constants.P12),
					"The given Two-Way SSL keystore location does not have the correct extension. [path: %s]", keystoreLocationPath);

			final Path keystorePasswordLocationPath;
			try {
				keystorePasswordLocationPath = Paths.get(keystorePasswordLocation).toRealPath(LinkOption.NOFOLLOW_LINKS);
			} catch (final IOException e) {
				throw new UncheckedIOException("The file does not exist.", e);
			}
			checkArgument(Files.isRegularFile(keystorePasswordLocationPath),
					"The given Two-Way SSL keystore password location is not a file. [path: %s]",
					keystorePasswordLocationPath);
			checkArgument(keystorePasswordLocation.endsWith(Constants.TXT),
					"The given Two-Way SSL keystore password location does not have the correct extension. [path: %s]", keystorePasswordLocationPath);
		}
	}

	public record HealthCheckProperties(long fixedRate, long readTimeout) {
		public HealthCheckProperties {
			checkArgument(readTimeout > 0, "The read timeout must be greater than 0. [readTimeout: %s]", readTimeout);
			checkArgument(readTimeout < fixedRate, "The read timeout must be less than the fixed rate. [readTimeout: %s, fixedRate: %s]", readTimeout,
					fixedRate);
		}
	}

	public record ResponseProperties(long timeout) {}

	public record RSocketProperties(String path, int mtu, long blockTimeout) {}

}



