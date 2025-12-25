/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.TestDatabaseCleanUpService;
import ch.post.it.evoting.controlcomponent.TestKeyStoreInitializer;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

@SpringBootTest
@ContextConfiguration(initializers = TestKeyStoreInitializer.class)
@ActiveProfiles("test")
@DisplayName("IdentifierValidator calling...")
class IdentifierValidationServiceIT {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	@Autowired
	private IdentifierValidationService identifierValidationService;

	@Autowired
	private ElectionEventService electionEventService;

	@Autowired
	private VerificationCardSetService verificationCardSetService;

	@Autowired
	private VerificationCardService verificationCardService;

	@Autowired
	private TestDatabaseCleanUpService testDatabaseCleanUpService;

	private String electionEventId;
	private String verificationCardSetId;
	private String verificationCardId;
	private ContextIds contextIds;

	@BeforeEach
	void setup() {
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();

		final GqGroup encryptionGroup = new GqGroup(BigInteger.valueOf(11), BigInteger.valueOf(5), BigInteger.valueOf(3));

		// Save election event.
		final ElectionEventEntity electionEventEntity = electionEventService.save(electionEventId, encryptionGroup);

		// Save verification card set.
		final VerificationCardSetEntity verificationCardSetEntity = new VerificationCardSetEntity.Builder()
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardSetAlias("alias-" + verificationCardSetId)
				.setVerificationCardSetDescription("Description " + verificationCardSetId)
				.setDomainsOfInfluence(ImmutableList.of("domain1", "domain2"))
				.setElectionEventEntity(electionEventEntity)
				.build();
		verificationCardSetService.save(verificationCardSetEntity);

		// Save verification card.
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		final ElGamalMultiRecipientPublicKey publicKey = elGamalGenerator.genRandomPublicKey(1);
		verificationCardService.save(new VerificationCard(verificationCardId, verificationCardSetId, publicKey));

		contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
	}

	@AfterEach
	void cleanUp() {
		testDatabaseCleanUpService.cleanUp();
	}

	@Test
	@DisplayName("validateContextIds with null argument throws a NullPointerException")
	void validateContextIdsWithNullInputThrows() {
		assertThrows(NullPointerException.class, () -> identifierValidationService.validateContextIds(null));
	}

	@Test
	@DisplayName("validateContextIds with consistent context ids does not throw")
	void validateContextIdsWithConsistentIdsDoesNotThrow() {
		assertDoesNotThrow(() -> identifierValidationService.validateContextIds(contextIds));
	}

	@Test
	@DisplayName("validateContextIds with wrong election event id throws an IllegalArgumentException")
	void validateContextIdsWithWrongElectionEventIdThrows() {
		final String wrongElectionEventId = uuidGenerator.generate();
		final ContextIds inconsistentContextIds = new ContextIds(wrongElectionEventId, verificationCardSetId, verificationCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateContextIds(inconsistentContextIds));
		final String errorMessage = String.format(
				"Verification card set and election event are not consistent. [verificationCardSetId: %s, electionEventId: %s]",
				verificationCardSetId, wrongElectionEventId);
		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("validateContextIds with wrong verification card set id throws an IllegalArgumentException")
	void validateContextIdsWithWrongVerificationCardSetIdThrows() {
		final String wrongVerificationCardSetId = uuidGenerator.generate();
		final ContextIds inconsistentContextIds = new ContextIds(electionEventId, wrongVerificationCardSetId, verificationCardId);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> identifierValidationService.validateContextIds(inconsistentContextIds));
		final String errorMessage = String.format(
				"Verification card and verification card set are not consistent. [verificationCardId: %s, verificationCardSetId: %s]",
				verificationCardId, wrongVerificationCardSetId);
		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("validateContextIds with inexistant verification card id throws an IllegalStateException")
	void validateContextIdsWithNonexistentVerificationCardIdThrows() {
		final String wrongVerificationCardId = uuidGenerator.generate();
		final ContextIds inconsistentContextIds = new ContextIds(electionEventId, verificationCardSetId, wrongVerificationCardId);
		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> identifierValidationService.validateContextIds(inconsistentContextIds));
		final String errorMessage = "No corresponding verificationCard found. [verificationCardId: %s]";
		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}
}
