/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodes;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodesPayload;
import ch.post.it.evoting.domain.generators.SetupComponentTallyDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKeyPayloadService;
import ch.post.it.evoting.securedatamanager.setup.process.VoterInitialCodesPayloadService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenCredDatOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenCredDatService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentTallyDataPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

@DisplayName("SetupComponentVerificationCardKeystoresPayloadGenerationService")
class SetupComponentVerificationCardKeystoresPayloadGenerationServiceTest {

	private static final Alphabet base64Alphabet = Base64Alphabet.getInstance();

	private static GqGroup gqGroup;
	private static String electionEventId;
	private static int numberOfEligibleVoters;
	private static String verificationCardSetId;
	private static ElGamalMultiRecipientPublicKey electionPublicKey;
	private static ElGamalMultiRecipientPublicKey electoralBoardPublicKey;
	private static ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey;
	private static GenCredDatService genCredDatService;
	private static ElectionEventService electionEventService;
	private static SignatureKeystore<Alias> signatureKeystoreService;
	private static VerificationCardSetService verificationCardSetService;
	private static SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService;
	private static SetupComponentVerificationCardKeystoresPayloadGenerationService generationService;

	@BeforeEach
	void setUp() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		verificationCardSetId = uuidGenerator.generate();
		numberOfEligibleVoters = 10;

		final SetupComponentPublicKeys setupComponentPublicKeys = new SetupComponentPublicKeysPayloadGenerator().generate()
				.getSetupComponentPublicKeys();
		gqGroup = setupComponentPublicKeys.electionPublicKey().getGroup();
		choiceReturnCodesEncryptionPublicKey = setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey();
		electionPublicKey = setupComponentPublicKeys.electionPublicKey();
		electoralBoardPublicKey = setupComponentPublicKeys.electoralBoardPublicKey();

		verificationCardSetService = mock(VerificationCardSetService.class);
		genCredDatService = mock(GenCredDatService.class);
		electionEventService = mock(ElectionEventService.class);
		signatureKeystoreService = mock(SignatureKeystore.class);
		final VoterInitialCodesPayloadService voterInitialCodesPayloadService = mock(VoterInitialCodesPayloadService.class);
		final ElectionEventContextPayloadService electionEventContextPayloadService = mock(ElectionEventContextPayloadService.class);
		setupComponentTallyDataPayloadService = mock(SetupComponentTallyDataPayloadService.class);
		final VerificationCardSecretKeyPayloadService verificationCardSecretKeyPayloadService = mock(
				VerificationCardSecretKeyPayloadService.class);
		final SetupComponentVerificationCardKeystoresPayloadService setupComponentVerificationCardKeystoresPayloadService
				= mock(SetupComponentVerificationCardKeystoresPayloadService.class);

