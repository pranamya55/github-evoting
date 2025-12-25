/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

public record LongVoteCastReturnCodesAllowListResponsePayload(int nodeId,
															  String electionEventId,
															  String verificationCardSetId) implements HashableList {

	public LongVoteCastReturnCodesAllowListResponsePayload {
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableBigInteger.from(nodeId),
				HashableString.from(electionEventId),
				HashableString.from(verificationCardSetId)
		);
	}
}
