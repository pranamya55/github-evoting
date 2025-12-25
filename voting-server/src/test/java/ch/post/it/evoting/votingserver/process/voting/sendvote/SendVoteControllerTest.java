/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.sendvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.PlaintextEqualityProof;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.messaging.ResponseCompletionCompletableFuture;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeService;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendVoteController")
class SendVoteControllerTest extends TestGroupSetup {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();
	private static final String TWO_POW_256 = "115792089237316195423570985008687907853269984665640564039457584007913129639936";

	private static SendVotePayload sendVotePayload;
	private static SendVoteController sendVoteController;

	private static String electionEventId;
	private static String verificationCardSetId;
	private static String credentialId;
	private static String verificationCardId;

	@Mock
	private ChoiceReturnCodesService mockChoiceReturnCodesService;

	@Mock
	private IdentifierValidationService mockIdentifierValidationService;

	@Mock
	private VerifyAuthenticationChallengeService mockVerifyAuthenticationChallengeService;

	@BeforeAll
	static void setUpAll() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		credentialId = uuidGenerator.generate();
		verificationCardId = uuidGenerator.generate();

		// Create payload.
		sendVotePayload = createSendVotePayload(verificationCardSetId, verificationCardId);
	}

	@BeforeEach
	void setUp() {
		sendVoteController = new SendVoteController(mockChoiceReturnCodesService, mockIdentifierValidationService,
				mockVerifyAuthenticationChallengeService);
	}

	@Test
	@DisplayName("retrieveShortChoiceReturnCodes with valid parameters and happy path")
	void retrieveShortChoiceReturnCodesHappyPath() {
		final SendVoteResponsePayload shotChoiceReturnCodes = new SendVoteResponsePayload(ImmutableList.of("1234"));

		/* Expectations */
		final ResponseCompletionCompletableFuture<ImmutableList<String>> future = new ResponseCompletionCompletableFuture<>(120);
		future.complete(shotChoiceReturnCodes.shortChoiceReturnCodes());
		when(mockChoiceReturnCodesService.retrieveShortChoiceReturnCodes(any(ContextIds.class), anyString(), any(EncryptedVerifiableVote.class)))
				.thenReturn(future);

		/* Execution */
		final SendVoteResponsePayload response = sendVoteController.retrieveShortChoiceReturnCodes(electionEventId, verificationCardSetId,
				credentialId, verificationCardId, sendVotePayload).block();

		/* Verification */
		verify(mockChoiceReturnCodesService).retrieveShortChoiceReturnCodes(any(ContextIds.class), anyString(), any(EncryptedVerifiableVote.class));

		assertNotNull(response);
		assertEquals(shotChoiceReturnCodes, response);
	}

	@Test
	@DisplayName("Invalid inputs throws Exception")
	void InvalidInput() {
		/* Execution */
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> sendVoteController.retrieveShortChoiceReturnCodes(null,
								verificationCardSetId, credentialId, verificationCardId, sendVotePayload)),
				() -> assertThrows(NullPointerException.class,
						() -> sendVoteController.retrieveShortChoiceReturnCodes(electionEventId,
								null, credentialId, verificationCardId, sendVotePayload)),
				() -> assertThrows(NullPointerException.class,
						() -> sendVoteController.retrieveShortChoiceReturnCodes(electionEventId,
								verificationCardSetId, null, verificationCardId, sendVotePayload)),
				() -> assertThrows(NullPointerException.class,
						() -> sendVoteController.retrieveShortChoiceReturnCodes(electionEventId,
								verificationCardSetId, credentialId, null, sendVotePayload)),
				() -> assertThrows(NullPointerException.class,
						() -> sendVoteController.retrieveShortChoiceReturnCodes(electionEventId,
								verificationCardSetId, credentialId, verificationCardId, null)),
				() -> assertThrows(FailedValidationException.class,
						() -> sendVoteController.retrieveShortChoiceReturnCodes("electionEventId",
								verificationCardSetId, credentialId, verificationCardId, sendVotePayload)),
				() -> assertThrows(FailedValidationException.class,
						() -> sendVoteController.retrieveShortChoiceReturnCodes(electionEventId,
								"verificationCardSetId", credentialId, verificationCardId, sendVotePayload)),
				() -> assertThrows(FailedValidationException.class,
						() -> sendVoteController.retrieveShortChoiceReturnCodes(electionEventId,
								verificationCardSetId, credentialId, "votingCardId", sendVotePayload))
		);
	}

	private static SendVotePayload createSendVotePayload(final String verificationCardSetId, final String verificationCardId) {
		final String derivedAuthenticationChallenge = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		final BigInteger authenticationNonce = random.genRandomInteger(new BigInteger(TWO_POW_256));

		final ContextIds contextIds = new ContextIds(electionEventId, verificationCardSetId, verificationCardId);
		final EncryptedVerifiableVote encryptedVerifiableVote = genEncryptedVerifiableVote(contextIds);
		final AuthenticationChallenge authenticationChallenge = new AuthenticationChallenge(credentialId, derivedAuthenticationChallenge,
				authenticationNonce);

		return new SendVotePayload(contextIds, gqGroup, encryptedVerifiableVote, authenticationChallenge);
	}

	private static EncryptedVerifiableVote genEncryptedVerifiableVote(final ContextIds contextIds) {
		final int numberOfWriteInsPlusOne = 1;
		final ElGamalMultiRecipientCiphertext encryptedVote = elGamalGenerator.genRandomCiphertext(numberOfWriteInsPlusOne);
		final GqGroup encryptionGroup = encryptedVote.getGroup();
		final ZqGroup zqGroup = ZqGroup.sameOrderAs(encryptionGroup);
		final BigInteger exponentValue = RandomFactory.createRandom().genRandomInteger(encryptionGroup.getQ());
		final ZqElement exponent = ZqElement.create(exponentValue, zqGroup);
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedVote = encryptedVote.getCiphertextExponentiation(exponent);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(zqGroup);
		final ExponentiationProof exponentiationProof = new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember());
		final PlaintextEqualityProof plaintextEqualityProof = new PlaintextEqualityProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementVector(2));
		final ElGamalMultiRecipientCiphertext encryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(numberOfWriteInsPlusOne);

		return new EncryptedVerifiableVote(contextIds, encryptedVote, encryptedPartialChoiceReturnCodes, exponentiatedEncryptedVote,
				exponentiationProof, plaintextEqualityProof);
	}

}
