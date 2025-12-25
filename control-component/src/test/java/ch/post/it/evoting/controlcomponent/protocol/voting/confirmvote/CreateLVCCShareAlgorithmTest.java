/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static ch.post.it.evoting.domain.Constants.MAX_CONFIRMATION_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

/**
 * Tests of CreateLVCCShareAlgorithm.
 */
@DisplayName("CreateLVCCShareService")
class CreateLVCCShareAlgorithmTest extends TestGroupSetup {

	private static final int NODE_ID = 1;
	private static final Hash hash = spy(Hash.class);
	private static final Base64 base64 = BaseEncodingFactory.createBase64();
	private static final KeyDerivation keyDerivation = spy(KeyDerivationFactory.createKeyDerivation());
	private static final VerificationCardStateService verificationCardStateService = mock(VerificationCardStateService.class);

	private static CreateLVCCShareAlgorithm createLVCCShareAlgorithm;

	@BeforeAll
	static void setUpAll() {
		createLVCCShareAlgorithm = new CreateLVCCShareAlgorithm(hash, base64, keyDerivation, verificationCardStateService);
	}

	@Nested
	@DisplayName("calling createLVCCShare with")
	class CreateLVCCTest {

		private GqElement confirmationKey;
		private ZqElement ccrjReturnCodesGenerationSecretKey;
		private String verificationCardId;
		private LVCCHashContext context;
		private CreateLVCCShareInput input;

		@BeforeEach
		void setUp() {
			confirmationKey = gqGroupGenerator.genMember();
			ccrjReturnCodesGenerationSecretKey = zqGroupGenerator.genRandomZqElementMember();

			final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
			final String electionEventId = uuidGenerator.generate();
			final String verificationCardSetId = uuidGenerator.generate();
			verificationCardId = uuidGenerator.generate();

			context = new LVCCHashContext(gqGroup, NODE_ID, electionEventId, verificationCardSetId, verificationCardId);
			input = new CreateLVCCShareInput(confirmationKey, ccrjReturnCodesGenerationSecretKey);
		}

		@Test
		@DisplayName("valid parameters does not throw")
		void validParameters() {
			when(verificationCardStateService.isSentVote(verificationCardId)).thenReturn(true);
			when(verificationCardStateService.isNotConfirmedVote(verificationCardId)).thenReturn(true);

			final GqGroup gqGroup = GroupTestData.getLargeGqGroup();
			final ZqGroup zqGroup = ZqGroup.sameOrderAs(gqGroup);
			final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(gqGroup);
			final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(zqGroup);

			confirmationKey = gqGroupGenerator.genMember();
			ccrjReturnCodesGenerationSecretKey = zqGroupGenerator.genRandomZqElementMember();

			final CreateLVCCShareInput createLVCCShareInput = new CreateLVCCShareInput(confirmationKey, ccrjReturnCodesGenerationSecretKey
			);
			final LVCCHashContext lvccHashContext = new LVCCHashContext(gqGroup, context.nodeId(), context.electionEventId(),
					context.verificationCardSetId(), context.verificationCardId());

			doReturn(ImmutableByteArray.of((byte) 0x4)).when(hash).recursiveHash(any(), any());
			doReturn(gqGroupGenerator.genMember()).when(hash).hashAndSquare(any(), any());

			final CreateLVCCShareOutput output = createLVCCShareAlgorithm.createLVCCShare(lvccHashContext, createLVCCShareInput);

			assertEquals(0, output.confirmationAttemptId());
			assertEquals(gqGroup, output.longVoteCastReturnCodeShare().getGroup());
		}

		@Test
		@DisplayName("any null parameter throws NullPointerException")
		void nullParameters() {
			assertAll(
					() -> assertThrows(NullPointerException.class,
							() -> createLVCCShareAlgorithm.createLVCCShare(context, null)),
					() -> assertThrows(NullPointerException.class,
							() -> createLVCCShareAlgorithm.createLVCCShare(null, input))
			);
		}

		@Test
		@DisplayName("confirmation key and secret key having different group order throws IllegalArgumentException")
		void diffGroupKeys() {
			final GqElement otherGroupConfirmationKey = otherGqGroupGenerator.genMember();

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> new CreateLVCCShareInput(otherGroupConfirmationKey, ccrjReturnCodesGenerationSecretKey));
			assertEquals("The confirmation key must have the same order as the CCRj Return Codes Generation secret key.", exception.getMessage());
		}

		@Test
		@DisplayName("long choice return codes not computed throw IllegalArgumentException")
		void notPreviouslyComputedLCC() {
			when(verificationCardStateService.isSentVote(verificationCardId)).thenReturn(false);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> createLVCCShareAlgorithm.createLVCCShare(context, input));
			final String message = String.format(
					"The CCR_j cannot create the LVCC Share since it did not compute the long Choice Return Code shares for the verification card. [vc_id: %s]",
					verificationCardId);
			assertEquals(message, exception.getMessage());
		}

		@Test
		@DisplayName("max confirmation attempts exceeded throws IllegalArgumentException")
		void exceededAttempts() {
			when(verificationCardStateService.isSentVote(verificationCardId)).thenReturn(true);
			when(verificationCardStateService.isNotConfirmedVote(verificationCardId)).thenReturn(true);
			when(verificationCardStateService.getNextConfirmationAttemptId(verificationCardId)).thenReturn(MAX_CONFIRMATION_ATTEMPTS);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> createLVCCShareAlgorithm.createLVCCShare(context, input));
			assertEquals(String.format("Max confirmation attempts of %s exceeded.", MAX_CONFIRMATION_ATTEMPTS), exception.getMessage());
		}

	}

}
