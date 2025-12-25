/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.configuration.generateenclongcodeshares;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.controlcomponent.process.BallotBoxService;
import ch.post.it.evoting.controlcomponent.process.CcrjReturnCodesKeysService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.process.ElectionEventState;
import ch.post.it.evoting.controlcomponent.process.ElectionEventStateService;
import ch.post.it.evoting.controlcomponent.process.IdentifierValidationService;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenEncLongCodeSharesOutput;
import ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting.GenEncLongCodeSharesService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ExponentiationProof;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeShare;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;

@Service
public class GenerateEncryptedLongReturnCodeSharesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEncryptedLongReturnCodeSharesService.class);

	private final BallotBoxService ballotBoxService;
	private final ElectionEventService electionEventService;
	private final ElectionEventStateService electionEventStateService;
	private final CcrjReturnCodesKeysService ccrjReturnCodesKeysService;
	private final IdentifierValidationService identifierValidationService;
	private final GenEncLongCodeSharesService genEncLongCodeSharesService;
	private final EncryptedLongReturnCodeSharesService encryptedLongReturnCodeSharesService;

	@Value("${nodeID}")
	private int nodeId;

	public GenerateEncryptedLongReturnCodeSharesService(
			final BallotBoxService ballotBoxService,
			final ElectionEventService electionEventService,
			final ElectionEventStateService electionEventStateService,
			final CcrjReturnCodesKeysService ccrjReturnCodesKeysService,
			final IdentifierValidationService identifierValidationService,
			final GenEncLongCodeSharesService genEncLongCodeSharesService,
			final EncryptedLongReturnCodeSharesService encryptedLongReturnCodeSharesService) {
		this.ballotBoxService = ballotBoxService;
		this.electionEventService = electionEventService;
		this.electionEventStateService = electionEventStateService;
		this.ccrjReturnCodesKeysService = ccrjReturnCodesKeysService;
		this.identifierValidationService = identifierValidationService;
		this.genEncLongCodeSharesService = genEncLongCodeSharesService;
		this.encryptedLongReturnCodeSharesService = encryptedLongReturnCodeSharesService;
	}

	@Transactional
	public ImmutableList<ControlComponentCodeShare> performGenEncLongCodeShares(
			final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload) {
		checkNotNull(setupComponentVerificationDataPayload);

		final String electionEventId = setupComponentVerificationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationDataPayload.getVerificationCardSetId();
		final int chunkId = setupComponentVerificationDataPayload.getChunkId();

		// Validate encryption group.
		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		checkState(setupComponentVerificationDataPayload.getEncryptionGroup().equals(encryptionGroup),
				"The group of te setup component verification data payload must be equal to the encryption group.");
		identifierValidationService.validateIds(electionEventId, verificationCardSetId);

		// Validate election event state.
		final ElectionEventState expectedState = ElectionEventState.INITIAL;
		final ElectionEventState electionEventState = electionEventStateService.getElectionEventState(electionEventId);
		checkState(expectedState.equals(electionEventState),
				"The election event is not in the expected state. [electionEventId: %s, nodeId: %s, expected: %s, actual: %s]", electionEventId,
				nodeId, expectedState, electionEventState);

		// Perform GenEncLongCodeShares.
		final ZqElement ccrjReturnCodesGenerationSecretKey = ccrjReturnCodesKeysService.getCcrjReturnCodesGenerationSecretKey(electionEventId);
		final int numberOfVotingOptions = ballotBoxService.getPrimesMappingTableByVerificationCardSetId(verificationCardSetId)
				.getNumberOfVotingOptions();
		final GenEncLongCodeSharesOutput genEncLongCodeSharesOutput = genEncLongCodeSharesService.genEncLongCodeShares(
				setupComponentVerificationDataPayload, ccrjReturnCodesGenerationSecretKey, numberOfVotingOptions);
		LOGGER.info("GenEnLongCodeShares algorithm successfully performed. [electionEventId: {}, verificationCardSetId: {}, nodeId: {}, chunkId: {}]",
				electionEventId, verificationCardSetId, nodeId, chunkId);

		// Save ControlComponentCodeShares.
		final ImmutableList<String> verificationCardIds = setupComponentVerificationDataPayload.getSetupComponentVerificationData().stream()
				.map(SetupComponentVerificationData::verificationCardId)
				.collect(toImmutableList());
		final int numberOfEligibleVoters = genEncLongCodeSharesOutput.getExponentiatedEncryptedHashedPartialChoiceReturnCodes().size();
		checkArgument(verificationCardIds.size() == numberOfEligibleVoters);

		final ImmutableList<ControlComponentCodeShare> controlComponentCodeShares = toControlComponentCodeShares(numberOfEligibleVoters,
				verificationCardIds, genEncLongCodeSharesOutput);

		encryptedLongReturnCodeSharesService.save(chunkId, verificationCardSetId, controlComponentCodeShares);
		LOGGER.info("Control component code shares successfully saved. [electionEventId: {}, verificationCardSetId: {}, nodeId: {}, chunkId: {}]",
				electionEventId, verificationCardSetId, nodeId, chunkId);

		return controlComponentCodeShares;
	}

	private static ImmutableList<ControlComponentCodeShare> toControlComponentCodeShares(final int numberOfEligibleVoters,
			final ImmutableList<String> verificationCardIds, final GenEncLongCodeSharesOutput genEncLongCodeSharesOutput) {
		return IntStream.range(0, numberOfEligibleVoters)
				.parallel()
				.mapToObj(i -> {
					final String verificationCardId = verificationCardIds.get(i);
					final GqElement voterChoiceReturnCodeGenerationPublicKeys = genEncLongCodeSharesOutput.getVoterChoiceReturnCodeGenerationPublicKeys()
							.get(i);
					final GqElement voterVoteCastReturnCodeGenerationPublicKeys = genEncLongCodeSharesOutput.getVoterVoteCastReturnCodeGenerationPublicKeys()
							.get(i);
					final ElGamalMultiRecipientCiphertext exponentiatedEncryptedHashedPartialChoiceReturnCodes = genEncLongCodeSharesOutput.getExponentiatedEncryptedHashedPartialChoiceReturnCodes()
							.get(i);
					final ExponentiationProof proofsCorrectExponentiationPartialChoiceReturnCodes = genEncLongCodeSharesOutput.getProofsCorrectExponentiationPartialChoiceReturnCodes()
							.get(i);
					final ElGamalMultiRecipientCiphertext exponentiatedEncryptedHashedConfirmationKeys = genEncLongCodeSharesOutput.getExponentiatedEncryptedHashedConfirmationKeys()
							.get(i);
					final ExponentiationProof proofsCorrectExponentiationConfirmationKeys = genEncLongCodeSharesOutput.getProofsCorrectExponentiationConfirmationKeys()
							.get(i);

					return new ControlComponentCodeShare(verificationCardId,
							new ElGamalMultiRecipientPublicKey(GroupVector.of(voterChoiceReturnCodeGenerationPublicKeys)),
							new ElGamalMultiRecipientPublicKey(GroupVector.of(voterVoteCastReturnCodeGenerationPublicKeys)),
							exponentiatedEncryptedHashedPartialChoiceReturnCodes, exponentiatedEncryptedHashedConfirmationKeys,
							proofsCorrectExponentiationPartialChoiceReturnCodes, proofsCorrectExponentiationConfirmationKeys);
				}).collect(toImmutableList());
	}
}
