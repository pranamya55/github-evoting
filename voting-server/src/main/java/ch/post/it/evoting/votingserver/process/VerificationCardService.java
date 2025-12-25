/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.domain.Constants.MAX_AUTHENTICATION_ATTEMPTS;
import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validatePartialUUID;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.votingserver.process.VerificationCardStateValidator.validateVerificationCardState;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.InvalidVerificationCardStateException;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.UsedVotingCardDto;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.VerificationCardNotFoundException;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.VotingCardDto;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.VotingCardSearchDto;

@Service
public class VerificationCardService {

	public static final int MIN_PARTIAL_UUID_LENGTH = 3;
	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardService.class);
	private static final String UPDATED_STATE_MESSAGE = "Updated state. [verificationCardId: {}, state: {}]";
	private final VerificationCardRepository verificationCardRepository;
	private final VerificationCardStateService verificationCardStateService;
	private final BallotBoxService ballotBoxService;

	@PersistenceContext
	private EntityManager entityManager;

	public VerificationCardService(
			final VerificationCardRepository verificationCardRepository,
			final VerificationCardStateService verificationCardStateService,
			final BallotBoxService ballotBoxService) {
		this.verificationCardRepository = verificationCardRepository;
		this.verificationCardStateService = verificationCardStateService;
		this.ballotBoxService = ballotBoxService;
	}

	@Transactional
	public void saveVerificationCards(final ImmutableList<VerificationCardEntity> verificationCardEntities) {
		checkNotNull(verificationCardEntities);

		// Save cards.
		verificationCardRepository.saveAll(verificationCardEntities);

		// Save associated states.
		final ImmutableList<VerificationCardStateEntity> verificationCardStateEntities = verificationCardEntities.stream()
				.map(VerificationCardEntity::getVerificationCardStateEntity)
				.collect(toImmutableList());
		verificationCardStateService.saveVerificationCardStates(verificationCardStateEntities);

		LOGGER.info("Saved verification cards and states. [amount: {}]", verificationCardEntities.size());
	}

	public VerificationCardEntity getVerificationCardEntity(final String verificationCardId) {
		validateUUID(verificationCardId);

		return verificationCardRepository.findById(verificationCardId)
				.orElseThrow(
						() -> new IllegalStateException(String.format("Verification card not found. [verificationCardId: %s]", verificationCardId)));
	}

	public VerificationCardEntity getVerificationCardEntityByCredentialId(final String credentialId) {
		validateUUID(credentialId);

		return verificationCardRepository.findByCredentialId(credentialId)
				.orElseThrow(
						() -> new IllegalStateException(String.format("Verification card not found. [credentialId: %s]", credentialId)));
	}

	/**
	 * Manually refresh the entity in the entityManager context. This is typically done in the following scenario:
	 * <p>
	 *   <ol>
	 *     <li>Fetch an entity.</li>
	 *     <li>Another new transaction (REQUIRES_NEW) modifies the entity.</li>
	 *     <li>The entity is used from the first fetch</li>
	 *   </ol>
	 * <p>
	 * Without manually refreshing the entityManager context, the step 3. would not see the changes made by step 2.
	 *
	 * @param credentialId the credentialId of the verification card. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code credentialId} is null.
	 * @throws FailedValidationException if {@code credentialId} is not a valid UUID.
	 */
	@Transactional
	public void refreshVerificationCardStateEntity(final String credentialId) {
		validateUUID(credentialId);
		entityManager.refresh(getVerificationCardEntityByCredentialId(credentialId).getVerificationCardStateEntity());
	}

	@Transactional // Required due to the lazy loading of entities.
	public ImmutableList<String> getShortChoiceReturnCodes(final String credentialId) {
		validateUUID(credentialId);

		return getVerificationCardEntityByCredentialId(credentialId).getVerificationCardStateEntity().getShortChoiceReturnCodes();
	}

	@Transactional // Required due to the lazy loading of entities.
	public String getShortVoteCastReturnCode(final String credentialId) {
		validateUUID(credentialId);

		return getVerificationCardEntityByCredentialId(credentialId).getVerificationCardStateEntity().getShortVoteCastReturnCode();
	}

	@Transactional // Required due to the lazy loading of entities.
	public VerificationCardState getVerificationCardState(final String credentialId) {
		validateUUID(credentialId);

		return getVerificationCardEntityByCredentialId(credentialId).getVerificationCardStateEntity().getState();
	}

	@Transactional // Required due to the lazy loading of entities.
	public int getAuthenticationAttempts(final String credentialId) {
		validateUUID(credentialId);

		return getVerificationCardEntityByCredentialId(credentialId).getVerificationCardStateEntity().getAuthenticationAttempts();
	}

	@Transactional
	public void incrementAuthenticationAttempts(final String credentialId) {
		validateUUID(credentialId);

		final String verificationCardId = getVerificationCardIdByCredentialId(credentialId);
		final int authenticationAttempts = verificationCardStateService.incrementAuthenticationAttempts(verificationCardId);

		// Check if the attempts have been exceeded.
		if (authenticationAttempts >= MAX_AUTHENTICATION_ATTEMPTS) {
			final VerificationCardEntity verificationCardEntity = getVerificationCardEntityByCredentialId(credentialId);
			final VerificationCardStateEntity verificationCardStateEntity = verificationCardEntity.getVerificationCardStateEntity();
			final VerificationCardState verificationCardState = verificationCardStateEntity.getState();

			// In case the card is already confirmed, don't update its state further.
			// CONFIRMED is a terminal state and must not be updated. CONFIRMING could transition to CONFIRMED and must not be updated as well.
			if (!VerificationCardState.CONFIRMED.equals(verificationCardState) && !VerificationCardState.CONFIRMING.equals(verificationCardState)) {
				verificationCardStateEntity.updateState(VerificationCardState.AUTHENTICATION_ATTEMPTS_EXCEEDED);
				verificationCardStateService.saveVerificationCardState(verificationCardStateEntity);
			}
		}
	}

	@Transactional // Required due to the lazy loading of entities.
	public long getLastTimeStep(final String credentialId) {
		validateUUID(credentialId);

		return getVerificationCardEntityByCredentialId(credentialId).getVerificationCardStateEntity().getLastSuccessfulAuthenticationTimeStep();
	}

	@Transactional // Required due to the lazy loading of entities.
	public ImmutableList<String> getSuccessfulAuthenticationChallenges(final String credentialId) {
		validateUUID(credentialId);

		return getVerificationCardEntityByCredentialId(credentialId).getVerificationCardStateEntity().getSuccessfulAuthenticationAttempts()
				.successfulChallenges();
	}

	public void setLastTimeStepAndSuccessfulAuthenticationChallenge(final String credentialId, final long timeStepT1,
			final String authenticationChallenge) {
		validateUUID(credentialId);
		checkNotNull(authenticationChallenge);

		final String verificationCard = getVerificationCardIdByCredentialId(credentialId);
		verificationCardStateService.addToListOfSuccessfulAuthenticationChallenges(verificationCard, timeStepT1, authenticationChallenge);
	}

	/**
	 * Gets the confirmation attempts for the given verification card id. It represents the next confirmation attempt id.
	 *
	 * @param verificationCardId the id of the verification card. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code verificationCardId} is null.
	 * @throws FailedValidationException if {@code verificationCardId} is not a valid UUID.
	 */
	public int getNextConfirmationAttemptId(final String verificationCardId) {
		validateUUID(verificationCardId);

		return verificationCardStateService.getNextConfirmationAttemptId(verificationCardId);
	}

	/**
	 * Increments the confirmation attempts and updates the verification card state to :
	 * <li>{@link VerificationCardState#SENT} if there are still some confirmation attempts left.</li>
	 * <li>{@link VerificationCardState#CONFIRMATION_ATTEMPTS_EXCEEDED} otherwise.</li>
	 *
	 * @param verificationCardId the id of the verification card to update. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code verificationCardId} is null.
	 * @throws FailedValidationException if {@code verificationCardId} is not a valid UUID.
	 */
	@Transactional
	public int incrementConfirmationAttempts(final String verificationCardId) {
		validateUUID(verificationCardId);

		final int incrementedConfirmationAttempts = verificationCardStateService.incrementConfirmationAttempts(verificationCardId);

		final VerificationCardEntity verificationCardEntity = getVerificationCardEntity(verificationCardId);
		final VerificationCardStateEntity verificationCardStateEntity = verificationCardEntity.getVerificationCardStateEntity();

		// Check if the attempts have been exceeded.
		if (incrementedConfirmationAttempts >= MAX_CONFIRMATION_ATTEMPTS) {
			verificationCardStateEntity.updateState(VerificationCardState.CONFIRMATION_ATTEMPTS_EXCEEDED);
			LOGGER.info(UPDATED_STATE_MESSAGE, verificationCardId, VerificationCardState.CONFIRMATION_ATTEMPTS_EXCEEDED);
		} else {
			verificationCardStateEntity.updateState(VerificationCardState.SENT);
			LOGGER.info(UPDATED_STATE_MESSAGE, verificationCardId, VerificationCardState.SENT);
		}

		verificationCardStateService.saveVerificationCardState(verificationCardStateEntity);

		return incrementedConfirmationAttempts;
	}

	/**
	 * Saves the short Choice Return Codes and updates the verification card state to {@link VerificationCardState#SENT}.
	 *
	 * @param verificationCardId     the id of the verification card to update. Must be non-null and a valid UUID.
	 * @param shortChoiceReturnCodes the list of short Choice Return Codes to save. Must be non-null and not empty.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code verificationCardId} is not a valid UUID.
	 * @throws IllegalArgumentException  if {@code shortChoiceReturnCodes} is empty.
	 */
	@Transactional // Required due to the lazy loading of entities.
	public void saveSentState(final String verificationCardId, final ImmutableList<String> shortChoiceReturnCodes) {
		validateUUID(verificationCardId);
		checkNotNull(shortChoiceReturnCodes);

		checkArgument(!shortChoiceReturnCodes.isEmpty(), "The list of short Choice Return Codes must not be empty. [verificationCardId: %s]",
				verificationCardId);

		final VerificationCardEntity verificationCardEntity = getVerificationCardEntity(verificationCardId);
		final VerificationCardStateEntity verificationCardStateEntity = verificationCardEntity.getVerificationCardStateEntity();

		validateVerificationCardState(AuthenticationStep.SEND_VOTE, verificationCardStateEntity.getState());

		verificationCardStateEntity.setShortChoiceReturnCodes(shortChoiceReturnCodes);
		verificationCardStateEntity.updateState(VerificationCardState.SENT);

		verificationCardStateService.saveVerificationCardState(verificationCardStateEntity);
		LOGGER.info("Saved short Choice Return Codes and updated state. [verificationCardId: {}]", verificationCardId);
	}

	/**
	 * Updates the verification card state to {@link VerificationCardState#CONFIRMING}.
	 *
	 * @param verificationCardId the id of the verification card to update. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code verificationCardId} is null.
	 * @throws FailedValidationException if {@code verificationCardId} is not a valid UUID.
	 */
	@Transactional // Required due to the lazy loading of entities.
	public void saveConfirmingState(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardEntity verificationCardEntity = getVerificationCardEntity(verificationCardId);
		final VerificationCardStateEntity verificationCardStateEntity = verificationCardEntity.getVerificationCardStateEntity();

		validateVerificationCardState(AuthenticationStep.CONFIRM_VOTE, verificationCardStateEntity.getState());

		verificationCardStateEntity.updateState(VerificationCardState.CONFIRMING);

		verificationCardStateService.saveVerificationCardState(verificationCardStateEntity);
		LOGGER.info(UPDATED_STATE_MESSAGE, verificationCardId, VerificationCardState.CONFIRMING);
	}

	/**
	 * Saves the short Vote Cast Return Code and updates the verification card state to {@link VerificationCardState#CONFIRMED}.
	 *
	 * @param verificationCardId      the id of the verification card to update. Must be non-null and a valid UUID.
	 * @param shortVoteCastReturnCode the short Vote Cast Return code to save. Must be non-null.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code verificationCardId} is not a valid UUID.
	 */
	@Transactional // Required due to the lazy loading of entities.
	public void saveConfirmedState(final String verificationCardId, final String shortVoteCastReturnCode) {
		validateUUID(verificationCardId);
		checkNotNull(shortVoteCastReturnCode);

		final VerificationCardEntity verificationCardEntity = getVerificationCardEntity(verificationCardId);
		final VerificationCardStateEntity verificationCardStateEntity = verificationCardEntity.getVerificationCardStateEntity();

		validateVerificationCardState(AuthenticationStep.CONFIRM_VOTE, verificationCardStateEntity.getState());

		verificationCardStateEntity.setShortVoteCastReturnCode(shortVoteCastReturnCode);
		verificationCardStateEntity.updateState(VerificationCardState.CONFIRMED);

		verificationCardStateService.saveVerificationCardState(verificationCardStateEntity);
		LOGGER.info("Saved short Vote Cast Return Code and updated state. [verificationCardId: {}]", verificationCardId);
	}

	@Transactional // Required due to the lazy loading of entities.
	public VotingCardDto getVotingCard(final String votingCardId) {
		validateUUID(votingCardId);

		return verificationCardRepository.findByVotingCardId(votingCardId)
				.map(verificationCardEntity -> {
					final VerificationCardStateEntity verificationCardStateEntity = verificationCardEntity.getVerificationCardStateEntity();
					return new VotingCardDto(
							verificationCardEntity.getVoterAuthenticationData().electionEventId(),
							verificationCardEntity.getVerificationCardSetEntity().getVerificationCardSetId(),
							verificationCardEntity.getVerificationCardId(),
							verificationCardEntity.getVotingCardId(),
							verificationCardStateEntity.getState(),
							verificationCardStateEntity.getStateDate());
				}).orElseThrow(() -> new VerificationCardNotFoundException(
						String.format("Verification card not found. [votingCardIdSearched: %s]", votingCardId)));
	}

	@Transactional // Required due to the lazy loading of entities.
	public VotingCardSearchDto searchVotingCard(final String partialVotingCardId) {
		validatePartialUUID(partialVotingCardId, MIN_PARTIAL_UUID_LENGTH);

		final long matchingVerificationCards = verificationCardRepository.countAllByVotingCardIdStartsWith(partialVotingCardId);

		// This limit must be aligned with the number in the findTop5ByVotingCardIdStartsWithOrderByVotingCardIdAsc method.
		final long votingCardLimitNumber = 5;
		final ImmutableList<VotingCardDto> votingCards = verificationCardRepository.findTop5ByVotingCardIdStartsWithOrderByVotingCardIdAsc(
						partialVotingCardId).stream()
				.map(verificationCardEntity -> {
					final VerificationCardStateEntity verificationCardStateEntity = verificationCardEntity.getVerificationCardStateEntity();
					return new VotingCardDto(
							verificationCardEntity.getVoterAuthenticationData().electionEventId(),
							verificationCardEntity.getVerificationCardSetEntity().getVerificationCardSetId(),
							verificationCardEntity.getVerificationCardId(),
							verificationCardEntity.getVotingCardId(),
							verificationCardStateEntity.getState(),
							verificationCardStateEntity.getStateDate());
				})
				.collect(toImmutableList());

		return new VotingCardSearchDto(
				votingCards,
				new VotingCardSearchDto.Metadata(votingCardLimitNumber, matchingVerificationCards));
	}

	@Transactional
	public void blockVotingCard(final String votingCardId) {
		validateUUID(votingCardId);

		final VerificationCardEntity verificationCardEntity = verificationCardRepository.findByVotingCardId(votingCardId).orElseThrow(
				() -> new VerificationCardNotFoundException(String.format("Verification card not found. [votingCardIdSearched: %s]", votingCardId)));

		final VerificationCardStateEntity verificationCardStateEntity = verificationCardEntity.getVerificationCardStateEntity();
		final VerificationCardState state = verificationCardStateEntity.getState();

		if (VerificationCardState.BLOCKED.equals(state) ||
				VerificationCardState.CONFIRMED.equals(state) ||
				VerificationCardState.CONFIRMING.equals(state)) {
			throw new InvalidVerificationCardStateException(votingCardId, state);
		}

		verificationCardStateEntity.updateState(VerificationCardState.BLOCKED);

		verificationCardStateService.saveVerificationCardState(verificationCardStateEntity);
	}

	/**
	 * Lists all used voting cards for a given election event id and, if provided, since a date and time of usage.
	 *
	 * @param electionEventId   the election event id. Must be non-null and a valid UUID.
	 * @param usageDateTime the date and time of usage from which to list the used voting cards. Optional.
	 * @return The list of used voting cards for a given election event id and, if provided, from a given date and time.
	 * @throws NullPointerException      if {@code electionEventId} or {@code optionalFromDateTime} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 */
	public ImmutableList<UsedVotingCardDto> getUsedVotingCardsByElectionEventIdAndSinceUsageDateTime(final String electionEventId,
			final LocalDateTime usageDateTime) {
		validateUUID(electionEventId);

		return ImmutableList.from(verificationCardRepository.findAllUsedByElectionEventIdAndSinceUsageDateTime(electionEventId, usageDateTime));
	}

	@Transactional // Required due to the lazy loading of entities.
	public VerificationCardSetEntity getVerificationCardSetEntity(final String credentialId) {
		validateUUID(credentialId);

		return getVerificationCardEntityByCredentialId(credentialId).getVerificationCardSetEntity();
	}

	@Transactional // Required due to the lazy loading of entities.
	public PrimesMappingTable getPrimesMappingTable(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardSetEntity verificationCardSetEntity = getVerificationCardEntity(verificationCardId).getVerificationCardSetEntity();
		return ballotBoxService.getPrimesMappingTableByVerificationCardSetId(verificationCardSetEntity.getVerificationCardSetId());
	}

	private String getVerificationCardIdByCredentialId(final String credentialId) {
		validateUUID(credentialId);

		return verificationCardRepository.findByCredentialId(credentialId)
				.orElseThrow(
						() -> new VerificationCardNotFoundException(String.format("Verification card not found. [credentialId: %s]", credentialId)))
				.getVerificationCardId();
	}
}
