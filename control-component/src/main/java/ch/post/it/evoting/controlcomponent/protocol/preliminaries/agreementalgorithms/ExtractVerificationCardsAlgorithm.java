/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.preliminaries.agreementalgorithms;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import java.util.Comparator;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponent.process.EncryptedVerifiableVoteService;
import ch.post.it.evoting.controlcomponent.process.HashedLVCCSharesService;
import ch.post.it.evoting.controlcomponent.process.VerificationCardStateService;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.evotinglibraries.domain.extractedelectionevent.ExtractedVerificationCard;

/**
 * Implements the ExtractVerificationCards algorithm used in case of dispute before the mixing phase.
 */
@Service
public class ExtractVerificationCardsAlgorithm {

	private final VerificationCardStateService verificationCardStateService;
	private final HashedLVCCSharesService hashedLVCCSharesService;
	private final EncryptedVerifiableVoteService encryptedVerifiableVoteService;

	public ExtractVerificationCardsAlgorithm(
			final VerificationCardStateService verificationCardStateService,
			final HashedLVCCSharesService hashedLVCCSharesService,
			final EncryptedVerifiableVoteService encryptedVerifiableVoteService) {
		this.verificationCardStateService = verificationCardStateService;
		this.hashedLVCCSharesService = hashedLVCCSharesService;
		this.encryptedVerifiableVoteService = encryptedVerifiableVoteService;
	}

	/**
	 * Extract the verification cards for the given election event id.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the list of {@link ExtractedVerificationCard}.
	 * @throws IllegalArgumentException if {@code electionEventId} is not a valid UUID.
	 * @throws NullPointerException     if {@code electionEventId} is null.
	 */
	@SuppressWarnings("java:S117")
	public ImmutableList<ExtractedVerificationCard> extractVerificationCards(final String electionEventId) {

		// Context.
		final String ee = validateUUID(electionEventId);

		// Operation.
		return encryptedVerifiableVoteService.getSentVotes(ee).stream()
				// for vc_id ∈ L_sentVotes,j
				.map(sentVote -> {
					final String vc_id = sentVote.contextIds().verificationCardId();

					final String vcs = sentVote.contextIds().verificationCardSetId();

					final ElGamalMultiRecipientCiphertext E1 = sentVote.encryptedVote();

					// if vc_id ∈ L_confirmedVotes,j
					if (verificationCardStateService.isConfirmedVote(vc_id)) {

						final ImmutableList<String> hlVCC_id_vector = hashedLVCCSharesService.getHashedLVCCShares(vc_id);

						return new ExtractedVerificationCard(vc_id, vcs, E1, hlVCC_id_vector);
					} else {
						return new ExtractedVerificationCard(vc_id, vcs, E1, ImmutableList.emptyList());
					}
				})
				.sorted(Comparator.comparing(ExtractedVerificationCard::verificationCardId)) // evc <- Order(evc)
				.collect(toImmutableList());
	}
}
