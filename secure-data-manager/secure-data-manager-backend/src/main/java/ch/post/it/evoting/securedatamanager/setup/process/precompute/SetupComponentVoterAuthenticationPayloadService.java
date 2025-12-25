/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.precompute;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationDataPayload;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVoterAuthenticationPayloadFileRepository;

@Service
@ConditionalOnProperty("role.isSetup")
public class SetupComponentVoterAuthenticationPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentVoterAuthenticationPayloadService.class);

	private final SetupComponentVoterAuthenticationPayloadFileRepository setupComponentVoterAuthenticationPayloadFileRepository;

	public SetupComponentVoterAuthenticationPayloadService(
			final SetupComponentVoterAuthenticationPayloadFileRepository setupComponentVoterAuthenticationPayloadFileRepository) {
		this.setupComponentVoterAuthenticationPayloadFileRepository = setupComponentVoterAuthenticationPayloadFileRepository;
	}

	/**
	 * Saves a setup component voter authentication payload in the corresponding verification card set folder.
	 *
	 * @param setupComponentVoterAuthenticationDataPayload the voter authentication payload to save.
	 * @throws NullPointerException if {@code setupComponentVoterAuthenticationDataPayload} is null.
	 */
	public void save(final SetupComponentVoterAuthenticationDataPayload setupComponentVoterAuthenticationDataPayload) {
		checkNotNull(setupComponentVoterAuthenticationDataPayload);

		final String electionEventId = setupComponentVoterAuthenticationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVoterAuthenticationDataPayload.getVerificationCardSetId();

		setupComponentVoterAuthenticationPayloadFileRepository.save(setupComponentVoterAuthenticationDataPayload);
		LOGGER.info("Saved setup component voter authentication payload. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
	}
}
