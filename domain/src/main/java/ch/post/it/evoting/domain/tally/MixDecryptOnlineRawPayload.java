/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

public record MixDecryptOnlineRawPayload(String electionEventId, String ballotBoxId,
										 ImmutableList<ImmutableByteArray> controlComponentBallotBoxRawPayloads,
										 ImmutableList<ImmutableByteArray> controlComponentShuffleRawPayloads) {

	public MixDecryptOnlineRawPayload {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(controlComponentBallotBoxRawPayloads);
		checkNotNull(controlComponentShuffleRawPayloads);

		checkArgument(ControlComponentNode.ids().size() == controlComponentBallotBoxRawPayloads.size(),
				"There must be exactly the expected number of control component ballot box payloads.");
		checkArgument(ControlComponentNode.ids().size() == controlComponentShuffleRawPayloads.size(),
				"There must be exactly the expected number of control component shuffle payloads.");
	}
}
