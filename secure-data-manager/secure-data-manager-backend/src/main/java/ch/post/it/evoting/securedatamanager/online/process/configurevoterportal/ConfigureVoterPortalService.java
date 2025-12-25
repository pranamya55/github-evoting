/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.configurevoterportal;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.CONFIGURE_VOTER_PORTAL;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.Files.getNameWithoutExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.configuration.VoterPortalConfigPayload;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowService;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

import reactor.core.publisher.Mono;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class ConfigureVoterPortalService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureVoterPortalService.class);

	private final PathResolver pathResolver;
	private final WorkflowService workflowService;
	private final WebClientFactory webClientFactory;
	private final WorkflowStepRunner workflowStepRunner;

	public ConfigureVoterPortalService(
			final PathResolver pathResolver,
			final WorkflowService workflowService,
			final WebClientFactory webClientFactory,
			final WorkflowStepRunner workflowStepRunner) {
		this.pathResolver = pathResolver;
		this.workflowService = workflowService;
		this.webClientFactory = webClientFactory;
		this.workflowStepRunner = workflowStepRunner;
	}

	public VoterPortalConfigPayload getVoterPortalConfiguration(final String electionEventId) {
		validateUUID(electionEventId);

		// Avoid fetching the configuration to the Voting Server if the step is not complete
		if (!workflowService.isStepComplete(CONFIGURE_VOTER_PORTAL)) {
			return null;
		}

		final Optional<VoterPortalConfigPayload> response = webClientFactory.getWebClient(
						String.format("Request for uploading voter portal configuration payload failed. [electionEventId: %s]", electionEventId))
				.get()
				.uri(uriBuilder -> uriBuilder.path(
						"api/v1/configuration/configurevoterportal/electionevent/{electionEventId}").build(electionEventId))
				.exchangeToMono(res -> {
					if (res.statusCode() == HttpStatus.OK) {
						return res.bodyToMono(VoterPortalConfigPayload.class);
					} else if (res.statusCode() == HttpStatus.NOT_FOUND) {
						return Mono.empty();
					} else {
						return res.createException().flatMap(Mono::error);
					}
				})
				.blockOptional();

		return response.orElse(null);
	}

	public VoterPortalConfigPayload getVoterPortalConfigurationFromLocal(final String electionEventId) {
		validateUUID(electionEventId);

		final Path voterPortalConfigurationFolderPath = getVoterPortalConfigurationPath();
		// Define the file patterns to search
		final Set<String> targetFilesWithoutExtension = Set.of("logo", "favicon");
		final String targetFileWithExtension = "config.json";

		final List<Path> matchingFiles;
		try (final Stream<Path> voterPortalConfigurationFolderWalk = Files.walk(voterPortalConfigurationFolderPath, 1)) {

			matchingFiles = voterPortalConfigurationFolderWalk
					.filter(path -> {
						final String fileName = path.getFileName().toString();
						// Check for files without extension
						final boolean matchesWithoutExtension = targetFilesWithoutExtension.stream()
								.anyMatch(name -> fileName.startsWith(name + "."));
						// Check for the exact match (config.json)
						final boolean matchesExact = fileName.equals(targetFileWithExtension);
						return matchesWithoutExtension || matchesExact;
					})
					.toList();

		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to walk into voter portal config folder.", e);
		}

		final Map<String, ImmutableByteArray> content = new HashMap<>();
		if (!matchingFiles.isEmpty()) {
			for (final Path filePath : matchingFiles) {
				try {

					// Read file as byte array
					final byte[] fileBytes = Files.readAllBytes(filePath);
					content.put(getNameWithoutExtension(filePath.getFileName().toString()), ImmutableByteArray.of(fileBytes));

				} catch (final IOException e) {
					throw new UncheckedIOException("Unable to read voter portal config files as byte array.", e);
				}
			}
		}

		return new VoterPortalConfigPayload(electionEventId, content.get("config"), content.get("favicon"),
				content.get("logo"));
	}

	public Path getVoterPortalConfigurationPath() {
		return pathResolver.resolveVoterPortalConfigurationPath();
	}

	public VoterPortalConfigurationPayloadState getPayloadState(final VoterPortalConfigPayload localPayload,
			final VoterPortalConfigPayload remotePayload) {
		if (isPayloadInvalid(localPayload)) {
			return createPayloadState(VoterPortalConfigurationPayloadStatus.NOT_FOUND, null, null);
		}

		final ImmutableByteArray voterPortalConfig = localPayload.config();

		if (localPayload.favicon() == null) {
			return createPayloadState(
					getStatus(voterPortalConfig, getRemoteConfig(remotePayload)),
					VoterPortalConfigurationPayloadStatus.NOT_FOUND,
					getStatus(localPayload.logo(), getRemoteLogo(remotePayload))
			);
		}

		if (localPayload.logo() == null) {
			return createPayloadState(
					getStatus(voterPortalConfig, getRemoteConfig(remotePayload)),
					getStatus(localPayload.favicon(), getRemoteFavicon(remotePayload)),
					VoterPortalConfigurationPayloadStatus.NOT_FOUND
			);
		}

		return createPayloadState(
				getStatus(voterPortalConfig, getRemoteConfig(remotePayload)),
				getStatus(localPayload.favicon(), getRemoteFavicon(remotePayload)),
				getStatus(localPayload.logo(), getRemoteLogo(remotePayload))
		);
	}

	private boolean isPayloadInvalid(final VoterPortalConfigPayload payload) {
		return payload == null || payload.config() == null;
	}

	private VoterPortalConfigurationPayloadState createPayloadState(
			final VoterPortalConfigurationPayloadStatus configStatus,
			final VoterPortalConfigurationPayloadStatus faviconStatus,
			final VoterPortalConfigurationPayloadStatus logoStatus) {
		return new VoterPortalConfigurationPayloadState(configStatus, faviconStatus, logoStatus);
	}

	private ImmutableByteArray getRemoteConfig(final VoterPortalConfigPayload remotePayload) {
		return remotePayload != null ? remotePayload.config() : null;
	}

	private ImmutableByteArray getRemoteFavicon(final VoterPortalConfigPayload remotePayload) {
		return remotePayload != null ? remotePayload.favicon() : null;
	}

	private ImmutableByteArray getRemoteLogo(final VoterPortalConfigPayload remotePayload) {
		return remotePayload != null ? remotePayload.logo() : null;
	}

	private VoterPortalConfigurationPayloadStatus getStatus(final ImmutableByteArray localPayloadContent,
			final ImmutableByteArray remotePayloadContent) {
		if (localPayloadContent == null || localPayloadContent.isEmpty()) {
			return VoterPortalConfigurationPayloadStatus.NOT_FOUND;
		} else if (localPayloadContent.equals(remotePayloadContent)) {
			return VoterPortalConfigurationPayloadStatus.SYNCHRONIZED;
		} else {
			return VoterPortalConfigurationPayloadStatus.NOT_SYNCHRONIZED;
		}
	}

	public void uploadVoterPortalConfiguration(final String electionEventId, final VoterPortalConfigPayload voterPortalConfigPayload) {
		validateUUID(electionEventId);
		checkNotNull(voterPortalConfigPayload);

		LOGGER.debug("Uploading the voter portal configuration... [electionEventId: {}]", electionEventId);

		final WorkflowTask workflowTask = new WorkflowTask(
				() -> performUpload(electionEventId, voterPortalConfigPayload),
				() -> LOGGER.info("Upload of voter portal configuration successful. [electionEventId: {}]", electionEventId),
				throwable -> LOGGER.error("Upload of voter portal configuration failed. [electionEventId: {}]", electionEventId, throwable)
		);

		workflowStepRunner.run(CONFIGURE_VOTER_PORTAL, workflowTask);
	}

	private void performUpload(final String electionEventId, final VoterPortalConfigPayload voterPortalConfigPayload) {
		final ResponseEntity<Void> response = webClientFactory.getWebClient(
						String.format("Request for uploading voter portal configuration payload failed. [electionEventId: %s]", electionEventId))
				.post()
				.uri(uriBuilder -> uriBuilder.path(
						"api/v1/configuration/configurevoterportal/electionevent/{electionEventId}").build(electionEventId))
				.body(Mono.just(voterPortalConfigPayload), VoterPortalConfigPayload.class)
				.exchangeToMono(res -> {
					if (res.statusCode() == HttpStatus.PRECONDITION_FAILED) {
						throw new IllegalStateException(String.format("Invalid voter portal configuration. [electionEventId: %s]", electionEventId));
					} else {
						return res.toBodilessEntity();
					}
				})
				.block();
		checkState(checkNotNull(response).getStatusCode().is2xxSuccessful());
	}

}
