/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.validations.CorrectnessInformationValidation;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the context values needed by the ExtractCRC algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Not null.</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>vc, the verification card id. Not null and a valid UUID.</li>
 *     <li>&tau;&#770;, the list of blank correctness information. Not null and contains valid correctness information.</li>
 * </ul>
 */
public record ExtractCRCContext(GqGroup encryptionGroup, String electionEventId, String verificationCardId,
								ImmutableList<String> blankCorrectnessInformation) {

	/**
	 * @throws NullPointerException      if any of the fields is null or {@code blankCorrectnessInformation} contains any null.
	 * @throws FailedValidationException if {@code electionEventId} is invalid.
	 * @throws IllegalArgumentException  if {@code blankCorrectnessInformation} is not in range [1,
	 *                                   {@value
	 *                                   ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants#MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS}].
	 */
	public ExtractCRCContext {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);
		validateUUID(verificationCardId);
		checkNotNull(blankCorrectnessInformation).forEach(CorrectnessInformationValidation::validate);

		final int psi = blankCorrectnessInformation.size();
		checkArgument(psi >= 1 && psi <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The blank correctness information size must be in range [1, %s].", MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
	}
}
