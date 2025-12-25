/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Regroups the context values needed by the GetVoterAuthenticationData algorithm.
 *
 * <ul>
 *     <li>ee, the election event id. Non-null and a valid UUID.</li>
 *     <li>N<sub>E</sub>, the number of eligible voters for the verification card set. Strictly positive.</li>
 *     <li>l<sub>EA</sub>, character length of the extended authentication factor. One of the possible extended authentication factor character lengths.</li>
 * </ul>
 */
public record GetVoterAuthenticationDataContext(String electionEventId, int numberOfEligibleVoters, int extendedAuthenticationFactorLength) {

	public GetVoterAuthenticationDataContext {
		validateUUID(electionEventId);
		checkArgument(numberOfEligibleVoters > 0, "The number of eligible voters must be strictly greater than 0.");
	}
}
