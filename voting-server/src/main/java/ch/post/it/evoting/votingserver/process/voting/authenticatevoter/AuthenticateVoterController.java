/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.authenticatevoter;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.votingserver.process.Constants;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/processor/voting/authenticatevoter")
public class AuthenticateVoterController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticateVoterController.class);

	private final AuthenticateVoterService authenticateVoterService;
	private final IdentifierValidationService identifierValidationService;
	private final VerifyAuthenticationChallengeService verifyAuthenticationChallengeService;

	public AuthenticateVoterController(
			final AuthenticateVoterService authenticateVoterService,
			final IdentifierValidationService identifierValidationService,
			final VerifyAuthenticationChallengeService verifyAuthenticationChallengeService) {
		this.authenticateVoterService = authenticateVoterService;
		this.identifierValidationService = identifierValidationService;
		this.verifyAuthenticationChallengeService = verifyAuthenticationChallengeService;
	}

	@PostMapping("electionevent/{electionEventId}/credentialId/{credentialId}/authenticate")
	public Mono<AuthenticateVoterResponsePayload> authenticate(
			@PathVariable(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathVariable(Constants.PARAMETER_VALUE_CREDENTIAL_ID)
			final String credentialId,
			@RequestBody
			final AuthenticateVoterPayload authenticateVoterPayload) {

		validateUUID(electionEventId);
		validateUUID(credentialId);
		checkNotNull(authenticateVoterPayload);

		LOGGER.debug("Received request to authenticate voter. [electionEventId: {}, credentialId: {}]", electionEventId, credentialId);

		checkArgument(electionEventId.equals(authenticateVoterPayload.electionEventId()));

		final AuthenticationChallenge authenticationChallenge = authenticateVoterPayload.authenticationChallenge();
		final String payloadCredentialId = authenticationChallenge.derivedVoterIdentifier();
		checkArgument(credentialId.equals(payloadCredentialId), "The request credential id does not match the payload credential id.");

		// Validate contexts ids coherence.
		identifierValidationService.validateCredentialId(electionEventId, credentialId);
		LOGGER.debug("Validated credential id. [electionEventId: {}, credentialId: {}]", electionEventId, credentialId);

		// Verify authentication challenge.
		verifyAuthenticationChallengeService.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.AUTHENTICATE_VOTER,
				authenticationChallenge);

		return Mono.fromSupplier(() -> {
			final AuthenticateVoterResponsePayload authenticateVoterResponsePayload = authenticateVoterService.retrieveAuthenticateVoterPayload(
					electionEventId, credentialId);

			LOGGER.info("Voter authenticated successfully. [electionEventId: {}, credentialId: {}, currentState: {}]", electionEventId, credentialId,
					authenticateVoterResponsePayload.verificationCardState());

			return authenticateVoterResponsePayload;
		});
	}

}
