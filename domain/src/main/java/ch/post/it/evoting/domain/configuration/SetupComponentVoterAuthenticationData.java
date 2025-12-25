/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;

public record SetupComponentVoterAuthenticationData(String electionEventId, String verificationCardSetId, String ballotBoxId,
													String verificationCardId, String votingCardId, String credentialId,
													String baseAuthenticationChallenge)
		implements HashableList {

	public SetupComponentVoterAuthenticationData {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		validateUUID(ballotBoxId);
		validateUUID(verificationCardId);
		validateUUID(votingCardId);
		validateUUID(credentialId);
		checkArgument(validateBase64Encoded(baseAuthenticationChallenge).length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
				"The base authentication challenge must be a valid Base64 string of size l_HB64.");
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(HashableString.from(electionEventId),
				HashableString.from(verificationCardSetId),
				HashableString.from(ballotBoxId),
				HashableString.from(verificationCardId),
				HashableString.from(votingCardId),
				HashableString.from(credentialId),
				HashableString.from(baseAuthenticationChallenge));
	}

}
