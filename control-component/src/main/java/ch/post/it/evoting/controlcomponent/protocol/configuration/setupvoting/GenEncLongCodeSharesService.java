/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.VotingOptionsConstants.MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;

@Service
public class GenEncLongCodeSharesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenEncLongCodeSharesService.class);

	private final GenEncLongCodeSharesAlgorithm genEncLongCodeSharesAlgorithm;

	@Value("${nodeID}")
	private int nodeId;

	public GenEncLongCodeSharesService(final GenEncLongCodeSharesAlgorithm genEncLongCodeSharesAlgorithm) {
		this.genEncLongCodeSharesAlgorithm = genEncLongCodeSharesAlgorithm;
	}

	/**
	 * Invokes the GenEncLongCodeShares algorithm.
	 *
	 * @param setupComponentVerificationDataPayload the setup component verification data payload. Must be non-null.
	 * @param ccrjReturnCodesGenerationSecretKey    the CCRj Choice Return Codes secret key. Must be non-null.
	 * @param numberOfVotingOptions                 the number of voting options. Must be in range [1, n<sub>sup</sub>].
	 * @throws NullPointerException     if any parameter is null.
	 * @throws IllegalArgumentException if the number of voting options is not in the expected range.
	 */
	public GenEncLongCodeSharesOutput genEncLongCodeShares(final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload,
			final ZqElement ccrjReturnCodesGenerationSecretKey, final int numberOfVotingOptions) {
		checkNotNull(setupComponentVerificationDataPayload);
		checkNotNull(ccrjReturnCodesGenerationSecretKey);
		checkArgument(numberOfVotingOptions > 0, "The number of voting options must be strictly positive.");
		checkArgument(numberOfVotingOptions <= MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS,
				"The number of voting options must be smaller or equal to the maximum supported number of voting options. [n: %s, n_sup: %s]",
				numberOfVotingOptions, MAXIMUM_SUPPORTED_NUMBER_OF_VOTING_OPTIONS);

		final GqGroup encryptionGroup = setupComponentVerificationDataPayload.getEncryptionGroup();
		final String electionEventId = setupComponentVerificationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationDataPayload.getVerificationCardSetId();
		final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData = setupComponentVerificationDataPayload.getSetupComponentVerificationData();
		final ImmutableList<String> verificationCardIds = setupComponentVerificationData.stream()
				.map(SetupComponentVerificationData::verificationCardId)
				.collect(toImmutableList());

		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashedPartialChoiceReturnCodes = setupComponentVerificationData.stream()
				.map(SetupComponentVerificationData::encryptedHashedSquaredPartialChoiceReturnCodes)
				.collect(GroupVector.toGroupVector());
		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeys = setupComponentVerificationData.stream()
				.map(SetupComponentVerificationData::verificationCardPublicKey)
				.collect(GroupVector.toGroupVector());
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> encryptedHashConfirmationKeys = setupComponentVerificationData.stream()
				.map(SetupComponentVerificationData::encryptedHashedSquaredConfirmationKey)
				.collect(GroupVector.toGroupVector());

		final GenEncLongCodeSharesContext genEncLongCodeSharesContext = new GenEncLongCodeSharesContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setNodeId(nodeId)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setNumberOfVotingOptions(numberOfVotingOptions)
				.build();

		final GenEncLongCodeSharesInput genEncLongCodeSharesInput = new GenEncLongCodeSharesInput.Builder()
				.setReturnCodesGenerationSecretKey(ccrjReturnCodesGenerationSecretKey)
				.setVerificationCardPublicKeys(verificationCardPublicKeys)
				.setEncryptedHashedPartialChoiceReturnCodes(encryptedHashedPartialChoiceReturnCodes)
				.setEncryptedHashedConfirmationKeys(encryptedHashConfirmationKeys)
				.build();

		LOGGER.debug("Performing GenEncLongCodeShares algorithm... [electionEventId: {}, verificationCardSetId: {}, nodeId: {}, chunkId: {}]",
				electionEventId, verificationCardSetId, nodeId, setupComponentVerificationDataPayload.getChunkId());

		return genEncLongCodeSharesAlgorithm.genEncLongCodeShares(genEncLongCodeSharesContext, genEncLongCodeSharesInput);
	}
}
