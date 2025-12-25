/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.decrypt;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

public record DecryptInput(ImmutableList<String> ballotBoxIds, ImmutableList<char[]> electoralBoardPasswords) {

	public DecryptInput {
		checkNotNull(ballotBoxIds).forEach(Validations::validateUUID);

		electoralBoardPasswords = checkNotNull(electoralBoardPasswords).stream()
				.map(char[]::clone)
				.collect(toImmutableList());

		checkArgument(electoralBoardPasswords.size() >= 2, "There must be at least two passwords.");
	}

	@Override
	public ImmutableList<char[]> electoralBoardPasswords() {
		return this.electoralBoardPasswords.stream()
				.map(char[]::clone)
				.collect(toImmutableList());
	}

}
