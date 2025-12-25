/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.protocol.configuration.setuptally.SetupTallyCCMOutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;

@Service
public class CcmjElectionKeysService {

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final CcmjElectionKeysRepository ccmjElectionKeysRepository;

	public CcmjElectionKeysService(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final CcmjElectionKeysRepository ccmjElectionKeysRepository) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.ccmjElectionKeysRepository = ccmjElectionKeysRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public CcmjElectionKeysEntity save(final String electionEventId, final SetupTallyCCMOutput setupTallyCCMOutput) {
		validateUUID(electionEventId);
		checkNotNull(setupTallyCCMOutput);

		final ImmutableByteArray ccmjElectionKeyPairBytes;
		final ImmutableByteArray ccmjSchnorrProofsBytes;
		try {
			ccmjElectionKeyPairBytes = new ImmutableByteArray(objectMapper.writeValueAsBytes(setupTallyCCMOutput.getCcmjElectionKeyPair()));
			ccmjSchnorrProofsBytes = new ImmutableByteArray(objectMapper.writeValueAsBytes(setupTallyCCMOutput.getSchnorrProofs()));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(String.format("Failed to serialize ccmj election keys. [electionEventId: %s]", electionEventId), e);
		}

		final ElectionEventEntity electionEventEntity = electionEventService.getElectionEventEntity(electionEventId);
		final CcmjElectionKeysEntity entityToSave = new CcmjElectionKeysEntity(electionEventEntity, ccmjElectionKeyPairBytes, ccmjSchnorrProofsBytes);

		return ccmjElectionKeysRepository.save(entityToSave);
	}

	public ElGamalMultiRecipientKeyPair getCcmjElectionKeyPair(final String electionEventId) {
		validateUUID(electionEventId);

		final CcmjElectionKeysEntity ccmjElectionKeysEntity = getCcmjElectionKeysEntity(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		try {
			return objectMapper.reader()
					.withAttribute("group", encryptionGroup)
					.readValue(ccmjElectionKeysEntity.getCcmjElectionKeyPair().elements(), ElGamalMultiRecipientKeyPair.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize ccmj election key pair. [electionEventId: %s]", electionEventId), e);
		}
	}

	public GroupVector<SchnorrProof, ZqGroup> getCcmjSchnorrProofs(final String electionEventId) {
		validateUUID(electionEventId);

		final CcmjElectionKeysEntity ccmjElectionKeysEntity = getCcmjElectionKeysEntity(electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		try {
			return GroupVector.from(
					ImmutableList.of(
							objectMapper.reader()
									.withAttribute("group", encryptionGroup)
									.readValue(ccmjElectionKeysEntity.getCcmjSchnorrProofs().elements(), SchnorrProof[].class)
					)
			);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Failed to deserialize ccmj Schnorr Proofs. [electionEventId: %s]", electionEventId), e);
		}
	}

	private CcmjElectionKeysEntity getCcmjElectionKeysEntity(final String electionEventId) {
		validateUUID(electionEventId);
		return ccmjElectionKeysRepository.findById(electionEventId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("The ccmj election keys are missing. [electionEventId: %s]", electionEventId)));
	}

}
