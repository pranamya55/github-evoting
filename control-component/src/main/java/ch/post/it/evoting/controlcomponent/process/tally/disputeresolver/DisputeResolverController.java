/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponent.process.tally.disputeresolver;

import static ch.post.it.evoting.evotinglibraries.domain.validations.TenantIdValidation.validateTenantId;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedElectionEventPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.ControlComponentExtractedVerificationCardsPayload;
import ch.post.it.evoting.domain.tally.disputeresolver.DisputeResolverResolvedConfirmedVotesPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;

/**
 * Endpoint used only for the dispute resolution process under exceptional circumstances. Not accessible under normal operation.
 */
@RestController
@RequestMapping("api/v1/disputeresolver")
@Profile("dispute-resolution")
public class DisputeResolverController {

	private final DisputeResolverExtractionService disputeResolverExtractionService;
	private final DisputeResolverUpdateService disputeResolverUpdateService;
	private final ContextHolder contextHolder;

	public DisputeResolverController(
			final DisputeResolverExtractionService disputeResolverExtractionService,
			final DisputeResolverUpdateService disputeResolverUpdateService,
			final ContextHolder contextHolder) {
		this.disputeResolverExtractionService = disputeResolverExtractionService;
		this.disputeResolverUpdateService = disputeResolverUpdateService;
		this.contextHolder = contextHolder;
	}

	@GetMapping("tenants/{tenantId}/electionevents/{electionEventId}")
	public ControlComponentExtractedElectionEventPayload extractElectionEvent(
			@PathVariable
			final String tenantId,
			@PathVariable
			final String electionEventId) {
		validateTenantId(tenantId);
		validateUUID(electionEventId);

		try {
			contextHolder.setTenantId(tenantId);
			return disputeResolverExtractionService.extractElectionEvent(electionEventId);
		} finally {
			contextHolder.clear();
		}
	}

	@GetMapping("tenants/{tenantId}/electionevents/{electionEventId}/verificationcards")
	public ControlComponentExtractedVerificationCardsPayload extractVerificationCards(
			@PathVariable
			final String tenantId,
			@PathVariable
			final String electionEventId) {
		validateTenantId(tenantId);
		validateUUID(electionEventId);

		try {
			contextHolder.setTenantId(tenantId);
			return disputeResolverExtractionService.extractVerificationCards(electionEventId);
		} finally {
			contextHolder.clear();
		}
	}

	@PutMapping("tenants/{tenantId}/electionevents/{electionEventId}/verificationcards")
	public void update(
			@PathVariable
			final String tenantId,
			@PathVariable
			final String electionEventId,
			@RequestBody
			final DisputeResolverResolvedConfirmedVotesPayload disputeResolverResolvedConfirmedVotesPayload) {
		validateTenantId(tenantId);
		validateUUID(electionEventId);
		checkNotNull(disputeResolverResolvedConfirmedVotesPayload);
		checkArgument(electionEventId.equals(disputeResolverResolvedConfirmedVotesPayload.getElectionEventId()));

		try {
			contextHolder.setTenantId(tenantId);
			disputeResolverUpdateService.update(electionEventId, disputeResolverResolvedConfirmedVotesPayload);
		} finally {
			contextHolder.clear();
		}
	}

}
