/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.configurevoterportal;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.domain.configuration.VoterPortalConfigPayload;
import ch.post.it.evoting.votingserver.process.ConfigureVoterPortalService;

@RestController
@RequestMapping("api/v1/configuration/configurevoterportal")
public class ConfigureVoterPortalController {

	private final ConfigureVoterPortalService configureVoterPortalService;

	public ConfigureVoterPortalController(final ConfigureVoterPortalService configureVoterPortalService) {
		this.configureVoterPortalService = configureVoterPortalService;
	}

	@PostMapping("electionevent/{electionEventId}")
	public void saveVoterPortalConfigPayload(
			@PathVariable
			final String electionEventId,
			@RequestBody
			final VoterPortalConfigPayload voterPortalConfigPayload) {
		validateUUID(electionEventId);
		checkNotNull(voterPortalConfigPayload);
		checkState(electionEventId.equals(voterPortalConfigPayload.electionEventId()));

		configureVoterPortalService.saveVoterPortalConfiguration(electionEventId, voterPortalConfigPayload);
	}

	@GetMapping("electionevent/{electionEventId}")
	public ResponseEntity<VoterPortalConfigPayload> getVoterPortalConfigPayload(
			@PathVariable
			final String electionEventId) {
		validateUUID(electionEventId);

		final Optional<VoterPortalConfigPayload> voterPortalConfigPayload = configureVoterPortalService.getVoterPortalConfiguration(electionEventId)
				.map(vpc -> new VoterPortalConfigPayload(vpc.getElectionEventId(), vpc.getConfig(), vpc.getFavicon(), vpc.getLogo()));

		return voterPortalConfigPayload.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}
}
