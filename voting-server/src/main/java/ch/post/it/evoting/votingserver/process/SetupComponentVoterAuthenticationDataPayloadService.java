/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationDataPayload;

@Service
public class SetupComponentVoterAuthenticationDataPayloadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetupComponentVoterAuthenticationDataPayloadService.class);

	private final VerificationCardService verificationCardService;
	private final VerificationCardSetService verificationCardSetService;

	public SetupComponentVoterAuthenticationDataPayloadService(
			final VerificationCardService verificationCardService,
			final VerificationCardSetService verificationCardSetService) {
		this.verificationCardService = verificationCardService;
		this.verificationCardSetService = verificationCardSetService;
	}

	@Transactional // Required otherwise foreign entity is detached during the save operation.
	public void save(final SetupComponentVoterAuthenticationDataPayload setupComponentVoterAuthenticationDataPayload) {
		checkNotNull(setupComponentVoterAuthenticationDataPayload);

		final String electionEventId = setupComponentVoterAuthenticationDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentVoterAuthenticationDataPayload.getVerificationCardSetId();
		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSetEntity(verificationCardSetId);

		final ImmutableList<VerificationCardEntity> verificationCardEntities = setupComponentVoterAuthenticationDataPayload.getSetupComponentVoterAuthenticationData()
				.stream()
				.map(setupComponentVoterAuthenticationData -> {
					final String verificationCardId = setupComponentVoterAuthenticationData.verificationCardId();
					final String votingCardId = setupComponentVoterAuthenticationData.votingCardId();
					final String credentialId = setupComponentVoterAuthenticationData.credentialId();

					final VerificationCardStateEntity verificationCardStateEntity = new VerificationCardStateEntity(verificationCardId);
					return new VerificationCardEntity(verificationCardId, verificationCardSetEntity, credentialId, votingCardId,
							setupComponentVoterAuthenticationData, verificationCardStateEntity);
				})
				.collect(toImmutableList());

		verificationCardService.saveVerificationCards(verificationCardEntities);
		LOGGER.info("Successfully saved all verification cards. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
	}

	public SetupComponentVoterAuthenticationData load(final String electionEventId, final String credentialId) {
		validateUUID(electionEventId);
		validateUUID(credentialId);

		final VerificationCardEntity verificationCardEntity = verificationCardService.getVerificationCardEntityByCredentialId(
				credentialId);

		final ElectionEventEntity electionEventEntity = verificationCardEntity.getVerificationCardSetEntity().getElectionEventEntity();
		checkState(electionEventId.equals(electionEventEntity.getElectionEventId()),
				"The request election event id does not match the election event id associated to the credential id. [electionEventId: %s, credentialId: %s]",
				electionEventId, credentialId);

		LOGGER.info("Retrieved voter authentication data. [electionEventId: {}, credentialId: {}]", electionEventId, credentialId);

		return verificationCardEntity.getVoterAuthenticationData();
	}

}
