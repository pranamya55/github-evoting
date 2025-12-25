/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BCK_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.StartVotingKeyValidation.validate;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableString;

public record VoterInitialCodes(String voterIdentification,
								String votingCardId,
								String verificationCardId,
								String startVotingKey,
								String extendedAuthenticationFactor,
								String ballotCastingKey) implements HashableList {

	private static final String DIGIT_OF_LENGTH = "^\\d{%s}$";
	private static final int EA_LENGTH_BIRTH_YEAR = 4;
	private static final int EA_LENGTH_BIRTH_DATE = 8;

	public VoterInitialCodes {
		checkNotNull(voterIdentification);
		validateUUID(votingCardId);
		validateUUID(verificationCardId);
		checkNotNull(extendedAuthenticationFactor);
		checkNotNull(ballotCastingKey);

		checkArgument(!voterIdentification.isEmpty() && !voterIdentification.isBlank());
		checkArgument(extendedAuthenticationFactor.matches(String.format(DIGIT_OF_LENGTH, EA_LENGTH_BIRTH_YEAR)) ||
						extendedAuthenticationFactor.matches(String.format(DIGIT_OF_LENGTH, EA_LENGTH_BIRTH_DATE)),
				"The extended authentication factor does not have the correct format.");

		validate(startVotingKey);

		final int l_BCK = BCK_LENGTH;
		checkArgument(ballotCastingKey.matches(String.format(DIGIT_OF_LENGTH, l_BCK)),
				"The ballot casting key should be a string of l_BCK decimal numbers. [l_BCK: %s]", l_BCK);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.of(
				HashableString.from(voterIdentification()),
				HashableString.from(votingCardId()),
				HashableString.from(verificationCardId()),
				HashableString.from(startVotingKey()),
				HashableString.from(extendedAuthenticationFactor()),
				HashableString.from(ballotCastingKey())
		);
	}
}

