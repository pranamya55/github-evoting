/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

/**
 * Regroups the context values needed by the CreateLVCCShare and VerifyLVCCHash algorithms.
 *
 * <ul>
 *     <li>(p, q, g), the {@code GqGroup} with modulus p, cardinality q and generator g. Not null.</li>
 *     <li>j, the CCR's index. In range [1, 4].</li>
 *     <li>ee, the election event id. Not null and a valid UUID.</li>
 *     <li>vcs, the verification card set id. Not null and a valid UUID.</li>
 *     <li>vc<sub>id</sub>, the verification card id. Not null and a valid UUID.</li>
 * </ul>
 */
public record LVCCHashContext(GqGroup encryptionGroup, int nodeId, String electionEventId, String verificationCardSetId, String verificationCardId) {

	public LVCCHashContext {
		checkNotNull(encryptionGroup);
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		validateUUID(verificationCardId);
	}

}
