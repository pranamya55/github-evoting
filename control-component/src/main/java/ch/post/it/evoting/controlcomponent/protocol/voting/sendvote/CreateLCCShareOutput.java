/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;

/**
 * Holds the output of the CreateLCCShare algorithm.
 *
 * <ul>
 *     <li>lCC<sub>j,id</sub>, CCR<sub>j</sub>’s long Choice Return Code share. Not null.</li>
 * </ul>
 */
public record CreateLCCShareOutput(GroupVector<GqElement, GqGroup> longChoiceReturnCodeShare) {

	public CreateLCCShareOutput {
		checkNotNull(longChoiceReturnCodeShare);
	}

}