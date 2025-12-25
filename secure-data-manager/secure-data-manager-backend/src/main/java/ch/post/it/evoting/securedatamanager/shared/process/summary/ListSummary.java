/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateNonBlankUCS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public record ListSummary(String listIdentification, String listIndentureNumber, ImmutableList<DescriptionSummary> listDescription,
						  int listOrderOfPrecedence,
						  ImmutableList<CandidatePositionSummary> candidatePositionsSummary) {

	public ListSummary {
		validateXsToken(listIdentification);
		validateNonBlankUCS(listIndentureNumber);
		checkNotNull(listDescription);
		checkArgument(listOrderOfPrecedence >= 0, "The list order of precedence must be positive.");
		checkNotNull(candidatePositionsSummary);
	}
}
