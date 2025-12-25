/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.Base64;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;

/**
 * Tests of GenVerDatAlgorithm.
 */
@DisplayName("GenVerDatAlgorithm")
@ExtendWith(MockitoExtension.class)
class GenVerDatAlgorithmTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static ElGamalGenerator elGamalGenerator;
	private static GenVerDatAlgorithm genVerDatAlgorithm;
	private static GenVerDatContext context;
	private static GqGroup gqGroup;
	private static String electionEventId;
	private static int maximumNumberOfVotingOptions;
	private static ElGamalMultiRecipientPublicKey setupPublicKey;
	private static PrimesMappingTable primesMappingTable;

	@BeforeAll
	static void setup() {
		gqGroup = GroupTestData.getGroupP59();
		electionEventId = uuidGenerator.generate();
		elGamalGenerator = new ElGamalGenerator(gqGroup);
		final int numberOfEligibleVoters = 2;
		maximumNumberOfVotingOptions = 3;
		setupPublicKey = elGamalGenerator.genRandomPublicKey(maximumNumberOfVotingOptions);

		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator(gqGroup);
		primesMappingTable = primesMappingTableGenerator.generate(2);

		context = new GenVerDatContext(gqGroup, electionEventId, numberOfEligibleVoters, primesMappingTable, maximumNumberOfVotingOptions);
		final Base64 base64 = BaseEncodingFactory.createBase64();
		genVerDatAlgorithm = new GenVerDatAlgorithm(ElGamalFactory.createElGamal(), HashFactory.createHash(), random, base64,
				new PrimesMappingTableAlgorithms());
	}

	@Test
	@DisplayName("null number of eligible voters throws IllegalArgumentException")
	void nullNumberOfEligibleVotersThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerDatContext(gqGroup, electionEventId, 0, primesMappingTable, maximumNumberOfVotingOptions));

		final String expected = "The number of eligible voters must be strictly greater than 0.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("negative number of eligible voters throws IllegalArgumentException")
	void negativeNumberOfEligibleVotersThrows() {
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new GenVerDatContext(gqGroup, electionEventId, -2, primesMappingTable, maximumNumberOfVotingOptions));

		final String expected = "The number of eligible voters must be strictly greater than 0.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("primes mapping table and secret key size with different groups throws IllegalArgumentException")
	void inputWithDifferentGroupThrows() {
		final GqGroup otherGroup = GroupTestData.getDifferentGqGroup(setupPublicKey.getGroup());
		final ElGamalGenerator otherGroupGenerator = new ElGamalGenerator(otherGroup);
		final ElGamalMultiRecipientPublicKey differentGroupSetupPublicKey = otherGroupGenerator.genRandomPublicKey(setupPublicKey.size());

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerDatAlgorithm.genVerDat(context, differentGroupSetupPublicKey));

		final String expected = "The context and input must have the same encryption group.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("number of voting options greater than secret key size throws IllegalArgumentException")
	void numberOfVotingOptionsGreaterThanSetupPublicKeyThrows() {
		final ElGamalMultiRecipientPublicKey tooSmallSetupPublicKey = elGamalGenerator.genRandomPublicKey(
				primesMappingTable.getNumberOfVotingOptions() - 1);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genVerDatAlgorithm.genVerDat(context, tooSmallSetupPublicKey));

		final String expected = String.format(
				"The setup public key length must be equal to the maximum number of voting options. [n_max: %s]",
				context.maximumNumberOfVotingOptions());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("with valid input generates valid output")
	void validInputGeneratesValidOutput() {
		final GqGroup largeGqGroup = GroupTestData.getLargeGqGroup();
		final String otherElectionEventId = uuidGenerator.generate();
		final int numberOfEligibleVoters = random.genRandomInteger(8) + 3;
		final int otherMaximumNumberOfVotingOptions = random.genRandomInteger(116) + 5;
		final PrimesMappingTable otherPrimesMappingTable = new PrimesMappingTableGenerator(largeGqGroup).generate(otherMaximumNumberOfVotingOptions);
		final GenVerDatContext genVerDatContext = new GenVerDatContext(largeGqGroup, otherElectionEventId, numberOfEligibleVoters,
				otherPrimesMappingTable, otherMaximumNumberOfVotingOptions);
		final ElGamalMultiRecipientPublicKey otherSetupPublicKey = new ElGamalGenerator(largeGqGroup).genRandomPublicKey(
				otherMaximumNumberOfVotingOptions);
		assertDoesNotThrow(() -> genVerDatAlgorithm.genVerDat(genVerDatContext, otherSetupPublicKey));
	}
}
