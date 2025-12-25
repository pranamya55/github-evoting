/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
public class LVCCAllowListEntryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LVCCAllowListEntryService.class);

	private final LVCCAllowListEntryRepository lvccAllowListEntryRepository;
	private final int batchSize;

	@PersistenceContext
	private EntityManager entityManager;

	public LVCCAllowListEntryService(
			final LVCCAllowListEntryRepository lvccAllowListEntryRepository,
			@Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
			final int batchSize) {
		this.lvccAllowListEntryRepository = lvccAllowListEntryRepository;
		this.batchSize = batchSize;
	}

	@Transactional
	public void saveAll(final ImmutableList<LVCCAllowListEntryEntity> castReturnCodeEntities) {
		checkNotNull(castReturnCodeEntities);
		checkArgument(!castReturnCodeEntities.isEmpty());

		final Iterator<LVCCAllowListEntryEntity> iterator = castReturnCodeEntities.iterator();

		// Save the list of cast return codes in batches
		List<LVCCAllowListEntryEntity> entities = new ArrayList<>(batchSize);
		while (iterator.hasNext()) {
			final LVCCAllowListEntryEntity longVoteCastReturnCode = iterator.next();
			entities.add(longVoteCastReturnCode);
			if (entities.size() == batchSize || !iterator.hasNext()) {
				lvccAllowListEntryRepository.saveAll(entities);
				entityManager.flush();
				entityManager.clear();
				entities = new ArrayList<>(batchSize);
			}
		}

		LOGGER.info("Long vote cast return codes successfully saved. [verificationCardSetId: {}]",
				castReturnCodeEntities.get(0).getVerificationCardSetEntity().getVerificationCardSetId());
	}

	/**
	 * Gets the long Vote Cast Return Codes allow list for the given verification card set id.
	 *
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the lVCC allow list.
	 */
	public ImmutableList<String> getLongVoteCastReturnCodes(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		return lvccAllowListEntryRepository.findAllByVerificationCardSetId(verificationCardSetId).stream()
				.map(LVCCAllowListEntryEntity::getLongVoteCastReturnCode)
				.collect(toImmutableList());
	}

	public boolean exists(final String verificationCardSetId, final String longVoteCastReturnCode) {
		return lvccAllowListEntryRepository.existsByLongVoteCastReturnCode(verificationCardSetId, longVoteCastReturnCode);
	}
}
