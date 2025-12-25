/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.controlcomponent.process.PartialChoiceReturnCodeAllowList;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
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
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivation;
import ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

/**
 * Tests of CreateLCCShareAlgorithm.
 */
@DisplayName("CreateLCCShareAlgorithm")
class CreateLCCShareAlgorithmTest extends TestGroupSetup {

	private static final int NODE_ID = 1;
	private static final int PSI = 5;
	private static final Hash hash = spy(HashFactory.createHash());
	private static final Base64 base64 = BaseEncodingFactory.createBase64();
	private static final VerificationCardStateService verificationCardStateServiceMock = mock(VerificationCardStateService.class);
	private static final KeyDerivation keyDerivation = spy(KeyDerivationFactory.createKeyDerivation());
	private static CreateLCCShareAlgorithm createLCCShareAlgorithm;

	@BeforeAll
	static void setUpAll() {
		createLCCShareAlgorithm = new CreateLCCShareAlgorithm(hash, base64, keyDerivation,
				verificationCardStateServiceMock);
	}

	@Nested
	@DisplayName("calling createLCCShare with")
	class CreateLCCShareTest {

		private String verificationCardId;
		private CreateLCCShareContext context;
		private CreateLCCShareInput input;

		@BeforeEach
		@SuppressWarnings("java:S117")
		void setUp() {
			boolean allDistinct;
			GroupVector<GqElement, GqGroup> partialChoiceReturnCodes;
			do {
				partialChoiceReturnCodes = gqGroupGenerator.genRandomGqElementVector(PSI);
				allDistinct = partialChoiceReturnCodes.stream()
						.allMatch(ConcurrentHashMap.newKeySet()::add);
			}
			while (!allDistinct);

			final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
			final String electionEventId = uuidGenerator.generate();
			final String verificationCardSetId = uuidGenerator.generate();
			verificationCardId = uuidGenerator.generate();

			final GqGroup gqGroup = GroupTestData.getLargeGqGroup();
			final ZqGroup zqGroup = ZqGroup.sameOrderAs(gqGroup);
			final GqGroupGenerator gqGroupGenerator = new GqGroupGenerator(gqGroup);
			final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(zqGroup);
			final ZqElement ccrjReturnCodesGenerationSecretKey = zqGroupGenerator.genRandomZqElementMember();

			final PrimesMappingTable primesMappingTable = new PrimesMappingTableGenerator(gqGroup).generate(PSI, 1);

			final ImmutableList<String> ciSelections = new PrimesMappingTableAlgorithms().getBlankCorrectnessInformation(primesMappingTable);
			doReturn(ImmutableByteArray.of((byte) 0x4)).when(hash).recursiveHash(any());
			final ImmutableList<String> allowList = partialChoiceReturnCodes.stream()
					.map(pCC_id_i -> hash.hashAndSquare(pCC_id_i.getValue(), gqGroup))
					.map(hpCC_id_i -> hash.recursiveHash(hpCC_id_i, HashableString.from(verificationCardId), HashableString.from(electionEventId)))
					.map(base64::base64Encode)
					.collect(toImmutableList());

			boolean otherAllDistinct;
			GroupVector<GqElement, GqGroup> otherPartialChoiceReturnCodes;
			do {
				otherPartialChoiceReturnCodes = gqGroupGenerator.genRandomGqElementVector(PSI);
				otherAllDistinct = otherPartialChoiceReturnCodes.stream()
						.allMatch(ConcurrentHashMap.newKeySet()::add);
			}
			while (!otherAllDistinct);

			context = new CreateLCCShareContext(gqGroup, NODE_ID, electionEventId, verificationCardSetId, verificationCardId, ciSelections);
			input = new CreateLCCShareInput(allowList::contains, otherPartialChoiceReturnCodes, ccrjReturnCodesGenerationSecretKey);
		}

		@Test
		@DisplayName("valid parameters does not throw")
		void validParameters() {
			when(verificationCardStateServiceMock.isPartiallyDecrypted(verificationCardId)).thenReturn(true);
			when(verificationCardStateServiceMock.isNotSentVote(verificationCardId)).thenReturn(true);

			final CreateLCCShareOutput output = createLCCShareAlgorithm.createLCCShare(context, input);

			assertEquals(PSI, output.longChoiceReturnCodeShare().size());
		}

