/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

@Service
public class HashedLVCCSharesService {

	private final ObjectMapper objectMapper;
	private final ElectionEventService electionEventService;
	private final VerificationCardService verificationCardService;
	private final HashedLVCCSharesRepository hashedLVCCSharesRepository;

	public HashedLVCCSharesService(
			final ObjectMapper objectMapper,
			final ElectionEventService electionEventService,
			final VerificationCardService verificationCardService,
			final HashedLVCCSharesRepository hashedLVCCSharesRepository) {
		this.objectMapper = objectMapper;
		this.electionEventService = electionEventService;
		this.verificationCardService = verificationCardService;
		this.hashedLVCCSharesRepository = hashedLVCCSharesRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final String verificationCardId, final ImmutableList<ControlComponenthlVCCSharePayload> controlComponenthlVCCPayloads,
			final boolean isVerified) {
		validateUUID(verificationCardId);
		checkNotNull(controlComponenthlVCCPayloads);
		checkArgument(controlComponenthlVCCPayloads.size() == ControlComponentNode.ids().size(),
				"There must be %s hashed Long Vote Cast Return Code shares.", ControlComponentNode.ids().size());

		final ImmutableList<String> hashedLVCCShares = controlComponenthlVCCPayloads.stream()
				.map(ControlComponenthlVCCSharePayload::getHashLongVoteCastReturnCodeShare)
				.collect(toImmutableList());

		final ImmutableByteArray serializedHashedLVCCShares;
		try {
			serializedHashedLVCCShares = new ImmutableByteArray(objectMapper.writeValueAsBytes(hashedLVCCShares));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException("Failed to serialize hashed Long Vote Cast Return Code shares.", e);
		}

		final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntity(verificationCardId);
		final HashedLVCCSharesEntity hashedLVCCSharesEntity = hashedLVCCSharesRepository.findById(verificationCardId)
				.orElseGet(() -> {
					final HashedLVCCSharesEntity result = new HashedLVCCSharesEntity();
					result.setVerificationCardEntity(verificationCardEntity);
					return result;
				});
		hashedLVCCSharesEntity.setHashedLongVoteCastReturnCodeShares(serializedHashedLVCCShares);
		hashedLVCCSharesEntity.setIsVerified(isVerified);

		hashedLVCCSharesRepository.save(hashedLVCCSharesEntity);
	}

	@Transactional // Required due to the lazy loading of entities.
	public ImmutableList<String> getHashedLVCCShares(final String verificationCardId) {
		validateUUID(verificationCardId);

		final HashedLVCCSharesEntity hashedLVCCSharesEntity = load(verificationCardId);

		final VerificationCardSetEntity verificationCardSetEntity = hashedLVCCSharesEntity.getVerificationCardEntity()
				.getVerificationCardSetEntity();
		final String electionEventId = verificationCardSetEntity.getElectionEventEntity().getElectionEventId();

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);

		return deserializeHashedLVCCShares(encryptionGroup, hashedLVCCSharesEntity.getHashedLongVoteCastReturnCodeShares());
	}

	@Transactional // Required due to the lazy loading of entities.
	public boolean isLVCCHashVerified(final String verificationCardId) {
		validateUUID(verificationCardId);

		final HashedLVCCSharesEntity hashedLVCCSharesEntity = load(verificationCardId);

		return hashedLVCCSharesEntity.isVerified();
	}

	private HashedLVCCSharesEntity load(final String verificationCardId) {
		return hashedLVCCSharesRepository.findById(verificationCardId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Hashed Long Vote Cast Return Code shares not found. [verificationCardId: %s]", verificationCardId)));
	}

	private ImmutableList<String> deserializeHashedLVCCShares(final GqGroup encryptionGroup, final ImmutableByteArray serializedHashedLVCCShares) {
		final ObjectReader reader = objectMapper.reader().withAttribute("group", encryptionGroup);

		final ImmutableList<String> hashedLVCCShares;
		try {
			hashedLVCCShares = Arrays.stream(reader.readValue(serializedHashedLVCCShares.elements(), String[].class)).collect(toImmutableList());
		} catch (final IOException e) {
			throw new UncheckedIOException("Failed to deserialize hashed Long Vote Cast Return Code shares.", e);
		}
		return hashedLVCCShares;
	}

}
