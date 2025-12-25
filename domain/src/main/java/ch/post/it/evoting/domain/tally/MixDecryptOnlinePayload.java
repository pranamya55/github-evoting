/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;

public record MixDecryptOnlinePayload(String electionEventId, String ballotBoxId,
									  ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads,
									  ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads) {

	public MixDecryptOnlinePayload {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(controlComponentBallotBoxPayloads);
		checkNotNull(controlComponentShufflePayloads);

		checkArgument(ControlComponentNode.ids().size() == controlComponentBallotBoxPayloads.size(),
				"There must be exactly the expected number of control component ballot box payloads.");
		checkArgument(ControlComponentNode.ids().size() == controlComponentShufflePayloads.size(),
				"There must be exactly the expected number of control component shuffle payloads.");

		checkArgument(ControlComponentNode.ids().equals(controlComponentBallotBoxPayloads.stream()
						.map(ControlComponentBallotBoxPayload::getNodeId)
						.collect(toImmutableSet())),
				"The node ids of the control component ballot box payloads must be part of the known node ids");
		checkArgument(ControlComponentNode.ids().equals(controlComponentShufflePayloads.stream()
						.map(ControlComponentShufflePayload::getNodeId)
						.collect(toImmutableSet())),
				"The node ids of the control component shuffle payloads must be part of the known node ids");

		checkArgument(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getElectionEventId),
				"All control component ballot box payloads must have the same election event id.");
		checkArgument(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getBallotBoxId),
				"All control component ballot box payloads must have the same ballot box id.");
		checkArgument(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getEncryptionGroup),
				"All control component ballot box payloads must have the same encryption group.");
		checkArgument(allEqual(controlComponentBallotBoxPayloads.stream(), ControlComponentBallotBoxPayload::getConfirmedEncryptedVotes),
				"All control component ballot box payloads must have the same confirmed encrypted votes.");
		checkArgument(
				controlComponentBallotBoxPayloads.get(0).getEncryptionGroup().equals(controlComponentShufflePayloads.get(0).getEncryptionGroup()),
				"The encryption group of the control component ballot box payloads and the control component shuffle payloads must be the same.");

		checkArgument(allEqual(controlComponentShufflePayloads.stream(), ControlComponentShufflePayload::getElectionEventId),
				"All control component shuffle payloads must have the same election event id.");
		checkArgument(allEqual(controlComponentShufflePayloads.stream(), ControlComponentShufflePayload::getBallotBoxId),
				"All control component shuffle payloads must have the same ballot box id.");
		checkArgument(allEqual(controlComponentShufflePayloads.stream(), ControlComponentShufflePayload::getEncryptionGroup),
				"All control component shuffle payloads must have the same encryption group.");

		checkArgument(controlComponentBallotBoxPayloads.get(0).getElectionEventId().equals(electionEventId),
				"The election event id of the mix decrypt online payload and the control component ballot box payloads must be the same.");
		checkArgument(controlComponentShufflePayloads.get(0).getElectionEventId().equals(electionEventId),
				"The election event id of the mix decrypt online payload and the control component shuffle payloads must be the same.");

		checkArgument(controlComponentBallotBoxPayloads.get(0).getBallotBoxId().equals(ballotBoxId),
				"The ballot box id of the mix decrypt online payload and the control component ballot box payloads must be the same.");
		checkArgument(controlComponentShufflePayloads.get(0).getBallotBoxId().equals(ballotBoxId),
				"The  ballot box id of the mix decrypt online payload and the control component shuffle payloads must be the same.");
	}

	/**
	 * Deserializes the raw payload into a {@link MixDecryptOnlinePayload}.
	 *
	 * @param mixDecryptOnlineRawPayload the raw payload to deserialize. Must not be null.
	 * @param objectMapper               the object mapper to use for deserialization. Must not be null.
	 * @return the deserialized payload.
	 */
	public static MixDecryptOnlinePayload from(final MixDecryptOnlineRawPayload mixDecryptOnlineRawPayload, final ObjectMapper objectMapper) {
		checkNotNull(mixDecryptOnlineRawPayload);
		checkNotNull(objectMapper);

		final ImmutableList<ControlComponentBallotBoxPayload> controlComponentBallotBoxPayloads = mixDecryptOnlineRawPayload
				.controlComponentBallotBoxRawPayloads().stream()
				.map(rawPayload -> {
					try {
						return objectMapper.readValue(rawPayload.elements(), ControlComponentBallotBoxPayload.class);
					} catch (final Exception e) {
						throw new IllegalArgumentException("Failed to deserialize control component ballot box payload.", e);
					}
				})
				.collect(ImmutableList.toImmutableList());

		final ImmutableList<ControlComponentShufflePayload> controlComponentShufflePayloads = mixDecryptOnlineRawPayload
				.controlComponentShuffleRawPayloads().stream()
				.map(rawPayload -> {
					try {
						return objectMapper.readValue(rawPayload.elements(), ControlComponentShufflePayload.class);
					} catch (final Exception e) {
						throw new IllegalArgumentException("Failed to deserialize control component shuffle payload.", e);
					}
				})
				.collect(ImmutableList.toImmutableList());

		return new MixDecryptOnlinePayload(
				mixDecryptOnlineRawPayload.electionEventId(),
				mixDecryptOnlineRawPayload.ballotBoxId(),
				controlComponentBallotBoxPayloads,
				controlComponentShufflePayloads);
	}
}
