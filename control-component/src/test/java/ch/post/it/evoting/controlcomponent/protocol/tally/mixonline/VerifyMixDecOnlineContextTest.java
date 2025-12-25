/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("Constructing a VerifyMixDecOnlineContext object with")
class VerifyMixDecOnlineContextTest extends TestGroupSetup {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
	private int nodeId;
	private String electionEventId;
	private String ballotBoxId;
	private int numberOfAllowedWriteInsPlusOne;
	private ElGamalMultiRecipientPublicKey electionPublicKey;

	private GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys;
	private ElGamalMultiRecipientPublicKey electoralBoardPublicKey;

	@BeforeEach
	void setup() {
		nodeId = SECURE_RANDOM.nextInt(2, ControlComponentNode.ids().size() + 1);
		numberOfAllowedWriteInsPlusOne = SECURE_RANDOM.nextInt(5) + 1;

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		ballotBoxId = uuidGenerator.generate();

		final int numberOfSelections = numberOfAllowedWriteInsPlusOne + 1;
		final SetupComponentPublicKeys setupComponentPublicKeys = new SetupComponentPublicKeysPayloadGenerator(gqGroup).generate(numberOfSelections,
				numberOfAllowedWriteInsPlusOne).getSetupComponentPublicKeys();
		electionPublicKey = setupComponentPublicKeys.electionPublicKey();
		ccmElectionPublicKeys = setupComponentPublicKeys.combinedControlComponentPublicKeys().stream()
				.map(ControlComponentPublicKeys::ccmjElectionPublicKey)
				.collect(GroupVector.toGroupVector());
		electoralBoardPublicKey = setupComponentPublicKeys.electoralBoardPublicKey();
	}

