/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;

/**
 * Regroups the context values needed for the SetupTallyCCM algorithm.
 *
 * <ul>
 *     <li>j, the CCM's index. In range [1, 4].</li>
 *     <li>election event context, the {@link ElectionEventContext}. Not null.</li>
 * </ul>
 */
public record SetupTallyCCMContext(int nodeId, ElectionEventContext electionEventContext) {

	public SetupTallyCCMContext {
		checkNotNull(electionEventContext);
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
	}
}
