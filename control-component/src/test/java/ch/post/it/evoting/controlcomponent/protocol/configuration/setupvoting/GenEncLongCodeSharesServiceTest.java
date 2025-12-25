/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.utils.KeyDerivationFactory.createKeyDerivation;
import static ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory.createZeroKnowledgeProof;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Throwables;

import ch.post.it.evoting.controlcomponent.process.VerificationCardService;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.domain.generators.SetupComponentVerificationDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;

@DisplayName("genEncLongCodeShares called with")
class GenEncLongCodeSharesServiceTest {

	private static GenEncLongCodeSharesService genEncLongCodeSharesService;
	private static SetupComponentVerificationDataPayload setupComponentVerificationDataPayload;
	private static ZqElement ccrjReturnCodesGenerationSecretKey;
	private static int numberOfVotingOptions;

	@BeforeAll
	static void setUpAll() {
		final VerificationCardService verificationCardService = mock(VerificationCardService.class);
		final GenEncLongCodeSharesAlgorithm genEncLongCodeSharesAlgorithm = new GenEncLongCodeSharesAlgorithm(createKeyDerivation(),
				createZeroKnowledgeProof(), verificationCardService);
		genEncLongCodeSharesService = new GenEncLongCodeSharesService(genEncLongCodeSharesAlgorithm);

		final SetupComponentVerificationDataPayloadGenerator setupComponentVerificationDataPayloadGenerator = new SetupComponentVerificationDataPayloadGenerator();
		setupComponentVerificationDataPayload = setupComponentVerificationDataPayloadGenerator.generate();
		numberOfVotingOptions = setupComponentVerificationDataPayload.getSetupComponentVerificationData().size();

		final GqGroup encryptionGroup = setupComponentVerificationDataPayload.getEncryptionGroup();
		final ZqGroup zqGroup = ZqGroup.sameOrderAs(encryptionGroup);
		final ZqGroupGenerator zqGroupGenerator = new ZqGroupGenerator(zqGroup);
		ccrjReturnCodesGenerationSecretKey = zqGroupGenerator.genRandomZqElementMember();

		when(verificationCardService.existsNone(any())).thenReturn(true);
		doNothing().when(verificationCardService).saveAll(any());
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, ccrjReturnCodesGenerationSecretKey, numberOfVotingOptions),
				Arguments.of(setupComponentVerificationDataPayload, null, numberOfVotingOptions)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void genEncLongCodeSharesWithNullParametersThrows(final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload,
			final ZqElement ccrjReturnCodesGenerationSecretKey, final int numberOfVotingOptions) {
		assertThrows(NullPointerException.class,
				() -> genEncLongCodeSharesService.genEncLongCodeShares(setupComponentVerificationDataPayload, ccrjReturnCodesGenerationSecretKey,
						numberOfVotingOptions));
	}

	@Test
	@DisplayName("negative number of voting options throws IllegalArgumentException")
	void genEncLongCodeSharesWithNegativeNumberOfVotingOptionsThrows() {
		final int negativeNumberOfVotingOptions = -1;

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genEncLongCodeSharesService.genEncLongCodeShares(setupComponentVerificationDataPayload, ccrjReturnCodesGenerationSecretKey,
						negativeNumberOfVotingOptions));

		final String expected = "The number of voting options must be strictly positive.";
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}

	@Test
	@DisplayName("too big number of voting options throws IllegalArgumentException")
	void genEncLongCodeSharesWithTooBigNumberOfVotingOptionsThrows() {
		final int tooBigNumberOfVotingOptions = MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS + 1;

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> genEncLongCodeSharesService.genEncLongCodeShares(setupComponentVerificationDataPayload, ccrjReturnCodesGenerationSecretKey,
						tooBigNumberOfVotingOptions));

		final String expected = String.format(
				"The number of voting options must be smaller or equal to the maximum supported number of voting options. [n: %s, n_sup: %s]",
				tooBigNumberOfVotingOptions, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);
		assertEquals(expected, Throwables.getRootCause(exception).getMessage());
	}
}
