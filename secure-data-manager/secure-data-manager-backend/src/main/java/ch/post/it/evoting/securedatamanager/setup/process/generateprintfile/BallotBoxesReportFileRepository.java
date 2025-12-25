/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.BALLOT_BOXES_REPORT_FILE_NAME;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Repository
@ConditionalOnProperty("role.isSetup")
public class BallotBoxesReportFileRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxesReportFileRepository.class);

	private final ObjectMapper objectMapper;
	private final PathResolver pathResolver;
	private final String filename;

	public BallotBoxesReportFileRepository(
			final ObjectMapper objectMapper,
			final PathResolver pathResolver,
			@Value("${sdm.election-event-seed}")
			final String electionEventSeed) {
		this.objectMapper = objectMapper;
		this.pathResolver = pathResolver;
		this.filename = String.format(BALLOT_BOXES_REPORT_FILE_NAME, validateSeed(electionEventSeed));
	}

	/**
	 * Saves the ballot boxes file report for the given election event in the printing output directory.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 */
	public void save(final String electionEventId, final BallotBoxesReport ballotBoxesReport) {
		validateUUID(electionEventId);
		checkNotNull(ballotBoxesReport);

		final Path printingOutputPath = pathResolver.resolvePrintingOutputPath();
		try {
			Files.write(printingOutputPath.resolve(filename), objectMapper.writeValueAsBytes(ballotBoxesReport));
			LOGGER.debug("Successfully wrote ballot boxes report file. [electionEventId: {}]", electionEventId);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to write ballot boxes report file. [electionEventId: %s]", electionEventId), e);
		}
	}

}
