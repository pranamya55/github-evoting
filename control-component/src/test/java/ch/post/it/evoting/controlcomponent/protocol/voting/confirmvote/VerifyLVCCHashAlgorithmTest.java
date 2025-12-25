/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashableList.toHashableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;

/**
 * Tests of VerifyLVCCHashAlgorithm.
 */
@DisplayName("VerifyLVCCHashServiceTest calling")
class VerifyLVCCHashAlgorithmTest {

	private static final Random random = RandomFactory.createRandom();
	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private static final int NODE_ID = 1;
	private static final Hash hash = HashFactory.createHash();
	private static final Base64 base64 = BaseEncodingFactory.createBase64();
	private static final VerificationCardStateService verificationCardStateServiceMock = mock(VerificationCardStateService.class);

	private static VerifyLVCCHashAlgorithm verifyLVCCHashAlgorithm;

	private String hlVCC1;
	private ImmutableList<String> otherCCRhlVCC;
	private String verificationCardId;
	private LVCCHashContext context;
	private VerifyLVCCHashInput.Builder inputBuilder;

	@BeforeAll
	static void setupAll() {
		verifyLVCCHashAlgorithm = new VerifyLVCCHashAlgorithm(hash, base64, verificationCardStateServiceMock);
	}

	@BeforeEach
	void setup() {
		hlVCC1 = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		otherCCRhlVCC = Stream.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(ControlComponentNode.ids().size() - 1)
				.collect(toImmutableList());

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		verificationCardId = uuidGenerator.generate();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();

		final GqGroup encryptionGroup = GroupTestData.getGqGroup();
		context = new LVCCHashContext(encryptionGroup, NODE_ID, electionEventId, verificationCardSetId, verificationCardId);
		final String longVoteCastCode = random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet);
		inputBuilder = new VerifyLVCCHashInput.Builder()
				.setLongVoteCastReturnCodesAllowList(castCode -> castCode.equals(longVoteCastCode))
				.setCcrjHashedLongVoteCastReturnCode(hlVCC1)
				.setOtherCCRsHashedLongVoteCastReturnCodes(otherCCRhlVCC);
	}

	@Test
	@DisplayName("verifyLVCCHash with null parameters throws a NullPointerException")
	void verifyLVCCHashWithNullParametersThrows() {
		final VerifyLVCCHashInput input = inputBuilder.build();
		assertThrows(NullPointerException.class, () -> verifyLVCCHashAlgorithm.verifyLVCCHash(null, input));
		assertThrows(NullPointerException.class, () -> verifyLVCCHashAlgorithm.verifyLVCCHash(context, null));

	}

	@Test
	@DisplayName("verifyLVCCHash with too few other CCRs hashed Long Vote Cast Return Codes throws an IllegalArgumentException")
	void verifyLVCCHashWithTooFewOtherCCRsHashedLongVoteCastReturnCodesThrows() {
		otherCCRhlVCC = Stream.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(ControlComponentNode.ids().size() - 2)
				.collect(toImmutableList());
		final VerifyLVCCHashInput.Builder otherInput = inputBuilder.setOtherCCRsHashedLongVoteCastReturnCodes(otherCCRhlVCC);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, otherInput::build);
		assertEquals("The number of other CCRs hashed long Vote Cast Return Codes must be equal to the number of known node ids - 1.",
				exception.getMessage());
	}

	@Test
	@DisplayName("verifyLVCCHash with too many other CCRs hashed Long Vote Cast Return Codes throws an IllegalArgumentException")
	void verifyLVCCHashWithTooManyOtherCCRsHashedLongVoteCastReturnCodesThrows() {
		otherCCRhlVCC = Stream.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(ControlComponentNode.ids().size())
				.collect(toImmutableList());
		final VerifyLVCCHashInput.Builder otherInput = inputBuilder.setOtherCCRsHashedLongVoteCastReturnCodes(otherCCRhlVCC);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, otherInput::build);
		assertEquals("The number of other CCRs hashed long Vote Cast Return Codes must be equal to the number of known node ids - 1.",
				exception.getMessage());
	}

	@Test
	@DisplayName("verifyLVCCHash with LCC Share not created throws an IllegalArgumentException")
	void verifyLVCCHashWithLCCSharesNotCreated() {

		when(verificationCardStateServiceMock.isSentVote(verificationCardId)).thenReturn(false);
		when(verificationCardStateServiceMock.isNotConfirmedVote(verificationCardId)).thenReturn(true);

		final VerifyLVCCHashInput input = inputBuilder.build();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyLVCCHashAlgorithm.verifyLVCCHash(context, input));
		assertEquals(String.format("The CCR_j did not compute the long Choice Return Code shares for the verification card. [vc_id: %s]",
						verificationCardId),
				exception.getMessage());
	}

	@Test
	@DisplayName("verifyLVCCHash with vote confirmed throws an IllegalArgumentException")
	void verifyLVCCHashWithVoteConfirmed() {

		when(verificationCardStateServiceMock.isSentVote(verificationCardId)).thenReturn(true);
		when(verificationCardStateServiceMock.isNotConfirmedVote(verificationCardId)).thenReturn(false);

		final VerifyLVCCHashInput input = inputBuilder.build();

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> verifyLVCCHashAlgorithm.verifyLVCCHash(context, input));
		assertEquals(
				String.format("The CCR_j did already confirm the long Choice Return Code shares for the verification card. [vc_id: %s]",
						verificationCardId),
				exception.getMessage());
	}

	@Test
	@SuppressWarnings("java:S117")
	@DisplayName("verifyLVCCHash with hash present in list returns true")
	void verifyLVCCHashWithHashsPresentInAllowList() {
		final String electionEventId = context.electionEventId();
		final String verificationCardSetId = context.verificationCardSetId();

		when(verificationCardStateServiceMock.isSentVote(verificationCardId)).thenReturn(true);
		when(verificationCardStateServiceMock.isNotConfirmedVote(verificationCardId)).thenReturn(true);

		final HashableList i_aux = Stream.of("VerifyLVCCHash", electionEventId, verificationCardSetId, verificationCardId)
				.map(HashableString::from)
				.collect(toHashableList());
		final HashableString hlVCC_id_1 = HashableString.from(hlVCC1);
		final HashableString hlVCC_id_2 = HashableString.from(otherCCRhlVCC.get(0));
		final HashableString hlVCC_id_3 = HashableString.from(otherCCRhlVCC.get(1));
		final HashableString hlVCC_id_4 = HashableString.from(otherCCRhlVCC.get(2));
		final String hhlVCC_id = base64.base64Encode(hash.recursiveHash(i_aux, hlVCC_id_1, hlVCC_id_2, hlVCC_id_3, hlVCC_id_4));

		inputBuilder.setLongVoteCastReturnCodesAllowList(hhlVCC_id::equals);
		final VerifyLVCCHashInput input = inputBuilder.build();

		assertTrue(verifyLVCCHashAlgorithm.verifyLVCCHash(context, input));
	}

	@Test
	@DisplayName("verifyLVCCHash with hash not present in list returns false")
	void verifyLVCCHashWithHashsNotPresentInAllowList() {

		when(verificationCardStateServiceMock.isSentVote(verificationCardId)).thenReturn(true);
		when(verificationCardStateServiceMock.isNotConfirmedVote(verificationCardId)).thenReturn(true);

		final VerifyLVCCHashInput input = inputBuilder.build();

		assertFalse(verifyLVCCHashAlgorithm.verifyLVCCHash(context, input));
	}
}
