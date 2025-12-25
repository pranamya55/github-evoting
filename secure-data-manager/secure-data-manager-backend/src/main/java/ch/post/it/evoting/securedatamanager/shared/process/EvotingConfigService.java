/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_MISSING_MESSAGE;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;

@Service
public class EvotingConfigService {

	private final ElectionEventService electionEventService;
	private final EvotingConfigFileRepository evotingConfigFileRepository;

	public EvotingConfigService(
			final ElectionEventService electionEventService,
			final EvotingConfigFileRepository evotingConfigFileRepository) {
		this.electionEventService = electionEventService;
		this.evotingConfigFileRepository = evotingConfigFileRepository;
	}

	public void copyConfigurationToInternalPath() {
		evotingConfigFileRepository.saveFromExternalConfiguration();
	}

	/**
	 * Loads the evoting-config.
	 * <p>
	 * The result of this method is stored in a synchronized cache.
	 *
	 * @return the evoting-config.
	 */
	@Cacheable(value = "evotingConfig", sync = true)
	public Configuration load() {
		final String electionEventId = electionEventService.findElectionEventId();

		return evotingConfigFileRepository.load()
				.orElseThrow(() -> new IllegalStateException(String.format(CONFIG_FILE_MISSING_MESSAGE + "[electionEventId: %s]", electionEventId)));
	}
}
