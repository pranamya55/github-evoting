/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

class GenCredDatContextTest extends TestGroupSetup {

	private static final int BOUND = 12;
	private static final int START = 21;
	private static final int DIFFERENT_START = 41;

	private final SecureRandom srand = new SecureRandom();
	private final int deltaMax = srand.nextInt(BOUND) + 1;
	private final int psiMax = srand.nextInt(BOUND) + 1;

	private String electionEventId;
	private String verificationCardSetId;
	private ImmutableList<String> verificationCardIds;
	private ElGamalMultiRecipientPublicKey electionPublicKey;
	private ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey;
	private PrimesMappingTable primesMappingTable;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		verificationCardIds = ImmutableList.of(
				uuidGenerator.generate(),
				uuidGenerator.generate(),
				uuidGenerator.generate()
		);

		electionPublicKey = elGamalGenerator.genRandomPublicKey(deltaMax);
		choiceReturnCodesEncryptionPublicKey = elGamalGenerator.genRandomPublicKey(psiMax);

		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator(electionPublicKey.getGroup());
		primesMappingTable = primesMappingTableGenerator.generate(1);
	}

	@Test
	@DisplayName("happyPath")
	void happyPath() {
		final GenCredDatContext context = assertDoesNotThrow(() -> new GenCredDatContext.Builder()
				.setEncryptionGroup(gqGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setElectionPublicKey(electionPublicKey)
				.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
				.setPrimesMappingTable(primesMappingTable)
				.build());

		assertEquals(context.electionEventId(), electionEventId, "Context electionEventId no equal");
		assertEquals(context.verificationCardSetId(), verificationCardSetId, "Context verificationCardSetId no equal");
		assertEquals(context.electionPublicKey().getGroup(), electionPublicKey.getGroup(),
				"Input electionPublicKey has different GqGroup");
		assertTrue(context.electionPublicKey().stream().allMatch(elt -> electionPublicKey.stream().anyMatch(elt2 -> elt2.equals(elt))),
				"Input electionPublicKey does not contains all elements");
		assertTrue(context.primesMappingTable().pTable().containsAll(primesMappingTable.pTable()),
				"Input primes mapping table does not contains all elements");
	}

	@Nested
	@DisplayName("A null arguments throws a NullPointerException")
	class NullArgumentThrowsNullPointerException {

		@Test
		@DisplayName("all arguments null")
		void constructWithNullArguments() {
			final GenCredDatContext.Builder builder = new GenCredDatContext.Builder();

			final NullPointerException ex = assertThrows(NullPointerException.class, builder::build);

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("encryption group argument null")
		void constructWithEncryptionGroup() {
			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final NullPointerException ex = assertThrows(NullPointerException.class, builder::build);

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("electionEventId argument null")
		void constructWithElectionEventId() {
			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final NullPointerException ex = assertThrows(NullPointerException.class, builder::build);

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("verificationCardSetId argument null")
		void constructWithVerificationCardSetId() {
			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final NullPointerException ex = assertThrows(NullPointerException.class, builder::build);

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("verificationCardIds argument null")
		void constructWithVerificationCardIds() {
			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final NullPointerException ex = assertThrows(NullPointerException.class, builder::build);

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("electionPublicKey argument null")
		void constructWithElectionPublicKey() {
			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final NullPointerException ex = assertThrows(NullPointerException.class, builder::build);

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("choiceReturnCodesEncryptionPublicKey argument null")
		void constructWithChoiceReturnCodesEncryptionPublicKey() {
			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final NullPointerException ex = assertThrows(NullPointerException.class, builder::build);

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("primesMappingTable argument null")
		void constructWithEncodedVotingOptions() {
			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey);

			final NullPointerException ex = assertThrows(NullPointerException.class, builder::build);

			final String expectedMessage = null;

			assertEquals(expectedMessage, ex.getMessage());
		}
	}

	@Nested
	@DisplayName("Invalid arguments throws an Exception")
	class InvalidArgumentThrowsException {

		@Test
		@DisplayName("electionEventId argument invalid")
		void invalidElectionEventIdTest() {
			final String invalidElectionEventId = electionEventId + "X";

			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(invalidElectionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final FailedValidationException ex = assertThrows(FailedValidationException.class, builder::build);

			final String expectedMessage =
					String.format("The given string does not comply with the required format. [string: %s, format: ^[0-9A-F]{32}$].",
							invalidElectionEventId);

			assertEquals(expectedMessage, ex.getMessage());

		}

		@Test
		@DisplayName("verificationCardSetId argument invalid")
		void invalidVerificationCardSetIdTest() {
			final String invalidVerificationCardSetId = verificationCardSetId + "X";

			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(invalidVerificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final FailedValidationException ex = assertThrows(FailedValidationException.class, builder::build);

			final String expectedMessage =
					String.format("The given string does not comply with the required format. [string: %s, format: ^[0-9A-F]{32}$].",
							invalidVerificationCardSetId);

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("electionPublicKey argument invalid")
		void invalidElectionPublicKeyTest() {
			final ElGamalMultiRecipientPublicKey electionPublicKey2 = otherGroupElGamalGenerator.genRandomPublicKey(srand.nextInt(BOUND) + START);

			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey2)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);

			final String expectedMessage = "All inputs must have the same group.";

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("choiceReturnCodesEncryptionPublicKey size invalid")
		void invalidSizeChoiceReturnCodesEncryptionPublicKeyTest() {
			final ElGamalMultiRecipientPublicKey invalidSizeChoiceReturnCodesEncryptionPublicKey =
					elGamalGenerator.genRandomPublicKey(MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1);

			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(invalidSizeChoiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTable);

			final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);

			final String expectedMessage = String.format(
					"The size of the CCR encryption public key must be smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
					MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1, MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("choiceReturnCodesEncryptionPublicKey invalid")
		void invalidChoiceReturnCodesEncryptionPublicKeyTest() {
			final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey2 =
					otherGroupElGamalGenerator.genRandomPublicKey(srand.nextInt(BOUND) + DIFFERENT_START);

			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey2)
					.setPrimesMappingTable(primesMappingTable);

			final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);

			final String expectedMessage = "All inputs must have the same group.";

			assertEquals(expectedMessage, ex.getMessage());
		}

		@Test
		@DisplayName("primesMappingTable and electionPublicKey have the same GqGroup.")
		void encodedVotingOptionsGqGroupTest() {
			final PrimesMappingTableGenerator otherGroupPrimesMappingTableGenerator = new PrimesMappingTableGenerator(otherGqGroup);
			final PrimesMappingTable primesMappingTableDifferentGqGroup = otherGroupPrimesMappingTableGenerator.generate(1);

			final GenCredDatContext.Builder builder = new GenCredDatContext
					.Builder()
					.setEncryptionGroup(gqGroup)
					.setElectionEventId(electionEventId)
					.setVerificationCardSetId(verificationCardSetId)
					.setVerificationCardIds(verificationCardIds)
					.setElectionPublicKey(electionPublicKey)
					.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
					.setPrimesMappingTable(primesMappingTableDifferentGqGroup);

			final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, builder::build);

			final String expectedMessage = "All inputs must have the same group.";

			assertEquals(expectedMessage, ex.getMessage());
		}
	}
}
