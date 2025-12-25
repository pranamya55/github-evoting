/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.NUMBER_OF_CONTROL_COMPONENTS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Function;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;

/**
 * Regroups the inputs of the ExtractCRC algorithm.
 *
 * <ul>
 *     <li>(lCC<sub>1,id</sub>, lCC<sub>2,id</sub>, lCC<sub>3,id</sub>, lCC<sub>4,id</sub>), CCR long Choice Return Code shares. Not null.</li>
 *     <li>CMtable, the Return Codes Mapping Table. Not null.</li>
 * </ul>
 */
public record ExtractCRCInput(ImmutableList<GroupVector<GqElement, GqGroup>> longChoiceReturnCodeShares,
							  ReturnCodesMappingTable returnCodesMappingTable) {

	/**
	 * @param longChoiceReturnCodeShares (lCC<sub>1,id</sub>, lCC<sub>2,id</sub>, lCC<sub>3,id</sub>, lCC<sub>4,id</sub>) ∈
	 *                                   (G<sub>q</sub><sup>&#x1D713;</sup>)<sup>4</sup>, CCR long Choice Return Code shares.
	 * @param returnCodesMappingTable    CMtable, the Return Codes Mapping Table.
	 * @throws NullPointerException      if any of the fields is null or any list contains null value.
	 * @throws IllegalArgumentException  if
	 *                                   <ul>
	 *                                       <li>The {@code longChoiceReturnCodeShares} size is not {@value ch.post.it.evoting.evotinglibraries.domain.common.Constants#NUMBER_OF_CONTROL_COMPONENTS}.</li>
	 *                                       <li>The GqGroup of each GroupVector of {@code longChoiceReturnCodeShares} is not the same.</li>
	 *                                       <li>Not all GroupVectors of {@code longChoiceReturnCodeShares} have the same size.</li>
	 *                                       <li>Not all GroupVectors of {@code longChoiceReturnCodeShares} size are in range [1, {@value ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants#MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS}].</li>
	 *                                   </ul>
	 * @throws FailedValidationException if the verification card id is not a valid UUID.
	 */
	public ExtractCRCInput {
		checkNotNull(longChoiceReturnCodeShares);
		checkNotNull(returnCodesMappingTable);

		checkArgument(longChoiceReturnCodeShares.size() == NUMBER_OF_CONTROL_COMPONENTS,
				String.format("There must be long Choice Return Code shares from %s control-components.", NUMBER_OF_CONTROL_COMPONENTS));

		// Cross-checks.
		checkArgument(allEqual(longChoiceReturnCodeShares.stream(), GroupVector::size),
				"All long Choice Return Code Shares must have the same size.");

		final int psi = longChoiceReturnCodeShares.get(0).size();
		checkArgument(psi >= 1 && psi <= MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS,
				"The long Choice Return Code Shares size must be in range [1, %s].", MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);

		final ImmutableList<GqGroup> gqGroups = longChoiceReturnCodeShares.stream().map(GroupVector::getGroup).collect(toImmutableList());
		checkArgument(allEqual(gqGroups.stream(), Function.identity()), "All long Choice Return Code Shares must have the same Gq group.");
	}

	public GqGroup getGroup() {
		return this.longChoiceReturnCodeShares.get(0).getGroup();
	}

}
