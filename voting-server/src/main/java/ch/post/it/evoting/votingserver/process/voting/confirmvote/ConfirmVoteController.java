/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.voting.confirmvote;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.evotinglibraries.domain.common.ContextIds;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceContext;
import ch.post.it.evoting.votingserver.idempotence.IdempotenceService;
import ch.post.it.evoting.votingserver.process.Constants;
import ch.post.it.evoting.votingserver.process.IdentifierValidationService;
import ch.post.it.evoting.votingserver.process.VerificationCardService;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationChallenge;
import ch.post.it.evoting.votingserver.process.voting.AuthenticationStep;
import ch.post.it.evoting.votingserver.protocol.voting.authenticatevoter.VerifyAuthenticationChallengeService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Web service for retrieving the short Vote Cast Return Code (in collaboration with the control components).
 */
@RestController
@RequestMapping("api/v1/processor/voting/confirmvote")
public class ConfirmVoteController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmVoteController.class);

	private final ConfirmVoteService confirmVoteService;
	private final VerificationCardService verificationCardService;
	private final IdentifierValidationService identifierValidationService;
	private final IdempotenceService<IdempotenceContext> idempotenceService;
	private final VerifyAuthenticationChallengeService verifyAuthenticationChallengeService;

	public ConfirmVoteController(
			final ConfirmVoteService confirmVoteService,
			final VerificationCardService verificationCardService,
			final IdentifierValidationService identifierValidationService,
			final IdempotenceService<IdempotenceContext> idempotenceService,
			final VerifyAuthenticationChallengeService verifyAuthenticationChallengeService) {
		this.confirmVoteService = confirmVoteService;
		this.verificationCardService = verificationCardService;
		this.identifierValidationService = identifierValidationService;
		this.idempotenceService = idempotenceService;
		this.verifyAuthenticationChallengeService = verifyAuthenticationChallengeService;
	}

	/**
	 * Retrieves the short Vote Cast Return Code for a given election event id and voting card id using a Confirmation Key.
	 *
	 * @param electionEventId       the election event identifier.
	 * @param verificationCardSetId the verification card set identifier.
	 * @param credentialId          the credential id
	 * @param verificationCardId    the verification card identifier
	 * @param confirmVotePayload    the confirmation key payload.
	 * @return the short Vote Cast Return Code.
	 */
	@PostMapping("electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/credentialId/{credentialId}/verificationcard/{verificationCardId}")
	public Mono<ConfirmVoteResponsePayload> retrieveShortVoteCastReturnCode(
			@PathVariable(Constants.PARAMETER_VALUE_ELECTION_EVENT_ID)
			final String electionEventId,
			@PathVariable(Constants.PARAMETER_VALUE_VERIFICATION_CARD_SET_ID)
			final String verificationCardSetId,
			@PathVariable(Constants.PARAMETER_VALUE_CREDENTIAL_ID)
			final String credentialId,
			@PathVariable(Constants.PARAMETER_VALUE_VERIFICATION_CARD_ID)
			final String verificationCardId,
			@RequestBody
			final ConfirmVotePayload confirmVotePayload) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		validateUUID(credentialId);
		validateUUID(verificationCardId);
		checkNotNull(confirmVotePayload);

		final ContextIds contextIds = confirmVotePayload.contextIds();
		LOGGER.debug("Received request to retrieve short Vote Cast Return Code. [contextIds: {}, credentialId: {}]", contextIds, credentialId);

		// Cross validate ids.
		checkArgument(electionEventId.equals(contextIds.electionEventId()),
				"The request election event id does not match the payload election event id.");
		checkArgument(verificationCardSetId.equals(contextIds.verificationCardSetId()),
				"The request verification card set id does not match the payload verification card set id.");
		checkArgument(verificationCardId.equals(contextIds.verificationCardId()),
				"The request verification card id does not match the payload verification card id.");

		final AuthenticationChallenge authenticationChallenge = confirmVotePayload.authenticationChallenge();
		final String payloadCredentialId = authenticationChallenge.derivedVoterIdentifier();
		checkArgument(credentialId.equals(payloadCredentialId), "The request credential id does not match the payload credential id.");

		// Validate context ids coherence.
		identifierValidationService.validateContextIdsAndCredentialId(contextIds, credentialId);
		LOGGER.debug("Validated context ids. [contextIds: {}, credentialId: {}]", contextIds, credentialId);

		// Prepare idempotency context.
		final int attemptId = verificationCardService.getNextConfirmationAttemptId(verificationCardId);
		final String executionKey = String.format("%s-%s-%s-%s-%s", contextIds.electionEventId(), contextIds.verificationCardSetId(),
				contextIds.verificationCardId(), credentialId, attemptId);
		final Supplier<CompletableFuture<String>> execution = () -> {
			// Verify authentication challenge.
			verifyAuthenticationChallengeService.verifyAuthenticationChallenge(electionEventId, AuthenticationStep.CONFIRM_VOTE,
					authenticationChallenge);

			return confirmVoteService.retrieveShortVoteCastCode(contextIds, confirmVotePayload.confirmationKey());
		};
		final Supplier<CompletableFuture<String>> getter = () ->
				// Here we don't need to verify the challenge again because we will bypass the execution.
				confirmVoteService.getShortVoteCastReturnCode(contextIds, credentialId);

		// Retrieve short Vote Cast Return Code.
		return Mono.just(1)
				.subscribeOn(Schedulers.boundedElastic())
				.map(integer ->
						idempotenceService.execute(IdempotenceContext.CONFIRM_VOTE, executionKey, confirmVotePayload, execution, getter)
								.thenApply(shortVoteCastReturnCode -> {
									LOGGER.info("Short Vote Cast Return Code retrieved successfully. [contextIds: {}, credentialId: {}]", contextIds,
											credentialId);
									return new ConfirmVoteResponsePayload(shortVoteCastReturnCode);
								}))
				.flatMap(Mono::fromFuture);
	}

}
