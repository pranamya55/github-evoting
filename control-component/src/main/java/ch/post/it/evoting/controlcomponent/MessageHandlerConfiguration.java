/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.controlcomponent.commandmessaging.Context;
import ch.post.it.evoting.controlcomponent.process.configuration.generatecckeys.GenerateCcKeysProcessor;
import ch.post.it.evoting.controlcomponent.process.configuration.generateenclongcodeshares.GenerateEncryptedLongReturnCodeSharesProcessor;
import ch.post.it.evoting.controlcomponent.process.configuration.upload.UploadLongVoteCastReturnCodesAllowListProcessor;
import ch.post.it.evoting.controlcomponent.process.configuration.upload.UploadSetupComponentPublicKeysProcessor;
import ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt.GetMixnetInitialCiphertextsProcessor;
import ch.post.it.evoting.controlcomponent.process.tally.mixdecrypt.MixDecryptProcessor;
import ch.post.it.evoting.controlcomponent.process.voting.confirmvote.LongVoteCastReturnCodesShareHashProcessor;
import ch.post.it.evoting.controlcomponent.process.voting.confirmvote.LongVoteCastReturnCodesShareVerifyProcessor;
import ch.post.it.evoting.controlcomponent.process.voting.sendvote.LongChoiceReturnCodeShareProcessor;
import ch.post.it.evoting.controlcomponent.process.voting.sendvote.PartialDecryptProcessor;
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

@Configuration
public class MessageHandlerConfiguration {

	private final GenerateCcKeysProcessor generateCcKeysProcessor;
	private final GenerateEncryptedLongReturnCodeSharesProcessor generateEncryptedLongReturnCodeSharesProcessor;
	private final UploadLongVoteCastReturnCodesAllowListProcessor uploadLongVoteCastReturnCodesAllowListProcessor;
	private final UploadSetupComponentPublicKeysProcessor uploadSetupComponentPublicKeysProcessor;
	private final LongChoiceReturnCodeShareProcessor longChoiceReturnCodeShareProcessor;
	private final LongVoteCastReturnCodesShareHashProcessor longVoteCastReturnCodesShareHashProcessor;
	private final LongVoteCastReturnCodesShareVerifyProcessor longVoteCastReturnCodesShareVerifyProcessor;
	private final GetMixnetInitialCiphertextsProcessor getMixnetInitialCiphertextsProcessor;
	private final PartialDecryptProcessor partialDecryptProcessor;
	private final MixDecryptProcessor mixDecryptProcessor;

	public MessageHandlerConfiguration(
			final GenerateCcKeysProcessor generateCcKeysProcessor,
			final GenerateEncryptedLongReturnCodeSharesProcessor generateEncryptedLongReturnCodeSharesProcessor,
			final UploadSetupComponentPublicKeysProcessor uploadSetupComponentPublicKeysProcessor,
			final UploadLongVoteCastReturnCodesAllowListProcessor uploadLongVoteCastReturnCodesAllowListProcessor,
			final LongChoiceReturnCodeShareProcessor longChoiceReturnCodeShareProcessor,
			final LongVoteCastReturnCodesShareHashProcessor longVoteCastReturnCodesShareHashProcessor,
			final LongVoteCastReturnCodesShareVerifyProcessor longVoteCastReturnCodesShareVerifyProcessor,
			final GetMixnetInitialCiphertextsProcessor getMixnetInitialCiphertextsProcessor,
			final PartialDecryptProcessor partialDecryptProcessor,
			final MixDecryptProcessor mixDecryptProcessor) {
		this.generateCcKeysProcessor = generateCcKeysProcessor;
		this.generateEncryptedLongReturnCodeSharesProcessor = generateEncryptedLongReturnCodeSharesProcessor;
		this.uploadLongVoteCastReturnCodesAllowListProcessor = uploadLongVoteCastReturnCodesAllowListProcessor;
		this.uploadSetupComponentPublicKeysProcessor = uploadSetupComponentPublicKeysProcessor;
		this.longChoiceReturnCodeShareProcessor = longChoiceReturnCodeShareProcessor;
		this.longVoteCastReturnCodesShareHashProcessor = longVoteCastReturnCodesShareHashProcessor;
		this.longVoteCastReturnCodesShareVerifyProcessor = longVoteCastReturnCodesShareVerifyProcessor;
		this.getMixnetInitialCiphertextsProcessor = getMixnetInitialCiphertextsProcessor;
		this.partialDecryptProcessor = partialDecryptProcessor;
		this.mixDecryptProcessor = mixDecryptProcessor;
	}

