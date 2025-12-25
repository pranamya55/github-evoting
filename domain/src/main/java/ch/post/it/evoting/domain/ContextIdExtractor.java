/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;

import ch.post.it.evoting.domain.configuration.SetupComponentPublicKeysResponsePayload;
import ch.post.it.evoting.domain.configuration.setupvoting.LongVoteCastReturnCodesAllowListResponsePayload;
import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.domain.tally.GetMixnetInitialCiphertextsRequestPayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRequestPayload;
import ch.post.it.evoting.domain.voting.confirmvote.ConfirmationKey;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCRequestPayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.VotingServerConfirmPayload;
import ch.post.it.evoting.domain.voting.sendvote.CombinedControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.VotingServerEncryptedVotePayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;

/**
 * Utility class that constructs context ids for each different payload used in messages exchanged between the voting-server and the
 * control-components.
 */
public class ContextIdExtractor {

	private ContextIdExtractor() {
		// Intentionally left blank.
	}

	/**
	 * Extracts the context id for the {@link ElectionEventContextPayload}.
	 *
	 * @param electionEventContextPayload the payload from which to extract the context id.
	 * @return the corresponding context id.
	 */
	public static String extract(final ElectionEventContextPayload electionEventContextPayload) {
		checkNotNull(electionEventContextPayload);
		return electionEventContextPayload.getElectionEventContext().electionEventId();
	}

	/**
	 * Extracts the context id for the {@link SetupComponentVerificationDataPayload}.
	 *
	 * @param setupComponentVerificationDataPayload the payload from which to extract the context id.
	 * @return the corresponding context id.
	 */
	public static String extract(final SetupComponentVerificationDataPayload setupComponentVerificationDataPayload) {
		checkNotNull(setupComponentVerificationDataPayload);

		final String electionEventId = setupComponentVerificationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationDataPayload.getVerificationCardSetId();
		final int chunkId = setupComponentVerificationDataPayload.getChunkId();

		return String.join("-", electionEventId, verificationCardSetId, String.valueOf(chunkId));
	}

	/**
	 * Extracts the context id for the {@link SetupComponentLVCCAllowListPayload}.
	 *
	 * @param setupComponentLVCCAllowListPayload the payload from which to extract the context id.
	 * @return the corresponding context id.
	 */
	public static String extract(final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload) {
		checkNotNull(setupComponentLVCCAllowListPayload);

		final String electionEventId = setupComponentLVCCAllowListPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentLVCCAllowListPayload.getVerificationCardSetId();

		return String.join("-", Arrays.asList(electionEventId, verificationCardSetId));
	}

	/**
	 * Extracts the context id for the {@link LongVoteCastReturnCodesAllowListResponsePayload}.
	 *
	 * @param longVoteCastReturnCodesAllowListResponsePayload the payload from which to extract the context id.
	 * @return the corresponding context id.
	 */
	public static String extract(final LongVoteCastReturnCodesAllowListResponsePayload longVoteCastReturnCodesAllowListResponsePayload) {
		checkNotNull(longVoteCastReturnCodesAllowListResponsePayload);

		final String electionEventId = longVoteCastReturnCodesAllowListResponsePayload.electionEventId();
		final String verificationCardSetId = longVoteCastReturnCodesAllowListResponsePayload.verificationCardSetId();

		return String.join("-", Arrays.asList(electionEventId, verificationCardSetId));
	}

	public static String extract(final VotingServerEncryptedVotePayload votingServerEncryptedVotePayload) {
		checkNotNull(votingServerEncryptedVotePayload);
		final String electionEventId = votingServerEncryptedVotePayload.getEncryptedVerifiableVote().contextIds().electionEventId();
		final String verificationCardSetId = votingServerEncryptedVotePayload.getEncryptedVerifiableVote().contextIds().verificationCardSetId();
		final String verificationCardId = votingServerEncryptedVotePayload.getEncryptedVerifiableVote().contextIds().verificationCardId();

		return String.join("-", electionEventId, verificationCardSetId, verificationCardId);
	}

	public static String extract(final CombinedControlComponentPartialDecryptPayload combinedControlComponentPartialDecryptPayload) {
		checkNotNull(combinedControlComponentPartialDecryptPayload);
		checkState(!combinedControlComponentPartialDecryptPayload.controlComponentPartialDecryptPayloads().isEmpty(),
				"the combinedControlComponentPartialDecrypt does not contains any controlComponentPartialDecryptPayloads");

		final ContextIds contextIds = combinedControlComponentPartialDecryptPayload.controlComponentPartialDecryptPayloads().get(0)
				.getPartiallyDecryptedEncryptedPCC().contextIds();

		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();

		return String.join("-", electionEventId, verificationCardSetId, verificationCardId);
	}

	public static String extract(final SetupComponentPublicKeysPayload setupComponentPublicKeysPayload) {
		checkNotNull(setupComponentPublicKeysPayload);

		return setupComponentPublicKeysPayload.getElectionEventId();
	}

	public static String extract(final SetupComponentPublicKeysResponsePayload setupComponentPublicKeysResponsePayload) {
		return setupComponentPublicKeysResponsePayload.electionEventId();
	}

	public static String extract(final GetMixnetInitialCiphertextsRequestPayload getMixnetInitialCiphertextsRequestPayload) {
		checkNotNull(getMixnetInitialCiphertextsRequestPayload);

		final String electionEventId = getMixnetInitialCiphertextsRequestPayload.electionEventId();
		final String ballotBoxId = getMixnetInitialCiphertextsRequestPayload.ballotBoxId();

		return String.join("-", electionEventId, ballotBoxId);
	}

	/**
	 * Extracts the context id for the {@link MixDecryptOnlineRequestPayload}.
	 *
	 * @param mixDecryptOnlineRequestPayload the payload from which to extract the context id.
	 * @return the corresponding context id.
	 */
	public static String extract(final MixDecryptOnlineRequestPayload mixDecryptOnlineRequestPayload) {
		checkNotNull(mixDecryptOnlineRequestPayload);

		final String electionEventId = mixDecryptOnlineRequestPayload.electionEventId();
		final String ballotBoxId = mixDecryptOnlineRequestPayload.ballotBoxId();

		return String.join("-", electionEventId, ballotBoxId);
	}

	public static String extract(final VotingServerConfirmPayload votingServerConfirmPayload) {
		checkNotNull(votingServerConfirmPayload);

		final ContextIds contextIds = votingServerConfirmPayload.getConfirmationKey().contextIds();

		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();
		final String confirmationAttemptId = String.valueOf(votingServerConfirmPayload.getConfirmationAttemptId());

		return String.join("-", electionEventId, verificationCardSetId, verificationCardId, confirmationAttemptId);
	}

	public static String extract(final ControlComponenthlVCCRequestPayload controlComponenthlVCCRequestPayload) {
		checkNotNull(controlComponenthlVCCRequestPayload);

		final ControlComponenthlVCCSharePayload controlComponenthlVCCSharePayload = controlComponenthlVCCRequestPayload.controlComponenthlVCCPayloads()
				.get(0);
		final ConfirmationKey confirmationKey = controlComponenthlVCCSharePayload.getConfirmationKey();
		final String confirmationAttemptId = String.valueOf(controlComponenthlVCCSharePayload.getConfirmationAttemptId());
		final ContextIds contextIds = confirmationKey.contextIds();

		final String electionEventId = contextIds.electionEventId();
		final String verificationCardSetId = contextIds.verificationCardSetId();
		final String verificationCardId = contextIds.verificationCardId();
		return String.join("-", electionEventId, verificationCardSetId, verificationCardId, confirmationAttemptId);
	}
}

