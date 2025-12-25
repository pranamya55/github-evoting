/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.upload;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.process.Status.GENERATED;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

/**
 * Service which will upload the information related to the verification card sets.
 */
@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class UploadVerificationCardSetService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadVerificationCardSetService.class);

	private final VerificationCardSetService verificationCardSetService;
	private final UploadReturnCodesMappingTableService uploadReturnCodesMappingTableService;
	private final UploadVoterAuthenticationDataService uploadVoterAuthenticationDataService;
	private final UploadLongVoteCastReturnCodesAllowListService uploadLongVoteCastReturnCodesAllowListService;

	public UploadVerificationCardSetService(
			final VerificationCardSetService verificationCardSetService,
			final UploadReturnCodesMappingTableService uploadReturnCodesMappingTableService,
			final UploadVoterAuthenticationDataService uploadVoterAuthenticationDataService,
			final UploadLongVoteCastReturnCodesAllowListService uploadLongVoteCastReturnCodesAllowListService
	) {
		this.verificationCardSetService = verificationCardSetService;
		this.uploadReturnCodesMappingTableService = uploadReturnCodesMappingTableService;
		this.uploadVoterAuthenticationDataService = uploadVoterAuthenticationDataService;
		this.uploadLongVoteCastReturnCodesAllowListService = uploadLongVoteCastReturnCodesAllowListService;
	}

	/**
	 * Uploads to the voter portal:
	 * <ul>
	 *     <li>the voter information.</li>
	 *     <li>the return codes mapping tables.</li>
	 *     <li>the long Vote Cast Return Codes allow lists.</li>
	 * </ul>
	 */
	public void upload(final String electionEventId) {
		validateUUID(electionEventId);

		final ImmutableList<String> verificationCardSetIds = verificationCardSetService.getVerificationCardSetIds(GENERATED);

		verificationCardSetIds.forEach(verificationCardSetId -> {
			LOGGER.debug("Uploading voter authentication... [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
					verificationCardSetId);
			uploadVoterAuthenticationDataService.upload(electionEventId, verificationCardSetId);

			LOGGER.debug("Uploading return codes mapping table... [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
					verificationCardSetId);
			uploadReturnCodesMappingTableService.upload(electionEventId, verificationCardSetId);

			LOGGER.debug("Uploading long Vote Cast Return Codes allow list... [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
					verificationCardSetId);
			uploadLongVoteCastReturnCodesAllowListService.upload(electionEventId, verificationCardSetId);

			verificationCardSetService.updateStatus(verificationCardSetId, Status.UPLOADED);

			LOGGER.info("The voting card and verification card sets where successfully uploaded. [electionEventId: {}, verificationCardSetId: {}]",
					electionEventId, verificationCardSetId);
		});
	}

}
