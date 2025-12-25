/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.confirmvote;

import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

public class LVCCShareEntityKey {

	private String verificationCardId;
	private int confirmationAttemptId = 0;

	public LVCCShareEntityKey() {
	}

	public LVCCShareEntityKey(final String verificationCardId, final int confirmationAttemptId) {
		this.verificationCardId = validateUUID(verificationCardId);
		this.confirmationAttemptId = confirmationAttemptId;

		checkArgument(confirmationAttemptId >= 0 && confirmationAttemptId < MAX_CONFIRMATION_ATTEMPTS,
				"The confirmation attempt id must be in range [0,%s).", MAX_CONFIRMATION_ATTEMPTS);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LVCCShareEntityKey that = (LVCCShareEntityKey) o;
		return confirmationAttemptId == that.confirmationAttemptId && Objects.equals(verificationCardId, that.verificationCardId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(verificationCardId, confirmationAttemptId);
	}
}
