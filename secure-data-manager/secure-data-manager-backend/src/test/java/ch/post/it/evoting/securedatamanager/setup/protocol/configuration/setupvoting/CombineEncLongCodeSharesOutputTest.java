/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("CombineEncLongCodeSharesOutput with")
class CombineEncLongCodeSharesOutputTest extends TestGroupSetup {

	private final Random random = RandomFactory.createRandom();
	private final Base64Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedPreChoiceReturnCodesVector4;
	private GroupVector<GqElement, GqGroup> preVoteCastReturnCodes4G1;
	private GroupVector<GqElement, GqGroup> preVoteCastReturnCodes3G1;
	private GroupVector<GqElement, GqGroup> preVoteCastReturnCodes4G2;
	private ImmutableList<String> longVoteCastReturnCodesAllowList4;
	private ImmutableList<String> longVoteCastReturnCodesAllowList3;

	@BeforeEach
	void setUp() {
		final ElGamal elGamal = ElGamalFactory.createElGamal();
		final int numberOfVotingOptions = 10;
		encryptedPreChoiceReturnCodesVector4 = GroupVector.of(elGamal.neutralElement(numberOfVotingOptions, gqGroup),
				elGamal.neutralElement(numberOfVotingOptions, gqGroup),
				elGamal.neutralElement(numberOfVotingOptions, gqGroup),
				elGamal.neutralElement(numberOfVotingOptions, gqGroup));

		preVoteCastReturnCodes4G1 = GroupVector.of(gqGroup.getGenerator(), gqGroup.getGenerator(), gqGroup.getGenerator(),
				gqGroup.getGenerator());

		preVoteCastReturnCodes3G1 = GroupVector.of(gqGroup.getGenerator(), gqGroup.getGenerator(),
				gqGroup.getGenerator());

		preVoteCastReturnCodes4G2 = GroupVector.of(otherGqGroup.getGenerator(), otherGqGroup.getGenerator(),
				otherGqGroup.getGenerator(),
				otherGqGroup.getGenerator());

		longVoteCastReturnCodesAllowList4 = Stream.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(4)
				.collect(toImmutableList());

		longVoteCastReturnCodesAllowList3 = Stream.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(3)
				.collect(toImmutableList());
	}

	@Test
	@DisplayName("null Vector encrypted pre-Choice Return Codes")
	void outputWithNullEncryptedPreChoiceReturnCodes() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(null)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes4G1)
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList4);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("null Vector pre-Choice Return Codes")
	void outputWithNullPreVoteCastReturnCodes() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(null)
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList4);

		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("null long Vote Cast Return Codes allow list")
	void outputWithNullLongVoteCastReturnCodesAllowList() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes4G1)
				.setLongVoteCastReturnCodesAllowList(null);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	@DisplayName("empty Vector Encrypted Pre-Choice Return Codes")
	void outputWithEmptyPreChoiceReturnCodes() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(GroupVector.from(ImmutableList.emptyList()))
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes4G1)
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList3);

		final Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The vector of encrypted pre-Choice Return Codes must have more than zero elements.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("empty Vector of pre-Vote Cast Return Codes")
	void outputWithEmptyPreVoteCastReturnCodes() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(GroupVector.from(ImmutableList.emptyList()))
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList3);

		final Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = String.format(
				"The vector of pre-Vote Cast Return Codes is of incorrect size [size: expected: %s, actual: %s].",
				encryptedPreChoiceReturnCodesVector4.size(), 0);
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("empty long Vote Cast Return Codes allow list")
	void outputWithEmptyLongVoteCastReturnCodesAllowList() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes4G1)
				.setLongVoteCastReturnCodesAllowList(ImmutableList.emptyList());

		final Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = String.format(
				"The long Vote Cast Return Codes allow list is of incorrect size [size: expected: %s, "
						+ "actual: %s].", encryptedPreChoiceReturnCodesVector4.size(), 0);
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("incorrect size of the pre-Vote Cast Return Codes")
	void buildInputWithWrongChoiceReturnCodesMatrixColumnSize() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes3G1)
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList4);

		final Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = String.format(
				"The vector of pre-Vote Cast Return Codes is of incorrect size [size: expected: %s, actual: %s].",
				encryptedPreChoiceReturnCodesVector4.size(), preVoteCastReturnCodes3G1.size());
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("incorrect size of the long Vote Cast Return Codes allow list")
	void buildInputWithWrongLongVoteCastReturnCodesAllowListSize() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes4G1)
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList3);

		final Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = String.format(
				"The long Vote Cast Return Codes allow list is of incorrect size [size: expected: %s, actual: %s].",
				encryptedPreChoiceReturnCodesVector4.size(), longVoteCastReturnCodesAllowList3.size());
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("incorrect size of the long Vote Cast Return Codes")
	void buildInputWithWrongLongVoteCastReturnCodesSize() {
		final ImmutableList<String> incorrectSizeLVCCAllowListElements = ImmutableList.of("lVCC1", "lVCC2", "lVCC3", "lVCC4");
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes4G1)
				.setLongVoteCastReturnCodesAllowList(incorrectSizeLVCCAllowListElements);

		final Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = String.format(
				"The long Vote Cast Return Code must be of size l_HB64. [size: %s, l_HB64: %s]",
				incorrectSizeLVCCAllowListElements.getFirst().length(), BASE64_ENCODED_HASH_OUTPUT_LENGTH);
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("not base64 encoded long Vote Cast Return Codes")
	void buildInputWithNotBase64EncodedLongVoteCastReturnCodes() {
		final ImmutableList<String> notBase64EncodedLVCCAllowListElements = Stream.generate(
						() -> "!" + random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH - 1, base64Alphabet))
				.limit(4)
				.collect(toImmutableList());
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes4G1)
				.setLongVoteCastReturnCodesAllowList(notBase64EncodedLVCCAllowListElements);

		final Exception exception = assertThrows(FailedValidationException.class, builder::build);

		final String expectedMessage = "The given string is not a valid Base64 encoded string.";
		final String actualMessage = exception.getMessage();

		assertTrue(actualMessage.startsWith(expectedMessage));
	}

	@Test
	@DisplayName("the Vector of pre-Choice Return Codes and the Vector of pre-Vote Cast Return Codes have different Gd groups")
	void buildInputWithWrongConfirmationKeysMatrixRowSize() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes4G2)
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList4);

		final Exception exception = assertThrows(IllegalArgumentException.class, builder::build);

		final String expectedMessage = "The vector of encrypted pre-Choice Return Codes and the vector of pre-Vote Cast Return Codes do not have the same group order.";
		final String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	@DisplayName("output for input with same size and Gd group")
	void outputWithCorrectValues() {
		final CombineEncLongCodeSharesOutput.Builder builder = new CombineEncLongCodeSharesOutput.Builder()
				.setEncryptedPreChoiceReturnCodesVector(encryptedPreChoiceReturnCodesVector4)
				.setPreVoteCastReturnCodesVector(preVoteCastReturnCodes4G1)
				.setLongVoteCastReturnCodesAllowList(longVoteCastReturnCodesAllowList4);

		final CombineEncLongCodeSharesOutput output = builder.build();

		assertEquals(encryptedPreChoiceReturnCodesVector4, output.getEncryptedPreChoiceReturnCodesVector());
		assertEquals(preVoteCastReturnCodes4G1, output.getPreVoteCastReturnCodesVector());
		assertEquals(longVoteCastReturnCodesAllowList4, output.getLongVoteCastReturnCodesAllowList());
	}
}
