/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.generateenclongcodeshares;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;

@Service
public class EncryptedLongReturnCodeSharesService {

	private final ObjectMapper objectMapper;
	private final VerificationCardSetService verificationCardSetService;
	private final EncryptedLongReturnCodeSharesRepository encryptedLongReturnCodeSharesRepository;

	public EncryptedLongReturnCodeSharesService(final ObjectMapper objectMapper,
			final VerificationCardSetService verificationCardSetService,
			final EncryptedLongReturnCodeSharesRepository encryptedLongReturnCodeSharesRepository) {
		this.objectMapper = objectMapper;
		this.verificationCardSetService = verificationCardSetService;
		this.encryptedLongReturnCodeSharesRepository = encryptedLongReturnCodeSharesRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final int chunkId, final String verificationCardSetId,
			final ImmutableList<ControlComponentCodeShare> controlComponentCodeShares) {
		checkArgument(chunkId >= 0, "The chunkId must be positive.");
		validateUUID(verificationCardSetId);
		checkNotNull(controlComponentCodeShares);

		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSet(verificationCardSetId);

		final ImmutableByteArray serializedControlComponentCodeShares;
		try {
			serializedControlComponentCodeShares = new ImmutableByteArray(objectMapper.writeValueAsBytes(controlComponentCodeShares));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize control component code shares.", e);
		}

		final EncryptedLongReturnCodeSharesEntity encryptedLongReturnCodeSharesEntity = new EncryptedLongReturnCodeSharesEntity(chunkId,
				verificationCardSetEntity, serializedControlComponentCodeShares);

		encryptedLongReturnCodeSharesRepository.save(encryptedLongReturnCodeSharesEntity);
	}

	@Transactional // Required due to the lazy loading of entities.
	public ImmutableList<ControlComponentCodeShare> getControlComponentCodeShares(final int chunkId, final String verificationCardSetId) {
		checkArgument(chunkId >= 0, "The chunkId must be positive.");
		validateUUID(verificationCardSetId);

		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSet(verificationCardSetId);
		final GqGroup encryptionGroup = verificationCardSetEntity.getElectionEventEntity().getEncryptionGroup();

		final EncryptedLongReturnCodeSharesEntityKey id = new EncryptedLongReturnCodeSharesEntityKey(chunkId, verificationCardSetId);
		final EncryptedLongReturnCodeSharesEntity encryptedLongReturnCodeSharesEntity = encryptedLongReturnCodeSharesRepository.findById(id)
				.orElseThrow(
						() -> new IllegalStateException(
								String.format("Encrypted long Return Code shares entity not found. [chunkId: %s, verificationCardSetId: %s]", chunkId,
										verificationCardSetId)));

		final ImmutableByteArray controlComponentCodeSharesBytes = encryptedLongReturnCodeSharesEntity.getControlComponentCodeShares();
		final ImmutableList<ControlComponentCodeShare> controlComponentCodeShares;
		try {
			controlComponentCodeShares = ImmutableList.of(objectMapper.reader()
					.withAttribute("group", encryptionGroup)
					.readValue(controlComponentCodeSharesBytes.elements(), ControlComponentCodeShare[].class));
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize control component code shares. [chunkId: %s, verificationCardSetId: %s]",
							chunkId, verificationCardSetId), e);
		}

		return controlComponentCodeShares;
	}
}
