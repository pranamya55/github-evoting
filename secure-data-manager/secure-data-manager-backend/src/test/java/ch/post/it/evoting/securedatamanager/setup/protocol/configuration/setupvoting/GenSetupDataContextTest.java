/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PartialPrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@DisplayName("GenSetupDataInput constructed with")
class GenSetupDataContextTest {

	private ImmutableMap<String, ImmutableList<PartialPrimesMappingTableEntry>> optionsInformation;
	private ImmutableMap<String, ImmutableList<PartialPrimesMappingTableEntry>> invalidOptionsInformation;

	@BeforeEach
	void setUp() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final ImmutableList<String> verificationCardSetIds = ImmutableList.of(
				uuidGenerator.generate(),
				uuidGenerator.generate()
		);
		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator();
		final GroupVector<PrimesMappingTableEntry, GqGroup> pTableVcs0 = primesMappingTableGenerator.generate(4, 1).pTable();
		final GroupVector<PrimesMappingTableEntry, GqGroup> pTableVcs1 = primesMappingTableGenerator.generate(2, 2).pTable();
		optionsInformation = ImmutableMap.of(
				verificationCardSetIds.get(0), pTableVcs0.stream()
						.map(entry -> new PartialPrimesMappingTableEntry(entry.actualVotingOption(), entry.semanticInformation(),
								entry.correctnessInformation()))
						.collect(toImmutableList()),
				verificationCardSetIds.get(1), pTableVcs1.stream()
						.map(entry -> new PartialPrimesMappingTableEntry(entry.actualVotingOption(), entry.semanticInformation(),
								entry.correctnessInformation()))
						.collect(toImmutableList())
		);
		invalidOptionsInformation = ImmutableMap.of(
				verificationCardSetIds.get(0), pTableVcs0.stream()
						.map(entry -> new PartialPrimesMappingTableEntry(entry.actualVotingOption(), entry.semanticInformation(),
								entry.correctnessInformation()))
						.collect(toImmutableList()),
				"invalid UUID", pTableVcs1.stream()
						.map(entry -> new PartialPrimesMappingTableEntry(entry.actualVotingOption(), entry.semanticInformation(),
								entry.correctnessInformation()))
						.collect(toImmutableList())
		);
	}

	@Test
	@DisplayName("any null parameter throws NullPointerException")
	void anyNullParamThrows() {
		assertThrows(NullPointerException.class, () -> new GenSetupDataContext(null));
	}

	@Test
	@DisplayName("invalid options information throws FailedValidationException")
	void invalidOptionsInformationThrows() {
		assertThrows(FailedValidationException.class, () -> new GenSetupDataContext(invalidOptionsInformation));

	}

	@Test
	@DisplayName("valid parameters does not throw")
	void validParamsDoesNotThrow() {
		assertDoesNotThrow(() -> new GenSetupDataContext(optionsInformation));
	}

}
