/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process.output;

import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;

/**
 * The output of the dispute resolution process.
 *
 * @param disputeResolverResolvedConfirmedVotesPayload the payload containing the resolved confirmed votes. Must be non-null.
 */
public record DisputeResolverOutput(DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload) {

	/**
	 * @throws NullPointerException     if the dispute resolver resolved confirmed votes payload is null.
	 */
	public DisputeResolverOutput {
		checkNotNull(disputeResolverResolvedConfirmedVotesPayload);
	}

}
