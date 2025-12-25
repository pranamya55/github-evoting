/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponent.process.LVCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCardSet;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

@DisplayName("An ExtractVerificationCardSetAlgorithm with")
class ExtractVerificationCardSetAlgorithmTest {

	private static final PCCAllowListEntryService pccAllowListEntryService = mock(PCCAllowListEntryService.class);
	private static final LVCCAllowListEntryService lvccAllowListEntryService = mock(LVCCAllowListEntryService.class);

	private static ExtractVerificationCardSetContext extractVerificationCardSetContext;
	private static ExtractVerificationCardSetAlgorithm extractVerificationCardSetAlgorithm;

	@BeforeAll
	static void setUpAll() {
		final Base64 base64 = BaseEncodingFactory.createBase64();
		final Hash hash = HashFactory.createHash();
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		final GetHashContextAlgorithm getHashContextAlgorithm = new GetHashContextAlgorithm(base64, hash, primesMappingTableAlgorithms);
		extractVerificationCardSetAlgorithm = new ExtractVerificationCardSetAlgorithm(getHashContextAlgorithm, lvccAllowListEntryService,
				pccAllowListEntryService);

		extractVerificationCardSetContext = createExtractVerificationCardSetContext();

		final int numberOfVotingOptions = extractVerificationCardSetContext.primesMappingTable().getNumberOfVotingOptions();
		mockDB(extractVerificationCardSetContext.verificationCardSetId(), numberOfVotingOptions);
	}

	private static ExtractVerificationCardSetContext createExtractVerificationCardSetContext() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final String electionEventId = uuidGenerator.generate();
		final String verificationCardSetId = uuidGenerator.generate();

		final int numberOfSelections = 10;
		final int numberOfWriteInsPlusOne = 3;
		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator();
		final PrimesMappingTable primesMappingTable = primesMappingTableGenerator.generate(numberOfSelections, numberOfWriteInsPlusOne);
		final GqGroup encryptionGroup = primesMappingTable.getEncryptionGroup();

		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator();
		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayloadGenerator.generate(numberOfSelections,
				numberOfWriteInsPlusOne).getSetupComponentPublicKeys();
		final ElGamalMultiRecipientPublicKey electionPublicKey = setupComponentPublicKeys.electionPublicKey();
		final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey = setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey();

		return new ExtractVerificationCardSetContext(encryptionGroup, electionEventId, verificationCardSetId, primesMappingTable, electionPublicKey,
				choiceReturnCodesEncryptionPublicKey);
	}

	@Test
	@DisplayName("valid parameter does not throw")
	void validParamDoesNotThrow() {
		final ExtractedVerificationCardSet extractedVerificationCardSet = assertDoesNotThrow(
				() -> extractVerificationCardSetAlgorithm.extractVerificationCardSet(extractVerificationCardSetContext));

		assertEquals(extractVerificationCardSetContext.verificationCardSetId(), extractedVerificationCardSet.verificationCardSetId());
	}

	@Test
	@DisplayName("null context throws NullPointerException")
	void nullContextThrows() {
		assertThrows(NullPointerException.class, () -> extractVerificationCardSetAlgorithm.extractVerificationCardSet(null));
	}

	private static void mockDB(final String verificationCardSetId, final int numberOfVotingOptions) {
		final Random random = RandomFactory.createRandom();
		final Base64Alphabet base64Alphabet = Base64Alphabet.getInstance();
		final int numberOfEligibleVoters = 3;
		final ImmutableList<String> partialChoiceReturnCodesAllowList = Stream
				.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit((long) numberOfVotingOptions * numberOfEligibleVoters)
				.collect(toImmutableList());
		when(pccAllowListEntryService.getPartialChoiceReturnCodes(verificationCardSetId)).thenReturn(partialChoiceReturnCodesAllowList);

		final ImmutableList<String> longVoteCastReturnCodesAllowList = Stream
				.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
				.limit(numberOfEligibleVoters)
				.collect(toImmutableList());
		when(lvccAllowListEntryService.getLongVoteCastReturnCodes(verificationCardSetId)).thenReturn(longVoteCastReturnCodesAllowList);
	}
}