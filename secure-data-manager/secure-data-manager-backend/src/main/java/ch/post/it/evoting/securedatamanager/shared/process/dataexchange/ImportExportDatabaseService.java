/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.dataexchange;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.readAllBytes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxEntity;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxStateEntity;
import ch.post.it.evoting.securedatamanager.shared.process.BallotBoxStateRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventEntity;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventRepository;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardEntity;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardRepository;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetRepository;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetStateEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetStateRepository;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowLog;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowLogRepository;

@Service
public class ImportExportDatabaseService {

	public static final String SDM = "sdm_";

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportExportDatabaseService.class);

	private final ObjectMapper objectMapper;
	private final WorkflowLogRepository workflowLogRepository;
	private final ElectionEventRepository electionEventRepository;
	private final BallotBoxStateRepository ballotBoxStateRepository;
	private final ElectoralBoardRepository electoralBoardRepository;
	private final VerificationCardSetRepository verificationCardSetRepository;
	private final VerificationCardSetStateRepository verificationCardSetStateRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@SuppressWarnings("java:S107")
	public ImportExportDatabaseService(
			final ObjectMapper objectMapper,
			final WorkflowLogRepository workflowLogRepository,
			final ElectionEventRepository electionEventRepository,
			final BallotBoxStateRepository ballotBoxStateRepository,
			final ElectoralBoardRepository electoralBoardRepository,
			final VerificationCardSetRepository verificationCardSetRepository,
			final VerificationCardSetStateRepository verificationCardSetStateRepository) {
		this.objectMapper = objectMapper;
		this.workflowLogRepository = workflowLogRepository;
		this.electionEventRepository = electionEventRepository;
		this.ballotBoxStateRepository = ballotBoxStateRepository;
		this.electoralBoardRepository = electoralBoardRepository;
		this.verificationCardSetRepository = verificationCardSetRepository;
		this.verificationCardSetStateRepository = verificationCardSetStateRepository;
	}

	public void exportDatabase(final Path dbDump, final String electionEventId) {
		checkNotNull(dbDump);
		validateUUID(electionEventId);

		LOGGER.debug("Exporting database to dump file: {}", dbDump);
		final DatabaseDump databaseDump;

		// Serialize DB entities.
		try {
			final ElectionEventEntity electionEventEntity = electionEventRepository.findById(electionEventId)
					.orElseThrow(() -> new IllegalStateException("Election Event not found"));
			final ImmutableByteArray electionEvent = ImmutableByteArray.of(objectMapper.writeValueAsBytes(electionEventEntity));

			final List<VerificationCardSetEntity> verificationCardSetEntities = verificationCardSetRepository.findByElectionEventId(electionEventId);
			final ImmutableByteArray verificationCardSets = ImmutableByteArray.of(objectMapper.writeValueAsBytes(verificationCardSetEntities));

			final List<BallotBoxStateEntity> ballotBoxStateEntities = ballotBoxStateRepository.findAll();
			final ImmutableByteArray ballotBoxStates = ImmutableByteArray.of(objectMapper.writeValueAsBytes(ballotBoxStateEntities));

			final List<VerificationCardSetStateEntity> verificationCardSetStateEntities = verificationCardSetStateRepository.findAll();
			final ImmutableByteArray verificationCardSetStates = ImmutableByteArray.of(objectMapper.writeValueAsBytes(verificationCardSetStateEntities));

			final ElectoralBoardEntity electoralBoardEntity = electoralBoardRepository.findAll().getFirst();
			final ImmutableByteArray electoralBoard = ImmutableByteArray.of(objectMapper.writeValueAsBytes(electoralBoardEntity));

			final List<WorkflowLog> workflowLogEntities = workflowLogRepository.findAll();
			final ImmutableByteArray workflowLogs = ImmutableByteArray.of(objectMapper.writeValueAsBytes(workflowLogEntities));

			databaseDump = new DatabaseDump(electionEvent, verificationCardSets, ballotBoxStates, verificationCardSetStates, electoralBoard, workflowLogs);

		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("An error occurred serializing DB entities.", e);
		}

		// Write to dump file.
		try {
			Files.write(dbDump, objectMapper.writeValueAsBytes(databaseDump));
			LOGGER.info("Database export to dump file has been completed successfully: {}", dbDump);
		} catch (final IOException e) {
			LOGGER.error("An error occurred writing DB dump to: {}", dbDump, e);
		}
	}

	@Transactional
	public void importDatabase(final Path dbDump) throws IOException {
		if (Files.notExists(dbDump)) {
			LOGGER.warn("There is no dump database to import");
			return;
		}

		final DatabaseDump databaseDump = readDatabaseDump(dbDump);

		// Election event.
		final ElectionEventEntity electionEventEntity = objectMapper.readValue(databaseDump.electionEvent().elements(), ElectionEventEntity.class);
		entityManager.merge(electionEventEntity);

		// Verification card sets and ballot boxes.
		final List<VerificationCardSetEntity> verificationCardSetEntities = Arrays.asList(
				objectMapper.readValue(databaseDump.verificationCardSets().elements(), VerificationCardSetEntity[].class));
		verificationCardSetEntities.forEach(verificationCardSetEntity -> {
			final BallotBoxEntity merged = entityManager.merge(verificationCardSetEntity.getBallotBoxEntity());
			verificationCardSetEntity.setBallotBoxEntity(merged);
			entityManager.merge(verificationCardSetEntity);
		});

		// Ballot box states and verification card set states.
		importStates(databaseDump);

		// Electoral board.
		final ElectoralBoardEntity electoralBoardEntity = objectMapper.readValue(databaseDump.electoralBoard().elements(), ElectoralBoardEntity.class);
		// Get the state from the database to check if it is after the dump state.
		final Optional<ElectoralBoardEntity> databaseState = electoralBoardRepository.findById(electoralBoardEntity.getElectoralBoardId());
		if (databaseState.isPresent()) {
			final Status dumpStatus = Status.valueOf(electoralBoardEntity.getStatus());
			final Status databaseStatus = Status.valueOf(databaseState.get().getStatus());
			if (dumpStatus.isAfter(databaseStatus)) {
				entityManager.merge(electoralBoardEntity);
			}
		} else {
			entityManager.merge(electoralBoardEntity);
		}

		// Workflow logs.
		final List<WorkflowLog> workflowLogEntities = Arrays.asList(objectMapper.readValue(databaseDump.workflowLogs().elements(), WorkflowLog[].class));
		workflowLogEntities.forEach(workflowLogEntity -> entityManager.merge(workflowLogEntity));
	}

	private void importStates(final DatabaseDump databaseDump) throws IOException {
		// Verification card set states.
		final List<VerificationCardSetStateEntity> verificationCardSetStateEntities = Arrays.asList(
				objectMapper.readValue(databaseDump.verificationCardSetStates().elements(), VerificationCardSetStateEntity[].class));
		verificationCardSetStateEntities.forEach(verificationCardSetStateEntity -> {
			// Get the state from the database to check if it is after the dump state.
			final Optional<VerificationCardSetStateEntity> databaseState = verificationCardSetStateRepository.findById(
					verificationCardSetStateEntity.getVerificationCardSetStateId());
			if (databaseState.isPresent()) {
				final Status dumpStatus = Status.valueOf(verificationCardSetStateEntity.getStatus());
				final Status databaseStatus = Status.valueOf(databaseState.get().getStatus());
				if (dumpStatus.isAfter(databaseStatus)) {
					entityManager.merge(verificationCardSetStateEntity);
				}
			} else {
				entityManager.merge(verificationCardSetStateEntity);
			}
		});

		// Ballot box states.
		final List<BallotBoxStateEntity> ballotBoxStateEntities = Arrays.asList(
				objectMapper.readValue(databaseDump.ballotBoxStates().elements(), BallotBoxStateEntity[].class));
		ballotBoxStateEntities.forEach(ballotBoxStateEntity -> {
			// Get the state from the database to check if it is after the dump state.
			final Optional<BallotBoxStateEntity> databaseState = ballotBoxStateRepository.findById(ballotBoxStateEntity.getBallotBoxStateId());
			if (databaseState.isPresent()) {
				final BallotBoxStatus dumpStatus = BallotBoxStatus.valueOf(ballotBoxStateEntity.getStatus());
				final BallotBoxStatus databaseStatus = BallotBoxStatus.valueOf(databaseState.get().getStatus());
				if (dumpStatus.isAfter(databaseStatus)) {
					entityManager.merge(ballotBoxStateEntity);
				}
			} else {
				entityManager.merge(ballotBoxStateEntity);
			}
		});
	}

	private DatabaseDump readDatabaseDump(final Path databaseDumpPath) throws IOException {
		final byte[] dump;
		try {
			dump = readAllBytes(databaseDumpPath);
			return objectMapper.readValue(dump, DatabaseDump.class);
		} catch (final IOException e) {
			throw new IOException("Error reading import file ", e);
		}
	}

	private record DatabaseDump(ImmutableByteArray electionEvent, ImmutableByteArray verificationCardSets, ImmutableByteArray ballotBoxStates,
								ImmutableByteArray verificationCardSetStates, ImmutableByteArray electoralBoard, ImmutableByteArray workflowLogs) {
	}
}
