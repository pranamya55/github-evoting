/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.configuration.SetupComponentVerificationCardKeystoresPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.process.SetupComponentVerificationCardKeystoreService;

/**
 * Web service for saving setup component verification card keystores.
 */
@RestController
@RequestMapping("api/v1/processor/configuration/setupcomponentverificationcardkeystores")
public class UploadSetupComponentVerificationCardKeystoresController {

	@VisibleForTesting
	static final String PARAMETER_VALUE_ELECTION_EVENT_ID = "electionEventId";

	@VisibleForTesting
	static final String PARAMETER_VALUE_VERIFICATION_CARD_SET_ID = "verificationCardSetId";

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadSetupComponentVerificationCardKeystoresController.class);

	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final SetupComponentVerificationCardKeystoreService setupComponentVerificationCardKeystoreService;
	private final IdempotenceService<IdempotenceContext> idempotenceService;

	public UploadSetupComponentVerificationCardKeystoresController(
			final SignatureKeystore<Alias> signatureKeystoreService,
			final SetupComponentVerificationCardKeystoreService setupComponentVerificationCardKeystoreService,
			final IdempotenceService<IdempotenceContext> idempotenceService) {
		this.setupComponentVerificationCardKeystoreService = setupComponentVerificationCardKeystoreService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.idempotenceService = idempotenceService;
	}

	@PostMapping("electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}")
	public void upload(
			@PathVariable(PARAMETER_VALUE_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathVariable(PARAMETER_VALUE_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@RequestBody
			final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(setupComponentVerificationCardKeystoresPayload);

		checkArgument(electionEventId.equals(setupComponentVerificationCardKeystoresPayload.getElectionEventId()),
				"The election event id does not correspond to the id in the uploaded payload.");
		checkArgument(verificationCardSetId.equals(setupComponentVerificationCardKeystoresPayload.getVerificationCardSetId()),
				"The verification card set id does not correspond to the id in the uploaded payload.");

		verifySignature(setupComponentVerificationCardKeystoresPayload);

		idempotenceService.execute(IdempotenceContext.SAVE_SETUP_COMPONENT_VERIFICATION_CARD_KEYSTORES,
				String.format("%s-%s", electionEventId, verificationCardSetId), setupComponentVerificationCardKeystoresPayload,
				() -> setupComponentVerificationCardKeystoreService.save(setupComponentVerificationCardKeystoresPayload));

		LOGGER.info("Successfully saved the setup component verification card keystores. [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId, verificationCardSetId);
	}

	private void verifySignature(final SetupComponentVerificationCardKeystoresPayload setupComponentVerificationCardKeystoresPayload) {

		final String electionEventId = setupComponentVerificationCardKeystoresPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVerificationCardKeystoresPayload.getVerificationCardSetId();

		final CryptoPrimitivesSignature signature = setupComponentVerificationCardKeystoresPayload.getSignature();

		checkState(signature != null,
				"The signature of the setup component verification card keystores payload is null. [electionEventId: %s, verificationCardSetId: %s]",
				electionEventId, verificationCardSetId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentVerificationCardKeystores(electionEventId,
				verificationCardSetId);
		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, setupComponentVerificationCardKeystoresPayload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format(
					"Unable to verify the setup component verification card keystores payload. [electionEventId: %s, verificationCardSetId: %s]",
					electionEventId, verificationCardSetId), e);
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(SetupComponentVerificationCardKeystoresPayload.class,
					String.format("[electionEventId: %s, verificationCardSetId: %s]", electionEventId, verificationCardSetId));
		}

	}
}
