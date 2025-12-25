/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.protocol.tally.disputeresolver;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

/**
 * Encapsulates the context of the UpdateConfirmedVotingCards algorithm.
 *
 * @param ccrIndex                          j, the CCR's index. Must be non-null.
 * @param electionEventId                   ee, the election event id. Must be non-null and a valid UUID.
 * @param longVoteCastReturnCodesAllowLists (L<sub>lVCC,0</sub>,...,L<sub>lVCC,N_bb-1</sub>), the long Vote Cast Return Codes allow lists. Must be
 *                                          non-null.
 */
public record UpdateConfirmedVotingCardsContext(int ccrIndex, String electionEventId,
												ImmutableMap<String, ImmutableList<String>> longVoteCastReturnCodesAllowLists) {

	public UpdateConfirmedVotingCardsContext {
		checkArgument(ControlComponentNode.ids().contains(ccrIndex), "The CCR's index must be a valid Control Component Node ID.");
		validateUUID(electionEventId);

		checkArgument(!checkNotNull(longVoteCastReturnCodesAllowLists).isEmpty(),
				"The long Vote Cast Return Codes allow lists must have at least one element.");
		longVoteCastReturnCodesAllowLists.forEach((verificationCardSetId, longVoteCastReturnCodesAllowList) -> {
			validateUUID(verificationCardSetId);
			checkArgument(!longVoteCastReturnCodesAllowList.isEmpty(), "The long Vote Cast Return Codes allow list must have at least one element.");
			longVoteCastReturnCodesAllowList.forEach(lVCC -> {
				checkArgument(lVCC.length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH,
						"The length of the long Vote Cast Return Codes allow list elements must be equal to l_HB64. [l_HB64: %s]",
						BASE64_ENCODED_HASH_OUTPUT_LENGTH);
				validateBase64Encoded(lVCC);
			});
		});
	}
}
