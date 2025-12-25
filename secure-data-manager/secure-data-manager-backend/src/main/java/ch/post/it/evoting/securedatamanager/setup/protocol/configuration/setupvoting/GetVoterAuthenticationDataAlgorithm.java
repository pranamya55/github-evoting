/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.ExtendedAuthenticationFactorValidation.validate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.IntStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;

/**
 * Implements the GetVoterAuthenticationData algorithm.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class GetVoterAuthenticationDataAlgorithm {

	private final DeriveCredentialIdAlgorithm deriveCredentialIdAlgorithm;
	private final DeriveBaseAuthenticationChallengeAlgorithm deriveBaseAuthenticationChallengeAlgorithm;

	public GetVoterAuthenticationDataAlgorithm(final DeriveCredentialIdAlgorithm deriveCredentialIdAlgorithm,
			final DeriveBaseAuthenticationChallengeAlgorithm deriveBaseAuthenticationChallengeAlgorithm) {
		this.deriveCredentialIdAlgorithm = deriveCredentialIdAlgorithm;
		this.deriveBaseAuthenticationChallengeAlgorithm = deriveBaseAuthenticationChallengeAlgorithm;
	}

	/**
	 * Derives the credentialID and hAuth from the start voting keys and extended authentication factors.
	 *
	 * @param context the {@link GetVoterAuthenticationDataContext}. Must be non-null.
	 * @param input   the {@link GetVoterAuthenticationDataInput}. Must be non-null.
	 * @return the derived credentialID and hAuth.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws IllegalArgumentException  if the Start Voting Key's size is not
	 *                                   l<sub>SVK</sub>={@value ch.post.it.evoting.evotinglibraries.domain.common.Constants#SVK_LENGTH}.
	 * @throws FailedValidationException if the election event id is not a valid UUID or the start voting key is not a valid Base32 string.
	 */
	@SuppressWarnings("java:S117")
	public GetVoterAuthenticationDataOutput getVoterAuthenticationData(final GetVoterAuthenticationDataContext context,
			final GetVoterAuthenticationDataInput input) {
		checkNotNull(context);
		checkNotNull(input);

		// Context.
		final String ee = context.electionEventId();
		final int N_E = context.numberOfEligibleVoters();
		final int l_EA = context.extendedAuthenticationFactorLength();

		// Input.
		final ImmutableList<String> SVK = input.startVotingKeys();
		final ImmutableList<String> EA = input.extendedAuthenticationFactors();

		// Cross-checks.
		EA.stream().parallel().forEach(extendedAuthenticationFactor -> validate(extendedAuthenticationFactor, l_EA));
		checkArgument(SVK.size() == N_E, "There must be as many start voting key as number of eligible voters. [N_E: %s]", N_E);

		// Operation.
		final ImmutableList<VoterAuthenticationData> voterAuthenticationData = IntStream.range(0, N_E).parallel()
				.mapToObj(id -> {
					final String SVK_id = SVK.get(id);
					final String credentialID_id = deriveCredentialIdAlgorithm.deriveCredentialId(ee, SVK_id);

					final String EA_id = EA.get(id);
					final String hAuth_id = deriveBaseAuthenticationChallengeAlgorithm.deriveBaseAuthenticationChallenge(ee, l_EA, SVK_id, EA_id);

					return new VoterAuthenticationData(credentialID_id, hAuth_id);
				}).collect(toImmutableList());

		final ImmutableList<String> credentialID = voterAuthenticationData.stream()
				.map(VoterAuthenticationData::credentialID_id)
				.collect(toImmutableList());

		final ImmutableList<String> hAuth = voterAuthenticationData.stream()
				.map(VoterAuthenticationData::hAuth_id)
				.collect(toImmutableList());

		return new GetVoterAuthenticationDataOutput(credentialID, hAuth);
	}

	private record VoterAuthenticationData(String credentialID_id, String hAuth_id) {
	}

}
