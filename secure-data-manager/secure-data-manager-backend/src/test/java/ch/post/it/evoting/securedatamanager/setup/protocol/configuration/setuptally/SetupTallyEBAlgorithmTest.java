/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setuptally;

import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair.genKeyPair;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static ch.post.it.evoting.cryptoprimitives.utils.Conversions.integerToString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.AuxiliaryInformation;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamal;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.SchnorrProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.evotinglibraries.domain.election.ControlComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashElectionEventContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.proofofcorrectkeygeneration.VerifyCCSchnorrProofsAlgorithm;

/**
 * Tests of SetupTallyEBAlgorithm.
 */
@DisplayName("A SetupTallyEBAlgorithm calling setupTallyEB with")
class SetupTallyEBAlgorithmTest {

	private static final String SETUP_TALLY_CCM = "SetupTallyCCM";
	private static GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm;
	private static SetupTallyEBAlgorithm setupTallyEBAlgorithm;
	private static ElectionEventContext electionEventContext;
	private static int deltaMax;
	private static GqGroup gqGroup;
	private static GqGroupGenerator gqGroupGenerator;
	private static GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> ccmElectionPublicKeys;
	private static GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> schnorrProofs;
	private static ZeroKnowledgeProof zeroKnowledgeProof;
	private static Random random;
	private ImmutableList<SafePasswordHolder> passwords;

	@BeforeAll
	static void setUpAll() {
		final Base64 base64 = BaseEncodingFactory.createBase64();
		final Hash hash = HashFactory.createHash();
		getHashElectionEventContextAlgorithm = new GetHashElectionEventContextAlgorithm(base64, hash);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContext = electionEventContextPayloadGenerator.generate().getElectionEventContext();
		gqGroup = electionEventContext.encryptionGroup();
		deltaMax = electionEventContext.maximumNumberOfWriteInsPlusOne();

		zeroKnowledgeProof = ZeroKnowledgeProofFactory.createZeroKnowledgeProof();
		final ElGamal elGamal = ElGamalFactory.createElGamal();
		final GetHashElectionEventContextAlgorithm getHashElectionEventContextAlgorithm = new GetHashElectionEventContextAlgorithm(
				base64, hash);
		setupTallyEBAlgorithm = new SetupTallyEBAlgorithm(hash, elGamal, zeroKnowledgeProof, new VerifyCCSchnorrProofsAlgorithm(zeroKnowledgeProof),
				getHashElectionEventContextAlgorithm);

		random = RandomFactory.createRandom();
		gqGroupGenerator = new GqGroupGenerator(gqGroup);

		final ElGamalMultiRecipientKeyPair keypair1 = genKeyPair(gqGroup, deltaMax, random);
		final ElGamalMultiRecipientPublicKey ccmElectionPublicKey1 = keypair1.getPublicKey();

		final ElGamalMultiRecipientKeyPair keypair2 = genKeyPair(gqGroup, deltaMax, random);
		final ElGamalMultiRecipientPublicKey ccmElectionPublicKey2 = keypair2.getPublicKey();

		final ElGamalMultiRecipientKeyPair keypair3 = genKeyPair(gqGroup, deltaMax, random);
		final ElGamalMultiRecipientPublicKey ccmElectionPublicKey3 = keypair3.getPublicKey();

		final ElGamalMultiRecipientKeyPair keypair4 = genKeyPair(gqGroup, deltaMax, random);
		final ElGamalMultiRecipientPublicKey ccmElectionPublicKey4 = keypair4.getPublicKey();

		final GroupVector<SchnorrProof, ZqGroup> schnorrProofs1 = generateSchnorrProofs(1, keypair1);
		final GroupVector<SchnorrProof, ZqGroup> schnorrProofs2 = generateSchnorrProofs(2, keypair2);
		final GroupVector<SchnorrProof, ZqGroup> schnorrProofs3 = generateSchnorrProofs(3, keypair3);
		final GroupVector<SchnorrProof, ZqGroup> schnorrProofs4 = generateSchnorrProofs(4, keypair4);

		ccmElectionPublicKeys = GroupVector.of(ccmElectionPublicKey1, ccmElectionPublicKey2, ccmElectionPublicKey3, ccmElectionPublicKey4);

		schnorrProofs = GroupVector.of(schnorrProofs1, schnorrProofs2, schnorrProofs3, schnorrProofs4);
	}

