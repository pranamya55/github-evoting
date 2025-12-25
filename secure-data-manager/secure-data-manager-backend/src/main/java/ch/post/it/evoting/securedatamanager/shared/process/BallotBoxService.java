/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.BallotBoxStatus;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Service to operate with ballot boxes.
 */
@Service
public class BallotBoxService {

	private final BallotBoxService self;
	private final BallotBoxRepository ballotBoxRepository;
	private final BallotBoxStateRepository ballotBoxStateRepository;

	public BallotBoxService(
			@Lazy
			final BallotBoxService self,
			final BallotBoxRepository ballotBoxRepository,
			final BallotBoxStateRepository ballotBoxStateRepository) {
		this.self = self;
		this.ballotBoxRepository = ballotBoxRepository;
		this.ballotBoxStateRepository = ballotBoxStateRepository;
	}

	/**
	 * Checks that the ballot box has status {@link BallotBoxStatus#DOWNLOADED}.
	 */
	public boolean isDownloaded(final String ballotBoxId) {
		validateUUID(ballotBoxId);

		return hasStatus(ballotBoxId, BallotBoxStatus.DOWNLOADED);
	}

	/**
	 * Checks that the ballot box has {@code expectedStatus}
	 *
	 * @throws IllegalStateException if the ballot box cannot be found in the ballot box repository or the subsequent retrieved JSON can't be
	 *                               correctly parsed.
	 */
	public boolean hasStatus(final String ballotBoxId, final BallotBoxStatus expectedStatus) {
		validateUUID(ballotBoxId);
		checkNotNull(expectedStatus);

		final BallotBoxStatus actualStatus = getBallotBoxStatus(ballotBoxId);

		return expectedStatus.equals(actualStatus);
	}

	/**
	 * Gets the status of a ballot box.
	 *
	 * @param ballotBoxId the ballot box id to get the status.
	 * @return the status of the ballot box.
	 */
	public BallotBoxStatus getBallotBoxStatus(final String ballotBoxId) {
		validateUUID(ballotBoxId);

		final BallotBoxStateEntity ballotBoxStateEntity = getBallotBoxStateEntity(ballotBoxId);
		return BallotBoxStatus.valueOf(ballotBoxStateEntity.getStatus());
	}

	/**
	 * Returns the grace period of the ballot box identified by the given ballotBoxId.
	 *
	 * @param ballotBoxId identifies the ballot box where to search. Must be non-null and a valid UUID.
	 * @return the grace period.
	 * @throws FailedValidationException if the given ballot box is null or not a valid UUID.
	 * @throws IllegalArgumentException  if the ballot box is not found.
	 */
	public int getGracePeriod(final String ballotBoxId) {
		validateUUID(ballotBoxId);
		return getBallotBox(ballotBoxId).gracePeriod();
	}

	/**
	 * Indicates if the ballot box corresponding to the given ballot box id is a test ballot box.
	 *
	 * @param ballotBoxId the ballot box id. Must be non-null and a valid UUID.
	 * @return true if the corresponding ballot box is a test ballot box, false otherwise.
	 * @throws FailedValidationException if the given ballot box is null or not a valid UUID.
	 * @throws IllegalArgumentException  if the found ballot box is not found.
	 */
	public boolean isTestBallotBox(final String ballotBoxId) {
		validateUUID(ballotBoxId);
		return getBallotBox(ballotBoxId).test();
	}

	/**
	 * Returns the date until which the ballot box identified by the given ballotBoxId is valid.
	 *
	 * @param ballotBoxId identifies the ballot box where to search. Must be non-null and a valid UUID.
	 * @return the ballot box finish date.
	 * @throws NullPointerException      if {@code ballotBoxId} is null.
	 * @throws FailedValidationException if {@code ballotBoxId} is not a valid UUID.
	 */
	public LocalDateTime getFinishTime(final String ballotBoxId) {
		validateUUID(ballotBoxId);
		return getBallotBox(ballotBoxId).finishTime();
	}

	/**
	 * Checks if the given ballot box has the {@link BallotBoxStatus#DECRYPTED} status.
	 *
	 * @param ballotBoxId the ballot box id to check.
	 * @return {@code true} if the ballot box has the decrypted status, {@code false} otherwise.
	 * @throws NullPointerException      if {@code ballotBoxId} is null.
	 * @throws FailedValidationException if {@code ballotBoxId} is invalid.
	 */
	public boolean isDecrypted(final String ballotBoxId) {
		validateUUID(ballotBoxId);

		return hasStatus(ballotBoxId, BallotBoxStatus.DECRYPTED);
	}

	/**
	 * Sets the status of the given ballot box to {@link BallotBoxStatus#DECRYPTED}.
	 *
	 * @param ballotBoxId the ballot box id to set the status.
	 * @throws NullPointerException      if {@code ballotBoxId} is null.
	 * @throws FailedValidationException if {@code ballotBoxId} is invalid.
	 */
	public void setDecrypted(final String ballotBoxId) {
		validateUUID(ballotBoxId);

		self.updateStatus(ballotBoxId, BallotBoxStatus.DECRYPTED);
	}

