/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.sendvote;

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

import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.votingserver.process.Constants;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Web service for retrieving the short Choice Return Codes (in collaboration with the control components).
 */
@RestController
@RequestMapping("api/v1/processor/voting/sendvote")
public class SendVoteController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SendVoteController.class);

	private final ChoiceReturnCodesService choiceReturnCodesService;
	private final IdentifierValidationService identifierValidationService;
	private final VerifyAuthenticationChallengeService verifyAuthenticationChallengeService;

	public SendVoteController(
			final ChoiceReturnCodesService choiceReturnCodesService,
			final IdentifierValidationService identifierValidationService,
			final VerifyAuthenticationChallengeService verifyAuthenticationChallengeService) {
		this.choiceReturnCodesService = choiceReturnCodesService;
		this.identifierValidationService = identifierValidationService;
		this.verifyAuthenticationChallengeService = verifyAuthenticationChallengeService;
	}

	/**
	 * Retrieves the short Choice Return Codes for a given election event id and voting card id using a vote.
	 *
	 * @param electionEventId       the election event id.
	 * @param verificationCardSetId the verification card set id.
	 * @param credentialId          the credential id
	 * @param verificationCardId    the verification card id.
	 * @param sendVotePayload       the payload containing the vote, needed ids and authentication data.
	 * @return the short Choice Return Codes.
	 */
	@PostMapping("electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/credentialId/{credentialId}/verificationcard/{verificationCardId}")
	public Mono<SendVoteResponsePayload> retrieveShortChoiceReturnCodes(
			@PathVariable(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathVariable(Constants.PARAMETER_VALUE_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@PathVariable(Constants.PARAMETER_VALUE_CREDENTIAL_ID)
			final String credentialId,
			@PathVariable(Constants.PARAMETER_VALUE_VERIFICATION_CARD_ID)
			final String verificationCardId,
			@RequestBody
			final SendVotePayload sendVotePayload) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		validateUUID(credentialId);
		validateUUID(verificationCardId);
		checkNotNull(sendVotePayload);

		final ContextIds contextIds = sendVotePayload.contextIds();
		LOGGER.debug("Received request to retrieve short Choice Return Codes. [contextIds: {}, credentialId: {}]", contextIds, credentialId);

		// Cross validate ids.
		checkArgument(electionEventId.equals(contextIds.electionEventId()),
				"The request election event id does not match the payload election event id.");
		checkArgument(verificationCardSetId.equals(contextIds.verificationCardSetId()),
				"The request verification card set id does not match the payload verification card set id.");
		checkArgument(verificationCardId.equals(contextIds.verificationCardId()),
				"The request verification card id does not match the payload verification card id.");

		final AuthenticationChallenge authenticationChallenge = sendVotePayload.authenticationChallenge();
		final String payloadCredentialId = authenticationChallenge.derivedVoterIdentifier();
		checkArgument(credentialId.equals(payloadCredentialId), "The request credential id does not match the payload credential id.");

		// Validate contexts ids coherence.
		identifierValidationService.validateContextIdsAndCredentialId(contextIds, credentialId);
		LOGGER.debug("Validated context ids. [contextIds: {}, credentialId: {}", contextIds, credentialId);

		// Verify authentication challenge.
		verifyAuthenticationChallengeService.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.SEND_VOTE, authenticationChallenge);

		// Retrieve short Choice Return Codes.
		return Mono.just(1)
				.subscribeOn(Schedulers.boundedElastic())
				.map(integer ->
						choiceReturnCodesService.retrieveShortChoiceReturnCodes(contextIds, credentialId, sendVotePayload.encryptedVerifiableVote())
								.thenApply(shortChoiceReturnCodes -> {
									LOGGER.info("Short Choice Return Codes retrieved successfully. [contextIds: {}, credentialId: {}]", contextIds,
											credentialId);
									return new SendVoteResponsePayload(shortChoiceReturnCodes);
								}))
				.flatMap(Mono::fromFuture);
	}

}
