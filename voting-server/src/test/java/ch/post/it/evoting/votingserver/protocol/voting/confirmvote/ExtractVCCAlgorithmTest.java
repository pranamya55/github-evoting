/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_VOTE_CAST_RETURN_CODE_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Optional;

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
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.Constants;

@DisplayName("ExtractVCC algorithm calling extractVCC with")
class ExtractVCCAlgorithmTest extends TestGroupSetup {

	private static final Random random = RandomFactory.createRandom();
	private static final Base64 base64 = BaseEncodingFactory.createBase64();

	private static Hash hash;
	private static Symmetric symmetric;
	private static KeyDerivation keyDerivation;
	private static ExtractVCCAlgorithm extractVCCAlgorithm;

	private String shortVoteCastReturnCode;
	private ExtractVCCInput extractVCCInput;
	private ExtractVCCContext extractVCCContext;

	@BeforeAll
	static void setUpAll() {
		hash = HashFactory.createHash();
		symmetric = SymmetricFactory.createSymmetric();
		keyDerivation = KeyDerivationFactory.createKeyDerivation();

		extractVCCAlgorithm = new ExtractVCCAlgorithm(hash, symmetric, keyDerivation);
	}

	@BeforeEach
	void setUp() {
		shortVoteCastReturnCode = random.genUniqueDecimalStrings(SHORT_VOTE_CAST_RETURN_CODE_LENGTH, 1).get(0);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();
		extractVCCContext = new ExtractVCCContext(gqGroup, electionEventId, verificationCardId);

		final GroupVector<GqElement, GqGroup> longVoteCastReturnCodeShares = gqGroupGenerator.genRandomGqElementVector(
				Constants.NUMBER_OF_CONTROL_COMPONENTS);
		final String encodedLVCC = getEncodedLVCC(gqGroup, longVoteCastReturnCodeShares, verificationCardId, electionEventId);
		extractVCCInput = new ExtractVCCInput(longVoteCastReturnCodeShares,
				hashedLongReturnCode -> Optional.of(encodedLVCC));
	}

	@Test
	@DisplayName("any null parameter throws NullPointedException")
	void nullParamsThrows() {
		assertThrows(NullPointerException.class, () -> extractVCCAlgorithm.extractVCC(null, extractVCCInput));
		assertThrows(NullPointerException.class, () -> extractVCCAlgorithm.extractVCC(extractVCCContext, null));
	}

	@Test
	@DisplayName("context and input with different group throws IllegalArgumentException")
	void differentGqGroupThrows() {
		final ExtractVCCContext differentExtractVCCContext = new ExtractVCCContext(otherGqGroup, extractVCCContext.electionEventId(),
				extractVCCContext.verificationCardId());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> extractVCCAlgorithm.extractVCC(differentExtractVCCContext, extractVCCInput));
		assertEquals("The context and input must have the same group.", Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("encrypted short Vote Cast Return Code not in CMtable throws IllegalStateException")
	void encryptedShortVoteCastReturnCodeNotInCMTableThrows() {
		final ExtractVCCInput emptyCMTableExtractVCCInput = new ExtractVCCInput(extractVCCInput.longVoteCastReturnCodeShares(),
				hashedLongReturnCode -> Optional.empty());

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> extractVCCAlgorithm.extractVCC(extractVCCContext, emptyCMTableExtractVCCInput));

		final String errorMessage = String.format(
				"Encrypted short Vote Cast Return Code not found in CMtable. [electionEventId: %s, verificationCardId: %s]",
				extractVCCContext.electionEventId(), extractVCCContext.verificationCardId());
		assertEquals(errorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters returns short Vote Cast Return code")
	void validParamsReturnsShortVoteCastReturnCode() {
		final ExtractVCCOutput extractVCCOutput = extractVCCAlgorithm.extractVCC(extractVCCContext, extractVCCInput);

		assertEquals(shortVoteCastReturnCode, extractVCCOutput.shortVoteCastReturnCode());
	}

	private String getEncodedLVCC(final GqGroup encryptionGroup, final GroupVector<GqElement, GqGroup> longVoteCastReturnCodeShares,
			final String verificationCardId, final String electionEventId) {

		final GqElement identity = GqElement.GqElementFactory.fromValue(BigInteger.ONE, encryptionGroup);
		final GqElement combinedShares = longVoteCastReturnCodeShares.stream().reduce(identity, GqElement::multiply);
		final ImmutableByteArray hashedCombinedShares = hash.recursiveHash(combinedShares, HashableString.from(verificationCardId),
				HashableString.from(electionEventId));
		final ImmutableByteArray skVCC = keyDerivation.KDF(hashedCombinedShares, ImmutableList.emptyList(), 32);

		final SymmetricCiphertext encryptedLVCC = symmetric.genCiphertextSymmetric(skVCC, Conversions.stringToByteArray(shortVoteCastReturnCode),
				ImmutableList.emptyList());

		return base64.base64Encode(ImmutableByteArray.concat(encryptedLVCC.ciphertext(), encryptedLVCC.nonce()));
	}

}