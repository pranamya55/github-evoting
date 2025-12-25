/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;

@DisplayName("ElectionContextServiceTest")
class ElectionEventContextServiceTest {

	private static final ElectionContextRepository electionContextRepository = mock(ElectionContextRepository.class);

	private static ElectionEventContextService electionEventContextService;
	private static ElectionEventContext electionEventContext;

	@BeforeAll
	static void setUpAll() {
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();

		final GqGroup encryptionGroup = GroupTestData.getGqGroup();
		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, encryptionGroup);

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

		final ImmutableMap<VerificationCardSetContext, VerificationCardSetEntity> verificationCardSets = electionEventContext.verificationCardSetContexts()
				.stream()
				.collect(toImmutableMap(
						Function.identity(),
						verificationCardSetContext -> new VerificationCardSetEntity.Builder()
								.setVerificationCardSetId(verificationCardSetContext.getVerificationCardSetId())
								.setVerificationCardSetAlias(verificationCardSetContext.getVerificationCardSetAlias())
								.setVerificationCardSetDescription(verificationCardSetContext.getVerificationCardSetDescription())
								.setDomainsOfInfluence(verificationCardSetContext.getDomainsOfInfluence())
								.setElectionEventEntity(electionEventEntity)
								.build())
				);
		final VerificationCardSetService verificationCardSetService = mock(VerificationCardSetService.class);
		doNothing().when(verificationCardSetService).saveFromContext(any());
		when(verificationCardSetService.findAllByElectionEventId(electionEventId)).thenReturn(verificationCardSets.values()
				.stream()
				.collect(toImmutableList()));

		final BallotBoxService ballotBoxService = mock(BallotBoxService.class);
		doNothing().when(ballotBoxService).saveAllFromContexts(any());
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		verificationCardSets.forEach((key, value) -> {
			final BallotBoxEntity ballotBoxEntity;
			try {
				ballotBoxEntity = new BallotBoxEntity.Builder()
						.setBallotBoxId(key.getBallotBoxId())
						.setVerificationCardSetEntity(value)
						.setBallotBoxStartTime(key.getBallotBoxStartTime())
						.setBallotBoxFinishTime(key.getBallotBoxFinishTime())
						.setTestBallotBox(key.isTestBallotBox())
						.setNumberOfEligibleVoters(key.getNumberOfEligibleVoters())
						.setGracePeriod(key.getGracePeriod())
						.setPrimesMappingTable(new ImmutableByteArray(objectMapper.writeValueAsBytes(key.getPrimesMappingTable())))
						.build();
			} catch (final JsonProcessingException e) {
				throw new RuntimeException(e);
			}
			when(ballotBoxService.getBallotBoxByVerificationCardSetId(value.getVerificationCardSetId())).thenReturn(ballotBoxEntity);
			when(ballotBoxService.getPrimesMappingTableByVerificationCardSetId(value.getVerificationCardSetId())).thenReturn(
					key.getPrimesMappingTable());
		});

		when(electionContextRepository.save(any())).thenReturn(electionContextEntity);
		when(electionContextRepository.findById(electionEventContext.electionEventId())).thenReturn(Optional.ofNullable(electionContextEntity));

		final ElectionEventService electionEventService = mock(ElectionEventService.class);
		when(electionEventService.getElectionEventEntity(electionEventId)).thenReturn(electionEventEntity);
		when(electionEventService.getEncryptionGroup(any())).thenReturn(encryptionGroup);

		electionEventContextService = new ElectionEventContextService(ballotBoxService, electionEventService, electionContextRepository,
				verificationCardSetService);
	}

	@Test
	@DisplayName("saving with valid ElectionEventContext does not throw")
	void savingValidElectionEventContextDoesNotThrow() {
		electionEventContextService.save(electionEventContext);
		verify(electionContextRepository, times(1)).save(any());

		final ElectionEventContext electionEventContextSaved = electionEventContextService.getElectionEventContext(
				electionEventContext.electionEventId());
		assertEquals(electionEventContext.electionEventAlias(), electionEventContextSaved.electionEventAlias());
	}

	@Test
	@DisplayName("loading non existing ElectionEventContext throws IllegalStateException")
	void loadNonExistingThrows() {
		final String nonExistingElectionId = "E77DBE3C70874EA584C490A0C6AC0CA4";
		final IllegalStateException exceptionCcm = assertThrows(IllegalStateException.class,
				() -> electionEventContextService.getElectionEventStartTime(nonExistingElectionId));
		assertEquals(String.format("Election context entity not found. [electionEventId: %s]", nonExistingElectionId),
				Throwables.getRootCause(exceptionCcm).getMessage());
	}
}
