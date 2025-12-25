/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.voting.confirmvote;

import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;

public record ConfirmationKey(ContextIds contextIds,
							  GqElement element) implements HashableList {

	public ConfirmationKey {
		checkNotNull(contextIds);
		checkNotNull(element);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				contextIds,
				element);
	}
}
