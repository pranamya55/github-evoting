/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Regroups the context values needed by the CreateLCCShare algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the {@code GqGroup} with modulus p, cardinality q and generator g. Not null.</li>
 *     <li>j, the CCR's index. In range [1, 4].</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>vcs, the verification card set id. Not null and a valid UUID.</li>
 *     <li>vc<sub>id</sub>, the verification card id. Not null and a valid UUID.</li>
 *     <li>&tau;&#770;, the list of blank correctness information. Not null, non-empty and size smaller than or equal to {@value ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants#MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS}.</li>
 * </ul>
 */
public record CreateLCCShareContext(GqGroup encryptionGroup,
									int nodeId,
									String electionEventId,
									String verificationCardSetId,
									String verificationCardId,
									ImmutableList<String> blankCorrectnessInformation
) {

	/**
	 * @throws NullPointerException      if any of the fields is null or {@code blankCorrectnessInformation} contains any null.
	 * @throws FailedValidationException if {@code electionEventId}, {@code verificationCardSetId} or {@code verificationCardId} is not a valid UUID.
	 * @throws IllegalArgumentException  if {@code blankCorrectnessInformation} is not in range [1,
	 *                                   {@value
	 *                                   ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants#MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS}].
	 */
	public CreateLCCShareContext {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		validateUUID(verificationCardId);
		checkNotNull(blankCorrectnessInformation);

		// Cross-checks
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);

		final int psi = blankCorrectnessInformation.size();
		checkArgument(1 <= psi && psi <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The blank correctness information size must be in range [1, %s].", MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
	}

}
