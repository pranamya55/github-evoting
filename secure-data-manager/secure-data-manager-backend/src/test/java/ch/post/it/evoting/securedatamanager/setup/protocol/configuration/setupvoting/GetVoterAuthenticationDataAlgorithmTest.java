/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BIRTH_YEAR;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Factory;
import ch.post.it.evoting.cryptoprimitives.hashing.Argon2Profile;
import ch.post.it.evoting.cryptoprimitives.hashing.Hash;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.Random;
import ch.post.it.evoting.cryptoprimitives.math.RandomFactory;
import ch.post.it.evoting.domain.ElectionSetupUtils;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ContestType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ExtendedAuthenticationKeysDefinitionType;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;

@DisplayName("GetVoterAuthenticationDataAlgorithm")
class GetVoterAuthenticationDataAlgorithmTest {

	private static final Random random = RandomFactory.createRandom();
	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final int NUMBER_OF_ELIGIBLE_VOTERS = random.genRandomInteger(2) + 1;
	private static final ImmutableList<String> startVotingKeys = IntStream.range(0, NUMBER_OF_ELIGIBLE_VOTERS)
			.mapToObj(ignored -> ElectionSetupUtils.genStartVotingKey())
			.collect(toImmutableList());
	private static final ImmutableList<String> extendedAuthenticationFactors = IntStream.range(0, NUMBER_OF_ELIGIBLE_VOTERS)
			.mapToObj(i -> "1944")
			.collect(toImmutableList());
	private static final GetVoterAuthenticationDataInput input = new GetVoterAuthenticationDataInput(startVotingKeys, extendedAuthenticationFactors);
	private static final int EXTENDED_AUTHENTICATION_FACTOR_LENGTH = 4;
	private static final String ELECTION_EVENT_ID = uuidGenerator.generate();
	private static final GetVoterAuthenticationDataContext context = new GetVoterAuthenticationDataContext(ELECTION_EVENT_ID,
			NUMBER_OF_ELIGIBLE_VOTERS,
			EXTENDED_AUTHENTICATION_FACTOR_LENGTH);
	private static final Hash hash = HashFactory.createHash();
	private static final Argon2 argon2 = Argon2Factory.createArgon2(Argon2Profile.TEST);
	private static final DeriveCredentialIdAlgorithm deriveCredentialIdAlgorithm = new DeriveCredentialIdAlgorithm(hash,
			BaseEncodingFactory.createBase16(),
			argon2);
	private static EvotingConfigService evotingConfigService;
	private static GetVoterAuthenticationDataAlgorithm getVoterAuthenticationDataAlgorithm;

	@BeforeAll
	static void setUp() {
		evotingConfigService = mock(EvotingConfigService.class);

		when(evotingConfigService.load()).thenReturn(getConfiguration(BIRTH_YEAR));

		final DeriveBaseAuthenticationChallengeAlgorithm deriveBaseAuthenticationChallengeAlgorithm = new DeriveBaseAuthenticationChallengeAlgorithm(
				hash, argon2, BaseEncodingFactory.createBase64());
		getVoterAuthenticationDataAlgorithm = new GetVoterAuthenticationDataAlgorithm(deriveCredentialIdAlgorithm,
				deriveBaseAuthenticationChallengeAlgorithm);
	}

	@Test
	@DisplayName("calling getVoterAuthenticationData with null parameters throws a NullPointerException.")
	void getVoterAuthenticationDataWithNullParametersThrows() {
		assertThrows(NullPointerException.class, () -> getVoterAuthenticationDataAlgorithm.getVoterAuthenticationData(null, input));
		assertThrows(NullPointerException.class, () -> getVoterAuthenticationDataAlgorithm.getVoterAuthenticationData(context, null));
	}

