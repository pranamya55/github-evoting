/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.validations.GracePeriodValidation;

@Service
public class BallotBoxService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BallotBoxService.class);

	private final ObjectMapper objectMapper;
	private final BallotBoxRepository ballotBoxRepository;
	private final ElectionEventService electionEventService;
	private final VerificationCardSetService verificationCardSetService;

	public BallotBoxService(
			final ObjectMapper objectMapper,
			final BallotBoxRepository ballotBoxRepository,
			final ElectionEventService electionEventService,
			final VerificationCardSetService verificationCardSetService) {
		this.objectMapper = objectMapper;
		this.ballotBoxRepository = ballotBoxRepository;
		this.electionEventService = electionEventService;
		this.verificationCardSetService = verificationCardSetService;
	}

	@VisibleForTesting
	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public BallotBoxEntity save(final String ballotBoxId, final String verificationCardSetId, final LocalDateTime ballotBoxStartTime,
			final LocalDateTime ballotBoxFinishTime, final boolean testBallotBox, final int numberOfEligibleVoters, final int gracePeriod,
			final PrimesMappingTable primesMappingTable) {
		validateUUID(ballotBoxId);
		validateUUID(verificationCardSetId);
		checkNotNull(ballotBoxStartTime);
		checkNotNull(ballotBoxFinishTime);
		checkArgument(ballotBoxStartTime.isBefore(ballotBoxFinishTime) || ballotBoxStartTime.equals(ballotBoxFinishTime),
				"Start time must be before finish time.");
		checkArgument(numberOfEligibleVoters > 0);
		GracePeriodValidation.validate(gracePeriod);
		checkNotNull(primesMappingTable);

		final ImmutableByteArray serializedPrimesMappingTable = serializePrimesMappingTable(primesMappingTable);

		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSetEntity(verificationCardSetId);
		final BallotBoxEntity ballotBoxEntity = new BallotBoxEntity.Builder()
				.setBallotBoxId(ballotBoxId)
				.setVerificationCardSetEntity(verificationCardSetEntity)
				.setBallotBoxStartTime(ballotBoxStartTime)
				.setBallotBoxFinishTime(ballotBoxFinishTime)
				.setTestBallotBox(testBallotBox)
				.setGracePeriod(gracePeriod)
				.setPrimesMappingTable(serializedPrimesMappingTable)
				.build();

		return ballotBoxRepository.save(ballotBoxEntity);
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void saveAllFromContexts(final ImmutableList<VerificationCardSetContext> verificationCardSetContexts) {
		checkNotNull(verificationCardSetContexts);

		final ImmutableList<BallotBoxEntity> ballotBoxEntities = verificationCardSetContexts.stream()
				.map(verificationCardSetContext -> {
					final String verificationCardSetId = verificationCardSetContext.getVerificationCardSetId();
					final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSetEntity(
							verificationCardSetId);

					final ImmutableByteArray serializedPrimesMappingTable = serializePrimesMappingTable(
							verificationCardSetContext.getPrimesMappingTable());

					LOGGER.info(
							"Successfully created ballot box. [verificationCardSetId: {}, ballotBoxId: {}, description: {}, isTest: {}, startTime: {}, finishTime: {}]",
							verificationCardSetId,
							verificationCardSetContext.getBallotBoxId(),
							verificationCardSetContext.getVerificationCardSetDescription(),
							verificationCardSetContext.isTestBallotBox(),
							LocalDateTimeUtils.format(verificationCardSetContext.getBallotBoxStartTime()),
							LocalDateTimeUtils.format(verificationCardSetContext.getBallotBoxFinishTime()));

					return new BallotBoxEntity.Builder()
							.setBallotBoxId(verificationCardSetContext.getBallotBoxId())
							.setBallotBoxStartTime(verificationCardSetContext.getBallotBoxStartTime())
							.setBallotBoxFinishTime(verificationCardSetContext.getBallotBoxFinishTime())
							.setTestBallotBox(verificationCardSetContext.isTestBallotBox())
							.setGracePeriod(verificationCardSetContext.getGracePeriod())
							.setPrimesMappingTable(serializedPrimesMappingTable)
							.setVerificationCardSetEntity(verificationCardSetEntity)
							.build();
				}).collect(toImmutableList());

		ballotBoxRepository.saveAll(ballotBoxEntities);
	}

	public BallotBoxEntity getBallotBoxByBallotBoxId(final String ballotBoxId) {
		validateUUID(ballotBoxId);

		return ballotBoxRepository.findById(ballotBoxId)
				.orElseThrow(() -> new IllegalStateException(String.format("Ballot box not found. [ballotBoxId: %s]", ballotBoxId)));
	}

	public BallotBoxEntity getBallotBoxByVerificationCardSetId(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		return ballotBoxRepository.findByVerificationCardSetId(verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Ballot box not found. [verificationCardSetId: %s]", verificationCardSetId)));
	}

	public boolean isMixed(final String ballotBoxId) {
		validateUUID(ballotBoxId);

		final BallotBoxEntity ballotBoxEntity = getBallotBoxByBallotBoxId(ballotBoxId);
		return ballotBoxEntity.isMixed();
	}

	public BallotBoxEntity setMixed(final String ballotBoxId) {
		validateUUID(ballotBoxId);

		final BallotBoxEntity ballotBoxEntity = getBallotBoxByBallotBoxId(ballotBoxId);

		checkState(!ballotBoxEntity.isMixed(), "The ballot box cannot be set to mixed because it is already mixed. [ballotBoxId: %s]", ballotBoxId);

		ballotBoxEntity.setMixed();

		return ballotBoxRepository.save(ballotBoxEntity);
	}

	@Transactional // Required due to the lazy loading of entities.
	public PrimesMappingTable getPrimesMappingTableByBallotBoxId(final String ballotBoxId) {
		validateUUID(ballotBoxId);

		final BallotBoxEntity ballotBoxEntity = getBallotBoxByBallotBoxId(ballotBoxId);
		final String electionEventId = ballotBoxEntity.getVerificationCardSetEntity().getElectionEventEntity().getElectionEventId();

		return deserializePrimesMappingTable(electionEventId, ballotBoxEntity.getPrimesMappingTable());
	}

	@Transactional // Required due to the lazy loading of entities.
	public PrimesMappingTable getPrimesMappingTableByVerificationCardSetId(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		final BallotBoxEntity ballotBoxEntity = getBallotBoxByVerificationCardSetId(verificationCardSetId);
		final String electionEventId = ballotBoxEntity.getVerificationCardSetEntity().getElectionEventEntity().getElectionEventId();

		return deserializePrimesMappingTable(electionEventId, ballotBoxEntity.getPrimesMappingTable());
	}

	private ImmutableByteArray serializePrimesMappingTable(final PrimesMappingTable primesMappingTable) {
		final ImmutableByteArray serializedPrimesMappingTable;
		try {
			serializedPrimesMappingTable = new ImmutableByteArray(objectMapper.writeValueAsBytes(primesMappingTable));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize the primes mapping table.", e);
		}
		return serializedPrimesMappingTable;
	}

	private PrimesMappingTable deserializePrimesMappingTable(final String electionEventId, final ImmutableByteArray primesMappingTableBytes) {
		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		final PrimesMappingTable primesMappingTable;
		try {
			primesMappingTable = objectMapper.reader()
					.withAttribute("group", encryptionGroup)
					.readValue(primesMappingTableBytes.elements(), PrimesMappingTable.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize the primes mapping table. [electionEventId: %s]", electionEventId),
					e);
		}

		return primesMappingTable;
	}
}