	@Bean
	@SuppressWarnings("java:S1452") // The usage of generic wildcard types is the chosen design in this particular case.
	public List<MessageHandler.Configuration<?, ?>> messageConsumerConfigurations() {
		return List.of(
				generateCcKeysConfiguration(),
				generateEncryptedLongReturnCodeSharesConfiguration(),
				uploadLongVoteCastReturnCodesAllowListConfiguration(),
				uploadSetupComponentPublicKeysConfiguration(),
				longVoteCastReturnCodesShareHashConfiguration(),
				longVoteCastReturnCodesShareVerifyConfiguration(),
				longChoiceReturnCodesShareConfiguration(),
				getMixnetInitialCiphertextsConfiguration(),
				partialDecryptConfiguration(),
				mixDecryptConfiguration()
		);
	}

	private MessageHandler.Configuration<ElectionEventContextPayload, ControlComponentPublicKeysPayload> generateCcKeysConfiguration() {
		return new MessageHandler.Configuration<>(
				ElectionEventContextPayload.class,
				generateCcKeysProcessor::verifyPayloadSignature,
				Context.CONFIGURATION_RETURN_CODES_GEN_KEYS_CCR,
				generateCcKeysProcessor::onRequest,
				generateCcKeysProcessor::onReplay,
				ContextIdExtractor::extract,
				generateCcKeysProcessor::deserializeRequest,
				ControlComponentPublicKeysPayload.class,
				generateCcKeysProcessor::serializeResponse
		);
	}

	private MessageHandler.Configuration<SetupComponentVerificationDataPayload, ControlComponentCodeSharesPayload> generateEncryptedLongReturnCodeSharesConfiguration() {
		return new MessageHandler.Configuration<>(
				SetupComponentVerificationDataPayload.class,
				generateEncryptedLongReturnCodeSharesProcessor::verifyPayloadSignature,
				Context.CONFIGURATION_RETURN_CODES_GEN_ENC_LONG_CODE_SHARES,
				generateEncryptedLongReturnCodeSharesProcessor::onRequest,
				generateEncryptedLongReturnCodeSharesProcessor::onReplay,
				ContextIdExtractor::extract,
				generateEncryptedLongReturnCodeSharesProcessor::deserializeRequest,
				ControlComponentCodeSharesPayload.class,
				generateEncryptedLongReturnCodeSharesProcessor::serializeResponse
		);
	}

	private MessageHandler.Configuration<SetupComponentLVCCAllowListPayload, LongVoteCastReturnCodesAllowListResponsePayload> uploadLongVoteCastReturnCodesAllowListConfiguration() {
		return new MessageHandler.Configuration<>(
				SetupComponentLVCCAllowListPayload.class,
				uploadLongVoteCastReturnCodesAllowListProcessor::verifyPayloadSignature,
				Context.CONFIGURATION_SETUP_VOTING_LONG_VOTE_CAST_RETURN_CODES_ALLOW_LIST,
				uploadLongVoteCastReturnCodesAllowListProcessor::onRequest,
				uploadLongVoteCastReturnCodesAllowListProcessor::onReplay,
				ContextIdExtractor::extract,
				uploadLongVoteCastReturnCodesAllowListProcessor::deserializeRequest,
				LongVoteCastReturnCodesAllowListResponsePayload.class,
				uploadLongVoteCastReturnCodesAllowListProcessor::serializeResponse
		);
	}

	private MessageHandler.Configuration<SetupComponentPublicKeysPayload, SetupComponentPublicKeysResponsePayload> uploadSetupComponentPublicKeysConfiguration() {
		return new MessageHandler.Configuration<>(
				SetupComponentPublicKeysPayload.class,
				uploadSetupComponentPublicKeysProcessor::verifyPayloadSignature,
				Context.CONFIGURATION_SETUP_COMPONENT_PUBLIC_KEYS,
				uploadSetupComponentPublicKeysProcessor::onRequest,
				uploadSetupComponentPublicKeysProcessor::onReplay,
				ContextIdExtractor::extract,
				uploadSetupComponentPublicKeysProcessor::deserializeRequest,
				SetupComponentPublicKeysResponsePayload.class,
				uploadSetupComponentPublicKeysProcessor::serializeResponse
		);
	}

