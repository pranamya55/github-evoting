/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.CorrectnessInformationValidation;
import ch.post.it.evoting.evotinglibraries.domain.validations.Validations;

@Service
@ConditionalOnProperty("role.isSetup")
public class GenCMTableService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GenCMTableService.class);

	private final GenCMTableAlgorithm genCMTableAlgorithm;

	public GenCMTableService(final GenCMTableAlgorithm genCMTableAlgorithm) {
		this.genCMTableAlgorithm = genCMTableAlgorithm;
	}

	/**
	 * Invokes the GenCMTable algorithm.
	 *
	 * @param electionEventContextPayload    the election event context payload. Must be non-null.
	 * @param correctnessInformation         the list of correctness information. Must be non-null.
	 * @param verificationCardSetId          the verification card set id. Must be non-null and a valid UUID.
	 * @param verificationCardIds            the list of verification card ids. Must be non-null and contain valid UUIDs.
	 * @param chunkId                        the chunk id. Must be positive.
	 * @param setupSecretKey                 the setup secret key. Must be non-null.
	 * @param combineEncLongCodeSharesOutput the output of the algorithm {@link CombineEncLongCodeSharesAlgorithm}. Must be non-null.
	 */
	public GenCMTableOutput genCMTable(final ElectionEventContextPayload electionEventContextPayload,
			final ImmutableList<String> correctnessInformation, final String verificationCardSetId, final ImmutableList<String> verificationCardIds,
			final int chunkId, final ElGamalMultiRecipientPrivateKey setupSecretKey,
			final CombineEncLongCodeSharesOutput combineEncLongCodeSharesOutput) {
		checkNotNull(electionEventContextPayload);
		checkNotNull(correctnessInformation).forEach(CorrectnessInformationValidation::validate);
		validateUUID(verificationCardSetId);
		checkNotNull(verificationCardIds).forEach(Validations::validateUUID);
		checkArgument(chunkId >= 0);
		checkNotNull(setupSecretKey);
		checkNotNull(combineEncLongCodeSharesOutput);

		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();
		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		final int maximumNumberOfVotingOptions = electionEventContextPayload.getElectionEventContext().maximumNumberOfVotingOptions();

		checkArgument(encryptionGroup.hasSameOrderAs(setupSecretKey.getGroup()),
				"The encryption group of the election event context payload and the setup secret key must have the same order.");
		checkArgument(encryptionGroup.equals(combineEncLongCodeSharesOutput.getEncryptedPreChoiceReturnCodesVector().getGroup()),
				"The encryption group of the election event context payload and the CombineEncLongCodeShares algorithm output must be the same.");

		final GenCMTableContext genCMTableContext = new GenCMTableContext.Builder()
				.setEncryptionGroup(encryptionGroup)
				.setElectionEventId(electionEventId)
				.setVerificationCardIds(verificationCardIds)
				.setCorrectnessInformation(correctnessInformation)
				.setMaximumNumberOfVotingOptions(maximumNumberOfVotingOptions)
				.build();
		final GenCMTableInput genCMTableInput = new GenCMTableInput(
				setupSecretKey,
				combineEncLongCodeSharesOutput.getEncryptedPreChoiceReturnCodesVector(),
				combineEncLongCodeSharesOutput.getPreVoteCastReturnCodesVector());

		LOGGER.debug("Performing GenCMTable algorithm... [electionEventId: {}, verificationCardSetId: {}, chunkId: {}]", electionEventId,
				verificationCardSetId, chunkId);

		return genCMTableAlgorithm.genCMTable(genCMTableContext, genCMTableInput);
	}
}
