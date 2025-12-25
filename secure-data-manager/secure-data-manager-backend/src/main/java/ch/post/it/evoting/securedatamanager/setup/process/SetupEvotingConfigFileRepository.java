/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.evotinglibraries.protocol.algorithms.channelsecurity.XMLSignatureService;
import ch.post.it.evoting.evotinglibraries.xml.XsdConstants;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.KeystoreRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Repository
@ConditionalOnProperty("role.isSetup")
public class SetupEvotingConfigFileRepository extends EvotingConfigFileRepository {

	private static final String EVOTING_CONFIG_XML = Constants.CONFIG_FILE_NAME_CONFIGURATION_ANONYMIZED;
	private static final Logger LOGGER = LoggerFactory.getLogger(SetupEvotingConfigFileRepository.class);

	private final PathResolver pathResolver;
	private final ElectionEventService electionEventService;

	public SetupEvotingConfigFileRepository(
			final PathResolver pathResolver,
			final ElectionEventService electionEventService,
			final KeystoreRepository keystoreRepository,
			final XMLSignatureService xmlSignatureService) {
		super(keystoreRepository, xmlSignatureService);
		this.pathResolver = pathResolver;
		this.electionEventService = electionEventService;
	}

	/**
	 * Saves the evoting-config file from the external configuration path and validates it against the related XSD. The file is saved in the following
	 * path {@value Constants#CONFIGURATION}/{@value EVOTING_CONFIG_XML}.
	 * <p>
	 * The evoting-config related XSD is located in {@value XsdConstants#CANTON_CONFIG_XSD}.
	 * <p>
	 * This method also validates the signature of the saved file.
	 *
	 * @throws NullPointerException  if any input is null.
	 * @throws IllegalStateException if
	 *                               <ul>
	 *                                   <li>there is no configuration-anonymized file in the external configuration path.</li>
	 *                                   <li>the signature is invalid, or it could not be verified.</li>
	 *                                   <li>the schema validation of the configuration-anonymized file failed.</li>
	 *                               </ul>
	 */
	@Override
	public void saveFromExternalConfiguration() {
		LOGGER.debug("Saving file from external configuration path. [file: {}]", EVOTING_CONFIG_XML);

		final Path externalConfigurationPath = pathResolver.resolveExternalConfigurationPath();
		final Path internalConfigurationPath = pathResolver.resolveConfigurationPath().resolve(EVOTING_CONFIG_XML);

		checkState(Files.exists(externalConfigurationPath),
				"There is no configuration-anonymized file in the external configuration path.");
		checkState(isSignatureValid(externalConfigurationPath), "The signature of the configuration-anonymized is not valid.");
		checkNotNull(read(externalConfigurationPath, XsdConstants.CANTON_CONFIG_XSD, Configuration.class),
				"The schema validation of the configuration-anonymized file failed.");
		LOGGER.info("Schema validation successful. [sourceFilePath: {}, schema: {}]", externalConfigurationPath, XsdConstants.CANTON_CONFIG_XSD);

		try {
			Files.copy(externalConfigurationPath, internalConfigurationPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Could not copy file from external configuration path to internal configuration path. [file: %s]",
							EVOTING_CONFIG_XML), e);
		}

		LOGGER.debug("File successfully saved. [file: {}]", EVOTING_CONFIG_XML);
	}

	/**
	 * Loads the evoting-config. The evoting-config is located in the {@value EVOTING_CONFIG_XML} file and the related XSD in
	 * {@value XsdConstants#CANTON_CONFIG_XSD}.
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

		LOGGER.debug("Loading canton config file. [electionEventId: {}]", electionEventId);

		final Path internalConfigurationPath = pathResolver.resolveConfigurationPath().resolve(EVOTING_CONFIG_XML);

		final Optional<Configuration> configuration = load(internalConfigurationPath);

		if (configuration.isEmpty()) {
			LOGGER.debug("The requested file does not exist. [electionEventId: {}, xmlFilePath: {}]", electionEventId, internalConfigurationPath);
		}

		LOGGER.debug("File successfully loaded. [file: {}]", EVOTING_CONFIG_XML);

		return configuration;
	}

	/**
	 * Loads the external evoting-config file from the external configuration path.
	 * <p>
	 * If the configuration-anonymized file does not exist this method returns an empty Optional.
	 * <p>
	 * This method also validates the signature of the loaded file.
	 *
	 * @return the configuration-anonymized as an {@link Optional}.
	 * @throws IllegalStateException if the signature is invalid, or it could not be verified.
	 */
	public Optional<Configuration> loadExternalConfiguration() {

		final Path externalConfigurationPath = pathResolver.resolveExternalConfigurationPath();

		return load(externalConfigurationPath);
	}

	/**
	 * Loads the evoting-config file from the given path.
	 * <p>
	 * If the configuration-anonymized file does not exist this method returns an empty Optional.
	 * <p>
	 * This method validates the signature of the loaded file.
	 *
	 * @param filePath the path to the configuration file.
	 * @return the configuration-anonymized as an {@link Optional}.
	 * @throws IllegalStateException if the signature is invalid, or it could not be verified.
	 */
	private Optional<Configuration> load(final Path filePath) {

		if (!Files.exists(filePath)) {
			return Optional.empty();
		}

		checkState(isSignatureValid(filePath), "The signature of the configuration-anonymized is not valid.");

		final Configuration configuration;
		configuration = readWithoutSchemaValidation(filePath, Configuration.class);

		return Optional.of(configuration);
	}
}
