/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.votingserver.process.Constants.DUMMY_ELECTION_EVENT_ID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.configuration.VoterPortalConfigPayload;
import ch.post.it.evoting.domain.voting.JsonSchemaConstants;
import ch.post.it.evoting.evotinglibraries.domain.validations.JsonSchemaValidation;

@Service
public class ConfigureVoterPortalService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureVoterPortalService.class);

	private final ImageDetectionService imageDetectionService;
	private final VoterPortalConfigurationRepository voterPortalConfigurationRepository;
	private final DummyVoterPortalConfigurationFileRepository dummyVoterPortalConfigPayloadFileRepository;
	private final ObjectMapper mapper;

	public ConfigureVoterPortalService(
			final ImageDetectionService imageDetectionService,
			final VoterPortalConfigurationRepository voterPortalConfigurationRepository,
			final DummyVoterPortalConfigurationFileRepository dummyVoterPortalConfigPayloadFileRepository,
			final ObjectMapper mapper) {
		this.imageDetectionService = imageDetectionService;
		this.voterPortalConfigurationRepository = voterPortalConfigurationRepository;
		this.dummyVoterPortalConfigPayloadFileRepository = dummyVoterPortalConfigPayloadFileRepository;
		this.mapper = mapper;
	}

	@Transactional
	public void saveVoterPortalConfiguration(final String electionEventId, final VoterPortalConfigPayload voterPortalConfigPayload) {
		validateUUID(electionEventId);
		checkNotNull(voterPortalConfigPayload);
		checkArgument(!DUMMY_ELECTION_EVENT_ID.equals(electionEventId), "The election event id must be different than %s", DUMMY_ELECTION_EVENT_ID);
		validatePayload(voterPortalConfigPayload, electionEventId);

		LOGGER.debug("Saving voter portal configuration for election event. [electionEventId: {}]", electionEventId);

		final VoterPortalConfiguration voterPortalConfiguration = voterPortalConfigurationRepository.findById(electionEventId)
				.orElse(new VoterPortalConfiguration(electionEventId));

		voterPortalConfiguration.setConfig(voterPortalConfigPayload.config());
		voterPortalConfiguration.setFavicon(voterPortalConfigPayload.favicon());
		voterPortalConfiguration.setLogo(voterPortalConfigPayload.logo());

		voterPortalConfigurationRepository.save(voterPortalConfiguration);

		LOGGER.info("Voter portal configuration saved for election event. [electionEventId: {}]", electionEventId);
	}

	public Optional<VoterPortalConfiguration> getVoterPortalConfiguration(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.debug("Getting voter portal configuration for election event. [electionEventId: {}]", electionEventId);

		if (DUMMY_ELECTION_EVENT_ID.equals(electionEventId)) {
			LOGGER.info("Returning dummy voter portal configuration for election event. [electionEventId: {}]", electionEventId);
			return Optional.of(dummyVoterPortalConfigPayloadFileRepository.getDummyVoterPortalConfiguration());
		}

		LOGGER.info("Returning voter portal configuration for election event. [electionEventId: {}]", electionEventId);
		return voterPortalConfigurationRepository.findById(electionEventId);
	}

	private void validatePayload(final VoterPortalConfigPayload voterPortalConfigPayload, final String electionEventId) {
		checkArgument(voterPortalConfigPayload.config() != null, "Config is required. [electionEventId: %s]", electionEventId);
		checkArgument(imageDetectionService.isAnIcon(voterPortalConfigPayload.favicon()), "Favicon is not an icon. [electionEventId: %s]",
				electionEventId);
		checkArgument(imageDetectionService.isAnImage(voterPortalConfigPayload.logo()), "Logo is not an image. [electionEventId: %s]",
				electionEventId);
		final JsonNode jsonNode;
		try {
			jsonNode = mapper.readTree(voterPortalConfigPayload.config().elements());
		} catch (final IOException e) {
			throw new UncheckedIOException("Voter portal configuration file is not in json format. [electionEventId: %s]".formatted(electionEventId),
					e);
		}
		JsonSchemaValidation.validate(jsonNode, JsonSchemaConstants.VOTER_PORTAL_CONFIG_SCHEMA);
	}
}