	/**
	 * Updates the status of a ballot box with {@code newStatus}.
	 *
	 * @param ballotBoxId the ballot box if to update the status.
	 * @param newStatus   the new status of the ballot box.
	 * @return the new status after update.
	 */
	@Transactional
	@CacheEvict(value = "ballotBoxes", allEntries = true)
	public BallotBoxStatus updateStatus(final String ballotBoxId, final BallotBoxStatus newStatus) {
		validateUUID(ballotBoxId);
		checkNotNull(newStatus);

		final BallotBoxStateEntity ballotBoxStateEntity = getBallotBoxStateEntity(ballotBoxId);
		ballotBoxStateEntity.setStatus(newStatus.name());
		ballotBoxStateRepository.save(ballotBoxStateEntity);

		return newStatus;
	}

	@Cacheable(value = "ballotBoxes", sync = true)
	public ImmutableList<BallotBox> getBallotBoxes(final String electionEventId) {
		validateUUID(electionEventId);

		final List<BallotBoxEntity> ballotBoxEntities = ballotBoxRepository.findByElectionEventId(electionEventId);

		return ballotBoxEntities.stream()
				.map(ballotBoxEntity -> {
					final BallotBoxStateEntity ballotBoxStateEntity = getBallotBoxStateEntity(ballotBoxEntity.getBallotBoxId());
					return getBallotBox(ballotBoxEntity, ballotBoxStateEntity);
				}).collect(toImmutableList());
	}

	private BallotBoxStateEntity getBallotBoxStateEntity(final String ballotBoxId) {
		return ballotBoxStateRepository.findById(ballotBoxId)
				.orElseThrow(() -> new IllegalStateException("The ballot box state set with the given id does not exist."));
	}

	private BallotBox getBallotBox(final BallotBoxEntity ballotBoxEntity, final BallotBoxStateEntity ballotBoxStateEntity) {
		return new BallotBox.BallotBoxBuilder()
				.setId(ballotBoxEntity.getBallotBoxId())
				.setDescription(ballotBoxEntity.getDescription())
				.setStartTime(ballotBoxEntity.getStartTime())
				.setFinishTime(ballotBoxEntity.getFinishTime())
				.setTest(ballotBoxEntity.isTest())
				.setGracePeriod(ballotBoxEntity.getGracePeriod())
				.setStatus(ballotBoxStateEntity.getStatus())
				.build();
	}

	/**
	 * Retrieves all ballot boxes associated to the election event with {@code electionEventId}.
	 *
	 * @param electionEventId the election event id for which to retrieve the ballot boxes. Must be a valid UUID.
	 * @return all ballot box ids.
	 */
	public ImmutableList<String> getBallotBoxIds(final String electionEventId) {
		validateUUID(electionEventId);
		return ImmutableList.from(ballotBoxRepository.findAllIdsByElectionEventId(electionEventId));
	}

	/**
	 * Retrieves all ballot boxes ids associated to the election event with {@code electionEventId} and with a status in {@code ballotBoxStatuses}.
	 *
	 * @param electionEventId   the election event id for which to retrieve the ballot boxes. Must be a valid UUID.
	 * @param ballotBoxStatuses the statuses of the ballot boxes to retrieve.
	 * @return the ballot box ids.
	 */
	public ImmutableList<String> getBallotBoxesIdByStatus(final String electionEventId, final ImmutableList<BallotBoxStatus> ballotBoxStatuses) {
		validateUUID(electionEventId);
		checkNotNull(ballotBoxStatuses);

		final ImmutableList<String> ballotBoxStatusesNames = ballotBoxStatuses.stream()
				.map(BallotBoxStatus::name)
				.collect(toImmutableList());

		final ImmutableList<BallotBox> ballotBoxes = self.getBallotBoxes(electionEventId);

		return ballotBoxes.stream()
				.filter(ballotBox -> ballotBoxStatusesNames.contains(ballotBox.status()))
				.map(BallotBox::id)
				.collect(toImmutableList());
	}

	/**
	 * Retrieves the ballot box with {@code ballotBoxId}.
	 *
	 * @param ballotBoxId the ballot box id to retrieve.
	 * @return the ballot box.
	 */
	public BallotBox getBallotBox(final String ballotBoxId) {
		validateUUID(ballotBoxId);

		final BallotBoxEntity ballotBoxEntity = ballotBoxRepository.findById(ballotBoxId)
				.orElseThrow(() -> new IllegalStateException("The ballot box with the given id does not exist."));
		final BallotBoxStateEntity ballotBoxStateEntity = getBallotBoxStateEntity(ballotBoxId);

		return getBallotBox(ballotBoxEntity, ballotBoxStateEntity);
	}

}
