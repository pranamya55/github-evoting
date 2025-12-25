/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.sendvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_CHOICE_RETURN_CODE_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentlCCSharePayload;
import ch.post.it.evoting.domain.voting.sendvote.LongChoiceReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.SerializationUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTableSupplier;

@DisplayName("extractCRC called with")
class ExtractCRCServiceTest {

	private static final Random random = RandomFactory.createRandom();

	private static ExtractCRCService extractCRCService;
	private static ContextIds contextIds;
	private static ImmutableList<ControlComponentlCCSharePayload> controlComponentLCCSharePayloads;

	@BeforeAll
	static void setUpAll() {
		final ExtractCRCAlgorithm extractCRCAlgorithm = mock(ExtractCRCAlgorithm.class);
		final ElectionEventService electionEventService = mock(ElectionEventService.class);
		final VerificationCardService verificationCardService = mock(VerificationCardService.class);
		final IdentifierValidationService identifierValidationService = mock(IdentifierValidationService.class);
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		final ReturnCodesMappingTableSupplier returnCodesMappingTableSupplier = mock(ReturnCodesMappingTableSupplier.class);
		extractCRCService = new ExtractCRCService(extractCRCAlgorithm, electionEventService, verificationCardService, identifierValidationService,
				primesMappingTableAlgorithms, returnCodesMappingTableSupplier);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		contextIds = new ContextIds(uuidGenerator.generate(), uuidGenerator.generate(), uuidGenerator.generate());
		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator();
		final PrimesMappingTable primesMappingTable = primesMappingTableGenerator.generate(1);
		final int numberOfSelections = primesMappingTableAlgorithms.getPsi(primesMappingTable);
		controlComponentLCCSharePayloads = ControlComponentNode.ids().stream()
				.map(nodeId -> {
					final GroupVector<GqElement, GqGroup> longChoiceCodes = SerializationUtils.getLongChoiceCodes(numberOfSelections);
					final LongChoiceReturnCodeShare longChoiceReturnCodeShare = new LongChoiceReturnCodeShare(contextIds.electionEventId(),
							contextIds.verificationCardSetId(), contextIds.verificationCardId(), nodeId, longChoiceCodes);
					final ControlComponentlCCSharePayload controlComponentLCCSharePayload = new ControlComponentlCCSharePayload(
							longChoiceCodes.getGroup(), longChoiceReturnCodeShare);

					final Hash hash = createHash();
					controlComponentLCCSharePayload.setSignature(new CryptoPrimitivesSignature(hash.recursiveHash(controlComponentLCCSharePayload)));

					return controlComponentLCCSharePayload;
				})
				.collect(toImmutableList());

		doNothing().when(identifierValidationService).validateContextIds(contextIds);

		final GqGroup encryptionGroup = controlComponentLCCSharePayloads.get(0).getEncryptionGroup();
		when(electionEventService.getEncryptionGroup(contextIds.electionEventId())).thenReturn(encryptionGroup);

		when(verificationCardService.getPrimesMappingTable(contextIds.verificationCardId())).thenReturn(primesMappingTable);

		final ReturnCodesMappingTable returnCodesMappingTable = hashedLongReturnCode -> Optional.of(
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance()));
		when(returnCodesMappingTableSupplier.get(contextIds.verificationCardSetId())).thenReturn(returnCodesMappingTable);

		when(extractCRCAlgorithm.extractCRC(any(), any()))
				.thenReturn(new ExtractCRCOutput(random.genUniqueDecimalStrings(SHORT_CHOICE_RETURN_CODE_LENGTH, 2)));
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, controlComponentLCCSharePayloads),
				Arguments.of(contextIds, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void extractCRCWithNullParametersThrows(final ContextIds contextIds,
			final ImmutableList<ControlComponentlCCSharePayload> controlComponentLCCSharePayloads) {
		assertThrows(NullPointerException.class,
				() -> extractCRCService.extractCRC(contextIds, controlComponentLCCSharePayloads));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void extractCRCWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(() -> extractCRCService.extractCRC(contextIds, controlComponentLCCSharePayloads));
	}

}
