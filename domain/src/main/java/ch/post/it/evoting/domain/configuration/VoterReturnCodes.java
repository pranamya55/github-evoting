/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;

@JsonDeserialize(using = VoterReturnCodesDeserializer.class)
public record VoterReturnCodes(String verificationCardId,
							   String voteCastReturnCode,
							   GroupVector<ChoiceReturnCodeToEncodedVotingOptionEntry, GqGroup> choiceReturnCodesToEncodedVotingOptions
) {

	public VoterReturnCodes {
		validateUUID(verificationCardId);
		checkNotNull(voteCastReturnCode);
		checkNotNull(choiceReturnCodesToEncodedVotingOptions);
	}

}

