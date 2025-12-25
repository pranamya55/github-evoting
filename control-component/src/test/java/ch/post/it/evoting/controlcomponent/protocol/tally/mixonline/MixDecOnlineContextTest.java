/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.mixonline;

import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
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
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("Constructing a MixDecryptContext with ")
class MixDecOnlineContextTest extends TestGroupSetup {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys;
	private ElGamalMultiRecipientPublicKey electoralBoardPublicKey;
	private MixDecOnlineContext.Builder mixDecOnlineContextBuilder;

	@BeforeEach
	void setup() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String ballotBoxId = uuidGenerator.generate();

		final int numberAllowedWriteInsPlusOne = SECURE_RANDOM.nextInt(10) + 1;
		final int delta_max = SECURE_RANDOM.nextInt(numberAllowedWriteInsPlusOne, MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);

		ccmElectionPublicKeys = Stream.generate(() -> elGamalGenerator.genRandomPublicKey(delta_max)).limit(4).collect(GroupVector.toGroupVector());
		electoralBoardPublicKey = elGamalGenerator.genRandomPublicKey(delta_max);
		mixDecOnlineContextBuilder = new MixDecOnlineContext.Builder()
				.setEncryptionGroup(gqGroup)
				.setNodeId(3)
				.setElectionEventId(electionEventId)
				.setBallotBoxId(ballotBoxId)
				.setNumberOfAllowedWriteInsPlusOne(numberAllowedWriteInsPlusOne)
				.setCcmElectionPublicKeys(ccmElectionPublicKeys)
				.setElectoralBoardPublicKey(electoralBoardPublicKey);
	}

	@Test
	@DisplayName("null arguments throws a NullPointerException")
	void constructWithNullArgumentsThrows() {
		final MixDecOnlineContext.Builder nullEncryptionGroup = mixDecOnlineContextBuilder.setEncryptionGroup(null);
		assertThrows(NullPointerException.class, nullEncryptionGroup::build);

		final MixDecOnlineContext.Builder nullElectionEventId = mixDecOnlineContextBuilder.setElectionEventId(null);
		assertThrows(NullPointerException.class, nullElectionEventId::build);

		final MixDecOnlineContext.Builder nullBallotBoxId = mixDecOnlineContextBuilder.setBallotBoxId(null);
		assertThrows(NullPointerException.class, nullBallotBoxId::build);

		final MixDecOnlineContext.Builder nullCcmElectionPublicKeys = mixDecOnlineContextBuilder.setCcmElectionPublicKeys(null);
		assertThrows(NullPointerException.class, nullCcmElectionPublicKeys::build);

		final MixDecOnlineContext.Builder nullElectoralBoardPublicKey = mixDecOnlineContextBuilder.setElectoralBoardPublicKey(null);
		assertThrows(NullPointerException.class, nullElectoralBoardPublicKey::build);
	}

	@Test
	@DisplayName("invalid identifiers throws a FailedValidationException")
	void constructWithInvalidIdentifiersThrows() {
		final String badId = "badElectionEventId";
		final MixDecOnlineContext.Builder badElectionEventId = mixDecOnlineContextBuilder.setElectionEventId(badId);
		assertThrows(FailedValidationException.class, badElectionEventId::build);

		final MixDecOnlineContext.Builder badBallotBoxId = mixDecOnlineContextBuilder.setBallotBoxId(badId);
		assertThrows(FailedValidationException.class, badBallotBoxId::build);
	}

	@Test
	@DisplayName("the number of allowed write-ins plus one too small throws an IllegalArgumentException")
	void constructWithTooSmallNumberOfAllowedWriteInsPlusOne() {
		final MixDecOnlineContext.Builder tooSmallNumberOfWriteInsPlusOne = mixDecOnlineContextBuilder.setNumberOfAllowedWriteInsPlusOne(0);
		assertThrows(IllegalArgumentException.class, tooSmallNumberOfWriteInsPlusOne::build);
	}

	@Test
	@DisplayName("CCM election public keys of size different 4 throws an IllegalArgumentException")
	void constructWithCcmElectionPublicKeysBadSizeThrows() {
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> tooFewCCMElectionPublicKeys = Stream.generate(
				() -> elGamalGenerator.genRandomPublicKey(ccmElectionPublicKeys.getElementSize())).limit(3).collect(GroupVector.toGroupVector());
		final MixDecOnlineContext.Builder builderWithTooFewCCMElectionPublicKeys = mixDecOnlineContextBuilder.setCcmElectionPublicKeys(
				tooFewCCMElectionPublicKeys);
		final IllegalArgumentException tooFewException = assertThrows(IllegalArgumentException.class,
				builderWithTooFewCCMElectionPublicKeys::build);
		assertEquals("There must be exactly 4 CCM election public keys.", Throwables.getRootCause(tooFewException).getMessage());

		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> tooManyCCMElectionPublicKeys = Stream.generate(
				() -> elGamalGenerator.genRandomPublicKey(ccmElectionPublicKeys.getElementSize())).limit(5).collect(GroupVector.toGroupVector());
		final MixDecOnlineContext.Builder builderWithTooManyCCMElectionPublicKeys = mixDecOnlineContextBuilder.setCcmElectionPublicKeys(
				tooManyCCMElectionPublicKeys);
		final IllegalArgumentException tooManyException = assertThrows(IllegalArgumentException.class,
				builderWithTooManyCCMElectionPublicKeys::build);
		assertEquals("There must be exactly 4 CCM election public keys.", Throwables.getRootCause(tooManyException).getMessage());
	}

	@Test
	@DisplayName("the CCM election public keys having a different group than the electoral board public key throws an IllegalArgumentException")
	void constructWithPublicKeysDifferentGroupsThrows() {
		final ElGamalMultiRecipientPublicKey differentElectoralBoardPublicKey = otherGroupElGamalGenerator.genRandomPublicKey(
				electoralBoardPublicKey.size());
		final MixDecOnlineContext.Builder builderWithDifferentElectoralBoardPublicKey = mixDecOnlineContextBuilder.setElectoralBoardPublicKey(
				differentElectoralBoardPublicKey);
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builderWithDifferentElectoralBoardPublicKey::build);
		assertEquals("The encryption group of the public keys must be equal to the encryption group.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid input is successful")
	void constructWithValidInputDoesNotThrow() {
		assertDoesNotThrow(() -> mixDecOnlineContextBuilder.build());
	}
}