	@Test
	@DisplayName("calling getVoterAuthenticationData with invalid election event id throws a FailedValidationException.")
	void getVoterAuthenticationDataWithInvalidParametersThrows() {
		assertThrows(FailedValidationException.class,
				() -> new GetVoterAuthenticationDataContext("not UUID", NUMBER_OF_ELIGIBLE_VOTERS, EXTENDED_AUTHENTICATION_FACTOR_LENGTH));
	}

	@Test
	@DisplayName("invalid extended authentication factors throws a FailedValidationException.")
	void invalidExtendedAuthenticationFactorsThrowsFailedValidation() {
		final GetVoterAuthenticationDataInput notDigitExtendedAuthenticationFactors = new GetVoterAuthenticationDataInput(startVotingKeys,
				extendedAuthenticationFactors.stream().map(ignored -> "notdigit").collect(toImmutableList()));
		assertThrows(FailedValidationException.class,
				() -> getVoterAuthenticationDataAlgorithm.getVoterAuthenticationData(context, notDigitExtendedAuthenticationFactors));

		final GetVoterAuthenticationDataInput wrongSizeExtendedAuthenticationFactors = new GetVoterAuthenticationDataInput(startVotingKeys,
				extendedAuthenticationFactors.stream().map(ignored -> "123").collect(toImmutableList()));
		assertThrows(FailedValidationException.class,
				() -> getVoterAuthenticationDataAlgorithm.getVoterAuthenticationData(context, wrongSizeExtendedAuthenticationFactors));
	}

	@Test
	@DisplayName("calling getVoterAuthenticationData with correct parameters does not throw.")
	void getVoterAuthenticationData() {
		when(evotingConfigService.load()).thenReturn(getConfiguration(BIRTH_YEAR));

		assertDoesNotThrow(() -> getVoterAuthenticationDataAlgorithm.getVoterAuthenticationData(context, input));
	}

	private static Configuration getConfiguration(final String keyName) {
		return new Configuration().withContest(
				new ContestType().withExtendedAuthenticationKeys(new ExtendedAuthenticationKeysDefinitionType().withKeyName(keyName)));
	}

	@Nested
	@DisplayName("a GetVoterAuthenticationDataInput built with")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class GetVoterAuthenticationDataInputTest {

		@Test
		@DisplayName("null parameters throws a NullPointerException.")
		void nullParametersThrowsANullPointer() {
			assertThrows(NullPointerException.class,
					() -> new GetVoterAuthenticationDataInput(null, extendedAuthenticationFactors));
			assertThrows(NullPointerException.class,
					() -> new GetVoterAuthenticationDataInput(startVotingKeys, null));
		}

		@Test
		@DisplayName("empty start voting keys throws an IllegalArgumentException.")
		void emptyStartVotingKeysThrowsIllegalArgument() {
			final ImmutableList<String> emptyStartVotingKeys = ImmutableList.emptyList();

			assertThrows(IllegalArgumentException.class,
					() -> new GetVoterAuthenticationDataInput(emptyStartVotingKeys, extendedAuthenticationFactors));
		}

		@Test
		@DisplayName("empty extended authentication factors throws an IllegalArgumentException.")
		void emptyExtendedAuthenticationFactorsThrowsIllegalArgument() {
			final ImmutableList<String> emptyExtendedAuthenticationFactors = ImmutableList.emptyList();

			assertThrows(IllegalArgumentException.class,
					() -> new GetVoterAuthenticationDataInput(startVotingKeys, emptyExtendedAuthenticationFactors));
		}

		@Test
		@DisplayName("invalid start voting keys throws a FailedValidationException.")
		void invalidStartVotingKeysThrowsFailedValidation() {
			final ImmutableList<String> invalidStartVotingKeys = startVotingKeys.stream().map(ignored -> "not base 32").collect(toImmutableList());

			assertThrows(FailedValidationException.class,
					() -> new GetVoterAuthenticationDataInput(invalidStartVotingKeys, extendedAuthenticationFactors));
		}

		@Test
		@DisplayName("start voting keys and extended authentication factors of different size throws an IllegalArgumentException.")
		void differentSizeStartVotingKeysThrowsIllegalArgument() {
			final ImmutableList<String> differentSizeStartVotingKeys = startVotingKeys.subList(1, startVotingKeys.size());

			assertThrows(IllegalArgumentException.class,
					() -> new GetVoterAuthenticationDataInput(differentSizeStartVotingKeys, extendedAuthenticationFactors));
		}
	}

