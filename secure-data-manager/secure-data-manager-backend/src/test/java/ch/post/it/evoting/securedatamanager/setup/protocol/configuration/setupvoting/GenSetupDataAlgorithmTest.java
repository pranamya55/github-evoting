/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.PartialPrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.xml.XmlFileRepository;
import ch.post.it.evoting.evotinglibraries.xml.primesmappingtable.PartialPrimesMappingTableEntryBuilder;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ElectionGroupBallotType;

/**
 * Tests of GenSetupDataAlgorithm.
 */
@DisplayName("GenSetupDataAlgorithm")
class GenSetupDataAlgorithmTest {

	private static ImmutableList<PartialPrimesMappingTableEntry> partialPrimesMappingTableEntries;
	private final Random random = RandomFactory.createRandom();
	private final GenSetupDataAlgorithm genSetupDataAlgorithm = new GenSetupDataAlgorithm(random,
			new PrimesMappingTableAlgorithms(), new GetElectionEventEncryptionParametersAlgorithm(ElGamalFactory.createElGamal()));
	private GenSetupDataContext context;

	@BeforeAll
	static void setUpAll() throws URISyntaxException {
		final URI uri = GenSetupDataAlgorithm.class.getResource("/configuration-anonymized.xml").toURI();
		final Path configurationPath = Path.of(uri);

		final XmlFileRepository<Configuration> repository = new XmlFileRepository<>();
		final Configuration configuration = repository.read(configurationPath, "/xsd/evoting-config-7-0.xsd", Configuration.class);

		final ImmutableList<String> domainsOfInfluence = Stream.concat(
						configuration.getContest().getElectionGroupBallot().stream().map(ElectionGroupBallotType::getDomainOfInfluence),
						configuration.getContest().getVoteInformation().stream().map(voteInformation -> voteInformation.getVote().getDomainOfInfluence()))
				.collect(toImmutableList());
		partialPrimesMappingTableEntries = PartialPrimesMappingTableEntryBuilder.create(configuration, domainsOfInfluence);
	}

	@BeforeEach
	void setUp() {
		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		final ImmutableMap<String, ImmutableList<PartialPrimesMappingTableEntry>> optionsInformationMap = ImmutableMap.of(
				uuidGenerator.generate(), partialPrimesMappingTableEntries
		);

		context = new GenSetupDataContext(optionsInformationMap);
	}

	@Test
	@DisplayName("calling genSetupData with null argument throws a NullPointerException.")
	void genSetupDataWithNullGroupThrows() {
		assertThrows(NullPointerException.class, () -> genSetupDataAlgorithm.genSetupData(null, "NE_20231124_TT05"));
		assertThrows(NullPointerException.class, () -> genSetupDataAlgorithm.genSetupData(context, null));
	}

	@Test
	@DisplayName("invalid seed throws FailedValidationException")
	void invalidSeedThrows() {
		final String wrongSizeSeed = "NE_20231124_TT055";
		FailedValidationException exception = assertThrows(FailedValidationException.class,
				() -> genSetupDataAlgorithm.genSetupData(context, wrongSizeSeed));
		String expected = String.format("The seed doesn't comply with the required format. [seed: %s]", wrongSizeSeed);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());

		final String invalidDateSeed = "NE_20231324_TT05";
		exception = assertThrows(FailedValidationException.class,
				() -> genSetupDataAlgorithm.genSetupData(context, invalidDateSeed));
		expected = String.format("The seed doesn't comply with the required format. [seed: %s]", invalidDateSeed);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());

	}

	@Test
	@DisplayName("calling genSetupData with a correct argument generates a keypair with the right size.")
	void genSetupData() {
		final GenSetupDataOutput output = genSetupDataAlgorithm.genSetupData(context, "NE_20231124_TT05");

		assertEquals(output.getMaximumNumberOfVotingOptions(), output.getSetupKeyPair().size());
	}
}
