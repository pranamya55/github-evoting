/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory.createArgon2;
import static ch.post.it.evoting.cryptoprimitives.hashing.HashFactory.createHash;
import static ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory.createBase64;
import static ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricFactory.createSymmetric;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BCK_LENGTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.math.Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodes;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodesPayload;
import ch.post.it.evoting.domain.generators.SetupComponentTallyDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKey;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKeyPayload;

@DisplayName("genCredDat called with")
class GenCredDatServiceTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();

	private static GenCredDatService genCredDatService;
	private static ElGamalMultiRecipientPublicKey electionPublicKey;
	private static ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey;
	private static SetupComponentTallyDataPayload setupComponentTallyDataPayload;
	private static VerificationCardSecretKeyPayload verificationCardSecretKeyPayload;
	private static PrimesMappingTable primesMappingTable;
	private static VoterInitialCodesPayload voterInitialCodesPayload;

	@BeforeAll
	static void setUpAll() {
		final Hash hash = createHash();
		final GetHashContextAlgorithm getHashContextAlgorithm = new GetHashContextAlgorithm(BaseEncodingFactory.createBase64(), hash,
				new PrimesMappingTableAlgorithms());
		final GenCredDatAlgorithm genCredDatAlgorithm = new GenCredDatAlgorithm(hash, createSymmetric(), createBase64(),
				createArgon2(Argon2Profile.TEST), getHashContextAlgorithm);
		genCredDatService = new GenCredDatService(genCredDatAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		final VerificationCardSetContext verificationCardSetContext = electionEventContextPayload.getElectionEventContext()
				.verificationCardSetContexts()
				.get(0);

		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final SetupComponentPublicKeys setupComponentPublicKeys = new SetupComponentPublicKeysPayloadGenerator(encryptionGroup).generate()
				.getSetupComponentPublicKeys();
		electionPublicKey = setupComponentPublicKeys.electionPublicKey();
		choiceReturnCodesEncryptionPublicKey = setupComponentPublicKeys.choiceReturnCodesEncryptionPublicKey();

		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		final String verificationCardSetId = verificationCardSetContext.getVerificationCardSetId();
		setupComponentTallyDataPayload = new SetupComponentTallyDataPayloadGenerator(encryptionGroup).generate(electionEventId, verificationCardSetId,
				verificationCardSetContext.getNumberOfEligibleVoters());
		final ImmutableList<String> verificationCardIds = setupComponentTallyDataPayload.getVerificationCardIds();

		final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(encryptionGroup);
		verificationCardSecretKeyPayload = new VerificationCardSecretKeyPayload(encryptionGroup,
				electionEventId, verificationCardSetId, verificationCardIds.stream()
				.map(verificationCardId -> new VerificationCardSecretKey(verificationCardId, elGamalGenerator.genRandomPrivateKey(1)))
				.collect(toImmutableList()));

		primesMappingTable = verificationCardSetContext.getPrimesMappingTable();

		final ImmutableList<VoterInitialCodes> voterInitialCodes = verificationCardIds.stream()
				.map(ignored -> generateVoterInitialCodes())
				.collect(toImmutableList());
		voterInitialCodesPayload = new VoterInitialCodesPayload(electionEventId, verificationCardSetId, voterInitialCodes);
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, primesMappingTable, voterInitialCodesPayload, electionPublicKey, choiceReturnCodesEncryptionPublicKey,
						verificationCardSecretKeyPayload),
				Arguments.of(setupComponentTallyDataPayload, null, voterInitialCodesPayload, electionPublicKey, choiceReturnCodesEncryptionPublicKey,
						verificationCardSecretKeyPayload),
				Arguments.of(setupComponentTallyDataPayload, primesMappingTable, null, electionPublicKey, choiceReturnCodesEncryptionPublicKey,
						verificationCardSecretKeyPayload),
				Arguments.of(setupComponentTallyDataPayload, primesMappingTable, voterInitialCodesPayload, null, choiceReturnCodesEncryptionPublicKey,
						verificationCardSecretKeyPayload),
				Arguments.of(setupComponentTallyDataPayload, primesMappingTable, voterInitialCodesPayload, electionPublicKey, null,
						verificationCardSecretKeyPayload),
				Arguments.of(setupComponentTallyDataPayload, primesMappingTable, voterInitialCodesPayload, electionPublicKey,
						choiceReturnCodesEncryptionPublicKey, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void genCredDatWithNullParametersThrows(final SetupComponentTallyDataPayload setupComponentTallyDataPayload,
			final PrimesMappingTable primesMappingTable, final VoterInitialCodesPayload voterInitialCodesPayload,
			final ElGamalMultiRecipientPublicKey electionPublicKey, final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey,
			final VerificationCardSecretKeyPayload verificationCardSecretKeyPayload) {
		assertThrows(NullPointerException.class,
				() -> genCredDatService.genCredDat(setupComponentTallyDataPayload, primesMappingTable, voterInitialCodesPayload, electionPublicKey,
						choiceReturnCodesEncryptionPublicKey, verificationCardSecretKeyPayload));
	}

	@Test
	@DisplayName("with valid input behaves as expected")
	void genCredDatWithValidInputBehavesAsExpected() {
		assertDoesNotThrow(
				() -> genCredDatService.genCredDat(setupComponentTallyDataPayload, primesMappingTable, voterInitialCodesPayload, electionPublicKey,
						choiceReturnCodesEncryptionPublicKey, verificationCardSecretKeyPayload));
	}

	private static VoterInitialCodes generateVoterInitialCodes() {
		final Alphabet base64Alphabet = Base64Alphabet.getInstance();
		final String voterIdentification = random.genRandomString(50, base64Alphabet);
		final String votingCardId = uuidGenerator.generate();
		final String verificationCardId = uuidGenerator.generate();
		final String startVotingKey = ElectionSetupUtils.genStartVotingKey();
		final String extendedAuthenticationFactor = String.join("", random.genUniqueDecimalStrings(4, 2));
		final String ballotCastingKey = random.genUniqueDecimalStrings(BCK_LENGTH, 1).get(0);
		return new VoterInitialCodes(voterIdentification, votingCardId, verificationCardId, startVotingKey, extendedAuthenticationFactor,
				ballotCastingKey);
	}

}
