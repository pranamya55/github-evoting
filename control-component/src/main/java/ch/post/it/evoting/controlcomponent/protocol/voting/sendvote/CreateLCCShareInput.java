/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.controlcomponent.process.PartialChoiceReturnCodeAllowList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;

/**
 * Regroups the inputs needed by the CreateLCCShare algorithm.
 *
 * <ul>
 *     <li>L<sub>pCC</sub>, the partial Choice Return Codes allow list. Not null.</li>
 *     <li>pCC<sub>id</sub>, the vector of partial Choice Return Codes. Not null.</li>
 *     <li>k'<sub>j</sub>, the CCR<sub>j</sub> Return Codes Generation secret key. Not null.</li>
 * </ul>
 */
public record CreateLCCShareInput(PartialChoiceReturnCodeAllowList pCCAllowList, GroupVector<GqElement, GqGroup> partialChoiceReturnCodes,
								  ZqElement ccrjReturnCodesGenerationSecretKey) {

	public CreateLCCShareInput {
		checkNotNull(pCCAllowList);
		checkNotNull(partialChoiceReturnCodes);
		checkNotNull(ccrjReturnCodesGenerationSecretKey);

		// Cross-checks
		final int psi = partialChoiceReturnCodes.size();
		checkArgument(1 <= psi && psi <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The the vector of partial Choice Return Codes size must be in range [1, %s].", MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
		checkArgument(partialChoiceReturnCodes.getGroup().hasSameOrderAs(ccrjReturnCodesGenerationSecretKey.getGroup()),
				"The partial choice return codes and return codes generation secret key must have the same group order.");

		checkArgument(hasNoDuplicates(partialChoiceReturnCodes), "All pCC must be distinct.");
	}
}
