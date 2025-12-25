/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;

@Service
public class VerificationCardSetService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardSetService.class);

	private final ElectionEventService electionEventService;
	private final PCCAllowListEntryService pccAllowListEntryService;
	private final VerificationCardSetRepository verificationCardSetRepository;
	private final LVCCAllowListEntryService lvccAllowListEntryService;

	public VerificationCardSetService(
			final ElectionEventService electionEventService,
			final PCCAllowListEntryService pccAllowListEntryService,
			final VerificationCardSetRepository verificationCardSetRepository,
			final LVCCAllowListEntryService lvccAllowListEntryService) {
		this.electionEventService = electionEventService;
		this.pccAllowListEntryService = pccAllowListEntryService;
		this.verificationCardSetRepository = verificationCardSetRepository;
		this.lvccAllowListEntryService = lvccAllowListEntryService;
	}

	@VisibleForTesting
	public VerificationCardSetEntity save(final VerificationCardSetEntity verificationCardSetEntity) {
		checkNotNull(verificationCardSetEntity);

		return verificationCardSetRepository.save(verificationCardSetEntity);
	}

	public VerificationCardSetEntity getVerificationCardSet(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		return verificationCardSetRepository.findById(verificationCardSetId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Verification card set not found. [verificationCardSetId: %s]", verificationCardSetId)));
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void saveFromContext(final ElectionEventContext electionEventContext) {
		checkNotNull(electionEventContext);

		final ElectionEventEntity electionEventEntity = electionEventService.getElectionEventEntity(electionEventContext.electionEventId());
		final ImmutableList<VerificationCardSetContext> verificationCardSetContexts = electionEventContext.verificationCardSetContexts();
		final ImmutableList<VerificationCardSetEntity> verificationCardSetEntities = verificationCardSetContexts.stream()
				.map(verificationCardSetContext -> new VerificationCardSetEntity.Builder()
						.setVerificationCardSetId(verificationCardSetContext.getVerificationCardSetId())
						.setVerificationCardSetAlias(verificationCardSetContext.getVerificationCardSetAlias())
						.setVerificationCardSetDescription(verificationCardSetContext.getVerificationCardSetDescription())
						.setDomainsOfInfluence(verificationCardSetContext.getDomainsOfInfluence())
						.setElectionEventEntity(electionEventEntity)
						.build())
				.collect(toImmutableList());

		verificationCardSetRepository.saveAll(verificationCardSetEntities);
	}

	/**
	 * Gets the partial Choice Return Codes allow list for the given verification card set id.
	 * <p>
	 * WARNING: This will not return the complete allow list if called before all chunks have been processed and saved.
	 *
	 * @param verificationCardSetId the verification card set id. Must be a valid UUID.
	 * @return the partial Choice Return Codes allow list.
	 */
	public PartialChoiceReturnCodeAllowList getPartialChoiceReturnCodesAllowList(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		return partialChoiceReturnCode -> pccAllowListEntryService.exists(verificationCardSetId, partialChoiceReturnCode);
	}

	/**
	 * Sets the given long vote cast return codes allow list into the verification card set corresponding to the given verification card set id.
	 *
	 * @param verificationCardSetId            the verification card set id. Must be non-null and a valid UUID.
	 * @param longVoteCastReturnCodesAllowList the long vote cast return codes allow list. Must be non-null.
	 */
	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void setLongVoteCastReturnCodesAllowList(final String verificationCardSetId,
			final ImmutableList<String> longVoteCastReturnCodesAllowList) {
		LOGGER.info("Updating verification card set with long vote cast return codes allow list... [verificationCardSetId: {}]",
				verificationCardSetId);

		validateUUID(verificationCardSetId);
		checkNotNull(longVoteCastReturnCodesAllowList);

		final VerificationCardSetEntity verificationCardSetEntity =
				verificationCardSetRepository.findById(verificationCardSetId)
						.orElseThrow(() -> new IllegalStateException(
								String.format("Could not find any matching verification card set [verificationCardSetId: %s]",
										verificationCardSetId)));

		final ImmutableList<LVCCAllowListEntryEntity> longVoteCastReturnCodeEntities = longVoteCastReturnCodesAllowList.stream().parallel()
				.map(castCode -> new LVCCAllowListEntryEntity(verificationCardSetEntity, castCode))
				.collect(toImmutableList());
		lvccAllowListEntryService.saveAll(longVoteCastReturnCodeEntities);

		LOGGER.info("Successfully updated verification card set with long vote cast return codes allow list. [verificationCardSetId: {}]",
				verificationCardSetId);
	}

	public ImmutableList<String> getLongVoteCastReturnCodesAllowList(final String verificationCardSetId) {
		validateUUID(verificationCardSetId);

		return lvccAllowListEntryService.getLongVoteCastReturnCodes(verificationCardSetId);
	}

	public ImmutableList<VerificationCardSetEntity> findAllByElectionEventId(final String electionEventId) {
		validateUUID(electionEventId);

		return ImmutableList.from(verificationCardSetRepository.findAllByElectionEventId(electionEventId));
	}

}
