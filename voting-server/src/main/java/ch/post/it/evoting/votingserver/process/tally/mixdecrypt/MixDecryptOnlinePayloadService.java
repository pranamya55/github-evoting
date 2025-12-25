/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.tally.mixdecrypt;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRawPayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentShufflePayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.tally.ControlComponentBallotBoxPayload;

@Service
public class MixDecryptOnlinePayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptOnlinePayloadService.class);

	private final ObjectMapper objectMapper;
	private final ControlComponentShufflePayloadRepository controlComponentShufflePayloadRepository;
	private final ControlComponentBallotBoxPayloadRepository controlComponentBallotBoxPayloadRepository;
	private final ControlComponentVotesHashPayloadRepository controlComponentVotesHashPayloadRepository;

	public MixDecryptOnlinePayloadService(
			final ObjectMapper objectMapper,
			final ControlComponentShufflePayloadRepository controlComponentShufflePayloadRepository,
			final ControlComponentBallotBoxPayloadRepository controlComponentBallotBoxPayloadRepository,
			final ControlComponentVotesHashPayloadRepository controlComponentVotesHashPayloadRepository) {
		this.objectMapper = objectMapper;
		this.controlComponentShufflePayloadRepository = controlComponentShufflePayloadRepository;
		this.controlComponentBallotBoxPayloadRepository = controlComponentBallotBoxPayloadRepository;
		this.controlComponentVotesHashPayloadRepository = controlComponentVotesHashPayloadRepository;
	}

	public void saveControlComponentBallotBoxPayload(final ControlComponentBallotBoxPayload controlComponentBallotBoxPayload) {
		checkNotNull(controlComponentBallotBoxPayload);

		final String electionEventId = controlComponentBallotBoxPayload.getElectionEventId();
		final String ballotBoxId = controlComponentBallotBoxPayload.getBallotBoxId();
		final int nodeId = controlComponentBallotBoxPayload.getNodeId();

		final ImmutableByteArray payload;
		try {
			payload = new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentBallotBoxPayload));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize control component ballot box payload.", e);
		}

		final ControlComponentBallotBoxPayloadEntity controlComponentBallotBoxPayloadEntity = new ControlComponentBallotBoxPayloadEntity(
				electionEventId, ballotBoxId, nodeId, payload);

		controlComponentBallotBoxPayloadRepository.save(controlComponentBallotBoxPayloadEntity);
	}

	public void saveControlComponentVotesHashPayloads(final ImmutableList<ControlComponentVotesHashPayload> controlComponentVotesHashPayloads) {
		checkNotNull(controlComponentVotesHashPayloads);

		checkArgument(allEqual(controlComponentVotesHashPayloads.stream(), ControlComponentVotesHashPayload::getElectionEventId),
				"The Control Component Votes Hash Payloads must have the same election event id.");
		checkArgument(allEqual(controlComponentVotesHashPayloads.stream(), ControlComponentVotesHashPayload::getBallotBoxId),
				"The Control Component Votes Hash Payloads must have the same ballot box id.");

		final ImmutableList<ControlComponentVotesHashPayloadEntity> controlComponentVotesHashPayloadEntities = controlComponentVotesHashPayloads.stream()
				.map(controlComponentVotesHashPayload -> {
					final String electionEventId = controlComponentVotesHashPayload.getElectionEventId();
					final String ballotBoxId = controlComponentVotesHashPayload.getBallotBoxId();
					final int nodeId = controlComponentVotesHashPayload.getNodeId();

					try {
						final ImmutableByteArray payload = new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentVotesHashPayload));
						return new ControlComponentVotesHashPayloadEntity(electionEventId, ballotBoxId, nodeId, payload);
					} catch (final JsonProcessingException e) {
						throw new UncheckedIOException("Failed to save the Control Component Votes Hash Payload.", e);
					}
				}).collect(toImmutableList());

		controlComponentVotesHashPayloadRepository.saveAll(controlComponentVotesHashPayloadEntities);
	}

	public void saveControlComponentShufflePayload(final ControlComponentShufflePayload controlComponentShufflePayload) {
		checkNotNull(controlComponentShufflePayload);

		final String electionEventId = controlComponentShufflePayload.getElectionEventId();
		final String ballotBoxId = controlComponentShufflePayload.getBallotBoxId();
		final int nodeId = controlComponentShufflePayload.getNodeId();

		try {
			final ImmutableByteArray payload = new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentShufflePayload));

			final ControlComponentShufflePayloadEntity controlComponentShufflePayloadEntity = new ControlComponentShufflePayloadEntity(
					electionEventId, ballotBoxId, nodeId, payload);

			controlComponentShufflePayloadRepository.save(controlComponentShufflePayloadEntity);
		} catch (final JsonProcessingException ex) {
			throw new UncheckedIOException("Failed to process the mixing DTO", ex);
		}
	}

	public int countMixDecryptOnlinePayloads(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		return controlComponentShufflePayloadRepository.countByElectionEventIdAndBallotBoxId(electionEventId, ballotBoxId);
	}

	public Optional<MixDecryptOnlineRawPayload> getMixDecryptOnlineRawPayload(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		LOGGER.debug("Retrieving MixDecryptOnlineRawPayload for ballot box. [ballotBoxId: {}]", ballotBoxId);

		final ImmutableList<ControlComponentBallotBoxPayloadEntity> controlComponentBallotBoxPayloadEntities =
				controlComponentBallotBoxPayloadRepository.findByElectionEventIdAndBallotBoxIdOrderByNodeId(electionEventId, ballotBoxId).stream()
						.collect(toImmutableList());

		LOGGER.info("Retrieved ControlComponentBallotBoxPayloadEntities for ballot box. [ballotBoxId: {}]", ballotBoxId);

		if (controlComponentBallotBoxPayloadEntities.size() != ControlComponentNode.ids().size()) {
			return Optional.empty();
		}

		LOGGER.debug("Retrieving ControlComponentShufflePayloadEntities for ballot box. [ballotBoxId: {}]", ballotBoxId);

		final ImmutableList<ControlComponentShufflePayloadEntity> shufflePayloadEntities =
				controlComponentShufflePayloadRepository.findByElectionEventIdAndBallotBoxIdOrderByNodeId(electionEventId, ballotBoxId).stream()
						.collect(toImmutableList());

		LOGGER.info("Retrieved ControlComponentShufflePayloadEntities for ballot box. [ballotBoxId: {}]", ballotBoxId);

		if (shufflePayloadEntities.size() != ControlComponentNode.ids().size()) {
			return Optional.empty();
		}

		return Optional.of(
				createMixDecryptOnlineRawPayload(electionEventId, ballotBoxId, controlComponentBallotBoxPayloadEntities, shufflePayloadEntities));
	}

	public ImmutableList<ControlComponentShufflePayload> getControlComponentShufflePayloadsOrderByNodeId(final String electionEventId,
			final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		return controlComponentShufflePayloadRepository.findByElectionEventIdAndBallotBoxIdOrderByNodeId(electionEventId, ballotBoxId)
				.stream()
				.map(ControlComponentShufflePayloadEntity::getShufflePayload)
				.map(ImmutableByteArray::elements)
				.map(bytes -> {
					try {
						return objectMapper.readValue(bytes, ControlComponentShufflePayload.class);
					} catch (final IOException e) {
						throw new UncheckedIOException("Couldn't deserialize a ControlComponentShufflePayload", e);
					}
				})
				.collect(toImmutableList());
	}

	public ImmutableList<ControlComponentVotesHashPayload> getControlComponentVotesHashPayloads(final String electionEventId,
			final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		return controlComponentVotesHashPayloadRepository.findByElectionEventIdAndBallotBoxId(electionEventId, ballotBoxId).stream()
				.map(ControlComponentVotesHashPayloadEntity::getControlComponentVotesHashPayload)
				.map(ImmutableByteArray::elements)
				.map(bytes -> {
					try {
						return objectMapper.readValue(bytes, ControlComponentVotesHashPayload.class);
					} catch (final IOException e) {
						throw new UncheckedIOException("Couldn't deserialize a ControlComponentVotesHashPayload", e);
					}
				})
				.collect(toImmutableList());
	}

	private MixDecryptOnlineRawPayload createMixDecryptOnlineRawPayload(final String electionEventId, final String ballotBoxId,
			final ImmutableList<ControlComponentBallotBoxPayloadEntity> controlComponentBallotBoxPayloadEntities,
			final ImmutableList<ControlComponentShufflePayloadEntity> shufflePayloadEntities) {

		LOGGER.debug("Creating MixDecryptOnlineRawPayload for ballot box. [ballotBoxId: {}]", ballotBoxId);

		final ImmutableList<ImmutableByteArray> controlComponentBallotBoxRawPayloads = controlComponentBallotBoxPayloadEntities.stream()
				.map(ControlComponentBallotBoxPayloadEntity::getControlComponentBallotBoxPayload)
				.collect(toImmutableList());

		final ImmutableList<ImmutableByteArray> controlComponentShuffleRawPayloads = shufflePayloadEntities.stream()
				.map(ControlComponentShufflePayloadEntity::getShufflePayload)
				.collect(toImmutableList());

		final MixDecryptOnlineRawPayload mixDecryptOnlineRawPayload = new MixDecryptOnlineRawPayload(electionEventId, ballotBoxId,
				controlComponentBallotBoxRawPayloads, controlComponentShuffleRawPayloads);

		LOGGER.info("Created MixDecryptOnlineRawPayload for ballot box. [ballotBoxId: {}]", ballotBoxId);

		return mixDecryptOnlineRawPayload;

	}

}
