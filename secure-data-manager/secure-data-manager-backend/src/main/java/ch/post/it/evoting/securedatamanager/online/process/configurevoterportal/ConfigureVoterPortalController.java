/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.configurevoterportal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.domain.configuration.VoterPortalConfigPayload;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;

@RestController
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
@RequestMapping("/sdm-online/configure-voter-portal")
public class ConfigureVoterPortalController {

	private final ElectionEventService electionEventService;
	private final ConfigureVoterPortalService configureVoterPortalService;
	private final String voterPortalUrl;

	public ConfigureVoterPortalController(
			final ElectionEventService electionEventService,
			final ConfigureVoterPortalService configureVoterPortalService,
			@Value("${voter-portal.url}")
			final String voterPortalUrl) {
		this.electionEventService = electionEventService;
		this.configureVoterPortalService = configureVoterPortalService;
		this.voterPortalUrl = checkNotNull(voterPortalUrl);
	}

	@PostMapping
	public void saveVoterPortalConfiguration() {
		final String electionEventId = electionEventService.findElectionEventId();

		final VoterPortalConfigPayload localVoterPortalConfiguration = configureVoterPortalService.getVoterPortalConfigurationFromLocal(
				electionEventId);

		checkNotNull(localVoterPortalConfiguration, "The local voter portal configuration is required.");
		checkNotNull(localVoterPortalConfiguration.config(), "The config in the voter portal configuration payload is required.");
		checkNotNull(localVoterPortalConfiguration.favicon(), "The favicon in the voter portal configuration payload is required.");
		checkNotNull(localVoterPortalConfiguration.logo(), "The logo in the voter portal configuration payload is required.");

		configureVoterPortalService.uploadVoterPortalConfiguration(electionEventId, localVoterPortalConfiguration);
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<VoterPortalConfigurationSdmPayload> getVoterPortalConfiguration() {
		final String electionEventId = electionEventService.findElectionEventId();
		final Path sourcePath = configureVoterPortalService.getVoterPortalConfigurationPath();
		final VoterPortalConfigPayload remoteVoterPortalConfiguration = configureVoterPortalService.getVoterPortalConfiguration(electionEventId);
		final VoterPortalConfigPayload localVoterPortalConfiguration = configureVoterPortalService.getVoterPortalConfigurationFromLocal(
				electionEventId);
		final VoterPortalConfigurationPayloadState state = configureVoterPortalService.getPayloadState(localVoterPortalConfiguration,
				remoteVoterPortalConfiguration);

		final VoterPortalConfigurationSdmPayload voterPortalConfigurationSdmPayload = new VoterPortalConfigurationSdmPayload(
				sourcePath.toString(),
				voterPortalUrl + electionEventId,
				localVoterPortalConfiguration,
				state);

		return ResponseEntity.ok(voterPortalConfigurationSdmPayload);
	}
}
