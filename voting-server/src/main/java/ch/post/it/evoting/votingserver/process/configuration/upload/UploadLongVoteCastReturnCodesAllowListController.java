/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.post.it.evoting.domain.configuration.setupvoting.SetupComponentLVCCAllowListPayload;

@RestController
@RequestMapping("/api/v1/configuration/setupvoting")
public class UploadLongVoteCastReturnCodesAllowListController {

	private final UploadLongVoteCastReturnCodesAllowListService uploadLongVoteCastReturnCodesAllowListService;

	public UploadLongVoteCastReturnCodesAllowListController(
			final UploadLongVoteCastReturnCodesAllowListService uploadLongVoteCastReturnCodesAllowListService) {
		this.uploadLongVoteCastReturnCodesAllowListService = uploadLongVoteCastReturnCodesAllowListService;
	}

	@PostMapping("/longvotecastreturncodesallowlist/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}")
	public void upload(
			@PathVariable
			final String electionEventId,
			@PathVariable
			final String verificationCardSetId,
			@RequestBody
			final SetupComponentLVCCAllowListPayload setupComponentLVCCAllowListPayload) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkNotNull(setupComponentLVCCAllowListPayload);
		checkArgument(electionEventId.equals(setupComponentLVCCAllowListPayload.getElectionEventId()), "Election event id mismatch.");
		checkArgument(verificationCardSetId.equals(setupComponentLVCCAllowListPayload.getVerificationCardSetId()),
				"Verification card set id mismatch.");

		final String correlationId = uploadLongVoteCastReturnCodesAllowListService.onRequest(electionEventId,
				verificationCardSetId, setupComponentLVCCAllowListPayload);
		uploadLongVoteCastReturnCodesAllowListService.waitForResponse(correlationId);
	}
}