	private MessageHandler.Configuration<VotingServerConfirmPayload, ControlComponenthlVCCSharePayload> longVoteCastReturnCodesShareHashConfiguration() {
		return new MessageHandler.Configuration<>(
				VotingServerConfirmPayload.class,
				longVoteCastReturnCodesShareHashProcessor::verifyPayloadSignature,
				Context.VOTING_RETURN_CODES_CREATE_LVCC_SHARE_HASH,
				longVoteCastReturnCodesShareHashProcessor::onRequest,
				longVoteCastReturnCodesShareHashProcessor::onReplay,
				ContextIdExtractor::extract,
				longVoteCastReturnCodesShareHashProcessor::deserializeRequest,
				ControlComponenthlVCCSharePayload.class,
				longVoteCastReturnCodesShareHashProcessor::serializeResponse
		);
	}

	private MessageHandler.Configuration<ControlComponenthlVCCRequestPayload, ControlComponentlVCCSharePayload> longVoteCastReturnCodesShareVerifyConfiguration() {
		return new MessageHandler.Configuration<>(
				ControlComponenthlVCCRequestPayload.class,
				longVoteCastReturnCodesShareVerifyProcessor::verifyPayloadSignature,
				Context.VOTING_RETURN_CODES_VERIFY_LVCC_SHARE_HASH,
				longVoteCastReturnCodesShareVerifyProcessor::onRequest,
				longVoteCastReturnCodesShareVerifyProcessor::onReplay,
				ContextIdExtractor::extract,
				longVoteCastReturnCodesShareVerifyProcessor::deserializeRequest,
				ControlComponentlVCCSharePayload.class,
				longVoteCastReturnCodesShareVerifyProcessor::serializeResponse
		);
	}

	private MessageHandler.Configuration<CombinedControlComponentPartialDecryptPayload, ControlComponentlCCSharePayload> longChoiceReturnCodesShareConfiguration() {
		return new MessageHandler.Configuration<>(
				CombinedControlComponentPartialDecryptPayload.class,
				longChoiceReturnCodeShareProcessor::verifyPayload,
				Context.VOTING_RETURN_CODES_CREATE_LCC_SHARE,
				longChoiceReturnCodeShareProcessor::onRequest,
				longChoiceReturnCodeShareProcessor::onReplay,
				ContextIdExtractor::extract,
				longChoiceReturnCodeShareProcessor::deserializeRequest,
				ControlComponentlCCSharePayload.class,
				longChoiceReturnCodeShareProcessor::serializeResponse
		);
	}

	private MessageHandler.Configuration<GetMixnetInitialCiphertextsRequestPayload, ControlComponentVotesHashPayload> getMixnetInitialCiphertextsConfiguration() {
		return new MessageHandler.Configuration<>(
				GetMixnetInitialCiphertextsRequestPayload.class,
				getMixnetInitialCiphertextsProcessor::verifyPayloadSignature,
				Context.MIXING_TALLY_GET_MIXNET_INITIAL_CIPHERTEXTS,
				getMixnetInitialCiphertextsProcessor::onRequest,
				getMixnetInitialCiphertextsProcessor::onReplay,
				ContextIdExtractor::extract,
				getMixnetInitialCiphertextsProcessor::deserializeRequest,
				ControlComponentVotesHashPayload.class,
				getMixnetInitialCiphertextsProcessor::serializeResponse
		);
	}

	private MessageHandler.Configuration<VotingServerEncryptedVotePayload, ControlComponentPartialDecryptPayload> partialDecryptConfiguration() {
		return new MessageHandler.Configuration<>(
				VotingServerEncryptedVotePayload.class,
				partialDecryptProcessor::verifyPayload,
				Context.VOTING_RETURN_CODES_PARTIAL_DECRYPT_PCC,
				partialDecryptProcessor::onRequest,
				partialDecryptProcessor::onReplay,
				ContextIdExtractor::extract,
				partialDecryptProcessor::deserializeRequest,
				ControlComponentPartialDecryptPayload.class,
				partialDecryptProcessor::serializeResponse
		);
	}

	private MessageHandler.Configuration<MixDecryptOnlineRequestPayload, MixDecryptOnlineResponsePayload> mixDecryptConfiguration() {
		return new MessageHandler.Configuration<>(
				MixDecryptOnlineRequestPayload.class,
				mixDecryptProcessor::verifyPayloadSignature,
				Context.MIXING_TALLY_MIX_DEC_ONLINE,
				mixDecryptProcessor::preValidateRequest,
				mixDecryptProcessor::onRequest,
				mixDecryptProcessor::onReplay,
				ContextIdExtractor::extract,
				mixDecryptProcessor::deserializeRequest,
				MixDecryptOnlineResponsePayload.class,
				mixDecryptProcessor::serializeResponse,
				mixDecryptProcessor.getTransactionTimeout()
		);
	}

}
