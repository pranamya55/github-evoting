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
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixonline.GetMixnetInitialCiphertextsOutput;

@Service
public class MixnetInitialCiphertextsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MixnetInitialCiphertextsService.class);

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final MixnetInitialCiphertextsRepository mixnetInitialCiphertextsRepository;

	public MixnetInitialCiphertextsService(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final MixnetInitialCiphertextsRepository mixnetInitialCiphertextsRepository) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.mixnetInitialCiphertextsRepository = mixnetInitialCiphertextsRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final String electionEventId, final String ballotBoxId,
			final GetMixnetInitialCiphertextsOutput getMixnetInitialCiphertextsOutput) {

		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
		checkNotNull(getMixnetInitialCiphertextsOutput);

		final ImmutableByteArray serializedMixnetInitialCiphertexts;
		try {
			serializedMixnetInitialCiphertexts = new ImmutableByteArray(
					objectMapper.writeValueAsBytes(getMixnetInitialCiphertextsOutput.mixnetInitialCiphertexts()));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(
					String.format("Failed to serialize mix net initial ciphertexts. [electionEventId: %s, ballotBoxId: %s]", electionEventId,
							ballotBoxId), e);
		}

		final ElectionEventEntity electionEventEntity = electionEventService.getElectionEventEntity(electionEventId);

		final MixnetInitialCiphertextsEntity mixnetInitialCiphertextsEntity = new MixnetInitialCiphertextsEntity(ballotBoxId,
				electionEventEntity, getMixnetInitialCiphertextsOutput.encryptedConfirmedVotesHash(), serializedMixnetInitialCiphertexts);

		mixnetInitialCiphertextsRepository.save(mixnetInitialCiphertextsEntity);

		LOGGER.debug("Mix net initial ciphertexts entity saved. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);
	}

	public String getEncryptedConfirmedVotesHash(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		return getMixnetInitialCiphertextsEntity(electionEventId, ballotBoxId).getEncryptedConfirmedVotesHash();
	}

	public GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getMixnetInitialCiphertexts(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final MixnetInitialCiphertextsEntity mixnetInitialCiphertextsEntity = getMixnetInitialCiphertextsEntity(electionEventId, ballotBoxId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		try {
			final ElGamalMultiRecipientCiphertext[] mixnetInitialCiphertexts = objectMapper.reader()
					.withAttribute("group", encryptionGroup)
					.readValue(mixnetInitialCiphertextsEntity.getMixnetInitialCiphertexts().elements(), ElGamalMultiRecipientCiphertext[].class);

			LOGGER.debug("Mix net initial ciphertexts deserialized. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

			return GroupVector.from(ImmutableList.of(mixnetInitialCiphertexts));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize mix net initial ciphertexts. [electionEventId: %s, ballotBoxId: %s]", electionEventId,
							ballotBoxId), e);
		}
	}

	private MixnetInitialCiphertextsEntity getMixnetInitialCiphertextsEntity(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final MixnetInitialCiphertextsEntity mixnetInitialCiphertextsEntity = mixnetInitialCiphertextsRepository.findByElectionEventIdAndBallotBoxId(
						electionEventId, ballotBoxId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Mix net initial ciphertexts entity not found. [electionEventId: %s, ballotBoxId: %s]", electionEventId,
								ballotBoxId)));

		LOGGER.debug("Mix net initial ciphertexts entity retrieved. [electionEventId: {}, ballotBoxId: {}]", electionEventId, ballotBoxId);

		return mixnetInitialCiphertextsEntity;
	}

}
