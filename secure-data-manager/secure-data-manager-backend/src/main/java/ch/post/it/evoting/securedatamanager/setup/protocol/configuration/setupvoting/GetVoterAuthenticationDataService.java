/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.EXTENDED_AUTHENTICATION_FACTORS;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.collect.MoreCollectors;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.common.ExtendedAuthenticationFactor;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.StartVotingKeyValidation;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;

@Service
@ConditionalOnProperty("role.isSetup")
public class GetVoterAuthenticationDataService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetVoterAuthenticationDataService.class);

	private final GetVoterAuthenticationDataAlgorithm getVoterAuthenticationDataAlgorithm;

	public GetVoterAuthenticationDataService(final GetVoterAuthenticationDataAlgorithm getVoterAuthenticationDataAlgorithm) {
		this.getVoterAuthenticationDataAlgorithm = getVoterAuthenticationDataAlgorithm;
	}

	/**
	 * Invokes the GetVoterAuthenticationData algorithm.
	 *
	 * @param electionEventContextPayload   the election event context payload. Must be non-null.
	 * @param verificationCardSetId         the verification card set id. Must be non-null and a valid UUID.
	 * @param configuration                 the configuration. Must be non-null.
	 * @param startVotingKeys               the start voting keys. Must be non-null.
	 * @param extendedAuthenticationFactors the extended authentication factors. Must be non-null.
	 */
	public GetVoterAuthenticationDataOutput getVoterAuthenticationData(final ElectionEventContextPayload electionEventContextPayload,
			final String verificationCardSetId, final Configuration configuration, final ImmutableList<String> startVotingKeys,
			final ImmutableList<String> extendedAuthenticationFactors) {
		checkNotNull(electionEventContextPayload);
		validateUUID(verificationCardSetId);
		checkNotNull(configuration);
		checkNotNull(startVotingKeys).stream().parallel().forEach(StartVotingKeyValidation::validate);
		checkNotNull(extendedAuthenticationFactors);

		final int numberOfEligibleVoters = electionEventContextPayload.getElectionEventContext().verificationCardSetContexts().stream()
				.filter(verificationCardSetContext -> verificationCardSetContext.getVerificationCardSetId().equals(verificationCardSetId))
				.map(VerificationCardSetContext::getNumberOfEligibleVoters)
				.collect(MoreCollectors.onlyElement());

		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		final int extendedAuthenticationFactor = getExtendedAuthenticationFactorLength(configuration);
		final GetVoterAuthenticationDataContext voterAuthenticationDataContext = new GetVoterAuthenticationDataContext(electionEventId,
				numberOfEligibleVoters, extendedAuthenticationFactor);
		final GetVoterAuthenticationDataInput voterAuthenticationDataInput = new GetVoterAuthenticationDataInput(startVotingKeys,
				extendedAuthenticationFactors);

		LOGGER.debug("Performing GetVoterAuthenticationData algorithm... [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId, verificationCardSetId);

		return getVoterAuthenticationDataAlgorithm.getVoterAuthenticationData(voterAuthenticationDataContext, voterAuthenticationDataInput);
	}

	private int getExtendedAuthenticationFactorLength(final Configuration configuration) {

		final ImmutableList<String> keyNames = ImmutableList.from(configuration.getContest().getExtendedAuthenticationKeys().getKeyName());
		checkState(keyNames.size() == 1, "There must be a single extended authentication key name. [size: %s]", keyNames.size());

		final String extendedAuthenticationFactorName = keyNames.get(0);
		final ExtendedAuthenticationFactor extendedAuthenticationFactor = EXTENDED_AUTHENTICATION_FACTORS.get(extendedAuthenticationFactorName);

		checkState(Objects.nonNull(extendedAuthenticationFactor), "Unsupported extended authentication factor. [name: %s]",
				extendedAuthenticationFactorName);

		return extendedAuthenticationFactor.length();
	}
}
