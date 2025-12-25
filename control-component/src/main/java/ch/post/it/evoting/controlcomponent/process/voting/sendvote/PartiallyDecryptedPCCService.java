/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

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

import ch.post.it.evoting.controlcomponent.process.VerificationCardEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.voting.sendvote.PartiallyDecryptedEncryptedPCC;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@Service
public class PartiallyDecryptedPCCService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartiallyDecryptedPCCService.class);

	private final ObjectMapper objectMapper;
	private final VerificationCardService verificationCardService;
	private final PartiallyDecryptedPCCRepository partiallyDecryptedPCCRepository;

	public PartiallyDecryptedPCCService(
			final ObjectMapper objectMapper,
			final VerificationCardService verificationCardService,
			final PartiallyDecryptedPCCRepository partiallyDecryptedPCCRepository) {
		this.objectMapper = objectMapper;
		this.verificationCardService = verificationCardService;
		this.partiallyDecryptedPCCRepository = partiallyDecryptedPCCRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final PartiallyDecryptedEncryptedPCC partiallyDecryptedEncryptedPCC) {
		checkNotNull(partiallyDecryptedEncryptedPCC);

		final ContextIds contextIds = partiallyDecryptedEncryptedPCC.contextIds();

		final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntity(contextIds.verificationCardId());

		final ImmutableByteArray partiallyDecryptedEncryptedPCCBytes;
		try {
			partiallyDecryptedEncryptedPCCBytes = new ImmutableByteArray(objectMapper.writeValueAsBytes(partiallyDecryptedEncryptedPCC));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(String.format("Failed to serialize partially decrypted encrypted PCC. [contextId: %s]", contextIds), e);
		}

		final PartiallyDecryptedPCCEntity partiallyDecryptedPCCEntity = new PartiallyDecryptedPCCEntity(verificationCardEntity,
				partiallyDecryptedEncryptedPCCBytes);
		partiallyDecryptedPCCRepository.save(partiallyDecryptedPCCEntity);

		LOGGER.debug("Partially decrypted encrypted PCC saved. [contextIds: {}]", contextIds);
	}

	@Transactional // Required due to the lazy loading of entities.
	public PartiallyDecryptedEncryptedPCC get(final String verificationCardId) {
		validateUUID(verificationCardId);

		final PartiallyDecryptedPCCEntity partiallyDecryptedPCCEntity = partiallyDecryptedPCCRepository.findById(verificationCardId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Partially decrypted encrypted pcc not found. [verificationCardId: %s]", verificationCardId)));
		final GqGroup encryptionGroup = partiallyDecryptedPCCEntity.getVerificationCardEntity().getVerificationCardSetEntity()
				.getElectionEventEntity().getEncryptionGroup();

		LOGGER.debug("Partially decrypted encrypted PCC retrieved. [contextIds: {}]", verificationCardId);

		try {
			return objectMapper.reader()
					.withAttribute("group", encryptionGroup)
					.readValue(partiallyDecryptedPCCEntity.getPartiallyDecryptedEncryptedPCC().elements(), PartiallyDecryptedEncryptedPCC.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize partially decrypted encrypted PCC. [verificationCardId: %s]", verificationCardId), e);
		}
	}

}
