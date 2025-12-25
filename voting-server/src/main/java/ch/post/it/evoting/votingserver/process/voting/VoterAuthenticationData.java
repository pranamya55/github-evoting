/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;

public record VoterAuthenticationData(String electionEventId, String verificationCardSetId, String ballotBoxId, String verificationCardId,
									  String votingCardId, String credentialId) implements HashableList {

	public VoterAuthenticationData {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		validateUUID(ballotBoxId);
		validateUUID(verificationCardId);
		validateUUID(votingCardId);
		validateUUID(credentialId);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(HashableString.from(electionEventId),
				HashableString.from(verificationCardSetId),
				HashableString.from(ballotBoxId),
				HashableString.from(verificationCardId),
				HashableString.from(votingCardId),
				HashableString.from(credentialId));
	}

}
