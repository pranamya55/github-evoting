/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalFactory.createElGamal;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.xml.XmlFileRepository;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ElectionGroupBallotType;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

@DisplayName("genSetupData called with")
class GenSetupDataServiceTest {

	private static final Random random = RandomFactory.createRandom();

	private static GenSetupDataService genSetupDataService;
	private static String electionEventId;
	private static String seed;

	@BeforeAll
	static void setUpAll() throws URISyntaxException {
		final GetElectionEventEncryptionParametersAlgorithm getElectionEventEncryptionParametersAlgorithm = new GetElectionEventEncryptionParametersAlgorithm(
				createElGamal());
		final GenSetupDataAlgorithm genSetupDataAlgorithm = new GenSetupDataAlgorithm(random, new PrimesMappingTableAlgorithms(),
				getElectionEventEncryptionParametersAlgorithm);
		final VerificationCardSetService verificationCardSetService = mock(VerificationCardSetService.class);

		final URI uri = GenSetupDataAlgorithm.class.getResource("/configuration-anonymized.xml").toURI();
		final Path configurationPath = Path.of(uri);

		final XmlFileRepository<Configuration> repository = new XmlFileRepository<>();
		final Configuration configuration = repository.read(configurationPath, "/xsd/evoting-config-7-0.xsd", Configuration.class);
		final EvotingConfigService evotingConfigService = mock(EvotingConfigService.class);
		when(evotingConfigService.load()).thenReturn(configuration);

		genSetupDataService = new GenSetupDataService(evotingConfigService, genSetupDataAlgorithm, verificationCardSetService);

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		electionEventId = uuidGenerator.generate();
		seed = "NE_20231124_TT05";

		final ImmutableList<String> domainsOfInfluence = Stream.concat(
						configuration.getContest().getElectionGroupBallot().stream().map(ElectionGroupBallotType::getDomainOfInfluence),
						configuration.getContest().getVoteInformation().stream().map(voteInformation -> voteInformation.getVote().getDomainOfInfluence()))
				.collect(toImmutableList());
		final ImmutableList<VerificationCardSetEntity> verificationCardSetIds = IntStream.range(0, 4)
				.mapToObj(i -> new VerificationCardSetEntity(uuidGenerator.generate(), null, null, "", "", "", 1,
						domainsOfInfluence))
				.collect(toImmutableList());

		when(verificationCardSetService.getVerificationCardSets(electionEventId)).thenReturn(verificationCardSetIds);
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, seed),
				Arguments.of(electionEventId, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void genSetupDataWithNullParametersThrows(final String electionEventId, final String seed) {
		assertThrows(NullPointerException.class, () -> genSetupDataService.genSetupData(electionEventId, seed));
	}

	@Test
	@DisplayName("invalid election event id throws FailedValidationException")
	void genSetupDataWithInvalidElectionEventIdThrows() {
		assertThrows(FailedValidationException.class, () -> genSetupDataService.genSetupData("InvalidElectionEventId", seed));
	}

	@Test
	@DisplayName("invalid seed throws FailedValidationException")
	void genSetupDataWithInvalidSeedThrows() {
		assertThrows(FailedValidationException.class, () -> genSetupDataService.genSetupData(electionEventId, "InvalidSeed_^``"));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void genSetupDataWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(() -> genSetupDataService.genSetupData(electionEventId, seed));
	}

}
