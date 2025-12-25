/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;

@Service
public class VerificationCardStateService {

	private final VerificationCardStateRepository verificationCardStateRepository;

	public VerificationCardStateService(final VerificationCardStateRepository verificationCardStateRepository) {
		this.verificationCardStateRepository = verificationCardStateRepository;
	}

	public void saveVerificationCardStates(final ImmutableList<VerificationCardStateEntity> verificationCardStateEntities) {
		checkNotNull(verificationCardStateEntities);

		verificationCardStateRepository.saveAll(verificationCardStateEntities);
	}

	public void saveVerificationCardState(final VerificationCardStateEntity verificationCardStateEntity) {
		checkNotNull(verificationCardStateEntity);

		verificationCardStateRepository.save(verificationCardStateEntity);
	}

	private VerificationCardStateEntity getVerificationCardStateEntity(final String verificationCardId) {
		return verificationCardStateRepository.findById(verificationCardId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Verification card state not found. [verificationCardId: %s]", verificationCardId)));
	}

	public int incrementAuthenticationAttempts(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardStateEntity(verificationCardId);
		final VerificationCardState verificationCardState = verificationCardStateEntity.getState();

		checkState(!verificationCardState.isInactive(),
				"The current state does not allow to increment authentication attempts. [verificationCardId: %s, verificationCardState: %s]",
				verificationCardId, verificationCardState);

		int authenticationAttempts = verificationCardStateEntity.getAuthenticationAttempts();
		verificationCardStateEntity.setAuthenticationAttempts(++authenticationAttempts);

		verificationCardStateRepository.save(verificationCardStateEntity);
		return verificationCardStateEntity.getAuthenticationAttempts();
	}

	/**
	 * Adds the authentication challenge to the list of successful authentication challenges for this verification card ID. To prevent bloating the
	 * list, we reinitialize the list if the current time step is larger than the time step of the last successful authentication challenge.
	 *
	 * @param verificationCardId      the verification card ID for which to add the authentication challenge. Must be a valid UUID.
	 * @param currentTimeStep         the time step associated with the authentication challenge.
	 * @param authenticationChallenge the authentication challenge to be added to the list. Must be non-null.
	 * @throws ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException if the verification card ID is not a valid UUID.
	 * @throws NullPointerException                                                             if the authentication challenge is null.
	 */
	public void addToListOfSuccessfulAuthenticationChallenges(final String verificationCardId, final long currentTimeStep,
			final String authenticationChallenge) {
		validateUUID(verificationCardId);
		checkNotNull(authenticationChallenge);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardStateEntity(verificationCardId);
		final long previousTimeStep = currentTimeStep - 1;
		final long lastSuccessfulAuthenticationTimeStep = verificationCardStateEntity.getLastSuccessfulAuthenticationTimeStep();
		if (lastSuccessfulAuthenticationTimeStep + 1 < previousTimeStep) { // Reinitialize the list
			verificationCardStateEntity.setLastSuccessfulAuthenticationTimeStep(currentTimeStep);
			verificationCardStateEntity.setSuccessfulAuthenticationAttempts(
					new SuccessfulAuthenticationAttempts(ImmutableList.of(authenticationChallenge)));
		} else { // Otherwise add challenge to existing list
			final SuccessfulAuthenticationAttempts successfulAuthenticationAttempts = verificationCardStateEntity.getSuccessfulAuthenticationAttempts();
			final ImmutableList<String> successfulChallenges = successfulAuthenticationAttempts.successfulChallenges();
			verificationCardStateEntity.setSuccessfulAuthenticationAttempts(
					new SuccessfulAuthenticationAttempts(successfulChallenges.append(authenticationChallenge)));
		}

		verificationCardStateRepository.save(verificationCardStateEntity);
	}

	public int getNextConfirmationAttemptId(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardStateEntity(verificationCardId);

		return verificationCardStateEntity.getConfirmationAttempts();
	}

	public int incrementConfirmationAttempts(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardStateEntity(verificationCardId);
		final VerificationCardState verificationCardState = verificationCardStateEntity.getState();

		checkState(VerificationCardState.CONFIRMING.equals(verificationCardState),
				"The current state does not allow to increment confirmation attempts. [verificationCardId: %s, verificationCardState: %s]",
				verificationCardId, verificationCardState);

		verificationCardStateEntity.incrementConfirmationAttempts();

		verificationCardStateRepository.save(verificationCardStateEntity);
		return verificationCardStateEntity.getConfirmationAttempts();
	}

}
