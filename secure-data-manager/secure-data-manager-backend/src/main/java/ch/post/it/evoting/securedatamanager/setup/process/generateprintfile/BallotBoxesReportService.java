/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_SETUP_COMPONENT_TALLY_DATA_PAYLOAD;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.AuthorizationType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoterType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoterTypeType;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Service
@ConditionalOnProperty("role.isSetup")
public class BallotBoxesReportService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxesReportService.class);

	private final PathResolver pathResolver;
	private final ObjectMapper objectMapper;
	private final EvotingConfigService evotingConfigService;
	private final BallotBoxesReportFileRepository ballotBoxesReportFileRepository;

	public BallotBoxesReportService(
			final PathResolver pathResolver,
			final ObjectMapper objectMapper,
			final EvotingConfigService evotingConfigService,
			final BallotBoxesReportFileRepository ballotBoxesReportFileRepository) {
		this.pathResolver = pathResolver;
		this.objectMapper = objectMapper;
		this.evotingConfigService = evotingConfigService;
		this.ballotBoxesReportFileRepository = ballotBoxesReportFileRepository;
	}

	/**
	 * Generates the Ballot Boxes report for the given election event.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 */
	public void generate(final String electionEventId) {
		validateUUID(electionEventId);

		LOGGER.debug("Generating ballot boxes report... [electionEventId: {}]", electionEventId);

		// Retrieve ballot boxes titles and verification card set ids from the setup component tally data payloads.
		final Path verificationCardSetsPath = pathResolver.resolveVerificationCardSetsPath(electionEventId);
		final ImmutableMap<String, String> tallyDataPayloadInfos;
		try (final Stream<Path> verificationCardSetsWalk = Files.walk(verificationCardSetsPath, 2)) {
			tallyDataPayloadInfos = verificationCardSetsWalk
					.filter(path -> path.endsWith(Path.of(CONFIG_FILE_NAME_SETUP_COMPONENT_TALLY_DATA_PAYLOAD)))
					.map(path -> {
						try {
							return objectMapper.readValue(Files.readAllBytes(path), SetupComponentTallyDataPayload.class);
						} catch (final IOException e) {
							throw new UncheckedIOException(
									String.format("Failed to deserialize setup component tally data payload. [electionEventId: %s, path: %s]",
											electionEventId, path), e);
						}
					})
					.collect(toImmutableMap(
							SetupComponentTallyDataPayload::getBallotBoxDefaultTitle,
							SetupComponentTallyDataPayload::getVerificationCardSetId));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to read setup component tally data payloads. [electionEventId: %s]", electionEventId), e);
		}
		LOGGER.debug("Loaded tally data payload information. [electionEventId: {}]", electionEventId);

		// Retrieve voter counts by authorization.
		final Configuration configuration = evotingConfigService.load();
		final ImmutableMap<AuthorizationType, Map<VoterTypeType, Long>> countsByAuthorization = configuration.getAuthorizations().getAuthorization()
				.stream()
				.map(authorization ->
						ImmutableMap.entry(
								authorization,
								configuration.getRegister().getVoter().stream()
										.filter(voter -> voter.getAuthorization().equals(authorization.getAuthorizationIdentification()))
										.collect(Collectors.groupingBy(VoterType::getVoterType, Collectors.counting())))
				)
				.collect(toImmutableMap(LinkedHashMap::new));
		LOGGER.debug("Calculated voter counts by authorization. [electionEventId: {}]", electionEventId);

		// Create the ballot boxes report.
		final ImmutableList<BallotBoxInformation> ballotBoxesInformation = countsByAuthorization.entrySet().stream()
				.map(entry -> {
					final AuthorizationType authorizationType = entry.key();
					final String authorizationName = authorizationType.getAuthorizationName();
					final String verificationCardSetId = tallyDataPayloadInfos.get(authorizationName);
					final int countICH = Math.toIntExact(entry.value().getOrDefault(VoterTypeType.SWISSRESIDENT, 0L));
					final int countACH = Math.toIntExact(entry.value().getOrDefault(VoterTypeType.SWISSABROAD, 0L));
					final int countForeigner = Math.toIntExact(entry.value().getOrDefault(VoterTypeType.FOREIGNER, 0L));

					return new BallotBoxInformation(authorizationName, verificationCardSetId, authorizationType.isAuthorizationTest(), countICH,
							countACH, countForeigner);
				})
				.collect(toImmutableList());

		final BallotBoxesReport ballotBoxesReport = new BallotBoxesReport(electionEventId, ballotBoxesInformation);
		LOGGER.debug("Created ballot boxes report. [electionEventId: {}]", electionEventId);

		// Write the ballot boxes report to file.
		ballotBoxesReportFileRepository.save(electionEventId, ballotBoxesReport);
		LOGGER.debug("Saved boxes report. [electionEventId: {}]", electionEventId);
	}

}
