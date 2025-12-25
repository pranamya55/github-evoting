/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.protocol.voting.confirmvote;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.SHORT_VOTE_CAST_RETURN_CODE_LENGTH;
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
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponentlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.LongVoteCastReturnCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.votingserver.process.ElectionEventService;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTable;
import ch.post.it.evoting.votingserver.process.voting.ReturnCodesMappingTableSupplier;

@DisplayName("extractVCC called with")
class ExtractVCCServiceTest {

	private static final Random random = RandomFactory.createRandom();

	private static ExtractVCCService extractVCCService;
	private static ContextIds contextIds;
	private static ImmutableList<ControlComponentlVCCSharePayload> controlComponentlVCCSharePayloads;

	@BeforeAll
	static void setUpAll() {
		final ExtractVCCAlgorithm extractVCCAlgorithm = mock(ExtractVCCAlgorithm.class);
		final ElectionEventService electionEventService = mock(ElectionEventService.class);
		final IdentifierValidationService identifierValidationService = mock(IdentifierValidationService.class);
		final ReturnCodesMappingTableSupplier returnCodesMappingTableSupplier = mock(ReturnCodesMappingTableSupplier.class);
		extractVCCService = new ExtractVCCService(extractVCCAlgorithm, electionEventService, identifierValidationService,
				returnCodesMappingTableSupplier);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		contextIds = new ContextIds(uuidGenerator.generate(), uuidGenerator.generate(), uuidGenerator.generate());

		final GqGroup encryptionGroup = GroupTestData.getLargeGqGroup();
		controlComponentlVCCSharePayloads = ControlComponentNode.ids().stream()
				.map(nodeId -> {
					final GqElement randomGqElement = new GqGroupGenerator(encryptionGroup).genMember();
					final LongVoteCastReturnCodeShare longVoteCastReturnCodeShare = new LongVoteCastReturnCodeShare(contextIds.electionEventId(),
							contextIds.verificationCardSetId(), contextIds.verificationCardId(), nodeId, randomGqElement);
					final ControlComponentlVCCSharePayload controlComponentlVCCSharePayload = new ControlComponentlVCCSharePayload(
							contextIds.electionEventId(), contextIds.verificationCardSetId(), contextIds.verificationCardId(), nodeId,
							encryptionGroup, longVoteCastReturnCodeShare, new ConfirmationKey(contextIds, randomGqElement), true);

					final Hash hash = createHash();
					controlComponentlVCCSharePayload.setSignature(
							new CryptoPrimitivesSignature(hash.recursiveHash(controlComponentlVCCSharePayload)));

					return controlComponentlVCCSharePayload;
				})
				.collect(toImmutableList());

		doNothing().when(identifierValidationService).validateContextIds(contextIds);
		when(electionEventService.getEncryptionGroup(contextIds.electionEventId())).thenReturn(encryptionGroup);

		final ReturnCodesMappingTable returnCodesMappingTable = hashedLongReturnCode -> Optional.of(
				random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance()));
		when(returnCodesMappingTableSupplier.get(contextIds.verificationCardSetId())).thenReturn(returnCodesMappingTable);

		when(extractVCCAlgorithm.extractVCC(any(), any()))
				.thenReturn(new ExtractVCCOutput(random.genUniqueDecimalStrings(SHORT_VOTE_CAST_RETURN_CODE_LENGTH, 1).get(0)));
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, controlComponentlVCCSharePayloads),
				Arguments.of(contextIds, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void extractVCCWithNullParametersThrows(final ContextIds contextIds,
			final ImmutableList<ControlComponentlVCCSharePayload> controlComponentlVCCSharePayloads) {
		assertThrows(NullPointerException.class,
				() -> extractVCCService.extractVCC(contextIds, controlComponentlVCCSharePayloads));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void extractVCCWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(() -> extractVCCService.extractVCC(contextIds, controlComponentlVCCSharePayloads));
	}

}
