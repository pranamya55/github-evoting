/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.disputeresolver;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.LVCCAllowListEntryService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetEntity;
import ch.post.it.evoting.controlcomponent.process.VerificationCardSetService;
import ch.post.it.evoting.controlcomponent.protocol.tally.disputeresolver.UpdateConfirmedVotingCardsService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

@Service
@Profile("dispute-resolution")
public class DisputeResolverUpdateService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DisputeResolverUpdateService.class);

	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final DisputeResolverValidationService disputeResolverValidationService;
	private final VerificationCardSetService verificationCardSetService;
	private final LVCCAllowListEntryService lvccAllowListEntryService;
	private final UpdateConfirmedVotingCardsService updateConfirmedVotingCardsService;

	public DisputeResolverUpdateService(
			final SignatureKeystore<Alias> signatureKeystoreService,
			final DisputeResolverValidationService disputeResolverValidationService,
			final VerificationCardSetService verificationCardSetService,
			final LVCCAllowListEntryService lvccAllowListEntryService,
			final UpdateConfirmedVotingCardsService updateConfirmedVotingCardsService) {
		this.signatureKeystoreService = signatureKeystoreService;
		this.disputeResolverValidationService = disputeResolverValidationService;
		this.verificationCardSetService = verificationCardSetService;
		this.lvccAllowListEntryService = lvccAllowListEntryService;
		this.updateConfirmedVotingCardsService = updateConfirmedVotingCardsService;
	}

	/**
	 * Updates the list of confirmed votes. The update can only be called once the election event has ended and is reserved exceptionally for the
	 * dispute resolution process.
	 * <p>
	 * The dispute resolution process comes into action when control components fail to agree on the list of confirmed votes, a prerequisite for
	 * initiating the tally process.
	 * <p>
	 * The update only modifies the state of the given verification cards from {@link VerificationCardState#SENT} to
	 * {@link VerificationCardState#CONFIRMED}.
	 *
	 * @param electionEventId                              the election event id. Must be non-null or a valid UUID.
	 * @param disputeResolverResolvedConfirmedVotesPayload the resolved confirmed votes payload from the dispute resolver. Must be non-null.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if {@code electionEventId} is not a valid UUID.
	 * @throws IllegalStateException     if
	 *                                   <ul>
	 *                                       <li>the update is not allowed.</li>
	 *                                       <li>the list of confirmed votes' update failed.</li>
	 *                                   </ul>
	 * @throws IllegalArgumentException  if the dispute resolver resolved confirmed votes payload does not correspond to the given election event id.
	 */
	public void update(final String electionEventId,
			final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload) {
		validateUUID(electionEventId);
		checkNotNull(disputeResolverResolvedConfirmedVotesPayload);
		checkArgument(disputeResolverResolvedConfirmedVotesPayload.getElectionEventId().equals(electionEventId),
				"The dispute resolver resolved confirmed votes payload does not correspond to the given election event id.");
		verifyPayloadSignature(disputeResolverResolvedConfirmedVotesPayload);

		disputeResolverValidationService.validate(electionEventId);

		final ImmutableMap<String, ImmutableList<String>> longVoteCastReturnCodesAllowLists = verificationCardSetService.findAllByElectionEventId(
						electionEventId)
				.stream()
				.map(VerificationCardSetEntity::getVerificationCardSetId)
				.collect(toImmutableMap(Function.identity(), lvccAllowListEntryService::getLongVoteCastReturnCodes));

		checkState(updateConfirmedVotingCardsService.updateConfirmedVotingCards(electionEventId, longVoteCastReturnCodesAllowLists,
						disputeResolverResolvedConfirmedVotesPayload),
				"The list of confirmed votes' update failed. [electionEventId: %s]", electionEventId);

		LOGGER.info("Updated list of confirmed votes. [electionEventId: {}]", electionEventId);
	}

	private void verifyPayloadSignature(final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload) {
		final CryptoPrimitivesSignature signature = disputeResolverResolvedConfirmedVotesPayload.getSignature();
		final String electionEventId = disputeResolverResolvedConfirmedVotesPayload.getElectionEventId();

		final Hashable additionalContextData = ChannelSecurityContextData.disputeResolverResolvedConfirmedVotes(electionEventId);
		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.DISPUTE_RESOLVER, disputeResolverResolvedConfirmedVotesPayload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format("Unable to verify signature. [electionEventId: %s]", electionEventId),
					e);
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(DisputeResolverResolvedConfirmedVotesPayload.class,
					String.format("[electionEventId: %s]", electionEventId));
		}
	}
}
