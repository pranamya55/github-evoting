/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.security.SignatureException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@Service
class ElectionEventContextServiceTest {

	private static ElectionEventContextService electionEventContextService;
	private static ElectionEventContextPayload electionEventContextPayload;

	@BeforeAll
	static void beforeAll() throws SignatureException {
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();

		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();
		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, electionEventContextPayload.getEncryptionGroup());

		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();

		final BallotBoxRepository ballotBoxRepository = mock(BallotBoxRepository.class);
		final VerificationCardSetRepository verificationCardSetRepository = mock(VerificationCardSetRepository.class);
		final ElectionEventService electionEventService = mock(ElectionEventService.class);
		final ElectionEventContextRepository electionEventContextRepository = mock(ElectionEventContextRepository.class);
		final SignatureKeystore<Alias> signatureKeystore = mock(SignatureKeystore.class);

		final VerificationCardSetService verificationCardSetService = spy(
				new VerificationCardSetService(electionEventService, verificationCardSetRepository));
		final BallotBoxService ballotBoxService = spy(
				new BallotBoxService(objectMapper, ballotBoxRepository, electionEventService, verificationCardSetService));

		when(electionEventService.retrieveElectionEventEntity(anyString())).thenReturn(electionEventEntity);
		when(electionEventContextRepository.save(any()))
				.thenReturn(new ElectionEventContextEntity(electionEventEntity, electionEventContext.startTime(), electionEventContext.finishTime(),
						electionEventContext.electionEventAlias(), electionEventContext.electionEventDescription(), electionEventContext.votesTexts(),
						electionEventContext.electionsTexts()));
		when(verificationCardSetRepository.findById(anyString())).thenReturn(genVerificationCardEntity(electionEventEntity));
		electionEventContextService = spy(new ElectionEventContextService(electionEventService, verificationCardSetService, ballotBoxService,
				electionEventContextRepository, signatureKeystore));

		doReturn(true).when(signatureKeystore).verifySignature(any(), any(), any(), any());
	}

	private static Optional<VerificationCardSetEntity> genVerificationCardEntity(final ElectionEventEntity electionEventEntity) {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		return Optional.of(new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(uuidGenerator.generate())
				.setVerificationCardSetDescription("description")
				.setVerificationCardSetAlias("alias-123")
				.setElectionEventEntity(electionEventEntity)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.build());
	}

	@DisplayName("saveElectionEventContext with valid arguments does not throw")
	@Test
	void saveElectionEventContextWithValidArgumentsDoesNotThrow() {
		assertDoesNotThrow(() -> electionEventContextService.saveElectionEventContext(electionEventContextPayload));
	}

	@DisplayName("saveElectionEventContext with null argument throws a NullPointerException")
	@Test
	void saveElectionEventContextWithNullArgumentThrows() {
		assertThrows(NullPointerException.class, () -> electionEventContextService.saveElectionEventContext(null));
	}
}
