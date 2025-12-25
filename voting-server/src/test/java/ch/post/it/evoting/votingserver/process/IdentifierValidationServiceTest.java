/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.voting.CredentialIdNotFoundException;

@DisplayName("IdentifierValidationService calling")
class IdentifierValidationServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static IdentifierValidationService identifierValidationService;
	private static VerificationCardRepository verificationCardRepository;
	private String electionEventId;
	private String verificationCardSetId;
	private String verificationCardId;
	private String credentialId;

	@BeforeAll
	static void setupAll() {
		verificationCardRepository = mock(VerificationCardRepository.class);
		final VerificationCardStateRepository verificationCardStateRepository = mock(VerificationCardStateRepository.class);
		final ObjectMapper objectMapper = DomainObjectMapper.getNewInstance();
		final ElectionEventRepository electionEventRepository = mock(ElectionEventRepository.class);
		final ElectionEventService electionEventService = new ElectionEventService(electionEventRepository);
		final VerificationCardSetRepository verificationCardSetRepository = mock(VerificationCardSetRepository.class);
		final VerificationCardSetService verificationCardSetService = new VerificationCardSetService(electionEventService,
				verificationCardSetRepository);
		final BallotBoxRepository ballotBoxRepository = mock(BallotBoxRepository.class);
		final BallotBoxService ballotBoxService = spy(
				new BallotBoxService(objectMapper, ballotBoxRepository, electionEventService, verificationCardSetService));
		final VerificationCardStateService verificationCardStateService = new VerificationCardStateService(verificationCardStateRepository);
		final VerificationCardService verificationCardService = new VerificationCardService(verificationCardRepository, verificationCardStateService,
				ballotBoxService);
		identifierValidationService = new IdentifierValidationService(verificationCardService);
	}

	@BeforeEach
	void setup() {
		reset(verificationCardRepository);
		electionEventId = uuidGenerator.generate();
		credentialId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();
	}

	@Test
	@DisplayName("validateCredentialId with null arguments throws a NullPointerException")
	void validateCredentialIdWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> identifierValidationService.validateCredentialId(null, credentialId));
		assertThrows(NullPointerException.class, () -> identifierValidationService.validateCredentialId(electionEventId, null));
	}

	@Test
	@DisplayName("validateCredentialId with non UUID arguments throws a FailedValidationException")
	void validateCredentialIdWithNonUuidArgumentsThrows() {
		final String nonUuid = "This is not a UUID";
		assertThrows(FailedValidationException.class, () -> identifierValidationService.validateCredentialId(nonUuid, credentialId));
		assertThrows(FailedValidationException.class, () -> identifierValidationService.validateCredentialId(electionEventId, nonUuid));
	}

	@Test
	@DisplayName("validateCredentialId when verification card not found for credential ID then throws a CredentialIdNotFoundException")
	void validateCredentialIdWhenVerificationCardNotFoundThrows() {
		when(verificationCardRepository.findByCredentialId(credentialId)).thenReturn(Optional.empty());
		final CredentialIdNotFoundException exception = assertThrows(CredentialIdNotFoundException.class,
				() -> identifierValidationService.validateCredentialId(electionEventId, credentialId));
		final String expectedMessage = String.format("No verification card found for given credentialId. [electionEventId: %s, credentialId: %s]",
				electionEventId,
				credentialId);
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	@DisplayName("validateCredentialId when verification card with different election event ID found for credential ID then throws an IllegalArgumentException")
	void validateCredentialIdWhenVerificationCardWithDifferentElectionEventIdFoundThrows() {
		final VerificationCardEntity verificationCardEntity = genVerificationCardEntity(
				uuidGenerator.generate(),
				uuidGenerator.generate(),
				credentialId);
		when(verificationCardRepository.findByCredentialId(credentialId)).thenReturn(Optional.of(verificationCardEntity));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateCredentialId(electionEventId, credentialId));
		final String expectedMessage = String.format("Election event and credential id are not consistent. [electionEventId: %s, credentialId: %s",
				electionEventId, credentialId);
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	@DisplayName("validateCredentialId when election event ID and credential ID are consistent then does not throw")
	void validateCredentialIdWhenElectionEventIdAndCredentialIdConsistentDoesNotThrow() {
		final VerificationCardEntity verificationCardEntity = genVerificationCardEntity(electionEventId, verificationCardSetId, credentialId);
		when(verificationCardRepository.findByCredentialId(credentialId)).thenReturn(Optional.of(verificationCardEntity));

		assertDoesNotThrow(() -> identifierValidationService.validateCredentialId(electionEventId, credentialId));
	}

	@Test
	@DisplayName("validateContextIdsAndCredentialId with null arguments throws a NullPointerException")
	void validateContextIdsAndCredentialIdWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class, () -> identifierValidationService.validateContextIdsAndCredentialId(null, credentialId));
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		assertThrows(NullPointerException.class, () -> identifierValidationService.validateContextIdsAndCredentialId(contextIds, null));
	}

	@Test
	@DisplayName("validateContextIdsAndCredentialId with non UUID arguments throws a FailedValidationException")
	void validateContextIdsAndCredentialIdWithNonUuidArgumentsThrows() {
		final String nonUuid = "This is not a UUID";
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		assertThrows(FailedValidationException.class, () -> identifierValidationService.validateContextIdsAndCredentialId(contextIds, nonUuid));
	}

	@Test
	@DisplayName("validateContextIdsAndCredentialId with inconsistent election event id and verification card set id throws an IllegalArgumentException")
	void validateContextIdsAndCredentialIdWithInconsistentElectionEventIdAndVerificationCardSetIdThrows() {
		final String differentElectionEventId = uuidGenerator.generate();
		final VerificationCardEntity verificationCardEntity = genVerificationCardEntity(differentElectionEventId, verificationCardSetId,
				credentialId);
		doReturn(Optional.of(verificationCardEntity)).when(verificationCardRepository).findById(verificationCardId);
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateContextIdsAndCredentialId(contextIds, credentialId));
		final String expectedErrorMessage = String.format(
				"Verification card set and election event are not consistent. [verificationCardSetId: %s, electionEventId: %s]",
				verificationCardSetId, electionEventId);
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("validateContextIdsAndCredentialId with inconsistent verification card set id and verification card id throws an IllegalArgumentException")
	void validateContextIdsAndCredentialIdWithInconsistentVerificationCardSetIdAndVerificationCardIdThrows() {
		final String differentVerificationCardSetId = uuidGenerator.generate();
		final VerificationCardEntity verificationCardEntity = genVerificationCardEntity(electionEventId, differentVerificationCardSetId,
				credentialId);
		doReturn(Optional.of(verificationCardEntity)).when(verificationCardRepository).findById(verificationCardId);
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateContextIdsAndCredentialId(contextIds, credentialId));
		final String expectedErrorMessage = String.format(
				"Verification card and verification card set are not consistent. [verificationCardId: %s, verificationCardSetId: %s]",
				verificationCardId, verificationCardSetId);
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("validateContextIdsAndCredentialId with inconsistent credential id throws an IllegalArgumentException")
	void validateContextIdsAndCredentialIdWithInconsistentCredentialIdThrows() {
		final String differentCredentialId = uuidGenerator.generate();
		final VerificationCardEntity verificationCardEntity = genVerificationCardEntity(electionEventId, verificationCardSetId,
				differentCredentialId);
		doReturn(Optional.of(verificationCardEntity)).when(verificationCardRepository).findById(verificationCardId);
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateContextIdsAndCredentialId(contextIds, credentialId));
		final String expectedErrorMessage = String.format(
				"Verification card id and credential id are not consistent. [verificationCardId: %s, credentialId: %s]", verificationCardId,
				credentialId);
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("validateContextIds with null argument throws a NullPointerException")
	void validateContextIdsWithNullArgumentThrows() {
		assertThrows(NullPointerException.class, () -> identifierValidationService.validateContextIds(null));
	}

	@Test
	@DisplayName("validateContextIds with inconsistent election event id and verification card set id throws an IllegalArgumentException")
	void validateContextIdsWithInconsistentElectionEventIdAndVerificationCardSetIdThrows() {
		final String differentElectionEventId = uuidGenerator.generate();
		final VerificationCardEntity verificationCardEntity = genVerificationCardEntity(differentElectionEventId, verificationCardSetId,
				credentialId);
		doReturn(Optional.of(verificationCardEntity)).when(verificationCardRepository).findById(verificationCardId);
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
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
		final String differentVerificationCardSetId = uuidGenerator.generate();
		final VerificationCardEntity verificationCardEntity = genVerificationCardEntity(electionEventId, differentVerificationCardSetId,
				credentialId);
		doReturn(Optional.of(verificationCardEntity)).when(verificationCardRepository).findById(verificationCardId);
		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateContextIds(contextIds));
		final String expectedErrorMessage = String.format(
				"Verification card and verification card set are not consistent. [verificationCardId: %s, verificationCardSetId: %s]",
				verificationCardId, verificationCardSetId);
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}

	private VerificationCardEntity genVerificationCardEntity(final String electionEventId, final String verificationCardSetId,
			final String credentialId) {
		final String randomVerificationCardId = uuidGenerator.generate();
		final String randomBallotBoxId = uuidGenerator.generate();
		final String randomVotingCardId = uuidGenerator.generate();
		final GqGroup encryptionGroup = GroupTestData.getGqGroup();
		final ElectionEventEntity electionEventEntity = new ElectionEventEntity(electionEventId, encryptionGroup);

		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardSetDescription("description")
				.setVerificationCardSetAlias("alias-123")
				.setElectionEventEntity(electionEventEntity)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.build();

		final String baseAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final SetupComponentVoterAuthenticationData voterAuthenticationData = new SetupComponentVoterAuthenticationData(electionEventId,
				verificationCardSetId, randomBallotBoxId, randomVerificationCardId, randomVotingCardId, credentialId, baseAuthenticationChallenge);
		final VerificationCardStateEntity verificationCardStateEntity = new VerificationCardStateEntity(randomVerificationCardId);
		return new VerificationCardEntity(randomVerificationCardId, verificationCardSetEntity, credentialId,
				randomVotingCardId, voterAuthenticationData, verificationCardStateEntity);
	}

}
