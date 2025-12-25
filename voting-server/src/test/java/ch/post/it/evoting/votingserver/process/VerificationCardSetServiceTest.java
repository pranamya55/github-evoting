/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class VerificationCardSetServiceTest {

	private static ElectionEventRepository electionEventRepository;
	private static VerificationCardSetRepository verificationCardSetRepository;
	private static VerificationCardSetService verificationCardSetService;
	private static ElectionEventService electionEventService;

	private String electionEventId;
	private GqGroup encryptionGroup;
	private ImmutableList<VerificationCardSetContext> verificationCardSetContexts;
	private VerificationCardSetEntity verificationCardSetEntity;

	@BeforeAll
	static void setupAll() {
		electionEventRepository = mock(ElectionEventRepository.class);
		electionEventService = spy(new ElectionEventService(electionEventRepository));
		verificationCardSetRepository = mock(VerificationCardSetRepository.class);
		verificationCardSetService = new VerificationCardSetService(electionEventService, verificationCardSetRepository);
	}

	@BeforeEach
	void setup() {
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		encryptionGroup = electionEventContextPayload.getEncryptionGroup();

		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		electionEventId = electionEventContext.electionEventId();
		verificationCardSetContexts = electionEventContext.verificationCardSetContexts();

		final VerificationCardSetContext verificationCardSetContext = verificationCardSetContexts.getFirst();
		final String verificationCardSetId = verificationCardSetContext.getVerificationCardSetId();
		verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardSetDescription("description")
				.setVerificationCardSetAlias("alias-123")
				.setElectionEventEntity(new ElectionEventEntity(electionEventId, encryptionGroup))
				.setDomainsOfInfluence(verificationCardSetContext.getDomainsOfInfluence())
				.build();
	}

	@Test
	void saveAllFromContextWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> verificationCardSetService.saveAllFromContext(null, verificationCardSetContexts));
		assertThrows(NullPointerException.class, () -> verificationCardSetService.saveAllFromContext(electionEventId, null));
	}

	@Test
	void saveAllFromContextWithNonUuidThrows() {
		assertThrows(FailedValidationException.class, () -> verificationCardSetService.saveAllFromContext("nonUUID", verificationCardSetContexts));
	}

	@Test
	void saveAllFromContextWithValidArgumentsThrows() {
		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, encryptionGroup);
		doReturn(Optional.of(electionEventEntity)).when(electionEventRepository).findById(electionEventId);

		assertDoesNotThrow(() -> verificationCardSetService.saveAllFromContext(electionEventId, verificationCardSetContexts));
	}

	@Test
	void getVerificationCardSetEntityWithNullArgumentThrows() {
		assertThrows(NullPointerException.class, () -> verificationCardSetService.getVerificationCardSetEntity(null));
	}

	@Test
	void getVerificationCardSetEntityWithNonUuidThrows() {
		assertThrows(FailedValidationException.class, () -> verificationCardSetService.getVerificationCardSetEntity("nonUUID"));
	}

	@Test
	void getVerificationCardSetEntityWhenPresentReturns() {
		doReturn(Optional.of(verificationCardSetEntity)).when(verificationCardSetRepository)
				.findById(verificationCardSetEntity.getVerificationCardSetId());

		final VerificationCardSetEntity returnedVerificationCardSetEntity = assertDoesNotThrow(
				() -> verificationCardSetService.getVerificationCardSetEntity(verificationCardSetEntity.getVerificationCardSetId()));
		assertEquals(verificationCardSetEntity, returnedVerificationCardSetEntity);
	}

	@Test
	void getVerificationCardSetEntityWhenNotPresentThrows() {
		final VerificationCardSetContext verificationCardSetContext = verificationCardSetContexts.get(0);
		final String verificationCardSetId = verificationCardSetContext.getVerificationCardSetId();
		doReturn(Optional.empty()).when(verificationCardSetRepository).findById(verificationCardSetId);

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> verificationCardSetService.getVerificationCardSetEntity(verificationCardSetId));

		final String expectedErrorMessage = String.format("Verification card set not found. [verificationCardSetId: %s]", verificationCardSetId);
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}
}
