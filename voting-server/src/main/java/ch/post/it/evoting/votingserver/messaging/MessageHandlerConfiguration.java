/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.messaging;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.domain.ContextIdExtractor;
import ch.post.it.evoting.domain.configuration.SetupComponentPublicKeysResponsePayload;
import ch.post.it.evoting.domain.configuration.setupvoting.LongVoteCastReturnCodesAllowListResponsePayload;
import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;
import ch.post.it.evoting.domain.tally.GetMixnetInitialCiphertextsRequestPayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineRequestPayload;
import ch.post.it.evoting.domain.tally.MixDecryptOnlineResponsePayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCRequestPayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponenthlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.ControlComponentlVCCSharePayload;
import ch.post.it.evoting.domain.voting.confirmvote.VotingServerConfirmPayload;
import ch.post.it.evoting.domain.voting.sendvote.CombinedControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentPartialDecryptPayload;
import ch.post.it.evoting.domain.voting.sendvote.ControlComponentlCCSharePayload;
import ch.post.it.evoting.domain.voting.sendvote.VotingServerEncryptedVotePayload;
import ch.post.it.evoting.evotinglibraries.domain.configuration.ControlComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ControlComponentVotesHashPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.SetupComponentPublicKeysPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.votingserver.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.votingserver.process.configuration.compute.ComputeEncryptedLongReturnCodeSharesService;
import ch.post.it.evoting.votingserver.process.configuration.requestcckeys.RequestCcKeysService;
import ch.post.it.evoting.votingserver.process.configuration.upload.UploadLongVoteCastReturnCodesAllowListService;
import ch.post.it.evoting.votingserver.process.tally.mixdecrypt.GetMixnetInitialCiphertextsService;
import ch.post.it.evoting.votingserver.process.tally.mixdecrypt.MixDecryptService;
import ch.post.it.evoting.votingserver.process.voting.confirmvote.VoteCastReturnCodeService;
import ch.post.it.evoting.votingserver.process.voting.sendvote.ChoiceReturnCodesService;

@Configuration
public class MessageHandlerConfiguration {

	private final RequestCcKeysService requestCcKeysService;
	private final MixDecryptService mixDecryptService;
	private final ChoiceReturnCodesService choiceReturnCodesService;
	private final VoteCastReturnCodeService voteCastReturnCodeService;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;
	private final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService;
	private final ComputeEncryptedLongReturnCodeSharesService computeEncryptedLongReturnCodeSharesService;
	private final UploadLongVoteCastReturnCodesAllowListService uploadLongVoteCastReturnCodesAllowListService;

	public MessageHandlerConfiguration(
			final RequestCcKeysService requestCcKeysService,
			final MixDecryptService mixDecryptService,
			final ChoiceReturnCodesService choiceReturnCodesService,
			final VoteCastReturnCodeService voteCastReturnCodeService,
			final SetupComponentPublicKeysService setupComponentPublicKeysService,
			final GetMixnetInitialCiphertextsService getMixnetInitialCiphertextsService,
			final ComputeEncryptedLongReturnCodeSharesService computeEncryptedLongReturnCodeSharesService,
			final UploadLongVoteCastReturnCodesAllowListService uploadLongVoteCastReturnCodesAllowListService) {
		this.requestCcKeysService = requestCcKeysService;
		this.mixDecryptService = mixDecryptService;
		this.choiceReturnCodesService = choiceReturnCodesService;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
		this.getMixnetInitialCiphertextsService = getMixnetInitialCiphertextsService;
		this.computeEncryptedLongReturnCodeSharesService = computeEncryptedLongReturnCodeSharesService;
		this.uploadLongVoteCastReturnCodesAllowListService = uploadLongVoteCastReturnCodesAllowListService;
		this.voteCastReturnCodeService = voteCastReturnCodeService;
	}

	@Bean
	@SuppressWarnings("java:S1452") // The usage of generic wildcard types is the chosen design in this particular case.
	public List<MessageHandler.Configuration<?, ?>> messageHandlerConfigurations() {
		return List.of(
				mixDecryptConfiguration(),
				requestCcKeysConfiguration(),
				getMixnetInitialCiphertextsConfiguration(),
				longVoteCastReturnCodesShareHashConfiguration(),
				longChoiceReturnCodesShareConfiguration(),
				longVoteCastReturnCodesShareVerifyConfiguration(),
				computeEncryptedLongReturnCodeSharesConfiguration(),
				partialDecryptConfiguration(),
				uploadLongVoteCastReturnCodesAllowListConfiguration(),
				uploadSetupComponentPublicKeysConfiguration()
		);
	}

	private MessageHandler.Configuration<ControlComponenthlVCCRequestPayload, ControlComponentlVCCSharePayload> longVoteCastReturnCodesShareVerifyConfiguration() {
		return new MessageHandler.Configuration<>(
				ControlComponenthlVCCRequestPayload.class,
				true,
				ContextIdExtractor::extract,
				ControlComponentlVCCSharePayload.class,
				voteCastReturnCodeService::extractNodeId,
				voteCastReturnCodeService::deserializeControlComponentLVCCSharePayload,
				voteCastReturnCodeService::onResponseLongVoteCastReturnCodesShareVerify,
				true);
	}

