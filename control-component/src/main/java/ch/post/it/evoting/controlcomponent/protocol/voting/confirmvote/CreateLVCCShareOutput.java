/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;

/**
 * Regroups the output values needed by the CreateLVCCShare algorithm.
 *
 * <ul>
 *     <li>lVCC<sub>j,id</sub>, the CCR<sub>j</sub>’s long Vote Cast Return Code share. Not null.</li>
 *     <li>hlVCC<sub>j,id</sub>, the hashed long Vote Cast Return Code share. Not null.</li>
 *     <li>attempts<sub>id</sub>, the confirmation attempt number. In range [0, 4].</li>
 * </ul>
 */
public record CreateLVCCShareOutput(GqElement longVoteCastReturnCodeShare, String hashedLongVoteCastReturnCodeShare, int confirmationAttemptId) {

	public CreateLVCCShareOutput {
		checkArgument(confirmationAttemptId >= 0, "The confirmation attempt id must be positive.");
		checkArgument(confirmationAttemptId < MAX_CONFIRMATION_ATTEMPTS, "The confirmation attempt id must be at most %s.",
				MAX_CONFIRMATION_ATTEMPTS);
		checkNotNull(longVoteCastReturnCodeShare);
		checkNotNull(hashedLongVoteCastReturnCodeShare);
	}

}
