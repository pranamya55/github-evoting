/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally;

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ControlComponentVotesHashPayloadValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.IntStream;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableBigInteger;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;

public record MixDecryptOnlineRequestPayload(String electionEventId,
											 String ballotBoxId,
											 int nodeId,
											 ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads,
											 ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads) implements HashableList {

	public MixDecryptOnlineRequestPayload {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		validate(electionEventId, ballotBoxId, controlComponentVotesHashPayloads);
		checkNotNull(controlComponentShufflePayloads);

		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);

		checkArgument(controlComponentShufflePayloads.size() == nodeId - 1,
				"There must be exactly (nodeId-1) control component shuffle payloads.");
		checkArgument(
				controlComponentShufflePayloads.isEmpty() || controlComponentShufflePayloads.get(0).getElectionEventId()
						.equals(electionEventId),
				"The election event id of the control component shuffle payloads and the mix decrypt online request payload must be the same.");
		checkArgument(
				controlComponentShufflePayloads.isEmpty() || controlComponentShufflePayloads.get(0).getBallotBoxId()
						.equals(ballotBoxId),
				"The ballot box id of the control component shuffle payloads and the mix decrypt online request payload must be the same.");
		checkArgument(allEqual(controlComponentShufflePayloads.stream(), ControlComponentShufflePayload::getElectionEventId),
				"All control component shuffle payloads must have the same election event id.");
		checkArgument(allEqual(controlComponentShufflePayloads.stream(), ControlComponentShufflePayload::getBallotBoxId),
				"All control component shuffle payloads must have the same ballot box id.");
		checkArgument(allEqual(controlComponentShufflePayloads.stream(), ControlComponentShufflePayload::getEncryptionGroup),
				"All control component shuffle payloads must have the same encryption group.");

		checkArgument(IntStream.range(1, nodeId)
						.allMatch(orderedNodeId -> controlComponentShufflePayloads.get(orderedNodeId - 1).getNodeId() == orderedNodeId),
				"The control component payloads must be sorted ascending by node id.");

	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(electionEventId),
				HashableString.from(ballotBoxId),
				HashableBigInteger.from(nodeId),
				HashableList.from(controlComponentVotesHashPayloads),
				HashableList.from(controlComponentShufflePayloads)
		);
	}
}
