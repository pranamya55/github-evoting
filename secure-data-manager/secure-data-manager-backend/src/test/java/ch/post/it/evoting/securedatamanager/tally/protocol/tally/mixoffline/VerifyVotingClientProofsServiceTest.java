/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.protocol.tally.mixoffline;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofFactory.createZeroKnowledgeProof;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashFactory;
import ch.post.it.evoting.cryptoprimitives.math.BaseEncodingFactory;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.domain.generators.ControlComponentBallotBoxPayloadGenerator;
import ch.post.it.evoting.domain.generators.SetupComponentTallyDataPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.common.EncryptedVerifiableVote;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.SetupComponentPublicKeys;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.ElectionEventContextPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.generators.SetupComponentPublicKeysPayloadGenerator;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.agreementalgorithms.GetHashContextAlgorithm;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.preliminaries.electoralmodel.PrimesMappingTableAlgorithms;
import ch.post.it.evoting.evotinglibraries.protocol.algorithms.tally.mixoffline.VerifyVotingClientProofsAlgorithm;
import ch.post.it.evoting.securedatamanager.tally.process.decrypt.IdentifierValidationService;

@DisplayName("verifyVotingClientProofs called with")
class VerifyVotingClientProofsServiceTest {

	private static VerifyVotingClientProofsService verifyVotingClientProofsService;
	private static VerificationCardSetContext verificationCardSetContext;
	private static SetupComponentPublicKeys setupComponentPublicKeys;
	private static ImmutableList<EncryptedVerifiableVote> confirmedEncryptedVotes;
	private static SetupComponentTallyDataPayload setupComponentTallyDataPayload;

	@BeforeAll
	static void setUpAll() {
		final IdentifierValidationService identifierValidationService = mock(IdentifierValidationService.class);
		final PrimesMappingTableAlgorithms primesMappingTableAlgorithms = new PrimesMappingTableAlgorithms();
		final GetHashContextAlgorithm getHashContextAlgorithm = new GetHashContextAlgorithm(BaseEncodingFactory.createBase64(),
				HashFactory.createHash(), primesMappingTableAlgorithms);
		final VerifyVotingClientProofsAlgorithm verifyVotingClientProofsAlgorithm = new VerifyVotingClientProofsAlgorithm(createZeroKnowledgeProof(),
				getHashContextAlgorithm, primesMappingTableAlgorithms);
		verifyVotingClientProofsService = new VerifyVotingClientProofsService(identifierValidationService, verifyVotingClientProofsAlgorithm);

		final ElectionEventContextPayloadGenerator electionEventContextPayloadGenerator = new ElectionEventContextPayloadGenerator();
		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadGenerator.generate();
		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		verificationCardSetContext = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().get(0);
		final String verificationCardSetId = verificationCardSetContext.getVerificationCardSetId();
		final String ballotBoxId = verificationCardSetContext.getBallotBoxId();

		final int psi = primesMappingTableAlgorithms.getPsi(verificationCardSetContext.getPrimesMappingTable());
		final int delta = primesMappingTableAlgorithms.getDelta(verificationCardSetContext.getPrimesMappingTable());
		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		setupComponentPublicKeys = new SetupComponentPublicKeysPayloadGenerator(encryptionGroup).generate(psi, delta).getSetupComponentPublicKeys();
		confirmedEncryptedVotes = new ControlComponentBallotBoxPayloadGenerator(encryptionGroup).generate(electionEventId, ballotBoxId, psi, delta)
				.get(0)
				.getConfirmedEncryptedVotes();

		final ImmutableList<String> verificationCardIds = confirmedEncryptedVotes.stream()
				.map(EncryptedVerifiableVote::contextIds)
				.map(ContextIds::verificationCardId)
				.collect(toImmutableList());
		setupComponentTallyDataPayload = new SetupComponentTallyDataPayloadGenerator(encryptionGroup).generate(electionEventId, verificationCardSetId,
				verificationCardIds);

		doNothing().when(identifierValidationService).validateBallotBoxRelatedIds(electionEventId, ballotBoxId);
	}

	private static Stream<Arguments> provideNullParameters() {
		return Stream.of(
				Arguments.of(null, setupComponentPublicKeys, setupComponentTallyDataPayload, confirmedEncryptedVotes),
				Arguments.of(verificationCardSetContext, null, setupComponentTallyDataPayload, confirmedEncryptedVotes),
				Arguments.of(verificationCardSetContext, setupComponentPublicKeys, null, confirmedEncryptedVotes),
				Arguments.of(verificationCardSetContext, setupComponentPublicKeys, setupComponentTallyDataPayload, null)
		);
	}

	@ParameterizedTest
	@MethodSource("provideNullParameters")
	@DisplayName("null parameters throws NullPointerException")
	void verifyVotingClientProofsWithNullParametersThrows(final VerificationCardSetContext verificationCardSetContext,
			final SetupComponentPublicKeys setupComponentPublicKeys, final SetupComponentTallyDataPayload setupComponentTallyDataPayload,
			final ImmutableList<EncryptedVerifiableVote> confirmedEncryptedVotes) {
		assertThrows(NullPointerException.class,
				() -> verifyVotingClientProofsService.verifyVotingClientProofs(verificationCardSetContext, setupComponentPublicKeys,
						setupComponentTallyDataPayload, confirmedEncryptedVotes));
	}

	@Test
	@DisplayName("valid parameters does not throw")
	void verifyVotingClientProofsWithValidParametersDoesNotThrow() {
		assertDoesNotThrow(() -> verifyVotingClientProofsService.verifyVotingClientProofs(verificationCardSetContext, setupComponentPublicKeys,
				setupComponentTallyDataPayload, confirmedEncryptedVotes));
	}

}
