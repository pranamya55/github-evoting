/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenKeysCCROutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;

@Service
public class CcrjReturnCodesKeysService {

	private static final String GROUP = "group";

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final CcrjReturnCodesKeysRepository ccrjReturnCodesKeysRepository;

	public CcrjReturnCodesKeysService(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final CcrjReturnCodesKeysRepository ccrjReturnCodesKeysRepository) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.ccrjReturnCodesKeysRepository = ccrjReturnCodesKeysRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public CcrjReturnCodesKeysEntity save(final String electionEventId, final GenKeysCCROutput genKeysCCROutput) {
		validateUUID(electionEventId);
		checkNotNull(genKeysCCROutput);

		final ElGamalMultiRecipientKeyPair ccrjChoiceReturnCodesEncryptionKeyPair = genKeysCCROutput.ccrjChoiceReturnCodesEncryptionKeyPair();
		final ZqElement ccrjReturnCodesGenerationSecretKey = genKeysCCROutput.ccrjReturnCodesGenerationSecretKey();
		final GroupVector<SchnorrProof, ZqGroup> ccrjSchnorrProofs = genKeysCCROutput.ccrjSchnorrProofs();

		final ImmutableByteArray ccrjChoiceReturnCodesEncryptionKeyPairBytes;
		final ImmutableByteArray ccrjReturnCodesGenerationSecretKeyBytes;
		final ImmutableByteArray ccrjSchnorrProofsBytes;
		try {
			ccrjChoiceReturnCodesEncryptionKeyPairBytes = new ImmutableByteArray(
					objectMapper.writeValueAsBytes(ccrjChoiceReturnCodesEncryptionKeyPair));
			ccrjReturnCodesGenerationSecretKeyBytes = new ImmutableByteArray(objectMapper.writeValueAsBytes(ccrjReturnCodesGenerationSecretKey));
			ccrjSchnorrProofsBytes = new ImmutableByteArray(objectMapper.writeValueAsBytes(ccrjSchnorrProofs));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(String.format("Failed to serialize ccrj Return Codes keys. [electionEventId: %s]", electionEventId), e);
		}

		final ElectionEventEntity electionEventEntity = electionEventService.getElectionEventEntity(electionEventId);
		final CcrjReturnCodesKeysEntity entityToSave = new CcrjReturnCodesKeysEntity(electionEventEntity, ccrjChoiceReturnCodesEncryptionKeyPairBytes,
				ccrjReturnCodesGenerationSecretKeyBytes, ccrjSchnorrProofsBytes);

		return ccrjReturnCodesKeysRepository.save(entityToSave);
	}

	@Cacheable("ccrjChoiceReturnCodesEncryptionKeyPair")
	public ElGamalMultiRecipientKeyPair getCcrjChoiceReturnCodesEncryptionKeyPair(final String electionEventId) {
		validateUUID(electionEventId);

		final CcrjReturnCodesKeysEntity ccrjReturnCodesKeysEntity = getCcrjReturnCodesKeysEntity(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		try {
			return objectMapper.reader()
					.withAttribute(GROUP, encryptionGroup)
					.readValue(ccrjReturnCodesKeysEntity.getCcrjChoiceReturnCodesEncryptionKeyPair().elements(), ElGamalMultiRecipientKeyPair.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize ccrj Choice Return Codes encryption key pair. [electionEventId: %s]", electionEventId), e);
		}
	}

	@Cacheable("ccrjReturnCodesGenerationSecretKey")
	public ZqElement getCcrjReturnCodesGenerationSecretKey(final String electionEventId) {
		validateUUID(electionEventId);

		final CcrjReturnCodesKeysEntity ccrjReturnCodesKeysEntity = getCcrjReturnCodesKeysEntity(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		try {
			return objectMapper.reader()
					.withAttribute(GROUP, encryptionGroup)
					.readValue(ccrjReturnCodesKeysEntity.getCcrjReturnCodesGenerationSecretKey().elements(), ZqElement.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize ccrj Return Codes generation secret key. [electionEventId: %s]", electionEventId), e);
		}
	}

	public GroupVector<SchnorrProof, ZqGroup> getCcrjSchnorrProofs(final String electionEventId) {
		validateUUID(electionEventId);

		final CcrjReturnCodesKeysEntity ccrjReturnCodesKeysEntity = getCcrjReturnCodesKeysEntity(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		try {
			return GroupVector.from(
					ImmutableList.of(
							objectMapper.reader()
									.withAttribute(GROUP, encryptionGroup)
									.readValue(ccrjReturnCodesKeysEntity.getCcrjSchnorrProofs().elements(), SchnorrProof[].class)
					)
			);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize ccrj Schnorr Proofs. [electionEventId: %s]", electionEventId), e);
		}
	}

	private CcrjReturnCodesKeysEntity getCcrjReturnCodesKeysEntity(final String electionEventId) {
		validateUUID(electionEventId);
		return ccrjReturnCodesKeysRepository.findById(electionEventId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("The ccrj Return Codes keys are missing. [electionEventId: %s]", electionEventId)));
	}

}
