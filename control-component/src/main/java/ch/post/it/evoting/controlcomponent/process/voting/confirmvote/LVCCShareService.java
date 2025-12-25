/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.process.BallotBoxEntity;
import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote.CreateLVCCShareOutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.LongVoteCastReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@Service
public class LVCCShareService {
	private static final Logger LOGGER = LoggerFactory.getLogger(LVCCShareService.class);

	private static final String GROUP = "group";

	private final LVCCShareRepository lvccShareRepository;
	private final VerificationCardService verificationCardService;
	private final ElectionEventService electionEventService;
	private final ObjectMapper objectMapper;
	private final BallotBoxService ballotBoxService;
	private final ElectionEventContextService electionEventContextService;

	public LVCCShareService(
			final LVCCShareRepository lvccShareRepository,
			final VerificationCardService verificationCardService,
			final ElectionEventService electionEventService,
			final ObjectMapper objectMapper,
			final BallotBoxService ballotBoxService,
			final ElectionEventContextService electionEventContextService) {
		this.lvccShareRepository = lvccShareRepository;
		this.verificationCardService = verificationCardService;
		this.electionEventService = electionEventService;
		this.objectMapper = objectMapper;
		this.ballotBoxService = ballotBoxService;
		this.electionEventContextService = electionEventContextService;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final ConfirmationKey confirmationKey, final CreateLVCCShareOutput createLVCCShareOutput) {
		checkNotNull(confirmationKey);
		checkNotNull(createLVCCShareOutput);

		final ContextIds contextIds = confirmationKey.contextIds();
		final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntity(contextIds.verificationCardId());

		final ImmutableByteArray longVoteCastReturnCodeShareSerialised;
		try {
			longVoteCastReturnCodeShareSerialised = new ImmutableByteArray(
					objectMapper.writeValueAsBytes(createLVCCShareOutput.longVoteCastReturnCodeShare()));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize long Vote Cast Return Code Share.", e);
		}

		final BigInteger confirmationKeySerialised = confirmationKey.element().getValue();

		final LVCCShareEntity lvccShareEntity = new LVCCShareEntity(verificationCardEntity,
				longVoteCastReturnCodeShareSerialised, createLVCCShareOutput.hashedLongVoteCastReturnCodeShare(),
				confirmationKeySerialised.toString(), createLVCCShareOutput.confirmationAttemptId());

		lvccShareRepository.save(lvccShareEntity);
		LOGGER.info("Saved the Long Vote Cast Return Codes Share. [contextIds: {}]", contextIds);
	}

	/**
	 * Gets the long Vote Cast Return Code Share corresponding to the given confirmation key.
	 * <p>
	 * Handles the scenario where a control component may store multiple entries for the same verification card id and confirmation key, but with
	 * different confirmation attempts. By design, the same confirmation key always results in the same long Vote Cast Return Code Share for a
	 * specific verification card id (the method sanity checks this condition). If there are multiple entries, the first element of the list is
	 * returned.
	 *
	 * @param confirmationKey the confirmation key for which to get the long Vote Cast Return Code Share. Must be non-null.
	 * @param nodeId          the corresponding node id. Must be a known node id.
	 * @return the long Vote Cast Return Code share.
	 * @throws NullPointerException     if {@code confirmationKey} is null.
	 * @throws IllegalArgumentException if {@code nodeId} is not part of {@link ControlComponentNode#ids()}.
	 * @throws IllegalStateException    if no long Vote Cast Return Code Share is found for the given confirmation key.
	 */
	public LongVoteCastReturnCodeShare getLongVoteCastReturnCodeShare(final ConfirmationKey confirmationKey, final int nodeId) {
		checkNotNull(confirmationKey);
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);

		final ContextIds contextIds = confirmationKey.contextIds();
		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		final LVCCShareEntity lvccShareEntity = load(confirmationKey);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		final GqElement longVoteCastReturnCodeShare = deserializeLongVoteCastReturnCodeShare(
				lvccShareEntity.getLongVoteCastReturnCodeShare(), contextIds, encryptionGroup);

