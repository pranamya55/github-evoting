/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.domain.Constants.MAX_AUTHENTICATION_ATTEMPTS;
import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.time.LocalDateTime;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.converters.ImmutableListConverter;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;

@Entity
@Table(name = "VERIFICATION_CARD_STATE")
public class VerificationCardStateEntity {

	@Id
	private String verificationCardId;

	@Convert(converter = ImmutableListConverter.class)
	private ImmutableList<String> shortChoiceReturnCodes = ImmutableList.emptyList();

	private String shortVoteCastReturnCode;

	private VerificationCardState state = VerificationCardState.INITIAL;

	private int authenticationAttempts = 0;

	private long lastSuccessfulAuthenticationTimeStep = 0;

	@Convert(converter = SuccessfulAuthenticationAttemptsConverter.class)
	private SuccessfulAuthenticationAttempts successfulAuthenticationAttempts = new SuccessfulAuthenticationAttempts(ImmutableList.emptyList());

	// Represents also the next confirmation attempt id.
	private int confirmationAttempts = 0;

	private LocalDateTime stateDate = LocalDateTimeUtils.now();

	@Version
	private Integer changeControlId;

	public VerificationCardStateEntity() {
	}

	public VerificationCardStateEntity(final String verificationCardId) {
		this.verificationCardId = verificationCardId;
	}

	public String getVerificationCardId() {
		return verificationCardId;
	}

	public ImmutableList<String> getShortChoiceReturnCodes() {
		return shortChoiceReturnCodes;
	}

	public void setShortChoiceReturnCodes(final ImmutableList<String> shortChoiceReturnCodes) {
		this.shortChoiceReturnCodes = checkNotNull(shortChoiceReturnCodes);
	}

	public String getShortVoteCastReturnCode() {
		return shortVoteCastReturnCode;
	}

	public void setShortVoteCastReturnCode(final String shortVoteCastReturnCode) {
		checkNotNull(shortVoteCastReturnCode);
		this.shortVoteCastReturnCode = shortVoteCastReturnCode;
	}

	public VerificationCardState getState() {
		return state;
	}

	private void setState(final VerificationCardState state) {
		checkNotNull(state);
		this.state = state;
	}

	public int getAuthenticationAttempts() {
		return authenticationAttempts;
	}

	public void setAuthenticationAttempts(final int authenticationAttempts) {
		checkArgument(authenticationAttempts > 0 && authenticationAttempts <= MAX_AUTHENTICATION_ATTEMPTS,
				"Authentication attempt must be in ]0,5].");
		this.authenticationAttempts = authenticationAttempts;
	}

	public long getLastSuccessfulAuthenticationTimeStep() {
		return lastSuccessfulAuthenticationTimeStep;
	}

	public void setLastSuccessfulAuthenticationTimeStep(final long lastTimeStep) {
		checkArgument(lastTimeStep >= 0, "Last time step must be positive.");
		this.lastSuccessfulAuthenticationTimeStep = lastTimeStep;
	}

	public SuccessfulAuthenticationAttempts getSuccessfulAuthenticationAttempts() {
		return successfulAuthenticationAttempts;
	}

	public void setSuccessfulAuthenticationAttempts(final SuccessfulAuthenticationAttempts lastSuccessfulAuthenticationChallenge) {
		this.successfulAuthenticationAttempts = lastSuccessfulAuthenticationChallenge;
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

	public LocalDateTime getStateDate() {
		return stateDate;
	}

	private void setStateDate(final LocalDateTime stateDate) {
		checkNotNull(stateDate);
		this.stateDate = stateDate;
	}

	/**
	 * Updates the state to the provided state. Subsequently, this method also updates the state date to {@code LocalDateTimeUtils.now()}.
	 *
	 * @param state the state to be set. Must be non-null.
	 * @throws NullPointerException if the provided state is null.
	 */
	public void updateState(final VerificationCardState state) {
		checkNotNull(state);
		setState(state);
		setStateDate(LocalDateTimeUtils.now());
	}

}
