/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.NUMBER_OF_CONTROL_COMPONENTS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;

/**
 * Regroups the inputs needed by the ExtractVCC algorithm.
 *
 * <ul>
 *     <li>(lVCC<sub>id,1</sub>, lVCC<sub>id,2</sub>, lVCC<sub>id,3</sub>, lVCC<sub>id,4</sub>), CCR long Vote Cast Return Code shares. Non-null.</li>
 *     <li>CMTable, Return Codes Mapping table. Non-null.</li>
 * </ul>
 */
public record ExtractVCCInput(GroupVector<GqElement, GqGroup> longVoteCastReturnCodeShares,
							  ReturnCodesMappingTable returnCodesMappingTable) {

	/**
	 * @param longVoteCastReturnCodeShares (lVCC<sub>1,id</sub>, lVCC<sub>2,id</sub>, lVCC<sub>3,id</sub>, lVCC<sub>4,id</sub>) ∈
	 *                                     G<sub>q</sub><sup>4</sup>, CCR long Vote Cast Return Code shares.
	 * @param returnCodesMappingTable      CMtable, the Return Codes Mapping Table.
	 * @throws NullPointerException      if any of the fields is null.
	 * @throws IllegalArgumentException  if the {@code longVoteCastReturnCodeShares} size is not
	 *                                   {@value ch.post.it.evoting.evotinglibraries.domain.common.Constants#NUMBER_OF_CONTROL_COMPONENTS}.
	 * @throws FailedValidationException if the {@code verificationCardId} do not comply the UUID format.
	 */
	public ExtractVCCInput {
		checkNotNull(longVoteCastReturnCodeShares);
		checkNotNull(returnCodesMappingTable);

		checkArgument(longVoteCastReturnCodeShares.size() == NUMBER_OF_CONTROL_COMPONENTS,
				String.format("There must be long Vote Cast Return Code shares from %s control-components.", NUMBER_OF_CONTROL_COMPONENTS));
	}

	public GqGroup getGroup() {
		return longVoteCastReturnCodeShares.get(0).getGroup();
	}

}
