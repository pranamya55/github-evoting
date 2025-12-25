/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;
import ch.post.it.evoting.domain.configuration.VerificationCardKeystore;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardState;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.votingserver.process.BallotBoxService;
import ch.post.it.evoting.votingserver.process.ElectionEventContextService;
import ch.post.it.evoting.votingserver.process.ElectionEventContextService.VotesElectionsTexts;
import ch.post.it.evoting.votingserver.process.SetupComponentPublicKeysService;
import ch.post.it.evoting.votingserver.process.SetupComponentVerificationCardKeystoreService;
import ch.post.it.evoting.votingserver.process.SetupComponentVoterAuthenticationDataPayloadService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.VotingClientPublicKeys;
import ch.post.it.evoting.votingserver.process.voting.VoterAuthenticationData;

@Service
public class AuthenticateVoterService {

	private final VerificationCardService verificationCardService;
	private final ElectionEventContextService electionEventContextService;
	private final SetupComponentVoterAuthenticationDataPayloadService setupComponentVoterAuthenticationDataPayloadService;
	private final SetupComponentPublicKeysService setupComponentPublicKeysService;
	private final SetupComponentVerificationCardKeystoreService setupComponentVerificationCardKeystoreService;
	private final BallotBoxService ballotBoxService;

	public AuthenticateVoterService(
			final VerificationCardService verificationCardService,
			final ElectionEventContextService electionEventContextService,
			final SetupComponentVoterAuthenticationDataPayloadService setupComponentVoterAuthenticationDataPayloadService,
			final SetupComponentPublicKeysService setupComponentPublicKeysService,
			final SetupComponentVerificationCardKeystoreService setupComponentVerificationCardKeystoreService,
			final BallotBoxService ballotBoxService) {
		this.verificationCardService = verificationCardService;
		this.electionEventContextService = electionEventContextService;
		this.setupComponentVoterAuthenticationDataPayloadService = setupComponentVoterAuthenticationDataPayloadService;
		this.setupComponentPublicKeysService = setupComponentPublicKeysService;
		this.setupComponentVerificationCardKeystoreService = setupComponentVerificationCardKeystoreService;
		this.ballotBoxService = ballotBoxService;
	}

	/**
	 * Returns the {@code AuthenticateVoterResponsePayload} containing the necessary data.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @param credentialId    the credential id.    Must be non-null and a valid UUID.
	 * @return the payload containing the necessary data. It's content varies depending on if a re-login is performed or not.
	 * @throws NullPointerException      if any parameter is null.
	 * @throws FailedValidationException if the {@code electionEventId} is invalid.
	 */
	public AuthenticateVoterResponsePayload retrieveAuthenticateVoterPayload(final String electionEventId, final String credentialId) {
		validateUUID(credentialId);
		validateUUID(electionEventId);

		// Construct response VoterAuthenticationData, which does not contain the baseAuthenticationChallenge.
		final SetupComponentVoterAuthenticationData setupComponentVoterAuthenticationData = setupComponentVoterAuthenticationDataPayloadService.load(
				electionEventId, credentialId);

		final String verificationCardSetId = setupComponentVoterAuthenticationData.verificationCardSetId();
		final String verificationCardId = setupComponentVoterAuthenticationData.verificationCardId();
		final String ballotBoxId = setupComponentVoterAuthenticationData.ballotBoxId();
		final String votingCardId = setupComponentVoterAuthenticationData.votingCardId();
		final VoterAuthenticationData voterAuthenticationData = new VoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId,
				verificationCardId, votingCardId, credentialId);

		// Construct authenticate voter response payload depending on the verification card state (re-login).
		final VerificationCardState verificationCardState = verificationCardService.getVerificationCardState(credentialId);

		return switch (verificationCardState) {
			case INITIAL -> {
				final VotesElectionsTexts verificationCardSetTexts = electionEventContextService.getVerificationCardSetTexts(
						electionEventId, verificationCardSetId);
				final VoterMaterial voterMaterial = new VoterMaterial(verificationCardSetTexts.votesTexts(),
						verificationCardSetTexts.electionsTexts());

				final VerificationCardKeystore verificationCardKeystore = setupComponentVerificationCardKeystoreService.loadVerificationCardKeystore(
						electionEventId, verificationCardSetId, verificationCardId);

				final VotingClientPublicKeys votingClientPublicKeys = setupComponentPublicKeysService.getVotingClientPublicKeys(electionEventId);

				final PrimesMappingTable primesMappingTable = ballotBoxService.getPrimesMappingTableByVerificationCardSetId(verificationCardSetId);

				yield new AuthenticateVoterResponsePayload(verificationCardState, voterMaterial, voterAuthenticationData, verificationCardKeystore,
						votingClientPublicKeys, primesMappingTable);
			}
			case SENT -> {
				final VotesElectionsTexts verificationCardSetTexts = electionEventContextService.getVerificationCardSetTexts(
						electionEventId, verificationCardSetId);
				final ImmutableList<String> shortChoiceReturnCodes = verificationCardService.getShortChoiceReturnCodes(credentialId);
				final VoterMaterial voterMaterial = new VoterMaterial(verificationCardSetTexts.votesTexts(),
						verificationCardSetTexts.electionsTexts(), shortChoiceReturnCodes);

				final VerificationCardKeystore verificationCardKeystore = setupComponentVerificationCardKeystoreService.loadVerificationCardKeystore(
						electionEventId, verificationCardSetId, verificationCardId);

				final VotingClientPublicKeys votingClientPublicKeys = setupComponentPublicKeysService.getVotingClientPublicKeys(electionEventId);

				final PrimesMappingTable primesMappingTable = ballotBoxService.getPrimesMappingTableByVerificationCardSetId(verificationCardSetId);

				yield new AuthenticateVoterResponsePayload(verificationCardState, voterMaterial, voterAuthenticationData, verificationCardKeystore,
						votingClientPublicKeys, primesMappingTable);
			}
			case CONFIRMED -> {
				final VoterMaterial voterMaterial = new VoterMaterial(verificationCardService.getShortVoteCastReturnCode(credentialId));

				yield new AuthenticateVoterResponsePayload(verificationCardState, voterMaterial);
			}
			default -> throw new IllegalStateException(
					String.format("Invalid verification card state. [verificationCardId: %s, verificationCardState: %s]", verificationCardId,
							verificationCardState));
		};
	}

}
