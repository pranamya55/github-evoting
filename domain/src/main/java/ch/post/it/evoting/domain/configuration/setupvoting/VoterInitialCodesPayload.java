/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;

public record VoterInitialCodesPayload(String electionEventId,
									   String verificationCardSetId,
									   ImmutableList<VoterInitialCodes> voterInitialCodes) {

	public VoterInitialCodesPayload {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(voterInitialCodes);

		checkArgument(!voterInitialCodes.isEmpty(), "The voter initial codes list must not be empty.");
		checkArgument(hasNoDuplicates(voterInitialCodes.stream().map(VoterInitialCodes::voterIdentification).collect(toImmutableList())),
				"The list of voter initial codes must not contain any duplicate voter identifications.");
		checkArgument(hasNoDuplicates(voterInitialCodes.stream().map(VoterInitialCodes::votingCardId).collect(toImmutableList())),
				"The list of voter initial codes must not contain any duplicate voting card ids.");
		checkArgument(hasNoDuplicates(voterInitialCodes.stream().map(VoterInitialCodes::verificationCardId).collect(toImmutableList())),
				"The list of voter initial codes must not contain any duplicate verification card ids.");
	}

}
