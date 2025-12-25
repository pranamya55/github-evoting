/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("IdentifierValidationService calling")
class IdentifierValidationServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static VerificationCardService verificationCardService;
	private static VerificationCardSetService verificationCardSetService;
	private static IdentifierValidationService identifierValidationService;

	private String electionEventId;
	private String verificationCardSetId;
	private ContextIds contextIds;

	@BeforeAll
	static void setupAll() {
		verificationCardService = mock(VerificationCardService.class);
		verificationCardSetService = mock(VerificationCardSetService.class);
		identifierValidationService = new IdentifierValidationService(verificationCardService, verificationCardSetService);
	}

	@BeforeEach
	void setup() {
		reset(verificationCardService);
		reset(verificationCardSetService);

		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();
		contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
	}

	@Test
	@DisplayName("validateContextIds with null argument throws a NullPointerException")
	void validateContextIdsWithNullArgumentThrows() {
		assertThrows(NullPointerException.class, () -> identifierValidationService.validateContextIds(null));
	}

	@Test
	@DisplayName("validateContextIds with inconsistent election event id and verification card set id throws an IllegalArgumentException")
	void validateContextIdsWithInconsistentElectionEventIdAndVerificationCardSetIdThrows() {
		final String verificationCardId = contextIds.verificationCardId();
		final String differentElectionEventId = uuidGenerator.generate();
		final VerificationCardEntity verificationCardEntity = generateVerificationCardEntity(differentElectionEventId, verificationCardSetId,
				verificationCardId);
		doReturn(verificationCardEntity).when(verificationCardService).getVerificationCardEntity(verificationCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateContextIds(contextIds));
		final String expectedErrorMessage = String.format(
				"Verification card set and election event are not consistent. [verificationCardSetId: %s, electionEventId: %s]",
				verificationCardSetId, electionEventId);
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("validateContextIds with inconsistent verification card set id and verification card id throws an IllegalArgumentException")
	void validateContextIdsWithInconsistentVerificationCardSetIdAndVerificationCardIdThrows() {
		final String verificationCardId = contextIds.verificationCardId();
		final String differentVerificationCardSetId = uuidGenerator.generate();
		final VerificationCardEntity verificationCardEntity = generateVerificationCardEntity(electionEventId, differentVerificationCardSetId,
				verificationCardId);
		doReturn(verificationCardEntity).when(verificationCardService).getVerificationCardEntity(verificationCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateContextIds(contextIds));
		final String expectedErrorMessage = String.format(
				"Verification card and verification card set are not consistent. [verificationCardId: %s, verificationCardSetId: %s]",
				verificationCardId, verificationCardSetId);
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("validateIds with null arguments throws a NullPointerException")
	void validateIdsWithNullArgumentsThrows() {
		assertAll(
				() -> assertThrows(NullPointerException.class, () -> identifierValidationService.validateIds(null, verificationCardSetId)),
				() -> assertThrows(NullPointerException.class, () -> identifierValidationService.validateIds(electionEventId, null))
		);
	}

	@Test
	@DisplayName("validateIds with invalid UUID arguments throws a FailedValidationException")
	void validateIdsWithInvalidUUIDArgumentsThrows() {
		final String invalidUUID = "ivalid UUID";
		assertAll(
				() -> assertThrows(FailedValidationException.class,
						() -> identifierValidationService.validateIds(invalidUUID, verificationCardSetId)),
				() -> assertThrows(FailedValidationException.class, () -> identifierValidationService.validateIds(electionEventId, invalidUUID))
		);
	}

	@Test
	@DisplayName("validateIds with inconsistent election event id and verification card set id throws an IllegalArgumentException")
	void validateIdsWithInconsistentElectionEventIdAndVerificationCardSetIdThrows() {
		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, GroupTestData.getGqGroup());
		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardSetAlias("alias-" + verificationCardSetId)
				.setVerificationCardSetDescription("Description " + verificationCardSetId)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.setElectionEventEntity(electionEventEntity)
				.build();
		when(verificationCardSetService.getVerificationCardSet(verificationCardSetId)).thenReturn(verificationCardSetEntity);

		final String differentElectionEventId = uuidGenerator.generate();
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateIds(differentElectionEventId, verificationCardSetId));
		final String expectedErrorMessage = String.format(
				"Verification card set and election event are not consistent. [verificationCardSetId: %s, electionEventId: %s]",
				verificationCardSetId, differentElectionEventId);
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}

	private VerificationCardEntity generateVerificationCardEntity(final String electionEventId, final String verificationCardSetId,
			final String verificationCardId) {
		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, GroupTestData.getGqGroup());
		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardSetAlias("alias-" + verificationCardSetId)
				.setVerificationCardSetDescription("Description " + verificationCardSetId)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.setElectionEventEntity(electionEventEntity)
				.build();
		return new VerificationCardEntity(verificationCardId, verificationCardSetEntity,
				ImmutableByteArray.of((byte) 0b0000001));
	}
}
