/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.VerificationCardService;

@Service
public class ConfirmVoteService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmVoteService.class);

	private final VerificationCardService verificationCardService;
	private final VoteCastReturnCodeService voteCastReturnCodeService;

	public ConfirmVoteService(
			final VerificationCardService verificationCardService,
			final VoteCastReturnCodeService voteCastReturnCodeService) {
		this.verificationCardService = verificationCardService;
		this.voteCastReturnCodeService = voteCastReturnCodeService;
	}

	/**
	 * Asks the control components for the short Vote Cast Code.
	 *
	 * @param contextIds      the context ids. Must be non-null.
	 * @param confirmationKey the confirmation key. Must be non-null.
	 * @return the short Vote Cast Return Code computed by the control components.
	 * @throws NullPointerException if any parameter is null.
	 */
	public CompletableFuture<String> retrieveShortVoteCastCode(final ContextIds contextIds, final GqElement confirmationKey) {
		checkNotNull(contextIds);
		checkNotNull(confirmationKey);

		return voteCastReturnCodeService.retrieveShortVoteCastCode(contextIds, confirmationKey)
				.thenApply(shortVoteCastReturnCode -> shortVoteCastReturnCode);
	}

	/**
	 * Gets the short Cote Cast Return Code directly from the database, as it was previously computed.
	 *
	 * @param contextIds   the context ids. Must be non-null.
	 * @param credentialId the credential id. Must be a valid UUID.
	 * @return the short Vote Cast Return Code previously computed.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException of {@code credentialId} is no a valid UUID.
	 */
	public CompletableFuture<String> getShortVoteCastReturnCode(final ContextIds contextIds, final String credentialId) {
		checkNotNull(contextIds);
		validateUUID(credentialId);

		LOGGER.warn(
				"The vote is already confirmed. Returning previously calculated short Vote Cast Return Code. [contextIds: {}, credentialId: {}]",
				contextIds, credentialId);
		final String shortVoteCastReturnCode = verificationCardService.getShortVoteCastReturnCode(credentialId);

		return CompletableFuture.completedFuture(shortVoteCastReturnCode);
	}

}
