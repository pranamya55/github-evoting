/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VerificationCardStateService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardStateService.class);

	private final VerificationCardStateRepository verificationCardStateRepository;

	public VerificationCardStateService(
			final VerificationCardStateRepository verificationCardStateRepository) {
		this.verificationCardStateRepository = verificationCardStateRepository;
	}

	public boolean isPartiallyDecrypted(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardState(verificationCardId);

		return verificationCardStateEntity.isPartiallyDecrypted();
	}

	public boolean isNotPartiallyDecrypted(final String verificationCardId) {
		return !isPartiallyDecrypted(verificationCardId);
	}

	public void setPartiallyDecrypted(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardState(verificationCardId);

		checkState(!verificationCardStateEntity.isPartiallyDecrypted(),
				"Verification card state cannot be set to partially decrypted because it is already partially decrypted. [verificationCardId: %s]",
				verificationCardId);

		verificationCardStateEntity.setPartiallyDecrypted();

		verificationCardStateRepository.save(verificationCardStateEntity);
		LOGGER.info("Set verification card state to partially decrypted [verificationCardId: {}]", verificationCardId);
	}

	public boolean isSentVote(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardState(verificationCardId);

		return verificationCardStateEntity.isLccShareCreated();
	}

	public boolean isNotSentVote(final String verificationCardId) {
		validateUUID(verificationCardId);

		return !isSentVote(verificationCardId);
	}

	public void setSentVote(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardState(verificationCardId);

		checkState(!verificationCardStateEntity.isLccShareCreated(),
				"Verification card state cannot be set to sent because it is already sent. [verificationCardId: %s]", verificationCardId);

		verificationCardStateEntity.setLccShareCreated();

		verificationCardStateRepository.save(verificationCardStateEntity);
		LOGGER.info("Set verification card state to LCC created [verificationCardId: {}]", verificationCardId);
	}

	public boolean isNotConfirmedVote(final String verificationCardId) {
		validateUUID(verificationCardId);

		return !isConfirmedVote(verificationCardId);
	}

	public boolean isConfirmedVote(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardState(verificationCardId);

		return verificationCardStateEntity.isConfirmed();
	}

	public void setConfirmedVote(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardState(verificationCardId);

		checkState(verificationCardStateEntity.isLccShareCreated(),
				"Verification card state cannot be set to confirmed because it is not yet sent. [verificationCardId: %s]", verificationCardId);
		checkState(!verificationCardStateEntity.isConfirmed(),
				"Verification card state cannot be set to confirmed because it is already confirmed. [verificationCardId: %s]", verificationCardId);

		verificationCardStateEntity.setConfirmed();

		verificationCardStateRepository.save(verificationCardStateEntity);
		LOGGER.info("Set verification card state to confirmed [verificationCardId: {}]", verificationCardId);
	}

	public int getNextConfirmationAttemptId(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardState(verificationCardId);

		return verificationCardStateEntity.getConfirmationAttempts();
	}

	public void incrementConfirmationAttempts(final String verificationCardId) {
		validateUUID(verificationCardId);

		final VerificationCardStateEntity verificationCardStateEntity = getVerificationCardState(verificationCardId);

		verificationCardStateEntity.incrementConfirmationAttempts();

		verificationCardStateRepository.save(verificationCardStateEntity);
	}

	private VerificationCardStateEntity getVerificationCardState(final String verificationCardId) {
		validateUUID(verificationCardId);

		return verificationCardStateRepository.findById(verificationCardId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("No verification card state found. [verificationCardId: %s]", verificationCardId)));
	}
}
