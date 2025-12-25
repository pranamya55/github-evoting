/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.process.Constants;
import ch.post.it.evoting.votingserver.process.SetupComponentVoterAuthenticationDataPayloadService;

@RestController
@RequestMapping("api/v1/processor/configuration/setupvoting/voterauthenticationdata")
public class UploadVoterAuthenticationDataController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadVoterAuthenticationDataController.class);

	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final SetupComponentVoterAuthenticationDataPayloadService setupComponentVoterAuthenticationDataPayloadService;
	private final IdempotenceService<IdempotenceContext> idempotenceService;

	public UploadVoterAuthenticationDataController(
			final SignatureKeystore<Alias> signatureKeystoreService,
			final SetupComponentVoterAuthenticationDataPayloadService setupComponentVoterAuthenticationDataPayloadService,
			final IdempotenceService<IdempotenceContext> idempotenceService) {
		this.signatureKeystoreService = signatureKeystoreService;
		this.setupComponentVoterAuthenticationDataPayloadService = setupComponentVoterAuthenticationDataPayloadService;
		this.idempotenceService = idempotenceService;
	}

	@PostMapping("electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}")
	public void upload(
			@PathVariable(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathVariable(Constants.PARAMETER_VALUE_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@RequestBody
			final SetupComponentVoterAuthenticationDataPayload setupComponentVoterAuthenticationDataPayload) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(electionEventId.equals(setupComponentVoterAuthenticationDataPayload.getElectionEventId()));
		checkArgument(verificationCardSetId.equals(setupComponentVoterAuthenticationDataPayload.getVerificationCardSetId()));

		LOGGER.debug("Verifying voter authentication data payload signature... [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
		verifyPayloadSignature(setupComponentVoterAuthenticationDataPayload);

		LOGGER.debug("Saving all voter authentication data... [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
		idempotenceService.execute(IdempotenceContext.SAVE_VOTER_AUTHENTICATION_DATA, String.format("%s-%s", electionEventId, verificationCardSetId),
				setupComponentVoterAuthenticationDataPayload,
				() -> setupComponentVoterAuthenticationDataPayloadService.save(setupComponentVoterAuthenticationDataPayload));

		LOGGER.info("Saved all voter authentication data. [electionEventId: {}, verificationCardSetId: {}]", electionEventId, verificationCardSetId);
	}

	private void verifyPayloadSignature(final SetupComponentVoterAuthenticationDataPayload setupComponentVoterAuthenticationDataPayload) {
		final String electionEventId = setupComponentVoterAuthenticationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVoterAuthenticationDataPayload.getVerificationCardSetId();

		final CryptoPrimitivesSignature signature = setupComponentVoterAuthenticationDataPayload.getSignature();
		checkState(signature != null,
				"The signature of the setup component voter authentication data payload is null. [electionEventId: %s, verificationCardSetId: %s]",
				electionEventId, verificationCardSetId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentVoterAuthenticationData(electionEventId,
				verificationCardSetId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, setupComponentVoterAuthenticationDataPayload,
					additionalContextData, signature.signatureContents());

		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format(
							"Couldn't verify the signature of the setup component voter authentication data payload. [electionEventId: %s, verificationCardSetId: %s]",
							electionEventId, verificationCardSetId), e);
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(SetupComponentVoterAuthenticationDataPayload.class,
					String.format("[electionEventId: %s, verificationCardSetId: %s]", electionEventId, verificationCardSetId));
		}
	}

}
