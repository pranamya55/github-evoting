/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the context values needed by the ExtractVCC algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Non-null.</li>
 *     <li>ee, the election event id. Non-null and a valid UUID.</li>
 *     <li>vc<sub>id</sub>, the verification card id. Non-null and a valid UUID.</li>
 * </ul>
 */
public record ExtractVCCContext(GqGroup encryptionGroup, String electionEventId, String verificationCardId) {

	/**
	 * @throws NullPointerException      if any of the fields is null.
	 * @throws FailedValidationException if {@code electionEventId} or {@code verificationCardId} are invalid UUIDs.
	 */
	public ExtractVCCContext {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);
		validateUUID(verificationCardId);
	}

}