	private MessageHandler.Configuration<VotingServerConfirmPayload, ControlComponenthlVCCSharePayload> longVoteCastReturnCodesShareHashConfiguration() {
		return new MessageHandler.Configuration<>(
				VotingServerConfirmPayload.class,
				true,
				ContextIdExtractor::extract,
				ControlComponenthlVCCSharePayload.class,
				voteCastReturnCodeService::extractNodeId,
				voteCastReturnCodeService::deserializeControlComponenthlVCCPayload,
				voteCastReturnCodeService::onResponseLongVoteCastReturnCodesShareHash,
				true);
	}

	private MessageHandler.Configuration<SetupComponentPublicKeysPayload, SetupComponentPublicKeysResponsePayload> uploadSetupComponentPublicKeysConfiguration() {
		return new MessageHandler.Configuration<>(
				SetupComponentPublicKeysPayload.class,
				true,
				ContextIdExtractor::extract,
				SetupComponentPublicKeysResponsePayload.class,
				setupComponentPublicKeysService::extractNodeId,
				setupComponentPublicKeysService::deserialize,
				setupComponentPublicKeysService::onResponse,
				true);
	}

	private MessageHandler.Configuration<CombinedControlComponentPartialDecryptPayload, ControlComponentlCCSharePayload> longChoiceReturnCodesShareConfiguration() {
		return new MessageHandler.Configuration<>(
				CombinedControlComponentPartialDecryptPayload.class,
				true,
				ContextIdExtractor::extract,
				ControlComponentlCCSharePayload.class,
				choiceReturnCodesService::extractNodeId,
				choiceReturnCodesService::deserializeLCCSharePayload,
				choiceReturnCodesService::onResponseLongChoiceReturnCodesShare,
				true);
	}

	private MessageHandler.Configuration<VotingServerEncryptedVotePayload, ControlComponentPartialDecryptPayload> partialDecryptConfiguration() {
		return new MessageHandler.Configuration<>(
				VotingServerEncryptedVotePayload.class,
				true,
				ContextIdExtractor::extract,
				ControlComponentPartialDecryptPayload.class,
				choiceReturnCodesService::extractNodeId,
				choiceReturnCodesService::deserializePartialDecryptPayload,
				choiceReturnCodesService::onResponsePartialDecrypt,
				true);
	}

	private MessageHandler.Configuration<ElectionEventContextPayload, ControlComponentPublicKeysPayload> requestCcKeysConfiguration() {
		return new MessageHandler.Configuration<>(
				ElectionEventContextPayload.class,
				true,
				ContextIdExtractor::extract,
				ControlComponentPublicKeysPayload.class,
				requestCcKeysService::extractNodeId,
				requestCcKeysService::deserialize,
				requestCcKeysService::onResponse,
				true);
	}

	private MessageHandler.Configuration<MixDecryptOnlineRequestPayload, MixDecryptOnlineResponsePayload> mixDecryptConfiguration() {
		return new MessageHandler.Configuration<>(
				MixDecryptOnlineRequestPayload.class,
				false,
				ContextIdExtractor::extract,
				MixDecryptOnlineResponsePayload.class,
				mixDecryptService::extractNodeId,
				mixDecryptService::deserialize,
				mixDecryptService::onResponse,
				false);
	}

	private MessageHandler.Configuration<SetupComponentVerificationDataPayload, ControlComponentCodeSharesPayload> computeEncryptedLongReturnCodeSharesConfiguration() {
		return new MessageHandler.Configuration<>(
				SetupComponentVerificationDataPayload.class,
				true,
				ContextIdExtractor::extract,
				ControlComponentCodeSharesPayload.class,
				computeEncryptedLongReturnCodeSharesService::extractNodeId,
				computeEncryptedLongReturnCodeSharesService::deserialize,
				computeEncryptedLongReturnCodeSharesService::onResponse,
				false);
	}

	private MessageHandler.Configuration<SetupComponentLVCCAllowListPayload, LongVoteCastReturnCodesAllowListResponsePayload> uploadLongVoteCastReturnCodesAllowListConfiguration() {
		return new MessageHandler.Configuration<>(
				SetupComponentLVCCAllowListPayload.class,
				true,
				ContextIdExtractor::extract,
				LongVoteCastReturnCodesAllowListResponsePayload.class,
				uploadLongVoteCastReturnCodesAllowListService::extractNodeId,
				uploadLongVoteCastReturnCodesAllowListService::deserializePayload,
				uploadLongVoteCastReturnCodesAllowListService::onResponse,
				true);
	}

	private MessageHandler.Configuration<GetMixnetInitialCiphertextsRequestPayload, ControlComponentVotesHashPayload> getMixnetInitialCiphertextsConfiguration() {
		return new MessageHandler.Configuration<>(
				GetMixnetInitialCiphertextsRequestPayload.class,
				true,
				ContextIdExtractor::extract,
				ControlComponentVotesHashPayload.class,
				getMixnetInitialCiphertextsService::extractNodeId,
				getMixnetInitialCiphertextsService::deserialize,
				getMixnetInitialCiphertextsService::onResponse,
				true);
	}
}