	@BeforeEach
	void setup() {
		passwords = ImmutableList.of(new SafePasswordHolder("Password_ElectoralBoard1".toCharArray()),
				new SafePasswordHolder("Password_ElectoralBoard2".toCharArray()));
	}

	@Test
	@DisplayName("a valid SetupTallyEBInput does not throw any Exception.")
	void validParamDoesNotThrow() {
		final SetupTallyEBInput input = new SetupTallyEBInput(ccmElectionPublicKeys, schnorrProofs, passwords);

		assertDoesNotThrow(() -> setupTallyEBAlgorithm.setupTallyEB(electionEventContext, input));
	}

	@Test
	@DisplayName("an invalid SetupTallyEBInput throws IllegalStateException")
	void invalidParamThrowIllegalStateException() {

		final GroupVector<SchnorrProof, ZqGroup> invalidSchnorrProof = generateSchnorrProofs(55, genKeyPair(gqGroup, deltaMax, random));

		final GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> invalidSchnorrProofs = GroupVector.of(schnorrProofs.get(0),
				schnorrProofs.get(1),
				schnorrProofs.get(2), invalidSchnorrProof);

		final SetupTallyEBInput input = new SetupTallyEBInput(ccmElectionPublicKeys, invalidSchnorrProofs, passwords);

		final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
				() -> setupTallyEBAlgorithm.setupTallyEB(electionEventContext, input));

