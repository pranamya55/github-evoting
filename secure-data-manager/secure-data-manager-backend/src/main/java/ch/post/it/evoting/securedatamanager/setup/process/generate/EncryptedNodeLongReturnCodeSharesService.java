/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generate;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.generate.ControlComponentCodeSharesPayloadService.ControlComponentCodeSharesPayloadsChunk;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;

/**
 * Allows to combine the node payloads responses.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class EncryptedNodeLongReturnCodeSharesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedNodeLongReturnCodeSharesService.class);

	private final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;

	public EncryptedNodeLongReturnCodeSharesService(
			final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository) {
		this.setupComponentVerificationDataPayloadFileRepository = setupComponentVerificationDataPayloadFileRepository;
	}

	/**
	 * Converts the {@link ControlComponentCodeSharesPayloadsChunk} and the {@link SetupComponentVerificationDataPayload} chunk into an
	 * {@link EncryptedNodeLongReturnCodeSharesChunk} for the given election event id and verification card set id.
	 *
	 * @param electionEventId                         the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId                   the verification card set id. Must be non-null and a valid UUID.
	 * @param controlComponentCodeSharesPayloadsChunk the chunk to convert. Must be non-null.
	 * @return an {@link EncryptedNodeLongReturnCodeSharesChunk}.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code electionEventId}, {@code verificationCardSetId} is invalid.
	 */
	public EncryptedNodeLongReturnCodeSharesChunk convertControlComponentCodeSharesPayloadsChunk(
			final String electionEventId, final String verificationCardSetId,
			final ControlComponentCodeSharesPayloadsChunk controlComponentCodeSharesPayloadsChunk) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(controlComponentCodeSharesPayloadsChunk);

		final int chunkId = controlComponentCodeSharesPayloadsChunk.chunkId();
		LOGGER.debug("Converting a node payloads chunk. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]",
				electionEventId, verificationCardSetId, chunkId);

		final SetupComponentVerificationDataPayload setupComponentVerificationDataPayloadChunk = setupComponentVerificationDataPayloadFileRepository.retrieve(
				electionEventId, verificationCardSetId, chunkId);
		verifyConsistencyChunk(electionEventId, verificationCardSetId, controlComponentCodeSharesPayloadsChunk,
				setupComponentVerificationDataPayloadChunk);

		final ImmutableList<EncryptedSingleNodeLongReturnCodeSharesChunk> encryptedSingleNodeLongReturnCodeSharesChunks =
				controlComponentCodeSharesPayloadsChunk.payloads().stream()
						.map(EncryptedSingleNodeLongReturnCodeSharesChunk::new)
						.collect(toImmutableList());

		return new EncryptedNodeLongReturnCodeSharesChunk.Builder()
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setChunkId(chunkId)
				.setControlComponentCodeSharesChunks(encryptedSingleNodeLongReturnCodeSharesChunks)
				.setSetupComponentVerificationData(setupComponentVerificationDataPayloadChunk.getSetupComponentVerificationData())
				.build();
	}

	/**
	 * Verifies:
	 * <ul>
	 *     <li>there is at least one node contribution response.</li>
	 *     <li>there is no missing node id.</li>
	 *     <li>the order of ControlComponentCodeSharesPayloads is sorted by node id.</li>
	 *     <li>the election event id verification card set id and chunk id of all the ControlComponentCodeSharesPayloads are correct.</li>
	 *     <li>the chunk count is the same for the SetupComponentVerificationDataPayloads and ControlComponentCodeSharesPayloads.</li>
	 *     <li>the ControlComponentCodeSharesPayloads have the same encryption group as the SetupComponentVerificationDataPayloads.</li>
	 *     <li>the verification card ids are unique among the ControlComponentCodeSharesPayload chunks.</li>
	 *     <li>the ControlComponentCodeSharesPayloads' verification card ids have the same content and order across all nodes.</li>
	 *     <li>the verification card ids of the SetupComponentVerificationDataPayload's chunks have the same content and order than the verification card ids of the ControlComponentCodeSharesPayload's chunks.</li>
	 * </ul>
	 */
	private void verifyConsistencyChunk(final String electionEventId, final String verificationCardSetId,
			final ControlComponentCodeSharesPayloadsChunk controlComponentCodeSharesPayloadsChunk,
			final SetupComponentVerificationDataPayload setupComponentVerificationDataPayloadChunk) {

		final int chunkId = controlComponentCodeSharesPayloadsChunk.chunkId();

		// At least one node contribution response
		checkState(!controlComponentCodeSharesPayloadsChunk.payloads().isEmpty(),
				"No node payloads responses. [electionEventId: %s, verificationCardSetId: %s, chunkId: %s]", electionEventId,
				verificationCardSetId, chunkId);

		// Missing node id
		checkState(controlComponentCodeSharesPayloadsChunk.payloads().size() == ControlComponentNode.ids().size(),
				"The node ID sequence is incomplete. [electionEventId: %s, verificationCardSetId: %s, chunkId: %s]", electionEventId,
				verificationCardSetId, chunkId);

		// Node id order
		checkState(IntStream.range(0, ControlComponentNode.ids().size() - 1)
						.allMatch(i -> controlComponentCodeSharesPayloadsChunk.payloads().get(i).getNodeId() == i + 1),
				"The node ID sequence is not in the correct order. [electionEventId: %s, verificationCardSetId: %s, chunkId: %s]", electionEventId,
				verificationCardSetId, chunkId);

		// ElectionEventId, VerificationCardSetId and ChunkId are consistent
		checkState(controlComponentCodeSharesPayloadsChunk.payloads().stream()
						.allMatch(payload -> electionEventId.equals(payload.getElectionEventId())
								&& verificationCardSetId.equals(payload.getVerificationCardSetId())
								&& chunkId == payload.getChunkId()),
				"All return code generation response payloads must be related to the correct election event id,"
						+ " verification card set id and chunkId. [electionEventId: %s, verificationCardSetId: %s, chunkId: %s]", electionEventId,
				verificationCardSetId, chunkId);

		final SetupComponentVerificationDataPayloadContent setupComponentVerificationDataPayloadContent = getSetupComponentVerificationDataPayloadContent(
				electionEventId, verificationCardSetId, chunkId, setupComponentVerificationDataPayloadChunk);

		controlComponentCodeSharesPayloadsChunk.payloads().forEach(controlComponentCodeSharesPayload -> {
			final int nodeId = controlComponentCodeSharesPayload.getNodeId();

			// ChunkIds are consistent
			checkState(chunkId == setupComponentVerificationDataPayloadContent.chunkId,
					"The ControlComponentCodeSharesPayload does not have the same chunk id as the SetupComponentVerificationDataPayload."
							+ " [controlComponentChunkId: %s, setupComponentChunkId: %s]", chunkId,
					setupComponentVerificationDataPayloadContent.chunkId);

			// Encryption groups are equals
			checkState(
					setupComponentVerificationDataPayloadContent.encryptionGroup()
							.equals(controlComponentCodeSharesPayload.getEncryptionGroup()),
					"The ControlComponentCodeSharesPayload does not have the same encryption group as the "
							+ "SetupComponentVerificationDataPayload. [electionEventId: %s, verificationCardSetId: %s, chunkId: %s, nodeId: %s]",
					electionEventId, verificationCardSetId, chunkId, nodeId);

			final ImmutableList<String> controlComponentVerificationCardIds = controlComponentCodeSharesPayload.getControlComponentCodeShares()
					.stream().parallel()
					.map(ControlComponentCodeShare::verificationCardId)
					.collect(toImmutableList());
			// The ControlComponentCodeShare payload ensures no verificationCardId duplicates.

			// The verification card ids are equals between payloads
			checkState(setupComponentVerificationDataPayloadContent.verificationCardIds().equals(controlComponentVerificationCardIds),
					"The ControlComponentCodeSharesPayload does not have the same verification card ids as the "
							+ "SetupComponentVerificationDataPayload. [electionEventId: %s, verificationCardSetId: %s, chunkId: %s, nodeId: %s]",
					electionEventId, verificationCardSetId, chunkId, nodeId);
		});

	}

	private SetupComponentVerificationDataPayloadContent getSetupComponentVerificationDataPayloadContent(final String electionEventId,
			final String verificationCardSetId, final int chunkId,
			final SetupComponentVerificationDataPayload setupComponentVerificationDataPayloadChunk) {
		checkState(electionEventId.equals(setupComponentVerificationDataPayloadChunk.getElectionEventId()),
				"The electionEventId in SetupComponentVerificationDataPayload is not correct. [expected: %s, actual: %s]", electionEventId,
				setupComponentVerificationDataPayloadChunk.getElectionEventId());
		checkState(verificationCardSetId.equals(setupComponentVerificationDataPayloadChunk.getVerificationCardSetId()),
				"The verificationCardSetId in SetupComponentVerificationDataPayload is not correct. [expected: %s, actual: %s]",
				verificationCardSetId, setupComponentVerificationDataPayloadChunk.getVerificationCardSetId());
		checkState(chunkId == setupComponentVerificationDataPayloadChunk.getChunkId(),
				"The chunkId in SetupComponentVerificationDataPayload is not correct. [expected: %s, actual: %s]",
				chunkId, setupComponentVerificationDataPayloadChunk.getChunkId());

		final ImmutableList<String> setupComponentVerificationCardIds = setupComponentVerificationDataPayloadChunk.getSetupComponentVerificationData()
				.stream()
				.map(SetupComponentVerificationData::verificationCardId)
				.collect(toImmutableList());

		return new SetupComponentVerificationDataPayloadContent(
				setupComponentVerificationDataPayloadChunk.getChunkId(),
				setupComponentVerificationDataPayloadChunk.getEncryptionGroup(),
				setupComponentVerificationCardIds
		);
	}

	record SetupComponentVerificationDataPayloadContent(int chunkId, GqGroup encryptionGroup, ImmutableList<String> verificationCardIds) {
	}

}
