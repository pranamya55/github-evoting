/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;

public record MixDecryptOnlineResponsePayload(ControlComponentBallotBoxPayload controlComponentBallotBoxPayload,
											  ControlComponentShufflePayload controlComponentShufflePayload) implements HashableList {

	public MixDecryptOnlineResponsePayload {
		checkNotNull(controlComponentBallotBoxPayload);
		checkNotNull(controlComponentShufflePayload);

		checkArgument(controlComponentBallotBoxPayload.getEncryptionGroup().equals(controlComponentShufflePayload.getEncryptionGroup()),
				"The control component ballot box payload and the control component shuffle payload must have the encryption group.");
		checkArgument(controlComponentBallotBoxPayload.getElectionEventId().equals(controlComponentShufflePayload.getElectionEventId()),
				"The control component ballot box payload and the control component shuffle payload must have the same election event id.");
		checkArgument(controlComponentBallotBoxPayload.getBallotBoxId().equals(controlComponentShufflePayload.getBallotBoxId()),
				"The control component ballot box payload and the control component shuffle payload must have the same ballot box id.");
		checkArgument(controlComponentBallotBoxPayload.getNodeId() == controlComponentShufflePayload.getNodeId(),
				"The control component ballot box payload and the control component shuffle payload must have the same node id.");

	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(controlComponentBallotBoxPayload, controlComponentShufflePayload);
	}
}