	@Nested
	@DisplayName("a GetVoterAuthenticationDataOutput built with")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class GetVoterAuthenticationDataOutputTest {

		private final GetVoterAuthenticationDataOutput output = getVoterAuthenticationDataAlgorithm.getVoterAuthenticationData(context, input);
		private final ImmutableList<String> derivedVoterIdentifiers = output.derivedVoterIdentifiers();
		private final ImmutableList<String> baseAuthenticationChallenges = output.baseAuthenticationChallenges();

		@Test
		@DisplayName("null parameters throws a NullPointerException.")
		void nullParameterThrowsANullPointer() {
			assertAll(
					() -> assertThrows(NullPointerException.class, () -> new GetVoterAuthenticationDataOutput(null, baseAuthenticationChallenges)),
					() -> assertThrows(NullPointerException.class, () -> new GetVoterAuthenticationDataOutput(derivedVoterIdentifiers, null))
			);
		}

		@Test
		@DisplayName("empty derived voter identifiers throws an IllegalArgumentException.")
		void emptyDerivedVoterIdentifiersThrowsIllegalArgument() {
			final ImmutableList<String> emptyDerivedVoterIdentifiers = ImmutableList.emptyList();

			assertThrows(IllegalArgumentException.class,
					() -> new GetVoterAuthenticationDataOutput(emptyDerivedVoterIdentifiers, baseAuthenticationChallenges));
		}

		@Test
		@DisplayName("empty base authentication challenges throws an IllegalArgumentException.")
		void emptyBaseAuthenticationChallengesThrowsIllegalArgument() {
			final ImmutableList<String> emptyBaseAuthenticationChallenges = ImmutableList.emptyList();

			assertThrows(IllegalArgumentException.class,
					() -> new GetVoterAuthenticationDataOutput(derivedVoterIdentifiers, emptyBaseAuthenticationChallenges));
		}

		@Test
		@DisplayName("invalid derived voter identifiers throws a FailedValidationException.")
		void invalidDerivedVoterIdentifiersThrowsIllegalArgument() {
			final ImmutableList<String> invalidDerivedVoterIdentifiers = derivedVoterIdentifiers.stream()
					.map(ignored -> "not valid UUID")
					.collect(toImmutableList());

			assertThrows(FailedValidationException.class,
					() -> new GetVoterAuthenticationDataOutput(invalidDerivedVoterIdentifiers, baseAuthenticationChallenges));
		}

		@Test
		@DisplayName("invalid base authentication challenges throws an FailedValidationException.")
		void invalidBaseAuthenticationChallengesThrowsIllegalArgument() {
			final ImmutableList<String> invalidBaseAuthenticationChallenges = baseAuthenticationChallenges.stream()
					.map(ignored -> "not valid Base64")
					.collect(toImmutableList());

			assertThrows(FailedValidationException.class,
					() -> new GetVoterAuthenticationDataOutput(derivedVoterIdentifiers, invalidBaseAuthenticationChallenges));
		}

		@Test
		@DisplayName("derived voter identifiers and base authentication challenges of different size throws an IllegalArgumentException.")
		void differentSizeBaseAuthenticationChallengesThrowsIllegalArgument() {
			final ImmutableList<String> differentSizeBaseAuthenticationChallenges = baseAuthenticationChallenges.subList(1,
					baseAuthenticationChallenges.size());

			assertThrows(IllegalArgumentException.class,
					() -> new GetVoterAuthenticationDataOutput(derivedVoterIdentifiers, differentSizeBaseAuthenticationChallenges));
		}
	}
}