		assertEquals("The CCM Schnorr proofs are invalid.", illegalStateException.getMessage());
	}

	@Test
	@DisplayName("a valid SetupTallyEBInput returns a non-null SetupTallyEBOutput with expected content.")
	void nonNullOutput() {
		final SetupTallyEBInput input = new SetupTallyEBInput(ccmElectionPublicKeys, schnorrProofs, passwords);

		final SetupTallyEBOutput output = assertDoesNotThrow(() -> setupTallyEBAlgorithm.setupTallyEB(electionEventContext, input));
		final ElGamalMultiRecipientPublicKey electionPublicKey = output.getElectionPublicKey();
		final ElGamalMultiRecipientPublicKey electoralBoardPublicKey = output.getElectoralBoardPublicKey();

		assertNotNull(output);
		assertNotNull(electoralBoardPublicKey);
		assertEquals(deltaMax, electionPublicKey.size());
		assertEquals(deltaMax, electoralBoardPublicKey.size());
	}

	@Test
	@DisplayName("non-matching groups throws an IllegalArgumentException.")
	void crossGroupChecksFailures() {
		final SetupComponentPublicKeysPayloadGenerator setupComponentPublicKeysPayloadGenerator = new SetupComponentPublicKeysPayloadGenerator(
				GroupTestData.getGroupP59());
		final SetupComponentPublicKeys setupComponentPublicKeys = setupComponentPublicKeysPayloadGenerator.generate().getSetupComponentPublicKeys();
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> otherGroupPublicKeys = setupComponentPublicKeys.combinedControlComponentPublicKeys()
				.stream()
				.map(ControlComponentPublicKeys::ccmjElectionPublicKey)
				.collect(toGroupVector());
		final GroupVector<GroupVector<SchnorrProof, ZqGroup>, ZqGroup> otherSchnorrProofs = setupComponentPublicKeys.combinedControlComponentPublicKeys()
				.stream()
				.map(ControlComponentPublicKeys::ccmjSchnorrProofs)
				.collect(toGroupVector());
		final SetupTallyEBInput input = new SetupTallyEBInput(otherGroupPublicKeys, otherSchnorrProofs, passwords);

		final IllegalArgumentException illegalArgumentException =
				assertThrows(IllegalArgumentException.class, () -> setupTallyEBAlgorithm.setupTallyEB(electionEventContext, input));

		final String errorMessage = "The context and input must have the same encryption group.";
		assertEquals(errorMessage, Throwables.getRootCause(illegalArgumentException).getMessage());
	}

	@Test
	@DisplayName("a null parameter throws a NullPointerException.")
	void nullParamThrowsANullPointer() {
		final SetupTallyEBInput input = new SetupTallyEBInput(ccmElectionPublicKeys, schnorrProofs, passwords);
		assertAll(
				() -> assertThrows(NullPointerException.class, () -> setupTallyEBAlgorithm.setupTallyEB(electionEventContext, null)),
				() -> assertThrows(NullPointerException.class, () -> setupTallyEBAlgorithm.setupTallyEB(null, input))
		);
	}

	@SuppressWarnings("java:S117")
	private static GroupVector<SchnorrProof, ZqGroup> generateSchnorrProofs(final int nodeId,
			final ElGamalMultiRecipientKeyPair elGamalMultiRecipientKeyPair) {

		// Operation.
		final ElGamalMultiRecipientPrivateKey EL_sk_j = elGamalMultiRecipientKeyPair.getPrivateKey();
		final ElGamalMultiRecipientPublicKey EL_pk_j = elGamalMultiRecipientKeyPair.getPublicKey();
		final String hContext = getHashElectionEventContextAlgorithm.getHashElectionEventContext(electionEventContext);
		final AuxiliaryInformation i_aux = AuxiliaryInformation.of(hContext, SETUP_TALLY_CCM, integerToString(nodeId));

		return IntStream.range(0, deltaMax).mapToObj(i -> {
			final ZqElement EL_sk_j_i = EL_sk_j.get(i);
			final GqElement EL_pk_j_i = EL_pk_j.get(i);
			return zeroKnowledgeProof.genSchnorrProof(EL_sk_j_i, EL_pk_j_i, i_aux);
		}).collect(toGroupVector());
	}

	@Nested
	@DisplayName("a SetupTallyEBInput built with")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class SetupTallyEBInputTest {

		@Test
		@DisplayName("null election public keys throws a NullPointerException.")
		void nullEPKThrowsANullPointer() {
			assertThrows(NullPointerException.class,
					() -> new SetupTallyEBInput(null, schnorrProofs, passwords));
		}

		@Test
		@DisplayName("a number election public keys different than 4 throws an IllegalArgumentException.")
		void nonValidNumberOfEPKThrowsAnIllegalArgument() {

			final ElGamalMultiRecipientPublicKey ccmElectionPublicKey1 =
					new ElGamalMultiRecipientPublicKey(gqGroupGenerator.genRandomGqElementVector(deltaMax));
			final ElGamalMultiRecipientPublicKey ccmElectionPublicKey2 =
					new ElGamalMultiRecipientPublicKey(gqGroupGenerator.genRandomGqElementVector(deltaMax));
			final ElGamalMultiRecipientPublicKey ccmElectionPublicKey3 =
					new ElGamalMultiRecipientPublicKey(gqGroupGenerator.genRandomGqElementVector(deltaMax));
			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> smallCcmElectionPublicKeys =
					GroupVector.of(ccmElectionPublicKey1, ccmElectionPublicKey2, ccmElectionPublicKey3);

			assertThrows(IllegalArgumentException.class,
					() -> new SetupTallyEBInput(smallCcmElectionPublicKeys, schnorrProofs, passwords));

			final ElGamalMultiRecipientPublicKey ccmElectionPublicKey4 =
					new ElGamalMultiRecipientPublicKey(gqGroupGenerator.genRandomGqElementVector(deltaMax));
			final ElGamalMultiRecipientPublicKey ccmElectionPublicKey5 =
					new ElGamalMultiRecipientPublicKey(gqGroupGenerator.genRandomGqElementVector(deltaMax));

			final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> bigCcmElectionPublicKeys =
					GroupVector.of(ccmElectionPublicKey1, ccmElectionPublicKey2, ccmElectionPublicKey3, ccmElectionPublicKey4, ccmElectionPublicKey5);

			assertThrows(IllegalArgumentException.class,
					() -> new SetupTallyEBInput(bigCcmElectionPublicKeys, schnorrProofs, passwords));
		}

		@Test
		@DisplayName("a null list of passwords of k electoral board members throws a NullPointerException.")
		void nullPasswordsThrowsANullPointer() {
			assertThrows(NullPointerException.class,
					() -> new SetupTallyEBInput(ccmElectionPublicKeys, schnorrProofs, null));
		}

		@Test
		@DisplayName("less than 2 electoral board members throws an IllegalArgumentException.")
		void nonValidNumberOfElectoralBoardMembersThrowsAnIllegalArgument() {
			final ImmutableList<SafePasswordHolder> nonValidK = ImmutableList.of(new SafePasswordHolder("Password_ElectoralBoard1".toCharArray()));

			assertThrows(IllegalArgumentException.class, () -> new SetupTallyEBInput(ccmElectionPublicKeys, schnorrProofs, nonValidK));
		}
	}
}
