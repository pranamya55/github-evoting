/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.math.GroupVector.toGroupVector;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodes;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodesPayload;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.election.PrimesMappingTable;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKeyPayload;

@Service
@ConditionalOnProperty("role.isSetup")
public class GenCredDatService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenCredDatService.class);

	private final GenCredDatAlgorithm genCredDatAlgorithm;

	public GenCredDatService(final GenCredDatAlgorithm genCredDatAlgorithm) {
		this.genCredDatAlgorithm = genCredDatAlgorithm;
	}

	/**
	 * Invokes the GenCredDat algorithm.
	 *
	 * @param setupComponentTallyDataPayload       the setup component tally data payload. Must be non-null.
	 * @param primesMappingTable                   the primes mapping table. Must be non-null.
	 * @param voterInitialCodesPayload             the voter initial codes payload. Must be non-null.
	 * @param electionPublicKey                    the election public key. Must be non-null.
	 * @param choiceReturnCodesEncryptionPublicKey the choice return codes encryption public key. Must be non-null.
	 * @param verificationCardSecretKeyPayload     the verification card secret key payload. Must be non-null.
	 */
	public GenCredDatOutput genCredDat(final SetupComponentTallyDataPayload setupComponentTallyDataPayload,
			final PrimesMappingTable primesMappingTable, final VoterInitialCodesPayload voterInitialCodesPayload,
			final ElGamalMultiRecipientPublicKey electionPublicKey, final ElGamalMultiRecipientPublicKey choiceReturnCodesEncryptionPublicKey,
			final VerificationCardSecretKeyPayload verificationCardSecretKeyPayload) {
		checkNotNull(setupComponentTallyDataPayload);
		checkNotNull(primesMappingTable);
		checkNotNull(voterInitialCodesPayload);
		checkNotNull(electionPublicKey);
		checkNotNull(choiceReturnCodesEncryptionPublicKey);
		checkNotNull(verificationCardSecretKeyPayload);

		final String electionEventId = setupComponentTallyDataPayload.getElectionEventId();
		final String verificationCardSetId = setupComponentTallyDataPayload.getVerificationCardSetId();
		final ImmutableList<String> verificationCardIds = setupComponentTallyDataPayload.getVerificationCardIds();
		final GqGroup encryptionGroup = setupComponentTallyDataPayload.getEncryptionGroup();

		final ImmutableList<String> startVotingKeys = voterInitialCodesPayload.voterInitialCodes()
				.stream()
				.map(VoterInitialCodes::startVotingKey)
				.collect(toImmutableList());

		final GroupVector<ZqElement, ZqGroup> verificationCardSecretKeys = verificationCardSecretKeyPayload.verificationCardSecretKeys()
				.stream()
				.flatMap(verificationCardSecretKey -> verificationCardSecretKey.privateKey().stream())
				.collect(toGroupVector());

		final GenCredDatContext genCredDatContext = new GenCredDatContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardSetId(verificationCardSetId)
				.setVerificationCardIds(verificationCardIds)
				.setPrimesMappingTable(primesMappingTable)
				.setElectionPublicKey(electionPublicKey)
				.setChoiceReturnCodesEncryptionPublicKey(choiceReturnCodesEncryptionPublicKey)
				.build();
		final GenCredDatInput genCredDatInput = new GenCredDatInput(verificationCardSecretKeys, startVotingKeys);

		LOGGER.debug("Performing GenCredDat algorithm... [electionEventId: {}, verificationCardSetId: {}]", electionEventId, verificationCardSetId);

		return genCredDatAlgorithm.genCredDat(genCredDatContext, genCredDatInput);
	}

}
