/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

/**
 * Holds the context of the GenKeysCCR algorithm.
 *
 * <ul>
 *     <li>(p, q, g), the encryption group. Non-null.</li>
 *     <li>j, the CCR’s index. In range [1, 4].</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>&psi;<sub>max</sub>, the maximum number of selections. In range [1, &psi;<sub>sup</sub>].</li>
 * </ul>
 */
public record GenKeysCCRContext(GqGroup encryptionGroup, int nodeId, String electionEventId, int maximumNumberOfSelections) {

	public GenKeysCCRContext {
		checkNotNull(encryptionGroup);
		validateUUID(electionEventId);

		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);

		final int psi_sup = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
		checkArgument(maximumNumberOfSelections >= 1, "The maximum number of selections must be greater or equal to 1. [psi_max: %s]",
				maximumNumberOfSelections);
		checkArgument(maximumNumberOfSelections <= psi_sup,
				"The maximum number of selections must be smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				maximumNumberOfSelections, psi_sup);
	}
}
