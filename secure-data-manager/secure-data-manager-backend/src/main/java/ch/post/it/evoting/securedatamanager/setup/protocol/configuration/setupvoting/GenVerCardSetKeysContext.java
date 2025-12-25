/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

/**
 * Regroups the context needed by the GenVerCardSetKeys algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Not null.</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>&psi;<sub>max</sub>, the maximum number of selections. In range [1, &psi;<sub>sup</sub>].</li>
 * </ul>
 */
public record GenVerCardSetKeysContext(GqGroup encryptionGroup, String electionEventId, int maximumNumberOfSelections) {

	public GenVerCardSetKeysContext {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);

		checkArgument(maximumNumberOfSelections > 0, "The maximum number of selections must be strictly positive. [psi_max: %s]",
				maximumNumberOfSelections);
		checkArgument(maximumNumberOfSelections <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The maximum number of selections must be smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				maximumNumberOfSelections, MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
	}
}