		return new LongVoteCastReturnCodeShare(electionEventId, verificationCardSetId, verificationCardId, nodeId, longVoteCastReturnCodeShare);
	}

	/**
	 * Loads the long Vote Cast Return Code Share corresponding to the given confirmation key.
	 *
	 * @param confirmationKey the confirmation key for which to get the long Vote Cast Return Code Share. Must be non-null.
	 * @param nodeId          the corresponding node id. Must be a known node id.
	 * @return the hashed long Vote Cast Return Code share.
	 * @throws NullPointerException     if {@code confirmationKey} is null.
	 * @throws IllegalArgumentException if {@code nodeId} is not part of {@link ControlComponentNode#ids()}.
	 * @throws IllegalStateException    if no hashed long Vote Cast Return Code Share is found for the given confirmation key.
	 */
	public String getHashedLongVoteCastReturnCodeShare(final ConfirmationKey confirmationKey, final int nodeId) {
		checkNotNull(confirmationKey);
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);

		final LVCCShareEntity lvccShareEntity = load(confirmationKey);

		return lvccShareEntity.getHashedLongVoteCastReturnCodeShare();
	}

	public boolean exists(final String verificationCardId, final ConfirmationKey confirmationKey) {
		validateUUID(verificationCardId);
		checkNotNull(confirmationKey);

		return lvccShareRepository.existsByVerificationCardIdAndConfirmationKey(verificationCardId,
				confirmationKey.element().getValue().toString());
	}

	@Transactional // Required due to the lazy loading of entities.
	public void validateConfirmationIsAllowed(final String electionEventId, final String verificationCardId, final Supplier<LocalDateTime> now) {
		validateUUID(electionEventId);
		validateUUID(verificationCardId);
		checkNotNull(now);

		final LocalDateTime electionStartTime = electionEventContextService.getElectionEventStartTime(electionEventId);
		final LocalDateTime electionEndTime = electionEventContextService.getElectionEventFinishTime(electionEventId);
		final LocalDateTime currentTime = now.get();

		final String verificationCardSetId = verificationCardService.getVerificationCardEntity(verificationCardId).getVerificationCardSetEntity()
				.getVerificationCardSetId();
		final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByVerificationCardSetId(verificationCardSetId);
		final boolean afterStartTime = currentTime.isAfter(electionStartTime) || currentTime.isEqual(electionStartTime);
		final boolean beforeEndTime = currentTime.isBefore(electionEndTime.plusSeconds(ballotBoxEntity.getGracePeriod())) || currentTime
				.isEqual(electionEndTime.plusSeconds(ballotBoxEntity.getGracePeriod()));

		checkState(afterStartTime && beforeEndTime,
				"Impossible to confirm vote before or after the dedicated time. [electionEventId: %s, ballotBoxId: %s, verificationCardId: %s, "
						+ "startTime: %s, finishTime: %s, gracePeriod: %s]",
				electionEventId, ballotBoxEntity.getBallotBoxId(), verificationCardId, electionStartTime, electionEndTime,
				ballotBoxEntity.getGracePeriod());
		checkState(!ballotBoxEntity.isMixed(),
				"Impossible to confirm vote in an already mixed ballot box. [electionEventId: %s, ballotBoxId: %s, verificationCardId: %s]",
				electionEventId, ballotBoxEntity.getBallotBoxId(), verificationCardId);
	}

	/**
	 * Gets the long Vote Cast Return Code Share entity corresponding to the given confirmation key.
	 * <p>
	 * Handles the scenario where a control component may store multiple entries for the same verification card id and confirmation key, but with
	 * different confirmation attempts. By design, the same confirmation key always results in the same long Vote Cast Return Code Share for a
	 * specific verification card id (the method sanity checks this condition). If there are multiple entries, the first element of the list is
	 * returned.
	 *
	 * @param confirmationKey the confirmation key for which to get the long Vote Cast Return Code Share entity. Must be non-null.
	 * @return the long Vote Cast Return Code share entity.
	 * @throws NullPointerException  if {@code confirmationKey} is null.
	 * @throws IllegalStateException if no long Vote Cast Return Code Share entity is found for the given confirmation key.
	 */
	private LVCCShareEntity load(final ConfirmationKey confirmationKey) {
		final String verificationCardId = confirmationKey.contextIds().verificationCardId();

		final ImmutableList<LVCCShareEntity> longVoteCastReturnCodesShares = ImmutableList.from(
				lvccShareRepository.findAllByVerificationCardIdAndConfirmationKey(verificationCardId,
						confirmationKey.element().getValue().toString()));

		if (longVoteCastReturnCodesShares.isEmpty()) {
			throw new IllegalStateException(
					String.format("Long Vote Cast Return Codes Share not found. [verificationCardId: %s]", verificationCardId));
		}

		allEqual(longVoteCastReturnCodesShares.stream(), LVCCShareEntity::getLongVoteCastReturnCodeShare);

		return longVoteCastReturnCodesShares.get(0);
	}

	private GqElement deserializeLongVoteCastReturnCodeShare(final ImmutableByteArray longVoteCastReturnCodeShareBytes, final ContextIds contextIds,
			final GqGroup encryptionGroup) {
		final GqElement longVoteCastReturnCodeShare;
		try {
			longVoteCastReturnCodeShare = objectMapper.reader().withAttribute(GROUP, encryptionGroup)
					.readValue(longVoteCastReturnCodeShareBytes.elements(), GqElement.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize long vote cast return code share. [contextIds: %s]", contextIds), e);
		}
		return longVoteCastReturnCodeShare;
	}

}
