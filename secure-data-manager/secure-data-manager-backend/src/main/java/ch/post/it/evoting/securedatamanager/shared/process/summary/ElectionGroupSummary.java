/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

/**
 * Summary of the election group.
 *
 * @param electionGroupId          the election group identifier.
 * @param electionGroupDescription the election group description.
 * @param authorizations           the list of authorizations.
 * @param elections                the list of elections.
 */
public record ElectionGroupSummary(String electionGroupId, ImmutableList<DescriptionSummary> electionGroupDescription, int electionGroupPosition,
								   ImmutableList<AuthorizationSummary> authorizations, ImmutableList<ElectionSummary> elections) {

	public ElectionGroupSummary {
		validateXsToken(electionGroupId);
		checkNotNull(electionGroupDescription);
		checkArgument(electionGroupPosition >= 1, "The election group position must be strictly positive.");
		checkNotNull(authorizations);
		checkNotNull(elections);
	}
}
