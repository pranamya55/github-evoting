/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.symmetric.Symmetric;
import ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricCiphertext;
import ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.utils.Conversions;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.Constants;

@DisplayName("Extract CRC algorithm calling extractCRC with")
class ExtractCRCAlgorithmTest extends TestGroupSetup {

	private static final Random random = RandomFactory.createRandom();
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static Hash hash;
	private static Base64 base64;
	private static Symmetric symmetric;
	private static KeyDerivation keyDerivation;
	private static ExtractCRCAlgorithm extractCRCAlgorithm;

	private final int psi = secureRandom.nextInt(1, 5);

	private ExtractCRCInput extractCRCInput;
	private ImmutableList<String> shortChoiceReturnCodes;
	private ExtractCRCContext extractCRCContext;

	@BeforeAll
	static void setUpAll() {
		hash = HashFactory.createHash();
		base64 = BaseEncodingFactory.createBase64();
		symmetric = SymmetricFactory.createSymmetric();
		keyDerivation = KeyDerivationFactory.createKeyDerivation();

		extractCRCAlgorithm = new ExtractCRCAlgorithm(hash, base64, symmetric, keyDerivation);
	}

	@BeforeEach
	void setUp() {
		shortChoiceReturnCodes = random.genUniqueDecimalStrings(SHORT_CHOICE_RETURN_CODE_LENGTH, psi);

		final String electionEventId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();
		final ImmutableList<String> blankCorrectnessInformation = ElectionSetupUtils.genBlankCorrectnessInformation(psi);
		extractCRCContext = new ExtractCRCContext(gqGroup, electionEventId, verificationCardId, blankCorrectnessInformation);

		final ImmutableList<GroupVector<GqElement, GqGroup>> longChoiceReturnCodeShares = IntStream.range(0, Constants.NUMBER_OF_CONTROL_COMPONENTS)
				.mapToObj(i -> gqGroupGenerator.genRandomGqElementVector(psi))
				.collect(toImmutableList());
		final Map<String, String> encodedLCCTable = getEncodedLCCTable(gqGroup, longChoiceReturnCodeShares, electionEventId, verificationCardId,
				blankCorrectnessInformation);
		extractCRCInput = new ExtractCRCInput(longChoiceReturnCodeShares,
				hashedLongReturnCode -> Optional.of(encodedLCCTable.get(hashedLongReturnCode)));
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void nullParamsThrows() {
		assertThrows(NullPointerException.class, () -> extractCRCAlgorithm.extractCRC(null, extractCRCInput));
		assertThrows(NullPointerException.class, () -> extractCRCAlgorithm.extractCRC(extractCRCContext, null));
	}

	@Test
	@DisplayName("context and input with different group throws IllegalArgumentException")
	void differentGqGroupThrows() {
		final ExtractCRCContext differentGroupExtractCRCContext = new ExtractCRCContext(otherGqGroup, extractCRCContext.electionEventId(),
				extractCRCContext.verificationCardId(), extractCRCContext.blankCorrectnessInformation());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> extractCRCAlgorithm.extractCRC(differentGroupExtractCRCContext, extractCRCInput));
		assertEquals("The context and input must have the same group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("blank correctness information and long Choice Return Code shares of different size throws IllegalArgumentException")
	void differentSizeBlankCorrectnessInformationLongChoiceReturnCodesSharesThrows() {
		final ImmutableList<String> incorrect = extractCRCContext.blankCorrectnessInformation()
				.append(uuidGenerator.generate());

		final ExtractCRCContext differentSizeExtractCRCContext = new ExtractCRCContext(extractCRCContext.encryptionGroup(),
				extractCRCContext.electionEventId(), extractCRCContext.verificationCardId(), incorrect);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> extractCRCAlgorithm.extractCRC(differentSizeExtractCRCContext, extractCRCInput));
		assertEquals("The blank correctness information and long Choice Return Code shares must have the same size psi.",
				Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("encrypted short Choice Return Code not in CMtable throws IllegalStateException")
	void encryptedShortChoiceReturnCodeNotInCMTableThrows() {
		final ExtractCRCInput emptyCMTableExtractCRCInput = new ExtractCRCInput(extractCRCInput.longChoiceReturnCodeShares(),
				hashedLongReturnCode -> Optional.empty());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> extractCRCAlgorithm.extractCRC(extractCRCContext, emptyCMTableExtractCRCInput));

		final String errorMessage = String.format(
				"Encrypted short Choice Return Code not found in CMtable. [electionEventId: %s, verificationCardId: %s, index: %s]",
				extractCRCContext.electionEventId(), extractCRCContext.verificationCardId(), 0);
		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters returns short Choice Return Codes")
	void validParamsReturnsShortChoiceReturnCodes() {
		final ExtractCRCOutput extractCRCOutput = extractCRCAlgorithm.extractCRC(extractCRCContext, extractCRCInput);

		assertEquals(shortChoiceReturnCodes, extractCRCOutput.shortChoiceReturnCodes());
	}

	@SuppressWarnings("java:S117")
	private Map<String, String> getEncodedLCCTable(final GqGroup encryptionGroup,
			final ImmutableList<GroupVector<GqElement, GqGroup>> longChoiceReturnCodeShares, final String electionEventId,
			final String verificationCardId,
			final ImmutableList<String> blankCorrectnessInformation) {

		final GqElement identity = GqElement.GqElementFactory.fromValue(BigInteger.ONE, encryptionGroup);

		final Map<String, String> encodedLCCTable = new HashMap<>(psi);
		for (int i = 0; i < psi; i++) {
			final int final_i = i;
			final GqElement combinedLCCShares = longChoiceReturnCodeShares.stream().map(lCC_j_id -> lCC_j_id.get(final_i))
					.reduce(identity, GqElement::multiply);

			final ImmutableByteArray hashedCombinedShares = hash.recursiveHash(
					combinedLCCShares,
					HashableString.from(verificationCardId),
					HashableString.from(electionEventId),
					HashableString.from(blankCorrectnessInformation.get(i)));
			final ImmutableByteArray skLCC = keyDerivation.KDF(hashedCombinedShares, ImmutableList.emptyList(), 32);
			final SymmetricCiphertext encryptedLCC = symmetric.genCiphertextSymmetric(skLCC,
					Conversions.stringToByteArray(shortChoiceReturnCodes.get(i)), ImmutableList.emptyList());

			final String key = base64.base64Encode(hash.recursiveHash(hashedCombinedShares));
			final String encodedLCC = base64.base64Encode(ImmutableByteArray.concat(encryptedLCC.ciphertext(), encryptedLCC.nonce()));
			encodedLCCTable.put(key, encodedLCC);
		}

		return encodedLCCTable;
	}

}
