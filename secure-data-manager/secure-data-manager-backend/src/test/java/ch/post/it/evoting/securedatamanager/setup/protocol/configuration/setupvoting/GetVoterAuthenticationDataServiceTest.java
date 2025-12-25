/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BIRTH_DATE;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.EXTENDED_AUTHENTICATION_FACTORS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;
import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.Base64Alphabet;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ContestType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ExtendedAuthenticationKeysDefinitionType;

@DisplayName("getVoterAuthenticationData called with")
class GetVoterAuthenticationDataServiceTest {

	private static final Random random = RandomFactory.createRandom();

	private static GetVoterAuthenticationDataService getVoterAuthenticationDataService;
	private static String verificationCardSetId;
	private static Configuration configuration;
	private static ImmutableList<String> startVotingKeys;
	private static ImmutableList<String> extendedAuthenticationFactors;
	private static ElectionEventContextPayload electionEventContextPayload;

	@BeforeAll
	static void setUpAll() {
		final GetVoterAuthenticationDataAlgorithm getVoterAuthenticationDataAlgorithm = mock(GetVoterAuthenticationDataAlgorithm.class);
		getVoterAuthenticationDataService = new GetVoterAuthenticationDataService(getVoterAuthenticationDataAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		verificationCardSetId = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().get(0)
				.getVerificationCardSetId();
		configuration = new Configuration().withContest(
				new ContestType().withExtendedAuthenticationKeys(new ExtendedAuthenticationKeysDefinitionType().withKeyName(BIRTH_DATE)));

		final int numberOfEligibleVoters = electionEventContextPayload.getElectionEventContext()
				.verificationCardSetContexts().stream()
				.filter(verificationCardSetContext -> verificationCardSetContext.getVerificationCardSetId().equals(verificationCardSetId))
				.map(VerificationCardSetContext::getNumberOfEligibleVoters)
				.collect(MoreCollectors.onlyElement());
		startVotingKeys = IntStream.range(0, numberOfEligibleVoters)
				.mapToObj(ignored -> ElectionSetupUtils.genStartVotingKey())
				.collect(toImmutableList());
		extendedAuthenticationFactors = IntStream.range(0, numberOfEligibleVoters)
				.mapToObj(ignored -> String.join("", random.genUniqueDecimalStrings(4, 2)))
				.collect(toImmutableList());

		final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
		when(getVoterAuthenticationDataAlgorithm.getVoterAuthenticationData(any(), any())).thenReturn(new GetVoterAuthenticationDataOutput(
				ImmutableList.of(uuidGenerator.generate()),
				ImmutableList.of(random.genRandomString(BASE64_ENCODED_HASH_OUTPUT_LENGTH, Base64Alphabet.getInstance())))
		);
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, verificationCardSetId, configuration, startVotingKeys, extendedAuthenticationFactors),
				Arguments.of(electionEventContextPayload, null, configuration, startVotingKeys, extendedAuthenticationFactors),
				Arguments.of(electionEventContextPayload, verificationCardSetId, null, startVotingKeys, extendedAuthenticationFactors),
				Arguments.of(electionEventContextPayload, verificationCardSetId, configuration, null, extendedAuthenticationFactors),
				Arguments.of(electionEventContextPayload, verificationCardSetId, configuration, startVotingKeys, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void getVoterAuthenticationDataWithNullParametersThrows(final ElectionEventContextPayload electionEventContextPayload,
			final String verificationCardSetId, final Configuration configuration, final ImmutableList<String> startVotingKeys,
			final ImmutableList<String> extendedAuthenticationFactors) {
		assertThrows(NullPointerException.class,
				() -> getVoterAuthenticationDataService.getVoterAuthenticationData(electionEventContextPayload, verificationCardSetId, configuration,
						startVotingKeys, extendedAuthenticationFactors));
	}

	@Test
	@DisplayName("invalid verification card set id throws FailedValidationException")
	void getVoterAuthenticationDataWithInvalidVerificationCardSetIdThrows() {
		assertThrows(FailedValidationException.class,
				() -> getVoterAuthenticationDataService.getVoterAuthenticationData(electionEventContextPayload, "InvalidVerificationCardSetId",
						configuration, startVotingKeys, extendedAuthenticationFactors));
	}

	@Test
	@DisplayName("multiple extended authentication factors throws IllegalStateException")
	void getVoterAuthenticationDataWithMultipleExtendedAuthenticationFactorsThrows() {
		final ExtendedAuthenticationKeysDefinitionType multipleExtendedAuthenticationFactors = new ExtendedAuthenticationKeysDefinitionType()
				.withKeyName(EXTENDED_AUTHENTICATION_FACTORS.keySet().asSet());
		final Configuration invalidConfiguration = new Configuration();
		invalidConfiguration.setContest(new ContestType().withExtendedAuthenticationKeys(multipleExtendedAuthenticationFactors));

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> getVoterAuthenticationDataService.getVoterAuthenticationData(electionEventContextPayload, verificationCardSetId,
						invalidConfiguration, startVotingKeys, extendedAuthenticationFactors));

		final String expected = String.format("There must be a single extended authentication key name. [size: %s]",
				multipleExtendedAuthenticationFactors.getKeyName().size());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("invalid extended authentication factor throws IllegalStateException")
	void getVoterAuthenticationDataWithInvalidExtendedAuthenticationFactorThrows() {
		final ExtendedAuthenticationKeysDefinitionType invalidExtendedAuthenticationFactor = new ExtendedAuthenticationKeysDefinitionType()
				.withKeyName("invalid");
		final Configuration invalidConfiguration = new Configuration();
		invalidConfiguration.setContest(new ContestType().withExtendedAuthenticationKeys(invalidExtendedAuthenticationFactor));

		final IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> getVoterAuthenticationDataService.getVoterAuthenticationData(electionEventContextPayload, verificationCardSetId,
						invalidConfiguration, startVotingKeys, extendedAuthenticationFactors));

		final String expected = String.format("Unsupported extended authentication factor. [name: %s]",
				invalidExtendedAuthenticationFactor.getKeyName().getFirst());
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void getVoterAuthenticationDataWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(
				() -> getVoterAuthenticationDataService.getVoterAuthenticationData(electionEventContextPayload, verificationCardSetId, configuration,
						startVotingKeys, extendedAuthenticationFactors));
	}

}
