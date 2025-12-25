/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.download;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mapper.DomainObjectMapper;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.configuration.EncLongCodeShareEntity;
import ch.post.it.evoting.votingserver.process.configuration.EncLongCodeShareRepository;

@DisplayName("DownloadEncryptedLongReturnCodeSharesService calling")
class DownloadEncryptedLongReturnCodeSharesServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static ObjectMapper objectMapper;
	private static EncLongCodeShareRepository encLongCodeShareRepository;
	private static DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService;

	private String electionEventId;
	private String verificationCardSetId;
	private int chunkId;

	@BeforeAll
	static void setupAll() {
		objectMapper = DomainObjectMapper.getNewInstance();
		encLongCodeShareRepository = mock(EncLongCodeShareRepository.class);
		downloadEncryptedLongReturnCodeSharesService = new DownloadEncryptedLongReturnCodeSharesService(encLongCodeShareRepository);
	}

	@BeforeEach
	void setup() {
		reset(encLongCodeShareRepository);

		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		chunkId = 0;
	}

	@DisplayName("download with null arguments throws a NullPointerException")
	@Test
	void getEncLongCodeSharesWithNullArgumentsThrows() {
		assertThrows(NullPointerException.class,
				() -> downloadEncryptedLongReturnCodeSharesService.download(null, verificationCardSetId, chunkId));
		assertThrows(NullPointerException.class, () -> downloadEncryptedLongReturnCodeSharesService.download(electionEventId, null, chunkId));
	}

	@DisplayName("download with non UUID arguments throws a FailedValidationException")
	@Test
	void getEncLongCodeSharesWithNonUuidArgumentsThrows() {
		assertThrows(FailedValidationException.class,
				() -> downloadEncryptedLongReturnCodeSharesService.download("nonUUID", verificationCardSetId, chunkId));
		assertThrows(FailedValidationException.class,
				() -> downloadEncryptedLongReturnCodeSharesService.download(electionEventId, "nonUUID", chunkId));
	}

	@DisplayName("download with chunk id smaller than zero throws an IllegalArgumentException")
	@Test
	void getEncLongCodeSharesWithChunkIdSmallerZeroThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> downloadEncryptedLongReturnCodeSharesService.download(electionEventId, verificationCardSetId, -1));
	}

	@DisplayName("download when the number of GenEncLongCodeShares is not a multiple of the number of control components throws an IllegalStateException")
	@Test
	void getEncLongCodeSharesWithBadNumberOfGenEncLongCodeSharesThrows() {
		doReturn(List.of(Mockito.mock(EncLongCodeShareEntity.class)))
				.when(encLongCodeShareRepository)
				.findByVerificationCardSetIdAndChunkId(verificationCardSetId, chunkId);
		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> downloadEncryptedLongReturnCodeSharesService.download(electionEventId, verificationCardSetId, chunkId));
		final String expectedErrorMessage = "The number of enc long code shares doesn't match the number of nodes. [numberOfEncLongCodeShares: 1, numberOfControlComponents: 4]";
		assertEquals(expectedErrorMessage, Throwables.getRootCause(exception).getMessage());
	}

	@DisplayName("download with valid input returns list of ControlComponentCodeSharesPayloads")
	@Test
	void getEncLongCodeSharesWithValidInputReturnsControlComponentCodeSharesPayloads() {
		final GqGroup gqGroup = GroupTestData.getGqGroup();
		final ImmutableList<EncLongCodeShareEntity> encLongCodeShareEntities = IntStream.rangeClosed(1, 4)
				.mapToObj(nodeId -> {
					final ImmutableList<ControlComponentCodeShare> controlComponentCodeShares = Stream.generate(
									() -> createControlComponentCodeShare(gqGroup))
							.limit(5)
							.collect(toImmutableList());
					final CryptoPrimitivesSignature payloadSignature = new CryptoPrimitivesSignature(
							ImmutableByteArray.of((byte) 0b0101010));
					final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload = new ControlComponentCodeSharesPayload(gqGroup,
							electionEventId, verificationCardSetId, nodeId, chunkId, controlComponentCodeShares, payloadSignature);
					try {
						final ImmutableByteArray controlComponentCodeSharesPayloadBytes = new ImmutableByteArray(objectMapper.writeValueAsBytes(
								controlComponentCodeSharesPayload));
						return new EncLongCodeShareEntity(verificationCardSetId, chunkId, nodeId, controlComponentCodeSharesPayloadBytes);
					} catch (final JsonProcessingException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(toImmutableList());
		doReturn(encLongCodeShareEntities.asList()).when(encLongCodeShareRepository)
				.findByVerificationCardSetIdAndChunkId(verificationCardSetId, chunkId);

		final ImmutableList<ImmutableByteArray> controlComponentCodeSharesPayloadsBytes = assertDoesNotThrow(
				() -> downloadEncryptedLongReturnCodeSharesService.download(electionEventId, verificationCardSetId, chunkId));
		assertEquals(encLongCodeShareEntities.size(), controlComponentCodeSharesPayloadsBytes.size());
		for (int i = 0; i < controlComponentCodeSharesPayloadsBytes.size(); i++) {
			final ImmutableByteArray expectedPayloadBytes = encLongCodeShareEntities.get(i).getEncLongCodeShare();
			assertEquals(expectedPayloadBytes, controlComponentCodeSharesPayloadsBytes.get(i));
		}
	}

	private ControlComponentCodeShare createControlComponentCodeShare(final GqGroup gqGroup) {
		final String verificationCardId = uuidGenerator.generate();
		final ZqGroup zqGroup = ZqGroup.sameOrderAs(gqGroup);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(zqGroup);
		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
		final ElGamalMultiRecipientPublicKey voterChoiceReturnCodeGenerationPublicKey = elGamalGenerator.genRandomPublicKey(1);
		final ElGamalMultiRecipientPublicKey voterVoteCastReturnCodeGenerationPublicKey = elGamalGenerator.genRandomPublicKey(1);
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedPartialChoiceReturnCodes = elGamalGenerator.genRandomCiphertext(1);
		final ExponentiationProof encryptedPartialChoiceReturnCodeExponentiationProof = new ExponentiationProof(
				zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember());
		final ElGamalMultiRecipientCiphertext exponentiatedEncryptedConfirmationKey = elGamalGenerator.genRandomCiphertext(1);
		final ExponentiationProof encryptedConfirmationKeyExponentiationProof = new ExponentiationProof(zqGroupGenerator.genRandomZqElementMember(),
				zqGroupGenerator.genRandomZqElementMember());
		return new ControlComponentCodeShare(verificationCardId, voterChoiceReturnCodeGenerationPublicKey, voterVoteCastReturnCodeGenerationPublicKey,
				exponentiatedEncryptedPartialChoiceReturnCodes, exponentiatedEncryptedConfirmationKey,
				encryptedPartialChoiceReturnCodeExponentiationProof, encryptedConfirmationKeyExponentiationProof);
	}
}
