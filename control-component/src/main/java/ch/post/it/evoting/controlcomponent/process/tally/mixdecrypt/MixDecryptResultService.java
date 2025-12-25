/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.process.ElectionEventEntity;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.protocol.tally.mixonline.MixDecOnlineOutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.VerifiableShuffle;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.VerifiableDecryptions;

@Service
public class MixDecryptResultService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixDecryptResultService.class);

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final MixDecryptResultRepository mixDecryptResultRepository;

	public MixDecryptResultService(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final MixDecryptResultRepository mixDecryptResultRepository) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.mixDecryptResultRepository = mixDecryptResultRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final String electionEventId, final String ballotBoxId, final MixDecOnlineOutput mixDecOnlineOutput) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(mixDecOnlineOutput);

		final ImmutableByteArray serializedVerifiableShuffle;
		final ImmutableByteArray serializedVerifiableDecryptions;
		try {
			serializedVerifiableShuffle = new ImmutableByteArray(objectMapper.writeValueAsBytes(mixDecOnlineOutput.verifiableShuffle()));
			serializedVerifiableDecryptions = new ImmutableByteArray(objectMapper.writeValueAsBytes(mixDecOnlineOutput.verifiableDecryptions()));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize the verifiable shuffle and decryptions. [electionEventId: %s, ballotBoxId: %s]",
							electionEventId, ballotBoxId), e);
		}

		final ElectionEventEntity electionEventEntity = electionEventService.getElectionEventEntity(electionEventId);

		final MixDecryptResultEntity mixDecryptResultEntity = new MixDecryptResultEntity(ballotBoxId, electionEventEntity,
				serializedVerifiableShuffle, serializedVerifiableDecryptions);

		mixDecryptResultRepository.save(mixDecryptResultEntity);

		LOGGER.debug("Mix/decrypt result entity saved. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);
	}

	public VerifiableShuffle getVerifiableShuffle(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final MixDecryptResultEntity mixDecryptResultEntity = getMixDecryptResultEntity(electionEventId, ballotBoxId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		try {
			return objectMapper.reader()
					.withAttribute("group", encryptionGroup)
					.readValue(mixDecryptResultEntity.getVerifiableShuffle().elements(), VerifiableShuffle.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize verifiable shuffle. [electionEventId: %s, ballotBoxId: %s]", electionEventId,
							ballotBoxId), e);
		}
	}

	public VerifiableDecryptions getVerifiableDecryptions(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final MixDecryptResultEntity mixDecryptResultEntity = getMixDecryptResultEntity(electionEventId, ballotBoxId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		try {
			return objectMapper.reader()
					.withAttribute("group", encryptionGroup)
					.readValue(mixDecryptResultEntity.getVerifiableDecryptions().elements(), VerifiableDecryptions.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize verifiable decryptions. [electionEventId: %s, ballotBoxId: %s]", electionEventId,
							ballotBoxId), e);
		}
	}

	private MixDecryptResultEntity getMixDecryptResultEntity(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final MixDecryptResultEntity mixDecryptResultEntity = mixDecryptResultRepository.findByElectionEventIdAndBallotBoxId(
						electionEventId, ballotBoxId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Mix net initial ciphertexts entity not found. [electionEventId: %s, ballotBoxId: %s]", electionEventId,
								ballotBoxId)));

		LOGGER.debug("Mix/decrypt result entity retrieved. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		return mixDecryptResultEntity;
	}

}
