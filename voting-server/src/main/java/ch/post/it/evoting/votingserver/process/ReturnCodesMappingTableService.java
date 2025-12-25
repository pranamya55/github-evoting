/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayload;

@Service
public class ReturnCodesMappingTableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReturnCodesMappingTableService.class);

	private final VerificationCardSetService verificationCardSetService;
	private final ReturnCodesMappingTableRepository returnCodesMappingTableRepository;
	private final int batchSize;

	@PersistenceContext
	private EntityManager entityManager;

	public ReturnCodesMappingTableService(
			final VerificationCardSetService verificationCardSetService,
			final ReturnCodesMappingTableRepository returnCodesMappingTableRepository,
			@Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
			final int batchSize) {
		this.verificationCardSetService = verificationCardSetService;
		this.returnCodesMappingTableRepository = returnCodesMappingTableRepository;
		this.batchSize = batchSize;
	}

	/**
	 * Saves the return codes mapping table.
	 *
	 * @param setupComponentCMTablePayload the request payload. Must be non-null.
	 * @throws UncheckedIOException if an error occurs while serializing the return code mapping table.
	 */
	@Transactional
	public void save(final SetupComponentCMTablePayload setupComponentCMTablePayload) {
		checkNotNull(setupComponentCMTablePayload);

		final String electionEventId = setupComponentCMTablePayload.getElectionEventId();
		final String verificationCardSetId = setupComponentCMTablePayload.getVerificationCardSetId();
		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSetEntity(verificationCardSetId);

		final ImmutableMap<String, String> returnCodesMappingTable = setupComponentCMTablePayload.getReturnCodesMappingTable();
		final Iterator<ImmutableMap.Entry<String, String>> iterator = returnCodesMappingTable.entrySet().iterator();

		// Save the CMTable in batches
		List<ReturnCodesMappingTableEntryEntity> entities = new ArrayList<>(batchSize);
		while (iterator.hasNext()) {
			final ImmutableMap.Entry<String, String> entry = iterator.next();
			entities.add(new ReturnCodesMappingTableEntryEntity(verificationCardSetEntity, entry.key(), entry.value()));
			if (entities.size() == batchSize || !iterator.hasNext()) {
				returnCodesMappingTableRepository.saveAll(entities);
				entityManager.flush();
				entityManager.clear();
				entities = new ArrayList<>(batchSize);
			}
		}

		LOGGER.info("Return codes mapping table successfully saved. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]", electionEventId,
				verificationCardSetId, setupComponentCMTablePayload.getChunkId());
	}

	public Optional<String> getEncryptedShortReturnCode(final String verificationCardSetId, final String hashLongReturnCode) {
		validateUUID(verificationCardSetId);
		checkNotNull(hashLongReturnCode);

		return returnCodesMappingTableRepository.findByHashedLongReturnCode(verificationCardSetId, hashLongReturnCode);
	}

}