		generationService = new SetupComponentVerificationCardKeystoresPayloadGenerationService(
				genCredDatService,
				electionEventService,
				signatureKeystoreService,
				verificationCardSetService,
				voterInitialCodesPayloadService,
				electionEventContextPayloadService,
				setupComponentTallyDataPayloadService,
				verificationCardSecretKeyPayloadService,
				setupComponentVerificationCardKeystoresPayloadService);
	}

	@Nested
	@DisplayName("calling generate with")
	class GenerateArgumentsTest {

		@Test
		@DisplayName("an invalid election event id input throws a FailedValidationException.")
		void invalidElectionEventIdArguments() {
			final String invalidElectionEventId = "electionEventId";
			assertThrows(FailedValidationException.class,
					() -> generationService.generate(invalidElectionEventId, choiceReturnCodesEncryptionPublicKey, electionPublicKey));
		}

		@Test
		@DisplayName("an election event id not in the database throws an IllegalArgumentException")
		void nonExistingElectionEventIdArgument() {
			final String nonExistingElectionEventId = "0123456789ABCDEF0123456789ABCDEF";
			assertThrows(IllegalArgumentException.class,
					() -> generationService.generate(nonExistingElectionEventId, choiceReturnCodesEncryptionPublicKey, electionPublicKey));
		}

		@Test
		@DisplayName("any null input throws a NullPointerException.")
		void nullArguments() {
			assertAll(
					() -> assertThrows(NullPointerException.class,
							() -> generationService.generate(null, choiceReturnCodesEncryptionPublicKey, electionPublicKey)),
					() -> assertThrows(NullPointerException.class,
							() -> generationService.generate(electionEventId, null, electionPublicKey)),
					() -> assertThrows(NullPointerException.class,
							() -> generationService.generate(electionEventId, choiceReturnCodesEncryptionPublicKey, null))
			);
		}

		@Test
		@DisplayName("inputs with different groups throws an IllegalArgumentException.")
		void invalidArguments() {
			when(electionEventService.exists(electionEventId)).thenReturn(true);

			final GqGroup otherGroup = GroupTestData.getDifferentGqGroup(gqGroup);
			final ElGamalGenerator otherGroupElGamalGenerator = new ElGamalGenerator(otherGroup);
			final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKeyDifferentGroup = otherGroupElGamalGenerator.genRandomPublicKey(
					numberOfEligibleVoters);
			final ElGamalMultiRecipientPublicKey electionPublicKeyDifferentGroup = otherGroupElGamalGenerator.genRandomPublicKey(
					numberOfEligibleVoters);

			assertAll(
					() -> {
						final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
								() -> generationService.generate(electionEventId, choiceReturnCodesEncryptionPublicKeyDifferentGroup,
										electionPublicKey));

						assertEquals("The choice return codes encryption public key and the election public key must have the same group",
								illegalArgumentException.getMessage());
					},
					() -> {
						final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
								() -> generationService.generate(electionEventId, choiceReturnCodesEncryptionPublicKey,
										electionPublicKeyDifferentGroup));

						assertEquals("The choice return codes encryption public key and the election public key must have the same group",
								illegalArgumentException.getMessage());
					}
			);
		}
	}

	@Nested
	@DisplayName("executing generate")
	class GenerateExecutionTest {

		@BeforeEach
		void setup() throws SignatureException {
			final SetupComponentTallyDataPayloadGenerator generator = new SetupComponentTallyDataPayloadGenerator(gqGroup);
			final SetupComponentTallyDataPayload setupComponentTallyDataPayload = generator.generate(electionEventId, verificationCardSetId,
					numberOfEligibleVoters);
			final ImmutableList<String> verificationCardIds = setupComponentTallyDataPayload.getVerificationCardIds();

			final GenCredDatOutput genCredDatOutput =
					new GenCredDatOutput(
							Stream.generate(() -> "%s=".formatted(RandomFactory.createRandom().genRandomString(571, base64Alphabet))).limit(
									numberOfEligibleVoters).collect(toImmutableList()));

			// Services mocks:
			when(electionEventService.exists(electionEventId)).thenReturn(true);

			// In generate method:
			when(verificationCardSetService.getVerificationCardSetIds(anyString())).thenReturn(verificationCardIds);

			final VoterInitialCodesPayload voterInitialCodesPayload = mock(VoterInitialCodesPayload.class);
			final VoterInitialCodes voterInitialCodes = mock(VoterInitialCodes.class);
			when(voterInitialCodes.startVotingKey()).thenReturn(ElectionSetupUtils.genStartVotingKey());
			when(voterInitialCodesPayload.voterInitialCodes()).thenReturn(verificationCardIds.stream()
					.map(id -> voterInitialCodes)
					.collect(toImmutableList()));

			when(genCredDatService.genCredDat(any(), any(), any(), any(), any(), any())).thenReturn(genCredDatOutput);

			// In createSetupComponentVerificationCardKeystoresPayload method:
			when(signatureKeystoreService.generateSignature(any(), any())).thenReturn(
					new ImmutableByteArray("signature".getBytes(StandardCharsets.UTF_8)));

			// In loadSetupComponentTallyDataPayload method:
			when(setupComponentTallyDataPayloadService.load(anyString(), anyString())).thenReturn(setupComponentTallyDataPayload);
			when(signatureKeystoreService.verifySignature(any(), any(), any(), any())).thenReturn(true);
		}

		@Test
		@DisplayName("behaves as expected.")
		void happyPath() throws SignatureException {
			assertDoesNotThrow(() -> generationService.generate(electionEventId, choiceReturnCodesEncryptionPublicKey, electoralBoardPublicKey));

			verify(verificationCardSetService, times(1)).getVerificationCardSetIds(anyString());
			verify(setupComponentTallyDataPayloadService, times(numberOfEligibleVoters)).load(anyString(), anyString());
			verify(signatureKeystoreService, times(numberOfEligibleVoters)).generateSignature(
					any(SetupComponentVerificationCardKeystoresPayload.class), any());
			verify(genCredDatService, times(numberOfEligibleVoters)).genCredDat(any(), any(), any(), any(), any(), any());
			verify(signatureKeystoreService, times(numberOfEligibleVoters)).generateSignature(any(), any());
		}

		@Test
		@DisplayName("with an empty verification card set found returns an empty list of SetupComponentVerificationCardKeystoresPayload.")
		void givenAnEmptyListOfVerificationCardSetIdsThenNodata() throws SignatureException {
			when(verificationCardSetService.getVerificationCardSetIds(anyString())).thenReturn(ImmutableList.emptyList());

			assertDoesNotThrow(() -> generationService.generate(electionEventId, choiceReturnCodesEncryptionPublicKey, electoralBoardPublicKey));

			verify(verificationCardSetService, times(1)).getVerificationCardSetIds(anyString());

			verify(setupComponentTallyDataPayloadService, times(0)).load(anyString(), anyString());

			verify(signatureKeystoreService, times(0)).generateSignature(any(), any());

			verify(genCredDatService, times(0)).genCredDat(any(), any(), any(), any(), any(), any());
		}

	}
}