		@Test
		@DisplayName("any null parameter throws NullPointerException")
		void nullParameters() {
			assertAll(
					() -> assertThrows(NullPointerException.class, () -> createLCCShareAlgorithm.createLCCShare(null, input)),
					() -> assertThrows(NullPointerException.class, () -> createLCCShareAlgorithm.createLCCShare(context, null))
			);
		}

		@Test
		@DisplayName("codes and keys having different group order throws IllegalArgumentException")
		void diffGroupCodesKeys() {
			final PartialChoiceReturnCodeAllowList pCCAllowList = input.pCCAllowList();
			final ZqElement otherGroupSecretKey = otherZqGroupGenerator.genRandomZqElementMember();
			final GroupVector<GqElement, GqGroup> partialChoiceReturnCodes = input.partialChoiceReturnCodes();

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> new CreateLCCShareInput(pCCAllowList, partialChoiceReturnCodes, otherGroupSecretKey));
			assertEquals("The partial choice return codes and return codes generation secret key must have the same group order.",
					exception.getMessage());
		}

		@Test
		@DisplayName("partial codes not all distinct throws IllegalArgumentException")
		void notDistinctCodes() {
			final PartialChoiceReturnCodeAllowList pCCAllowlist = input.pCCAllowList();
			final GroupVector<GqElement, GqGroup> notDistinctCodes = GroupVector.from(input.partialChoiceReturnCodes())
					.append(input.partialChoiceReturnCodes().getFirst());
			final ZqElement ccrjReturnCodesGenerationSecretKey = input.ccrjReturnCodesGenerationSecretKey();

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> new CreateLCCShareInput(pCCAllowlist, notDistinctCodes, ccrjReturnCodesGenerationSecretKey));
			assertEquals("All pCC must be distinct.", exception.getMessage());
		}

		@Test
		@DisplayName("not yet partially decrypted codes throws IllegalArgumentException")
		void didNotYetPartiallyDecrypt() {
			when(verificationCardStateServiceMock.isPartiallyDecrypted(verificationCardId)).thenReturn(false);
			when(verificationCardStateServiceMock.isNotSentVote(verificationCardId)).thenReturn(true);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> createLCCShareAlgorithm.createLCCShare(context, input));
			assertEquals(
					String.format("The partial Choice Return Codes have not yet been partially decrypted. [vc_id: %s].",
							verificationCardId), exception.getMessage());
		}

		@Test
		@DisplayName("long choice return code shares already generated throws IllegalArgumentException")
		void alreadyGeneratedLongChoiceReturnCodesShare() {
			when(verificationCardStateServiceMock.isPartiallyDecrypted(verificationCardId)).thenReturn(true);
			when(verificationCardStateServiceMock.isNotSentVote(verificationCardId)).thenReturn(false);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> createLCCShareAlgorithm.createLCCShare(context, input));
			assertEquals(
					String.format("The CCR_j already generated the long Choice Return Code share in a previous attempt. [vc_id: %s].",
							verificationCardId), exception.getMessage());
		}

		@Test
		@DisplayName("partial choice return code not in allow list throws IllegalArgumentException")
		void pccNotInAllowList() {
			when(verificationCardStateServiceMock.isPartiallyDecrypted(verificationCardId)).thenReturn(true);
			when(verificationCardStateServiceMock.isNotSentVote(verificationCardId)).thenReturn(true);

			final GroupVector<GqElement, GqGroup> partialChoiceReturnCodes = input.partialChoiceReturnCodes();
			final ZqElement ccrjReturnCodesGenerationSecretKey = input.ccrjReturnCodesGenerationSecretKey();

			final CreateLCCShareInput otherInput = new CreateLCCShareInput(partialChoiceCode -> false, partialChoiceReturnCodes,
					ccrjReturnCodesGenerationSecretKey);
			final IllegalStateException exception = assertThrows(IllegalStateException.class,
					() -> createLCCShareAlgorithm.createLCCShare(context, otherInput));
			assertEquals("The partial Choice Return Codes allow list does not contain the partial Choice Return Code.", exception.getMessage());
		}
	}

}