	@Test
	@DisplayName("null arguments throws a NullPointerException")
	void constructWithNullArgumentsThrows() {
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new VerifyMixDecOnlineContext(null, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne,
								electionPublicKey,
								ccmElectionPublicKeys, electoralBoardPublicKey)),
				() -> assertThrows(NullPointerException.class,
						() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, null, ballotBoxId, numberOfAllowedWriteInsPlusOne, electionPublicKey,
								ccmElectionPublicKeys, electoralBoardPublicKey)),
				() -> assertThrows(NullPointerException.class,
						() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, null, numberOfAllowedWriteInsPlusOne, electionPublicKey,
								ccmElectionPublicKeys, electoralBoardPublicKey)),
				() -> assertThrows(NullPointerException.class,
						() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne, null,
								ccmElectionPublicKeys, electoralBoardPublicKey)),
				() -> assertThrows(NullPointerException.class,
						() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne,
								electionPublicKey, null, electoralBoardPublicKey)),
				() -> assertThrows(NullPointerException.class,
						() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne,
								electionPublicKey, ccmElectionPublicKeys, null))
		);
	}

	@Test
	@DisplayName("too small number of allowed write-ins + 1 throws an IllegalArgumentException")
	void constructWithTooSmallNumberOfWriteInsPlusOneThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, 0, electionPublicKey,
						ccmElectionPublicKeys, electoralBoardPublicKey));
		assertEquals("The number of allowed write-ins + 1 must be greater than or equal to 1.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("invalid node id throws an IllegalArgumentException")
	void constructWithInvalidNodeIdThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, 7, electionEventId, ballotBoxId, 0, electionPublicKey,
						ccmElectionPublicKeys, electoralBoardPublicKey));
		final String expected = String.format("The control component index must be in range [2, %s].", ControlComponentNode.ids().size());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("node id 1 throws an IllegalArgumentException")
	void constructWithCC1Throws() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, 1, electionEventId, ballotBoxId, 0, electionPublicKey,
						ccmElectionPublicKeys, electoralBoardPublicKey));
		final String expected = String.format("The control component index must be in range [2, %s].", ControlComponentNode.ids().size());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("invalid UUIDs throws a FailedValidationExeption")
	void constructWithInvalidUUIDThrows() {
		final String badId = "bad ID";
		assertThrows(FailedValidationException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, badId, ballotBoxId, numberOfAllowedWriteInsPlusOne, electionPublicKey,
						ccmElectionPublicKeys, electoralBoardPublicKey));
		assertThrows(FailedValidationException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, badId, numberOfAllowedWriteInsPlusOne, electionPublicKey,
						ccmElectionPublicKeys, electoralBoardPublicKey));
	}

	@Test
	@DisplayName("election public key and electoral board public key not having the same size throws IllegalArgumentException")
	void electionElectoralBoardPublicKeyDifferentSizeThrows() {
		final ElGamalMultiRecipientPublicKey tooLongElectoralBoardPublicKey = elGamalGenerator.genRandomPublicKey(numberOfAllowedWriteInsPlusOne + 2);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne, electionPublicKey,
						ccmElectionPublicKeys, tooLongElectoralBoardPublicKey));
		assertEquals("The election public key and the electoral board public key must have the same size.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("election public key having more elements than the CCM election public keys throws IllegalArgumentException")
	void electionPublicKeyMoreElementsThanCCMElectionPublicKeysThrows() {
		final ElGamalMultiRecipientPublicKey tooLongElectionPublicKey = elGamalGenerator.genRandomPublicKey(numberOfAllowedWriteInsPlusOne + 2);
		final ElGamalMultiRecipientPublicKey tooLongElectoralBoardPublicKey = elGamalGenerator.genRandomPublicKey(numberOfAllowedWriteInsPlusOne + 2);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne,
						tooLongElectionPublicKey,
						ccmElectionPublicKeys, tooLongElectoralBoardPublicKey));
		assertEquals("The election public key and the CCM election public keys must have the same size.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("number of CCM election public keys different 4 throws IllegalArgumentException")
	void ccmElectionPublicKeysBadVectorSizeThrows() {
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> tooFewCcmElectionPublicKeys = Stream.generate(
						() -> elGamalGenerator.genRandomPublicKey(numberOfAllowedWriteInsPlusOne))
				.limit(3)
				.collect(GroupVector.toGroupVector());
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne, electionPublicKey,
						tooFewCcmElectionPublicKeys, electoralBoardPublicKey));
		assertEquals(String.format("There must be exactly %s CCM election public keys. [%s, 3]", ControlComponentNode.ids().size(),
						ControlComponentNode.ids().size()),
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("election public key not having the same group throws IllegalArgumentException")
	void electionPublicKeyDifferentGroupThrows() {
		final ElGamalGenerator otherElGamalGenerator = new ElGamalGenerator(otherGqGroup);
		final ElGamalMultiRecipientPublicKey otherElectionPublicKey = otherElGamalGenerator.genRandomPublicKey(numberOfAllowedWriteInsPlusOne);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne,
						otherElectionPublicKey,
						ccmElectionPublicKeys, electoralBoardPublicKey));
		assertEquals("The election public key's group must be equal to the encryption group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("CCM election public keys not having the same group throws IllegalArgumentException")
	void ccmElectionPublicKeysDifferentGroupThrows() {
		final ElGamalGenerator otherElGamalGenerator = new ElGamalGenerator(otherGqGroup);
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherCcmElectionPublicKey = Stream.generate(
				() -> otherElGamalGenerator.genRandomPublicKey(numberOfAllowedWriteInsPlusOne)).limit(4).collect(GroupVector.toGroupVector());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne, electionPublicKey,
						otherCcmElectionPublicKey, electoralBoardPublicKey));
		assertEquals("The CCM election public keys' group must be equal to the encryption group.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("electoral board public key not having the same group throws IllegalArgumentException")
	void electoralBoardPublicKeyDifferentGroupThrows() {
		final ElGamalGenerator otherElGamalGenerator = new ElGamalGenerator(otherGqGroup);
		final ElGamalMultiRecipientPublicKey otherElectoralBoardPublicKey = otherElGamalGenerator.genRandomPublicKey(numberOfAllowedWriteInsPlusOne);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne, electionPublicKey,
						ccmElectionPublicKeys, otherElectoralBoardPublicKey));
		assertEquals("The electoral board public key's group must be equal to the encryption group.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("election public key not correctly constituted throws IllegalArgumentException")
	void electionPublicKeyIncorrectlyConstitutedThrows() {
		ElGamalMultiRecipientPublicKey differentElectionPublicKey;
		do {
			differentElectionPublicKey = elGamalGenerator.genRandomPublicKey(numberOfAllowedWriteInsPlusOne);
		} while (differentElectionPublicKey.equals(electionPublicKey));
		final ElGamalMultiRecipientPublicKey badElectionPublicKey = differentElectionPublicKey;
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne,
						badElectionPublicKey,
						ccmElectionPublicKeys, electoralBoardPublicKey));
		assertEquals("Multiplication of the ccmElectionPublicKeys times the electoralBoardPublicKey must equal the electionPublicKey.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid arguments does not throw")
	void constructWithValidArgumentsDoesNotThrow() {
		assertDoesNotThrow(
				() -> new VerifyMixDecOnlineContext(gqGroup, nodeId, electionEventId, ballotBoxId, numberOfAllowedWriteInsPlusOne, electionPublicKey,
						ccmElectionPublicKeys, electoralBoardPublicKey));
	}

}
