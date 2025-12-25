/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement.PrimeGqElementFactory.getSmallPrimeGroupMembers;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;

@DisplayName("GenSetupDataOutput constructed with")
class GenSetupDataOutputTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final GqGroup group = GroupTestData.getLargeGqGroup();
	private GenSetupDataOutput.Builder outputBuilder;

	@BeforeEach
	void setUp() {
		final ImmutableList<String> verificationCardSetIds = ImmutableList.of(
				uuidGenerator.generate(),
				uuidGenerator.generate()
		);
		final GroupVector<PrimeGqElement, GqGroup> smallPrimes = getSmallPrimeGroupMembers(group, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator(group);
		final ImmutableMap<String, PrimesMappingTable> primesMappingTables = ImmutableMap.of(
				verificationCardSetIds.get(0), primesMappingTableGenerator.generate(4, 1),
				verificationCardSetIds.get(1), primesMappingTableGenerator.generate(2, 2)
		);
		final int maximumNumberOfVotingOptions = 12;
		final int maximumNumberOfSelections = 4;
		final int maximumNumberOfWriteInsPlusOne = 2;
		final ElGamalMultiRecipientKeyPair setupKeyPair = ElGamalMultiRecipientKeyPair.genKeyPair(group, maximumNumberOfVotingOptions, random);

		outputBuilder = new GenSetupDataOutput.Builder()
				.setEncryptionGroup(group)
				.setSmallPrimes(smallPrimes)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.setMaximumNumberOfSelections(maximumNumberOfSelections)
				.setMaximumNumberOfWriteInsPlusOne(maximumNumberOfWriteInsPlusOne)
				.setPrimesMappingTables(primesMappingTables)
				.setSetupKeyPair(setupKeyPair);
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void anyNullParamThrows() {
		final GenSetupDataOutput.Builder nullEncryptionGroup = outputBuilder.setEncryptionGroup(null);
		assertThrows(NullPointerException.class, nullEncryptionGroup::build);

		final GenSetupDataOutput.Builder nullSmallPrimes = outputBuilder.setSmallPrimes(null);
		assertThrows(NullPointerException.class, nullSmallPrimes::build);

		final GenSetupDataOutput.Builder nullPrimesMappingTables = outputBuilder.setPrimesMappingTables(null);
		assertThrows(NullPointerException.class, nullPrimesMappingTables::build);

		final GenSetupDataOutput.Builder nullSetupKeyPair = outputBuilder.setSetupKeyPair(null);
		assertThrows(NullPointerException.class, nullSetupKeyPair::build);
	}

	@Test
	@DisplayName("invalid maximum number of voting options throws IllegalArgumentException")
	void invalidMaximumNumberOfVotingOptionsThrows() {
		final GenSetupDataOutput.Builder negativeMaximumNumberOfVotingOptions = outputBuilder.setMaximumNumberOfVotingOptions(-3);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, negativeMaximumNumberOfVotingOptions::build);
		String expected = String.format(
				"The maximum number of voting options must be strictly greater than zero and smaller or equal to the maximum supported number of voting options. [n_max: %s, n_sup: %s]",
				-3, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());

		final GenSetupDataOutput.Builder tooBigMaximumNumberOfVotingOptions = outputBuilder.setMaximumNumberOfVotingOptions(
				MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1);
		exception = assertThrows(IllegalArgumentException.class, tooBigMaximumNumberOfVotingOptions::build);
		expected = String.format(
				"The maximum number of voting options must be strictly greater than zero and smaller or equal to the maximum supported number of voting options. [n_max: %s, n_sup: %s]",
				MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("invalid maximum number of selections throws IllegalArgumentException")
	void invalidMaximumNumberOfSelectionsThrows() {
		final GenSetupDataOutput.Builder negativeMaximumNumberOfSelections = outputBuilder.setMaximumNumberOfSelections(-3);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, negativeMaximumNumberOfSelections::build);
		String expected = String.format(
				"The maximum number of selections must be strictly greater than zero and smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				-3, MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());

		final GenSetupDataOutput.Builder tooBigMaximumNumberOfSelections = outputBuilder.setMaximumNumberOfSelections(
				MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1);
		exception = assertThrows(IllegalArgumentException.class, tooBigMaximumNumberOfSelections::build);
		expected = String.format(
				"The maximum number of selections must be strictly greater than zero and smaller or equal to the maximum supported number of selections. [psi_max: %s, psi_sup: %s]",
				MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS + 1, MAXIMUM_SUPPORTED_NUMBER_OF_SELECTIONS);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("invalid maximum number of write-ins + 1 throws IllegalArgumentException")
	void invalidMaximumNumberOfWriteInsPlusOneThrows() {
		final GenSetupDataOutput.Builder zeroMaximumNumberOfWriteInsPlusOne = outputBuilder.setMaximumNumberOfWriteInsPlusOne(0);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, zeroMaximumNumberOfWriteInsPlusOne::build);
		String expected = String.format(
				"The maximum number of write-ins + 1 must be strictly greater than zero and smaller or equal to the maximum supported number of write-ins + 1. [delta_max: %s, delta_sup: %s]",
				0, MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());

		final GenSetupDataOutput.Builder tooBigMaximumNumberOfWriteInsPlusOne = outputBuilder.setMaximumNumberOfWriteInsPlusOne(
				MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 2);
		exception = assertThrows(IllegalArgumentException.class, tooBigMaximumNumberOfWriteInsPlusOne::build);
		expected = String.format(
				"The maximum number of write-ins + 1 must be strictly greater than zero and smaller or equal to the maximum supported number of write-ins + 1. [delta_max: %s, delta_sup: %s]",
				MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 2, MAXIMUM_SUPPORTED_NUMBER_OF_WRITE_INS + 1);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		assertDoesNotThrow(outputBuilder::build);
	}

}
