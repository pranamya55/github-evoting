/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.preconfigure;

import static ch.post.it.evoting.cryptoprimitives.math.PrimeGqElement.PrimeGqElementFactory.getSmallPrimeGroupMembers;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.generators.PrimesMappingTableGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.setup.process.SetupKeyPairService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenSetupDataOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenSetupDataService;

@DisplayName("An electionEventService")
@ExtendWith(MockitoExtension.class)
class ElectionEventConfigServiceTest {

	public static final String SEED = "NE_20231124_TT05";
	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static GenSetupDataOutput genSetupDataOutput;

	@Mock
	private GenSetupDataService genSetupDataService;
	@Mock
	private SetupKeyPairService setupKeyPairService;
	@Mock
	private ElectionEventContextPersistenceService electionEventContextPersistenceService;

	private ElectionEventConfigService electionEventConfigService;

	@BeforeEach
	void setUp() {
		electionEventConfigService = new ElectionEventConfigService(SEED, genSetupDataService, setupKeyPairService,
				electionEventContextPersistenceService);
	}

	@BeforeAll
	static void setUpAll() {
		final GqGroup gqGroup = GroupTestData.getLargeGqGroup();
		final ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, 2, random);
		final PrimesMappingTableGenerator primesMappingTableGenerator = new PrimesMappingTableGenerator(gqGroup);
		genSetupDataOutput = new GenSetupDataOutput.Builder()
				.setEncryptionGroup(gqGroup)
				.setSmallPrimes(getSmallPrimeGroupMembers(gqGroup, 5000))
				.setMaximumNumberOfVotingOptions(2)
				.setMaximumNumberOfSelections(1)
				.setMaximumNumberOfWriteInsPlusOne(1)
				.setPrimesMappingTables(ImmutableMap.of(uuidGenerator.generate(), primesMappingTableGenerator.generate()))
				.setSetupKeyPair(keyPair)
				.build();
	}

	@DisplayName("executing create() with an invalid election event id throws a FailedValidationException.")
	@Test
	void createInvalidElectionEventId() {
		assertThrows(FailedValidationException.class, () -> electionEventConfigService.create("invalid id"));
	}

	@DisplayName("executing create() with valid election event id does not throw.")
	@Test
	void create() {
		when(genSetupDataService.genSetupData(anyString(), anyString())).thenReturn(genSetupDataOutput);

		doNothing().when(setupKeyPairService).save(anyString(), any());
		doNothing().when(electionEventContextPersistenceService).persist(anyString(), anyString(), any());

		assertDoesNotThrow(() -> electionEventConfigService.create(ELECTION_EVENT_ID));
	}

}
