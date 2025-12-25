/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static com.google.common.base.Preconditions.checkState;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.domain.converters.BooleanConverter;

@Entity
@Table(name = "VERIFICATION_CARD_STATE")
public class VerificationCardStateEntity {

	@Id
	private String verificationCardId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VERIFICATION_CARD_ID", referencedColumnName = "VERIFICATION_CARD_ID")
	private VerificationCardEntity verificationCardEntity;

	@Convert(converter = BooleanConverter.class)
	private boolean partiallyDecrypted = false;

	@Convert(converter = BooleanConverter.class)
	private boolean lccShareCreated = false;

	// Represents also the next confirmation attempt id.
	private int confirmationAttempts = 0;

	@Convert(converter = BooleanConverter.class)
	private boolean confirmed = false;

	@Version
	private Integer changeControlId;

	public VerificationCardStateEntity() {
		// Needed by the repository.
	}

	public void setVerificationCardEntity(final VerificationCardEntity verificationCardEntity) {
		this.verificationCardEntity = verificationCardEntity;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public boolean isPartiallyDecrypted() {
		return partiallyDecrypted;
	}

	public boolean isLccShareCreated() {
		return lccShareCreated;
	}

	public int getConfirmationAttempts() {
		return confirmationAttempts;
	}

	public void incrementConfirmationAttempts() {
		// When all confirmation attempts are exhausted, the value of confirmationAttempts is equal to MAX_CONFIRMATION_ATTEMPTS.
		checkState(confirmationAttempts >= 0 && confirmationAttempts < MAX_CONFIRMATION_ATTEMPTS,
				"The confirmation attempts must be in range [0,%s) in order to increment. [confirmationAttempts: %s]", MAX_CONFIRMATION_ATTEMPTS,
				confirmationAttempts);
		this.confirmationAttempts += 1;
	}

	public boolean isConfirmed() {
		return confirmed;
	}

	public void setPartiallyDecrypted() {
		this.partiallyDecrypted = true;
	}

	public void setLccShareCreated() {
		this.lccShareCreated = true;
	}

	public void setConfirmed() {
		this.confirmed = true;
	}

}
