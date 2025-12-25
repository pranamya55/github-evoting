/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.hasNoDuplicates;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

@JsonDeserialize(using = VoterReturnCodesPayloadDeserializer.class)
public record VoterReturnCodesPayload(GqGroup encryptionGroup,
									  String electionEventId,
									  String verificationCardSetId,
									  ImmutableList<VoterReturnCodes> voterReturnCodes) {

	public VoterReturnCodesPayload(final GqGroup encryptionGroup,
			final String electionEventId,
			final String verificationCardSetId,
			final ImmutableList<VoterReturnCodes> voterReturnCodes) {
		this.encryptionGroup = checkNotNull(encryptionGroup);
		this.electionEventId = validateUUID(electionEventId);
		this.verificationCardSetId = validateUUID(verificationCardSetId);
		this.voterReturnCodes = checkNotNull(voterReturnCodes);

		checkArgument(!this.voterReturnCodes.isEmpty(), "There must be at least one Voter Return Codes.");
		checkArgument(this.voterReturnCodes.stream()
						.allMatch(voterReturnCode -> voterReturnCode.choiceReturnCodesToEncodedVotingOptions().getGroup().equals(encryptionGroup)),
				"The encoded voting options group must be equal to the encryption group.");

		checkArgument(hasNoDuplicates(this.voterReturnCodes().stream().map(VoterReturnCodes::verificationCardId).collect(toImmutableList())),
				"The list of voter return codes must not contain any duplicate verification card ids.");
	}
}
