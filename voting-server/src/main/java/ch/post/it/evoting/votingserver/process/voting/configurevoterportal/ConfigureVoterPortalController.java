/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.configurevoterportal;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import java.time.Instant;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.votingserver.process.ConfigureVoterPortalService;

@RestController
@RequestMapping("api/v1/processor/voting/configurevoterportal")
public class ConfigureVoterPortalController {

	private final ConfigureVoterPortalService configureVoterPortalService;

	public ConfigureVoterPortalController(final ConfigureVoterPortalService configureVoterPortalService) {
		this.configureVoterPortalService = configureVoterPortalService;
	}

	@GetMapping(value = "electionevent/{electionEventId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<VoterPortalConfigurationResponsePayload> getVoterPortalConfiguration(
			@PathVariable
			final String electionEventId) {
		validateUUID(electionEventId);

		final Optional<VoterPortalConfigurationResponsePayload> voterPortalConfigurationResponsePayload = configureVoterPortalService.getVoterPortalConfiguration(
						electionEventId)
				.map(vpc -> new VoterPortalConfigurationResponsePayload(vpc.getElectionEventId(), vpc.getConfig(), vpc.getFavicon(), vpc.getLogo(),
						Instant.now().getEpochSecond()));

		return voterPortalConfigurationResponsePayload.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}
}
