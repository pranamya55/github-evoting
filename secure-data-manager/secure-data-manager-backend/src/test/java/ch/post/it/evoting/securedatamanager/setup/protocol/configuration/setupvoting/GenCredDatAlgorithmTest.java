/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.symmetric.Symmetric;
import ch.post.it.evoting.cryptoprimitives.symmetric.SymmetricFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

/**
 * Tests of GenCredDatAlgorithm.
 */
@SuppressWarnings("java:S116")
@DisplayName("GenCredDatAlgorithm")
class GenCredDatAlgorithmTest {

	private static final GqGroup gqGroup = GroupTestData.getLargeGqGroup();
	private static final ZqGroup zqGroup = ZqGroup.sameOrderAs(gqGroup);
	private static final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(zqGroup);
	private static final ElGamalGenerator elGamalGenerator = new ElGamalGenerator(gqGroup);
	private static final int BOUND = 12;
	private static final int MAX_SIZE = 4;
	private static final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator(gqGroup);

	private final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private final SecureRandom srand = new SecureRandom();
	private final int size = srand.nextInt(BOUND) + 1;
	private final ImmutableList<String> verificationCardIds = Stream.generate(uuidGenerator::generate)
			.limit(size)
			.collect(toImmutableList());
	private final ImmutableList<String> startVotingKeys = Stream.generate(ElectionSetupUtils::genStartVotingKey)
			.limit(size)
			.collect(toImmutableList());
	private final GroupVector<ZqElement, ZqGroup> verificationCardSecretKeys = Stream.generate(zqGroupGenerator::genRandomZqElementMember)
			.limit(size)
			.collect(GroupVector.toGroupVector());
	private final int delta_max = srand.nextInt(BOUND) + 1;
	private final ElGamalMultiRecipientPublicKey electionPublicKey = elGamalGenerator.genRandomPublicKey(delta_max);
	private final int psi = srand.nextInt(BOUND) + 1;
	private final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey = elGamalGenerator.genRandomPublicKey(psi);
	private final GroupVector<PrimeGqElement, GqGroup> encodedVotingOptions = PrimeGqElement.PrimeGqElementFactory.getSmallPrimeGroupMembers(gqGroup,
					MAX_SIZE).stream()
			.distinct()
			.collect(GroupVector.toGroupVector());
	private final PrimesMappingTable primesMappingTable = primesMappingTableGenerator.generate(encodedVotingOptions);
	private final String electionEventId = uuidGenerator.generate();
	private final String verificationCardSetId = uuidGenerator.generate();
	private final Hash hash = HashFactory.createHash();
	private final Symmetric symmetric = SymmetricFactory.createSymmetric();
	private final Base64 base64 = BaseEncodingFactory.createBase64();
	private final Argon2 argon2 = Argon2Factory.createArgon2(Argon2Profile.TEST);
	private final GetHashContextAlgorithm getHashContextAlgorithm = new GetHashContextAlgorithm(base64, hash, new PrimesMappingTableAlgorithms());

	private GenCredDatContext context;
	private GenCredDatInput input;
	private GenCredDatAlgorithm genCredDatAlgorithm;

	@BeforeEach
	void setup() {
		context = buildCredDataContext();
		input = new GenCredDatInput(verificationCardSecretKeys, startVotingKeys);

		assertNotNull(hash, "hashService");
		assertNotNull(symmetric, "symmetricService");

		genCredDatAlgorithm = new GenCredDatAlgorithm(hash, symmetric, base64, argon2, getHashContextAlgorithm);
	}

	@Test
	@SuppressWarnings("java:S117")
	void happyPath() {
		final GenCredDatOutput output = genCredDatAlgorithm.genCredDat(context, input);

		assertNotNull(output, "output is null");
		assertNotNull(output.verificationCardKeystores(), "output.verificationCardKeystores() is null");
		assertEquals(output.verificationCardKeystores().size(), context.getVerificationCardIds().size());
		output.verificationCardKeystores().stream().forEach(VCks_id -> assertEquals(572, VCks_id.length()));
	}

	@Test
	void nullContextArgument() {
		assertThrows(NullPointerException.class, () -> genCredDatAlgorithm.genCredDat(null, input));
	}

	@Test
	void nullInputArgument() {
		assertThrows(NullPointerException.class, () -> genCredDatAlgorithm.genCredDat(context, null));
	}

	private GenCredDatContext buildCredDataContext() {
		return new GenCredDatContext
				.Builder()
				.setEncryptionGroup(gqGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setPrimesMappingTable(primesMappingTable)
				.setElectionPublicKey(electionPublicKey)
				.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
				.build();
	}

}
