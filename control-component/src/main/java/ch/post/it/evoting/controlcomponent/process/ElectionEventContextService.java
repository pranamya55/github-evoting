/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class ElectionEventContextService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventContextService.class);

	private final BallotBoxService ballotBoxService;
	private final ElectionEventService electionEventService;
	private final ElectionContextRepository electionContextRepository;
	private final VerificationCardSetService verificationCardSetService;

	public ElectionEventContextService(
			final BallotBoxService ballotBoxService,
			final ElectionEventService electionEventService,
			final ElectionContextRepository electionContextRepository,
			final VerificationCardSetService verificationCardSetService) {
		this.ballotBoxService = ballotBoxService;
		this.electionEventService = electionEventService;
		this.electionContextRepository = electionContextRepository;
		this.verificationCardSetService = verificationCardSetService;
	}

	@Transactional
	public void save(final ElectionEventContext electionEventContext) {
		checkNotNull(electionEventContext);

		// Save election event context entity.
		final String electionEventId = electionEventContext.electionEventId();
		final ElectionEventEntity electionEventEntity = electionEventService.getElectionEventEntity(electionEventId);
		final ElectionContextEntity electionContextEntity = new ElectionContextEntity.Builder()
				.setElectionEventEntity(electionEventEntity)
				.setElectionEventAlias(electionEventContext.electionEventAlias())
				.setElectionEventDescription(electionEventContext.electionEventDescription())
				.setStartTime(electionEventContext.startTime())
				.setFinishTime(electionEventContext.finishTime())
				.setMaxNumberOfVotingOptions(electionEventContext.maximumNumberOfVotingOptions())
				.setMaxNumberOfSelections(electionEventContext.maximumNumberOfSelections())
				.setMaxNumberOfWriteInsPlusOne(electionEventContext.maximumNumberOfWriteInsPlusOne())
				.setVotesTexts(electionEventContext.votesTexts())
				.setElectionsTexts(electionEventContext.electionsTexts())
				.build();
		electionContextRepository.save(electionContextEntity);
		LOGGER.info("Saved election context entity. [electionEventId: {}]", electionEventId);

		verificationCardSetService.saveFromContext(electionEventContext);
		LOGGER.info("Saved verification card set entities. [electionEventId: {}]", electionEventId);

		// Save ballot box entities.
		ballotBoxService.saveAllFromContexts(electionEventContext.verificationCardSetContexts());
		LOGGER.info("Saved ballot box entities. [electionEventId: {}]", electionEventId);
	}

	public ElectionEventContext getElectionEventContext(final String electionEventId) {
		validateUUID(electionEventId);

		final ElectionContextEntity electionContextEntity = getElectionContextEntity(electionEventId);

		final ImmutableList<VerificationCardSetContext> verificationCardSetContexts = Flux.fromIterable(
						verificationCardSetService.findAllByElectionEventId(
								electionEventId))
				.publishOn(Schedulers.boundedElastic())
				.map(verificationCardSetEntity -> {
					final String verificationCardSetId = verificationCardSetEntity.getVerificationCardSetId();
					final BallotBoxEntity ballotBoxEntity = ballotBoxService.getBallotBoxByVerificationCardSetId(verificationCardSetId);
					final PrimesMappingTable primesMappingTable = ballotBoxService.getPrimesMappingTableByVerificationCardSetId(
							verificationCardSetId);
					return new VerificationCardSetContext.Builder()
							.setVerificationCardSetId(verificationCardSetId)
							.setVerificationCardSetAlias(verificationCardSetEntity.getVerificationCardSetAlias())
							.setVerificationCardSetDescription(verificationCardSetEntity.getVerificationCardSetDescription())
							.setBallotBoxId(ballotBoxEntity.getBallotBoxId())
							.setBallotBoxStartTime(ballotBoxEntity.getBallotBoxStartTime())
							.setBallotBoxFinishTime(ballotBoxEntity.getBallotBoxFinishTime())
							.setTestBallotBox(ballotBoxEntity.isTestBallotBox())
							.setNumberOfEligibleVoters(ballotBoxEntity.getNumberOfEligibleVoters())
							.setGracePeriod(ballotBoxEntity.getGracePeriod())
							.setPrimesMappingTable(primesMappingTable)
							.setDomainsOfInfluence(verificationCardSetEntity.getDomainsOfInfluence())
							.build();
				})
				.collect(toImmutableList())
				.block();

		return new ElectionEventContext(electionEventId, electionContextEntity.getElectionEventAlias(),
				electionContextEntity.getElectionEventDescription(), verificationCardSetContexts, electionContextEntity.getStartTime(),
				electionContextEntity.getFinishTime(), electionContextEntity.getMaxNumberOfVotingOptions(),
				electionContextEntity.getMaxNumberOfSelections(), electionContextEntity.getMaxNumberOfWriteInsPlusOne(),
				electionContextEntity.getVotesTexts(), electionContextEntity.getElectionsTexts());
	}

	public LocalDateTime getElectionEventStartTime(final String electionEventId) {
		validateUUID(electionEventId);

		return getElectionContextEntity(electionEventId).getStartTime();
	}

	public LocalDateTime getElectionEventFinishTime(final String electionEventId) {
		validateUUID(electionEventId);

		return getElectionContextEntity(electionEventId).getFinishTime();
	}

	private ElectionContextEntity getElectionContextEntity(final String electionEventId) {
		validateUUID(electionEventId);

		final Optional<ElectionContextEntity> electionContextEntity = electionContextRepository.findById(electionEventId);

		return electionContextEntity.orElseThrow(
				() -> new IllegalStateException(String.format("Election context entity not found. [electionEventId: %s]", electionEventId)));
	}
}
