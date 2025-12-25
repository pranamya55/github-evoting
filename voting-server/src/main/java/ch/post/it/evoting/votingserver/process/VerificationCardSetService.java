/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;

@Service
public class VerificationCardSetService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardSetService.class);

	private final ElectionEventService electionEventService;
	private final VerificationCardSetRepository verificationCardSetRepository;

	public VerificationCardSetService(
			final ElectionEventService electionEventService,
			final VerificationCardSetRepository verificationCardSetRepository) {
		this.electionEventService = electionEventService;
		this.verificationCardSetRepository = verificationCardSetRepository;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void saveAllFromContext(final String electionEventId, final ImmutableList<VerificationCardSetContext> verificationCardSetContexts) {
		validateUUID(electionEventId);
		checkNotNull(verificationCardSetContexts);

		final ElectionEventEntity electionEventEntity = electionEventService.retrieveElectionEventEntity(electionEventId);

		final ImmutableList<VerificationCardSetEntity> verificationCardSetEntities = verificationCardSetContexts.stream()
				.map(verificationCardSetContext -> {
					final String verificationCardSetId = verificationCardSetContext.getVerificationCardSetId();

					LOGGER.info(
							"Successfully created verification card set. [verificationCardSetId: {}, description: {}]",
							verificationCardSetId,
							verificationCardSetContext.getVerificationCardSetDescription());
					return new VerificationCardSetEntity.Builder()
							.setVerificationCardSetId(verificationCardSetId)
							.setVerificationCardSetDescription(verificationCardSetContext.getVerificationCardSetDescription())
							.setVerificationCardSetAlias(verificationCardSetContext.getVerificationCardSetAlias())
							.setDomainsOfInfluence(verificationCardSetContext.getDomainsOfInfluence())
							.setElectionEventEntity(electionEventEntity)
							.build();

				})
				.collect(toImmutableList());

		verificationCardSetRepository.saveAll(verificationCardSetEntities);
		LOGGER.info("Successfully saved verification card sets. [electionEventId: {}]", electionEventId);
	}

	public VerificationCardSetEntity getVerificationCardSetEntity(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		return verificationCardSetRepository.findById(verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Verification card set not found. [verificationCardSetId: %s]", verificationCardSetId)));
	}
}
