/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("Constructing an ExtractVerificationCardSetContext with")
class ExtractVerificationCardSetContextTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final GqGroup encryptionGroup = GroupTestData.getLargeGqGroup();
	private static final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator();
	private static final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator();

	private String electionEventId;
	private String verificationCardSetId;
	private PrimesMappingTable primesMappingTable;
	private ElGamalMultiRecipientPublicKey electionPublicKey;
	private ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey;

	@BeforeEach
	void setup() {
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();

		final int numberOfSelections = 10;
		final int numberOfWriteInsPlusOne = 3;
		primesMappingTable = primesMappingTableGenerator.generate(numberOfSelections, numberOfWriteInsPlusOne);

		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayloadGenerator.generate(numberOfSelections,
				numberOfWriteInsPlusOne).getSetupComponentPublicKeys();
		electionPublicKey = setupComponentPublicKeys.electionPublicKey();
		choiceReturnCodesEncryptionPublicKey = setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey();
	}

	@Test
	@DisplayName("with null arguments throws a NullPointerException")
	void constructWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> new ExtractVerificationCardSetContext(null, electionEventId, verificationCardSetId, primesMappingTable,
						electionPublicKey, choiceReturnCodesEncryptionPublicKey));
		assertThrows(NullPointerException.class,
				() -> new ExtractVerificationCardSetContext(encryptionGroup, null, verificationCardSetId, primesMappingTable,
						electionPublicKey, choiceReturnCodesEncryptionPublicKey));
		assertThrows(NullPointerException.class,
				() -> new ExtractVerificationCardSetContext(encryptionGroup, electionEventId, null, primesMappingTable,
						electionPublicKey, choiceReturnCodesEncryptionPublicKey));
		assertThrows(NullPointerException.class,
				() -> new ExtractVerificationCardSetContext(encryptionGroup, electionEventId, verificationCardSetId, null,
						electionPublicKey, choiceReturnCodesEncryptionPublicKey));
		assertThrows(NullPointerException.class,
				() -> new ExtractVerificationCardSetContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable,
						null, choiceReturnCodesEncryptionPublicKey));
		assertThrows(NullPointerException.class,
				() -> new ExtractVerificationCardSetContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable,
						electionPublicKey, null));
	}

	@Test
	@DisplayName("with invalid election id throws a FailedValidationException")
	void constructWithInvalidElectionEventIdThrows() {
		final String nonUuid = "non UUID!";
		assertThrows(FailedValidationException.class,
				() -> new ExtractVerificationCardSetContext(encryptionGroup, nonUuid, verificationCardSetId, primesMappingTable,
						electionPublicKey, choiceReturnCodesEncryptionPublicKey));
	}

	@Test
	@DisplayName("with invalid verification card set id throws a FailedValidationException")
	void constructWithInvalidVerificationCardSetIdThrows() {
		final String nonUuid = "non UUID!";
		assertThrows(FailedValidationException.class,
				() -> new ExtractVerificationCardSetContext(encryptionGroup, electionEventId, nonUuid, primesMappingTable,
						electionPublicKey, choiceReturnCodesEncryptionPublicKey));
	}

	@Test
	@DisplayName("with different encryption groups throws an IllegalArgumentException")
	void constructWithDifferentEncryptionGroupThrows() {
		final GqGroup differentEncryptionGroup = GroupTestData.getGroupP59();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ExtractVerificationCardSetContext(differentEncryptionGroup, electionEventId, verificationCardSetId, primesMappingTable,
						electionPublicKey, choiceReturnCodesEncryptionPublicKey));

		final String expected = "All parameters must have the same group.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("with too big maximum number of selections throws an IllegalArgumentException")
	void constructWithTooBigMaximumNumberOfSelectionsThrows() {
		final int tooBigPsiMax = MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1;
		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayloadGenerator.generate(tooBigPsiMax,
						electionPublicKey.size())
				.getSetupComponentPublicKeys();
		final ElGamalMultiRecipientPublicKey tooBigPsiMaxPublicKey = setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ExtractVerificationCardSetContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable,
						electionPublicKey, tooBigPsiMaxPublicKey));

		final String expected = String.format(
				"The size of the CCR encryption public key must be smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				tooBigPsiMax, MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("with too big maximum number of write-ins + 1 throws an IllegalArgumentException")
	void constructWithTooBigMaximumNumberOfWriteInsPlusOneThrows() {
		final int tooBigDeltaMax = MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 2;
		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayloadGenerator.generate(
						choiceReturnCodesEncryptionPublicKey.size(),
						tooBigDeltaMax)
				.getSetupComponentPublicKeys();
		final ElGamalMultiRecipientPublicKey tooBigDeltaMaxPublicKey = setupComponentPublicKeys.electionPublicKey();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ExtractVerificationCardSetContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable,
						tooBigDeltaMaxPublicKey, choiceReturnCodesEncryptionPublicKey));

		final String expected = String.format(
				"The size of the election public key must be smaller or equal to the maximum supported number of write-ins + 1. [delta_max: %s, delta_sup: %s]",
				tooBigDeltaMax, MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}
}