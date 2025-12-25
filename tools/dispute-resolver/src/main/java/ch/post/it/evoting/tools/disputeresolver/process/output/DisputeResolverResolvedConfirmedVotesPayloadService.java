/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.output;

import static com.google.common.base.Preconditions.checkNotNull;

import java.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;

@Service
public class DisputeResolverResolvedConfirmedVotesPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DisputeResolverResolvedConfirmedVotesPayloadService.class);

	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final DisputeResolverResolvedConfirmedVotesPayloadFileRepository disputeResolverResolvedConfirmedVotesPayloadFileRepository;

	public DisputeResolverResolvedConfirmedVotesPayloadService(
			final SignatureKeystore<Alias> signatureKeystoreService,
			final DisputeResolverResolvedConfirmedVotesPayloadFileRepository disputeResolverResolvedConfirmedVotesPayloadFileRepository) {
		this.signatureKeystoreService = signatureKeystoreService;
		this.disputeResolverResolvedConfirmedVotesPayloadFileRepository = disputeResolverResolvedConfirmedVotesPayloadFileRepository;

	}

	/**
	 * Signs and saves the dispute resolver resolved confirmed votes payload to the file system.
	 *
	 * @param disputeResolverResolvedConfirmedVotesPayload the dispute resolver resolved confirmed votes payload to sign and save. Must not be null.
	 * @throws NullPointerException     if the payload is null.
	 * @throws IllegalStateException    if an error occurs while signing the payload.
	 */
	public void save(final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload) {
		checkNotNull(disputeResolverResolvedConfirmedVotesPayload);

		LOGGER.debug("Signing dispute resolver resolved confirmed votes payload...");

		signPayload(disputeResolverResolvedConfirmedVotesPayload);

		LOGGER.debug("Dispute resolver resolved confirmed votes payload signed successfully.");

		disputeResolverResolvedConfirmedVotesPayloadFileRepository.save(disputeResolverResolvedConfirmedVotesPayload);

		LOGGER.debug("Dispute resolver resolved confirmed votes payload saved successfully.");
	}

	private void signPayload(final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload) {

		final String electionEventId = disputeResolverResolvedConfirmedVotesPayload.getElectionEventId();

		final Hashable additionalContextData = ChannelSecurityContextData.disputeResolverResolvedConfirmedVotes(electionEventId);
		final ImmutableByteArray signature;
		try {
			signature = signatureKeystoreService.generateSignature(disputeResolverResolvedConfirmedVotesPayload, additionalContextData);
		} catch (final SignatureException e) {
			throw new IllegalStateException("Failed to generate dispute resolver resolved confirmed votes payload signature.", e);
		}

		disputeResolverResolvedConfirmedVotesPayload.setSignature(new CryptoPrimitivesSignature(signature));
	}

}
