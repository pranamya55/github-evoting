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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.configuration.SetupComponentCMTablePayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.process.ReturnCodesMappingTableService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Web service for saving and retrieving the return codes mapping table.
 */
@RestController
@RequestMapping("api/v1/processor/configuration/returncodesmappingtable")
public class UploadReturnCodesMappingTableController {

	@VisibleForTesting
	static final String PARAMETER_VALUE_ELECTION_EVENT_ID = "electionEventId";
	@VisibleForTesting
	static final String PARAMETER_VALUE_VERIFICATION_CARD_SET_ID = "verificationCardSetId";

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadReturnCodesMappingTableController.class);

	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ReturnCodesMappingTableService returnCodesMappingTableService;

	private final IdempotenceService<IdempotenceContext> idempotenceService;

	public UploadReturnCodesMappingTableController(
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ReturnCodesMappingTableService returnCodesMappingTableService,
			final IdempotenceService<IdempotenceContext> idempotenceService) {
		this.signatureKeystoreService = signatureKeystoreService;
		this.returnCodesMappingTableService = returnCodesMappingTableService;
		this.idempotenceService = idempotenceService;
	}

	@PostMapping(value = "electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}", consumes = MediaType.APPLICATION_NDJSON_VALUE)
	public Mono<Void> upload(
			@PathVariable(PARAMETER_VALUE_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathVariable(PARAMETER_VALUE_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@RequestBody
			final Flux<SetupComponentCMTablePayload> setupComponentCMTablePayloads) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(setupComponentCMTablePayloads);

		return setupComponentCMTablePayloads
				.publishOn(Schedulers.boundedElastic())
				.doOnNext(setupComponentCMTablePayload -> {
					checkNotNull(setupComponentCMTablePayload);
					checkArgument(electionEventId.equals(setupComponentCMTablePayload.getElectionEventId()));
					checkArgument(verificationCardSetId.equals(setupComponentCMTablePayload.getVerificationCardSetId()));

					verifyPayloadSignature(setupComponentCMTablePayload);

					idempotenceService.execute(IdempotenceContext.SAVE_RETURN_CODES_MAPPING_TABLE,
							String.format("%s-%s-%s", electionEventId, verificationCardSetId, setupComponentCMTablePayload.getChunkId()),
							setupComponentCMTablePayload, () -> returnCodesMappingTableService.save(setupComponentCMTablePayload));
					LOGGER.info("Successfully saved the return codes mapping table. [electionEventId: {}, verificationCardSetId: {}]",
							electionEventId, verificationCardSetId);
				})
				.then();
	}

	private void verifyPayloadSignature(final SetupComponentCMTablePayload setupComponentCMTablePayload) {

		final String electionEventId = setupComponentCMTablePayload.getElectionEventId();
		final String verificationCardSetId = setupComponentCMTablePayload.getVerificationCardSetId();

		final CryptoPrimitivesSignature signature = setupComponentCMTablePayload.getSignature();
		checkState(signature != null,
				"The signature of the setup component CMTable payload is null. [electionEventId: %s, verificationCardSetId: %s]", electionEventId,
				verificationCardSetId);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentCMTable(electionEventId, verificationCardSetId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, setupComponentCMTablePayload, additionalContextData,
					signature.signatureContents());

		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format(
							"Couldn't verify the signature of the setup component CMTable payload. [electionEventId: %s, verificationCardSetId: %s]",
							electionEventId, verificationCardSetId), e);
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(SetupComponentCMTablePayload.class,
					String.format("[electionEventId: %s, verificationCardSetId: %s]", electionEventId, verificationCardSetId));
		}
	}
}
