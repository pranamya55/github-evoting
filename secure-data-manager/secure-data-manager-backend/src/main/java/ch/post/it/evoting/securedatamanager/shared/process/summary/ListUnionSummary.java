/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process.summary;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateXsToken;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public record ListUnionSummary(String listUnionIdentification, ImmutableList<ListUnionDescriptionInfo> listUnionDescription, int listUnionType,
							   ImmutableList<String> referencedLists) {

	public ListUnionSummary {
		validateXsToken(listUnionIdentification);
		checkNotNull(listUnionDescription);
		checkArgument(listUnionType >= 0, "The list union type must be positive.");
		checkArgument(!referencedLists.isEmpty());
	}

}
