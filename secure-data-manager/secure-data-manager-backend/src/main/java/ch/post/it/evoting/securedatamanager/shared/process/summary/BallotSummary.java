/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public record BallotSummary(String ballotId, int ballotPosition, ImmutableList<DescriptionSummary> ballotDescription,
							ImmutableList<QuestionSummary> questions) {

	public BallotSummary {
		validateXsToken(ballotId);
		checkArgument(ballotPosition >= 0, "The ballot position must be positive.");
		checkNotNull(ballotDescription);
		checkNotNull(questions);
	}
}
