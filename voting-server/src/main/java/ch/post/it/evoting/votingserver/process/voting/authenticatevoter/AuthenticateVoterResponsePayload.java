/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.authenticatevoter;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

import ch.post.it.evoting.domain.configuration.VerificationCardKeystore;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.votingserver.process.VotingClientPublicKeys;
import ch.post.it.evoting.votingserver.process.voting.VoterAuthenticationData;

/**
 * Response to the voting-client after a successful authentication.
 * <p>
 * During a re-login when the verification card is {@link VerificationCardState#CONFIRMED}, only {@code verificationCardState} and
 * {@code voterMaterial} are present.
 *
 * @param verificationCardState    the current state of the verification card.
 * @param voterMaterial            the material associated to this verification card.
 * @param voterAuthenticationData  the voter authentication data associated to this verification card.
 * @param verificationCardKeystore the verification card keystore.
 * @param votingClientPublicKeys   the voting client public keys corresponding to this verification card.
 * @param primesMappingTable       the primes mapping table associated to this verification card
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthenticateVoterResponsePayload(VerificationCardState verificationCardState,
											   VoterMaterial voterMaterial,
											   VoterAuthenticationData voterAuthenticationData,
											   VerificationCardKeystore verificationCardKeystore,
											   VotingClientPublicKeys votingClientPublicKeys,
											   PrimesMappingTable primesMappingTable) {

	/**
	 * @throws NullPointerException if {@code verificationCardState} or {@code voterMaterial} is null, or if any other is null when the state is
	 *                              {@link VerificationCardState#INITIAL} or {@link VerificationCardState#SENT}.
	 */
	public AuthenticateVoterResponsePayload {
		checkNotNull(verificationCardState);
		checkNotNull(voterMaterial);

		if (VerificationCardState.INITIAL.equals(verificationCardState) || VerificationCardState.SENT.equals(verificationCardState)) {
			checkNotNull(voterAuthenticationData);
			checkNotNull(verificationCardKeystore);
			checkNotNull(votingClientPublicKeys);
			checkNotNull(primesMappingTable);
		}
	}

	/**
	 * @throws NullPointerException if any field is null.
	 */
	public AuthenticateVoterResponsePayload(final VerificationCardState verificationCardState, final VoterMaterial voterMaterial) {
		this(checkNotNull(verificationCardState), checkNotNull(voterMaterial), null, null, null, null);
	}

}
