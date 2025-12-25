/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateLanguageMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.Language;

/**
 * The vote summary.
 *
 * @param voteId            the vote id.
 * @param votePosition      the vote position.
 * @param voteDescription   the vote description.
 * @param domainOfInfluence the domain of influence.
 * @param authorizations    the authorizations.
 * @param ballots           the ballots.
 */
public record VoteSummary(String voteId, int votePosition, ImmutableMap<String, String> voteDescription, String domainOfInfluence,
						  ImmutableList<AuthorizationSummary> authorizations, ImmutableList<BallotSummary> ballots) {
	public VoteSummary {
		validateXsToken(voteId);
		checkArgument(votePosition >= 0, "The vote position must be positive.");
		validateLanguageMap(checkNotNull(voteDescription).entrySet().stream()
				.collect(ImmutableMap.toImmutableMap(entry -> Language.valueOfInsensitive(entry.key()), ImmutableMap.Entry::value)));
		validateXsToken(domainOfInfluence);
		checkNotNull(authorizations);
		checkNotNull(ballots);
	}
}
