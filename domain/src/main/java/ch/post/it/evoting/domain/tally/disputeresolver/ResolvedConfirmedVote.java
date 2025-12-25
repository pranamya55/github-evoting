/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.tally.disputeresolver;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

/**
 * @param verificationCardId                 vc<sub>id</sub>, the verification card id. Must be non-null and a valid UUID.
 * @param verificationCardSetId              vcs, the verification card set id. Must be non-null and a valid UUID.
 * @param hashedLongVoteCastReturnCodeShares (hlVCC<sub>id,1</sub>, hlVCC<sub>id,2</sub>, hlVCC<sub>id,3</sub>, hlVCC<sub>id,4</sub>), the CCR's
 *                                           hashed long Vote Cast Return Code shares. Must be non-null.
 */
public record ResolvedConfirmedVote(String verificationCardId,
									String verificationCardSetId,
									ImmutableList<String> hashedLongVoteCastReturnCodeShares) implements HashableList {

	public ResolvedConfirmedVote {
		validateUUID(verificationCardId);
		validateUUID(verificationCardSetId);
		checkNotNull(hashedLongVoteCastReturnCodeShares);

		checkArgument(hashedLongVoteCastReturnCodeShares.size() == ControlComponentNode.ids().size(),
				"There must be exactly %s hashed Long Vote Cast Return Code shares.", ControlComponentNode.ids().size());
		hashedLongVoteCastReturnCodeShares.forEach(hlVCC -> {
			checkArgument(hlVCC.length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
					"The hashed long Vote Cast Return Code shares must be of size l_HB64. [size: %s, l_HB64: %s]", hlVCC.length(),
					BASE64_ENCODED_HASH_OUTPUT_LENGTH);
			validateBase64Encoded(hlVCC);
		});
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(verificationCardId),
				HashableString.from(verificationCardSetId),
				hashedLongVoteCastReturnCodeShares.stream()
						.map(HashableString::from)
						.collect(HashableList.toHashableList())
		);
	}
}
