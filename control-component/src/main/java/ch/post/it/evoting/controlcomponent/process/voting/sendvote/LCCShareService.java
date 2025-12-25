/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.voting.sendvote;

import static com.google.common.base.Preconditions.checkArgument;
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
import ch.post.it.evoting.controlcomponent.protocol.voting.sendvote.CreateLCCShareOutput;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.domain.voting.sendvote.LongChoiceReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@Service
public class LCCShareService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LCCShareService.class);

	private final ObjectMapper objectMapper;
	private final VerificationCardService verificationCardService;
	private final LCCShareRepository lccShareRepository;

	public LCCShareService(
			final ObjectMapper objectMapper,
			final VerificationCardService verificationCardService,
			final LCCShareRepository lccShareRepository) {
		this.objectMapper = objectMapper;
		this.verificationCardService = verificationCardService;
		this.lccShareRepository = lccShareRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final ContextIds contextIds, final CreateLCCShareOutput createLCCShareOutput) {
		checkNotNull(contextIds);
		checkNotNull(createLCCShareOutput);

		final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntity(contextIds.verificationCardId());

		final ImmutableByteArray longChoiceReturnCodeShareBytes;
		try {
			longChoiceReturnCodeShareBytes = new ImmutableByteArray(objectMapper.writeValueAsBytes(createLCCShareOutput.longChoiceReturnCodeShare()));
		} catch (final JsonProcessingException e) {
			throw new UncheckedIOException(String.format("Failed to serialize long Choice Return Code share. [contextId: %s]", contextIds), e);
		}

		final LCCShareEntity lccShareEntity = new LCCShareEntity(verificationCardEntity, longChoiceReturnCodeShareBytes);
		lccShareRepository.save(lccShareEntity);

		LOGGER.debug("Long Choice Return Code share saved. [contextIds: {}]", contextIds);
	}

	@Transactional // Required due to the lazy loading of entities.
	public LongChoiceReturnCodeShare getLongChoiceReturnCodeShare(final ContextIds contextIds, final int nodeId) {
		checkNotNull(contextIds);
		checkArgument(ControlComponentNode.ids().contains(nodeId), "The node id must be part of the known node ids. [nodeId: %s]", nodeId);

		final String verificationCardId = contextIds.verificationCardId();

		final LCCShareEntity lccShareEntity = lccShareRepository.findById(verificationCardId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Long Choice Return Code share not found. [verificationCardId: %s]", verificationCardId)));
		final GqGroup encryptionGroup = lccShareEntity.getVerificationCardEntity()
				.getVerificationCardSetEntity()
				.getElectionEventEntity()
				.getEncryptionGroup();

		LOGGER.debug("Long Choice Return Code share retrieved. [contextIds: {}]", verificationCardId);

		final GroupVector<GqElement, GqGroup> longChoiceReturnCodeShare;
		try {
			longChoiceReturnCodeShare = GroupVector.from(
					ImmutableList.of(
							objectMapper.reader()
									.withAttribute("group", encryptionGroup)
									.readValue(lccShareEntity.getLongChoiceReturnCodeShare().elements(), GqElement[].class)
					)
			);
		} catch (final IOException e) {
			throw new UncheckedIOException(
					String.format("Failed to deserialize long Choice Return Code share. [verificationCardId: %s]", verificationCardId), e);
		}

		return new LongChoiceReturnCodeShare(contextIds.electionEventId(), contextIds.verificationCardSetId(), verificationCardId, nodeId,
				longChoiceReturnCodeShare);
	}

}
