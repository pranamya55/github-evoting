/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.disputeresolver;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.UncheckedIOException;
import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.ElectionEventService;
import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.ExtractElectionEventService;
import ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms.ExtractVerificationCardsService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedElectionEvent;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
@Profile("dispute-resolution")
public class DisputeResolverExtractionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DisputeResolverExtractionService.class);

	private final int nodeId;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final ElectionEventService electionEventService;
	private final ExtractElectionEventService extractElectionEventService;
	private final ExtractVerificationCardsService extractVerificationCardsService;
	private final DisputeResolverValidationService disputeResolverValidationService;

	public DisputeResolverExtractionService(
			@Value("${nodeID}")
			final int nodeId,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final ElectionEventService electionEventService,
			final ExtractElectionEventService extractElectionEventService,
			final ExtractVerificationCardsService extractVerificationCardsService,
			final DisputeResolverValidationService disputeResolverValidationService) {
		this.nodeId = nodeId;
		this.signatureKeystoreService = signatureKeystoreService;
		this.electionEventService = electionEventService;
		this.extractElectionEventService = extractElectionEventService;
		this.extractVerificationCardsService = extractVerificationCardsService;
		this.disputeResolverValidationService = disputeResolverValidationService;
	}

	/**
	 * Extracts the election event. The extraction can only be called once the election event has ended and is reserved exceptionally for the dispute
	 * resolution process.
	 * <p>
	 * The dispute resolution process comes into action when control components fail to agree on the list of confirmed votes, a prerequisite for
	 * initiating the tally process.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalStateException     if the extraction is not allowed.
	 */
	public ControlComponentExtractedElectionEventPayload extractElectionEvent(final String electionEventId) {
		validateUUID(electionEventId);

		disputeResolverValidationService.validate(electionEventId);

		final ExtractedElectionEvent extractedElectionEvent = extractElectionEventService.extractElectionEvent(electionEventId);
		LOGGER.info("Extracted election event. [electionEventId: {}]", electionEventId);

		final ControlComponentExtractedElectionEventPayload controlComponentExtractedElectionEventPayload = new ControlComponentExtractedElectionEventPayload(
				nodeId, extractedElectionEvent);

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentExtractedElectionEvent(nodeId, electionEventId);
		final CryptoPrimitivesSignature cryptoPrimitivesSignature = generateSignature(additionalContextData,
				controlComponentExtractedElectionEventPayload);
		controlComponentExtractedElectionEventPayload.setSignature(cryptoPrimitivesSignature);
		LOGGER.debug("Generated Extracted Election Event payload signature. [electionEventId: {}]", electionEventId);

		return controlComponentExtractedElectionEventPayload;
	}

	/**
	 * Extracts the verification cards. The extraction can only be called once the election event has ended and is reserved exceptionally for the
	 * dispute resolution process.
	 * <p>
	 * The dispute resolution process comes into action when control components fail to agree on the list of confirmed votes, a prerequisite for
	 * initiating the tally process.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalStateException     if the extraction is not allowed.
	 */
	public ControlComponentExtractedVerificationCardsPayload extractVerificationCards(final String electionEventId) {
		validateUUID(electionEventId);

		disputeResolverValidationService.validate(electionEventId);

		final ImmutableList<ExtractedVerificationCard> extractedVerificationCards = extractVerificationCardsService.extractVerificationCards(
				electionEventId);
		LOGGER.info("Extracted verification cards. [electionEventId: {}]", electionEventId);

		final GqGroup encryptionGroup = electionEventService.getEncryptionGroup(electionEventId);
		final ControlComponentExtractedVerificationCardsPayload controlComponentExtractedVerificationCardsPayload = new ControlComponentExtractedVerificationCardsPayload(
				encryptionGroup, electionEventId, nodeId, extractedVerificationCards);

		final Hashable additionalContextData = ChannelSecurityContextData.controlComponentExtractedVerificationCards(nodeId,
				electionEventId);
		final CryptoPrimitivesSignature cryptoPrimitivesSignature = generateSignature(additionalContextData,
				controlComponentExtractedVerificationCardsPayload);
		controlComponentExtractedVerificationCardsPayload.setSignature(cryptoPrimitivesSignature);
		LOGGER.debug("Generated Extracted Verification Cards payload signature. [electionEventId: {}]", electionEventId);

		return controlComponentExtractedVerificationCardsPayload;
	}

	/**
	 * Signs the payload with the additional context data.
	 *
	 * @param additionalContextData the additional context data. Must be non-null.
	 * @param payload               the payload to sign. Must be non-null.
	 * @param <T>                   the payload type. Must extend {@link SignedPayload}.
	 * @return the signature of the payload.
	 * @throws NullPointerException if any parameter is null.
	 * @throws UncheckedIOException if the generation of the payload signature fails.
	 */
	private <T extends SignedPayload> CryptoPrimitivesSignature generateSignature(final Hashable additionalContextData, final T payload) {
		checkNotNull(additionalContextData);
		checkNotNull(payload);

		try {
			final ImmutableByteArray signature = signatureKeystoreService.generateSignature(payload, additionalContextData);
			return new CryptoPrimitivesSignature(signature);
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Failed to generate payload signature. [payload: %s, additionalContextData: %s]",
							payload.getClass().getSimpleName(), additionalContextData), e);
		}
	}

}
