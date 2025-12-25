/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BASE64_ENCODED_HASH_OUTPUT_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateBase64Encoded;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

/**
 * Regroups the output values of the GetVoterAuthenticationData algorithm.
 *
 * <ul>
 *     <li>credentialID, the vector of derived voter identifiers. Non-null and contains N<sub>E</sub> valid UUIDs.</li>
 *     <li>hAuth, the vector of base authentication challenges. Non-null and contains N<sub>E</sub> valid Base64 of length l<sub>HB64</sub>.</li>
 * </ul>
 */
public record GetVoterAuthenticationDataOutput(ImmutableList<String> derivedVoterIdentifiers, ImmutableList<String> baseAuthenticationChallenges) {

	public GetVoterAuthenticationDataOutput {
		checkNotNull(derivedVoterIdentifiers);
		checkArgument(!derivedVoterIdentifiers.isEmpty(), "The credentialID must not be empty.");
		derivedVoterIdentifiers.forEach(Validations::validateUUID);

		checkNotNull(baseAuthenticationChallenges);
		checkArgument(!baseAuthenticationChallenges.isEmpty(), "The hAuth must not be empty.");
		checkArgument(baseAuthenticationChallenges.stream()
						.allMatch(challenge -> validateBase64Encoded(challenge).length() == BASE64_ENCODED_HASH_OUTPUT_LENGTH),
				"The base authentication challenge must be a valid Base64 string of size l_HB64.");

		checkArgument(derivedVoterIdentifiers.size() == baseAuthenticationChallenges.size(),
				"There must be as many base authentication challenges as derived voter identifiers.");
	}
}
