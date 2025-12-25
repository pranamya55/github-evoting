/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

@Service
public class PCCAllowListEntryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PCCAllowListEntryService.class);

	private final PCCAllowListEntryRepository pccAllowListEntryRepository;
	private final int batchSize;

	@PersistenceContext
	private EntityManager entityManager;

	public PCCAllowListEntryService(
			final PCCAllowListEntryRepository pccAllowListEntryRepository,
			@Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
			final int batchSize) {
		this.pccAllowListEntryRepository = pccAllowListEntryRepository;
		this.batchSize = batchSize;
	}

	@Transactional
	public void saveAll(final ImmutableList<PCCAllowListEntryEntity> pccAllowListEntryEntities) {
		checkNotNull(pccAllowListEntryEntities);
		checkArgument(!pccAllowListEntryEntities.isEmpty());

		final Iterator<PCCAllowListEntryEntity> iterator = pccAllowListEntryEntities.iterator();

		// Save the list of cast return codes in batches
		List<PCCAllowListEntryEntity> entities = new ArrayList<>(batchSize);
		while (iterator.hasNext()) {
			final PCCAllowListEntryEntity partialChoiceReturnCode = iterator.next();
			entities.add(partialChoiceReturnCode);
			if (entities.size() == batchSize || !iterator.hasNext()) {
				pccAllowListEntryRepository.saveAll(entities);
				entityManager.flush();
				entityManager.clear();
				entities = new ArrayList<>(batchSize);
			}
		}

		final int chunkId = pccAllowListEntryEntities.get(0).getChunkId();
		LOGGER.debug("Saved pcc allow list chunk entity. [chunkId: {}]", chunkId);
	}

	/**
	 * Gets the partial Choice Return Codes allow list for the given verification card set id.
	 *
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the pCC allow list.
	 */
	public ImmutableList<String> getPartialChoiceReturnCodes(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		return getPCCAllowListEntries(verificationCardSetId).stream()
				.map(PCCAllowListEntryEntity::getPartialChoiceReturnCode)
				.collect(toImmutableList());
	}

	/**
	 * Gets the list of {@link PCCAllowListEntryEntity} for the given verification card set id.
	 *
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the list of {@link PCCAllowListEntryEntity}.
	 */
	public ImmutableList<PCCAllowListEntryEntity> getPCCAllowListEntries(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		final ImmutableList<PCCAllowListEntryEntity> pccAllowList = ImmutableList.from(
				pccAllowListEntryRepository.findAllByVerificationCardSetId(verificationCardSetId));
		checkState(!pccAllowList.isEmpty(), "No pCC allow list found. [verificationCardSetId: %s]", verificationCardSetId);
		LOGGER.debug("Loaded pCC allow list. [verificationCardSetId: {}, size: {}]", verificationCardSetId, pccAllowList.size());

		return pccAllowList;
	}

	public boolean exists(final String verificationCardSetId, final String longVoteCastReturnCode) {
		return pccAllowListEntryRepository.existsByPartialChoiceReturnCode(verificationCardSetId, longVoteCastReturnCode);
	}
}
