/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class DummyVoterPortalConfigurationFileRepository {

	private static final String DUMMY_FILE_NAME = "/dummy-voter-portal-configuration.json";

	private final ObjectMapper objectMapper;

	public DummyVoterPortalConfigurationFileRepository(final ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public VoterPortalConfiguration getDummyVoterPortalConfiguration() {
		try (final InputStream resourceAsStream = this.getClass().getResourceAsStream(DUMMY_FILE_NAME)) {
			checkNotNull(resourceAsStream);
			final BufferedReader in = new BufferedReader(new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8));
			return objectMapper.readValue(in, VoterPortalConfiguration.class);
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to read the dummy voter portal config payload file.", e);
		}
	}
}
