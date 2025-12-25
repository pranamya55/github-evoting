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

import ch.post.it.evoting.controlcomponent.process.ElectionEventContextService;
import ch.post.it.evoting.controlcomponent.process.LVCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.PCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

@DisplayName("An ExtractElectionEventAlgorithm with")
class ExtractElectionEventAlgorithmTest {

	private static final ElectionEventContextService electionEventContextService = mock(ElectionEventContextService.class);
	private static final SetupComponentPublicKeysService setupComponentPublicKeysService = mock(SetupComponentPublicKeysService.class);
	private static final LVCCAllowListEntryService lvccAllowListEntryService = mock(LVCCAllowListEntryService.class);
	private static final PCCAllowListEntryService pccAllowListEntryService = mock(PCCAllowListEntryService.class);

	private static String electionEventId;
	private static ExtractElectionEventAlgorithm extractElectionEventAlgorithm;

	@BeforeAll
	static void setUpAll() {
		final Base64 base64 = BaseEncodingFactory.createBase64();
		final Hash hash = HashFactory.createHash();
		final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm = new GetHashElectionEventContextAlgorithm(base64, hash);
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		final GetHashContextAlgorithm getHashContextAlgorithm = new GetHashContextAlgorithm(base64, hash, primesMappingTableAlgorithms);
		final ExtractVerificationCardSetAlgorithm extractVerificationCardSetAlgorithm = new ExtractVerificationCardSetAlgorithm(
				getHashContextAlgorithm, lvccAllowListEntryService, pccAllowListEntryService);

		extractElectionEventAlgorithm = new ExtractElectionEventAlgorithm(electionEventContextService, getHashElectionEventContextAlgorithm,
				setupComponentPublicKeysService, extractVerificationCardSetAlgorithm);

		electionEventId = UUIDGenerator.getInstance().generate();

		mockDB();
	}

	@Test
	@DisplayName("valid parameter does not throw")
	void validParamDoesNotThrow() {
		final ExtractedElectionEvent extractedElectionEvent = assertDoesNotThrow(
				() -> extractElectionEventAlgorithm.extractElectionEvent(electionEventId));

		assertEquals(electionEventId, extractedElectionEvent.electionEventId());
	}

	@Test
	@DisplayName("null election event id throws NullPointerException")
	void nullElectionEventIdThrows() {
		assertThrows(NullPointerException.class, () -> extractElectionEventAlgorithm.extractElectionEvent(null));
	}

	@Test
	@DisplayName("non UUID election event id throws FailedValidationException")
	void nonUUIDElectionEventIdThrows() {
		assertThrows(FailedValidationException.class, () -> extractElectionEventAlgorithm.extractElectionEvent("non-UUID"));
	}

	private static void mockDB() {
		// Create Election Event Context.
		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContext electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		when(electionEventContextService.getElectionEventContext(electionEventId)).thenReturn(electionEventContext);

		// Create Setup Component Public Keys.
		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator(
				electionEventContext.encryptionGroup());
		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayloadGenerator.generate(
						electionEventContext.maximumNumberOfSelections(), electionEventContext.maximumNumberOfWriteInsPlusOne())
				.getSetupComponentPublicKeys();
		when(setupComponentPublicKeysService.getCombinedControlComponentPublicKeys(electionEventId)).thenReturn(
				setupComponentPublicKeys.combinedControlComponentPublicKeys());
		when(setupComponentPublicKeysService.getElectionPublicKey(electionEventId)).thenReturn(setupComponentPublicKeys.electionPublicKey());
		when(setupComponentPublicKeysService.getChoiceReturnCodesEncryptionPublicKey(electionEventId)).thenReturn(
				setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey());

		// Create lVCC and pCC allow list.
		final Random random = RandomFactory.createRandom();
		final Base64Alphabet base64Alphabet = Base64Alphabet.getInstance();
		electionEventContext.verificationCardSetContexts().forEach(
				verificationCardSetContext -> {
					final ImmutableList<String> partialChoiceReturnCodesAllowList = Stream
							.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
							.limit(verificationCardSetContext.getNumberOfEligibleVoters())
							.collect(toImmutableList());
					when(pccAllowListEntryService.getPartialChoiceReturnCodes(verificationCardSetContext.getVerificationCardSetId()))
							.thenReturn(partialChoiceReturnCodesAllowList);

					final ImmutableList<String> longVoteCastReturnCodesAllowList = Stream
							.generate(() -> random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, base64Alphabet))
							.limit(verificationCardSetContext.getNumberOfEligibleVoters())
							.collect(toImmutableList());
					when(lvccAllowListEntryService.getLongVoteCastReturnCodes(verificationCardSetContext.getVerificationCardSetId()))
							.thenReturn(longVoteCastReturnCodesAllowList);
				}
		);
	}
}
