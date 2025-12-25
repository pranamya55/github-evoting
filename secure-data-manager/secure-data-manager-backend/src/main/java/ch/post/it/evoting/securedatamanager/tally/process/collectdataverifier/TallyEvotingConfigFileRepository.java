/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.collectdataverifier;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.XMLSignatureService;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.KeystoreRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Repository
@ConditionalOnProperty("role.isTally")
public class TallyEvotingConfigFileRepository extends EvotingConfigFileRepository {

	private static final String EVOTING_CONFIG_XML = Constants.CONFIG_FILE_NAME_CONFIGURATION_ANONYMIZED;
	private static final Logger LOGGER = LoggerFactory.getLogger(TallyEvotingConfigFileRepository.class);

	private final PathResolver pathResolver;
	private final ElectionEventService electionEventService;

	public TallyEvotingConfigFileRepository(
			final PathResolver pathResolver,
			final ElectionEventService electionEventService,
			final KeystoreRepository keystoreRepository,
			final XMLSignatureService xmlSignatureService) {
		super(keystoreRepository, xmlSignatureService);
		this.pathResolver = pathResolver;
		this.electionEventService = electionEventService;
	}

	@Override
	public void saveFromExternalConfiguration() {
		throw new UnsupportedOperationException("This method is not supported for the Tally role.");
	}

	/**
	 * Loads the evoting-config. The canton config is located in the {@value EVOTING_CONFIG_XML} file.
	 * <p>
	 * If the configuration-anonymized file does not exist this method returns an empty Optional.
	 * <p>
	 * This method also validates the signature of the loaded file.
	 *
	 * @return the configuration-anonymized as an {@link Optional}.
	 * @throws IllegalStateException if the signature is invalid, or it could not be verified.
	 */
	@Override
	public Optional<Configuration> load() {
		final String electionEventId = electionEventService.findElectionEventId();

		validateUUID(electionEventId);

		LOGGER.debug("Loading canton config file. [electionEventId: {}]", electionEventId);

		final Path xmlFilePath = pathResolver.resolveConfigurationPath().resolve(EVOTING_CONFIG_XML);

		if (!Files.exists(xmlFilePath)) {
			LOGGER.debug("The requested file does not exist. [electionEventId: {}, xmlFilePath: {}]", electionEventId, xmlFilePath);
			return Optional.empty();
		}

		checkState(isSignatureValid(xmlFilePath), "The signature of the configuration-anonymized is not valid.");

		final Configuration configuration = readWithoutSchemaValidation(xmlFilePath, Configuration.class);

		LOGGER.debug("File successfully loaded. [electionEventId: {}, file: {}]", electionEventId, EVOTING_CONFIG_XML);

		return Optional.of(configuration);
	}

}
