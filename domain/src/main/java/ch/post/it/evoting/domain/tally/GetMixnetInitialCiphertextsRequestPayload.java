/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;

public record GetMixnetInitialCiphertextsRequestPayload(String electionEventId, String ballotBoxId) implements HashableList {

	public GetMixnetInitialCiphertextsRequestPayload {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(electionEventId),
				HashableString.from(ballotBoxId)
		);
	}
}
