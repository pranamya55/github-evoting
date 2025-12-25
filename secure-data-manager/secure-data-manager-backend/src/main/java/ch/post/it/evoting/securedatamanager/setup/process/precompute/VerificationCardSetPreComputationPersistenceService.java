/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.precompute;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.BIRTH_DATE;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.EXTENDED_AUTHENTICATION_FACTORS;
import static ch.post.it.evoting.evotinglibraries.domain.common.Constants.ID_LENGTH;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.security.SignatureException;
import java.util.Objects;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationData;
import ch.post.it.evoting.domain.configuration.SetupComponentVoterAuthenticationDataPayload;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodes;
import ch.post.it.evoting.domain.configuration.setupvoting.VoterInitialCodesPayload;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.common.ExtendedAuthenticationFactor;
import ch.post.it.evoting.evotinglibraries.domain.configuration.SetupComponentTallyDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationData;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.SetupComponentVerificationDataPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.evotinglibraries.domain.signature.SignedPayload;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ExtendedAuthenticationKeyType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.ExtendedAuthenticationKeysType;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.VoterType;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKey;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKeyPayload;
import ch.post.it.evoting.securedatamanager.setup.process.VerificationCardSecretKeyPayloadService;
import ch.post.it.evoting.securedatamanager.setup.process.VoterInitialCodesPayloadService;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GenVerDatOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GetVoterAuthenticationDataOutput;
import ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting.GetVoterAuthenticationDataService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventContextPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.ElectionEventService;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentTallyDataPayloadService;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

/**
 * Service that deals with the persistence of payloads generated in the pre-computation of verification card sets.
 */
@Service
@ConditionalOnProperty("role.isSetup")
public class VerificationCardSetPreComputationPersistenceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardSetPreComputationPersistenceService.class);

	private final String votingCardIdSuffix;
	private final ElectionEventService electionEventService;
	private final EvotingConfigService evotingConfigService;
	private final SignatureKeystore<Alias> signatureKeystoreService;
	private final VerificationCardSetService verificationCardSetService;
	private final VoterInitialCodesPayloadService voterInitialCodesPayloadService;
	private final GetVoterAuthenticationDataService getVoterAuthenticationDataService;
	private final ElectionEventContextPayloadService electionEventContextPayloadService;
	private final SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService;
	private final VerificationCardSecretKeyPayloadService verificationCardSecretKeyPayloadService;
	private final SetupComponentVoterAuthenticationPayloadService setupComponentVoterAuthenticationPayloadService;
	private final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;

	public VerificationCardSetPreComputationPersistenceService(
			@Value("${sdm.voting-card-id-suffix}")
			final String votingCardIdSuffix,
			final ElectionEventService electionEventService,
			final EvotingConfigService evotingConfigService,
			final SignatureKeystore<Alias> signatureKeystoreService,
			final VerificationCardSetService verificationCardSetService,
			final VoterInitialCodesPayloadService voterInitialCodesPayloadService,
			final GetVoterAuthenticationDataService getVoterAuthenticationDataService,
			final ElectionEventContextPayloadService electionEventContextPayloadService,
			final SetupComponentTallyDataPayloadService setupComponentTallyDataPayloadService,
			final VerificationCardSecretKeyPayloadService verificationCardSecretKeyPayloadService,
			final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository,
			final SetupComponentVoterAuthenticationPayloadService setupComponentVoterAuthenticationPayloadService) {

		this.votingCardIdSuffix = checkNotNull(votingCardIdSuffix);
		checkArgument(!votingCardIdSuffix.isBlank(), "The voting card id suffix cannot be blank.");
		this.electionEventService = electionEventService;
		this.evotingConfigService = evotingConfigService;
		this.signatureKeystoreService = signatureKeystoreService;
		this.verificationCardSetService = verificationCardSetService;
		this.voterInitialCodesPayloadService = voterInitialCodesPayloadService;
		this.getVoterAuthenticationDataService = getVoterAuthenticationDataService;
		this.electionEventContextPayloadService = electionEventContextPayloadService;
		this.setupComponentTallyDataPayloadService = setupComponentTallyDataPayloadService;
		this.verificationCardSecretKeyPayloadService = verificationCardSecretKeyPayloadService;
		this.setupComponentVoterAuthenticationPayloadService = setupComponentVoterAuthenticationPayloadService;
		this.setupComponentVerificationDataPayloadFileRepository = setupComponentVerificationDataPayloadFileRepository;
	}

	/**
	 * Persists the payloads containing the information from the output of the GenVerDat algorithm.
	 */
	public void persistPreComputationPayloads(final PrecomputeContext precomputeContext, final ImmutableList<GenVerDatOutput> genVerDatOutputs) {

		checkNotNull(precomputeContext);
		checkNotNull(genVerDatOutputs);

		final String electionEventId = precomputeContext.electionEventId();
		final String verificationCardSetId = precomputeContext.verificationCardSetId();

		checkArgument(electionEventService.exists(electionEventId), "The election event id of the given context does not exist.");

		final ElectionEventContextPayload electionEventContextPayload = electionEventContextPayloadService.load(electionEventId);
		final GqGroup encryptionGroup = electionEventContextPayload.getEncryptionGroup();
		// Load the configuration-anonymized
		final Configuration configuration = evotingConfigService.load();

		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSet(verificationCardSetId);
		final String ballotBoxDescription = verificationCardSetEntity.getBallotBoxEntity().getDescription();
		final ImmutableList<VoterType> voters = getVotersFromConfigurationAnonymized(verificationCardSetEntity, configuration);
		final ImmutableList<String> extendedAuthenticationFactors = voters.stream().parallel()
				.map(VoterType::getExtendedAuthenticationKeys)
				.map(ExtendedAuthenticationKeysType::getExtendedAuthenticationKey)
				.map(extendedAuthenticationKeyTypes -> parseExtendedAuthenticationFactor(extendedAuthenticationKeyTypes.getFirst()))
				.collect(toImmutableList());
		final ImmutableList<String> verificationCardIds = genVerDatOutputs.stream()
				.map(GenVerDatOutput::getVerificationCardIds)
				.flatMap(ImmutableList::stream)
				.collect(toImmutableList());
		final ImmutableList<String> startVotingKeys = genVerDatOutputs.stream()
				.map(GenVerDatOutput::getStartVotingKeys)
				.flatMap(ImmutableList::stream)
				.collect(toImmutableList());
		final ImmutableList<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs = genVerDatOutputs.stream()
				.map(GenVerDatOutput::getVerificationCardKeyPairs)
				.flatMap(ImmutableList::stream)
				.collect(toImmutableList());

		checkArgument(voters.size() == startVotingKeys.size(),
				"The number of start voting keys does not correspond to the number of voters. [electionEventId: %s, verificationCardSetId: %s]",
				electionEventId, verificationCardSetId);
		checkArgument(verificationCardIds.size() == startVotingKeys.size(),
				"The number of start voting keys does not correspond to the number of verification card ids. [electionEventId: %s, verificationCardSetId: %s]",
				electionEventId, verificationCardSetId);

		// Build and persist payloads to request the return code generation (choice return codes and vote cast return code) from the control components.
		persistSetupComponentVerificationData(precomputeContext, encryptionGroup, genVerDatOutputs);
		persistVerificationCardSecretKeyPayload(precomputeContext, encryptionGroup, verificationCardIds, verificationCardKeyPairs);
		persistVoterInitialCodesPayload(precomputeContext, genVerDatOutputs, verificationCardIds, startVotingKeys, voters,
				extendedAuthenticationFactors);
		persistSetupComponentTallyDataPayload(precomputeContext, encryptionGroup, verificationCardIds, verificationCardKeyPairs,
				ballotBoxDescription);
		persistVoterAuthenticationData(precomputeContext, electionEventContextPayload, verificationCardIds, startVotingKeys,
				extendedAuthenticationFactors, configuration);

		LOGGER.info("Successfully persisted all pre-computation payloads. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);
	}

	private void persistSetupComponentVerificationData(final PrecomputeContext precomputeContext, final GqGroup encryptionGroup,
			final ImmutableList<GenVerDatOutput> genVerDatOutputs) {
		final String electionEventId = precomputeContext.electionEventId();
		final String verificationCardSetId = precomputeContext.verificationCardSetId();

		// Delete all existent SetupComponentVerificationDataPayloads in case the pre-computation has failed before.
		setupComponentVerificationDataPayloadFileRepository.remove(electionEventId, verificationCardSetId);

		IntStream.range(0, genVerDatOutputs.size())
				.parallel()
				.forEach(chunkId -> {
					final GenVerDatOutput genVerDatOutput = genVerDatOutputs.get(chunkId);
					final ImmutableList<SetupComponentVerificationData> setupComponentVerificationData = IntStream.range(0, genVerDatOutput.size())
							.parallel()
							.mapToObj(i -> new SetupComponentVerificationData(
									genVerDatOutput.getVerificationCardIds().get(i),
									genVerDatOutput.getVerificationCardKeyPairs().get(i).getPublicKey(),
									genVerDatOutput.getEncryptedHashedPartialChoiceReturnCodes().get(i),
									genVerDatOutput.getEncryptedHashedConfirmationKeys().get(i)))
							.collect(toImmutableList());
					final SetupComponentVerificationDataPayload payload = new SetupComponentVerificationDataPayload(encryptionGroup, electionEventId,
							verificationCardSetId, chunkId, genVerDatOutput.getPartialChoiceReturnCodesAllowList(), setupComponentVerificationData);

					final Hashable additionalContextData = ChannelSecurityContextData.setupComponentVerificationData(electionEventId,
							verificationCardSetId);
					payload.setSignature(getPayloadSignature(payload, additionalContextData));

					setupComponentVerificationDataPayloadFileRepository.store(payload);

					LOGGER.debug(
							"Successfully created and persisted the setup component verification data payload. [electionEventId: {}, verificationCardSet: {}, chunkId {}]",
							electionEventId, verificationCardSetId, chunkId);
				});
	}

	private void persistVerificationCardSecretKeyPayload(final PrecomputeContext precomputeContext, final GqGroup encryptionGroup,
			final ImmutableList<String> verificationCardIds, final ImmutableList<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs) {
		final String electionEventId = precomputeContext.electionEventId();
		final String verificationCardSetId = precomputeContext.verificationCardSetId();

		final ImmutableList<VerificationCardSecretKey> verificationCardSecretKeys = IntStream.range(0, verificationCardIds.size())
				.parallel()
				.mapToObj(i -> new VerificationCardSecretKey(verificationCardIds.get(i), verificationCardKeyPairs.get(i).getPrivateKey()))
				.collect(toImmutableList());
		final VerificationCardSecretKeyPayload verificationCardSecretKeyPayload = new VerificationCardSecretKeyPayload(encryptionGroup,
				electionEventId, verificationCardSetId, verificationCardSecretKeys);

		verificationCardSecretKeyPayloadService.save(verificationCardSecretKeyPayload);
		LOGGER.info("Successfully created and persisted the verification card secret key payload. [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId, verificationCardSetId);

	}

	private void persistVoterInitialCodesPayload(final PrecomputeContext precomputeContext, final ImmutableList<GenVerDatOutput> genVerDatOutputs,
			final ImmutableList<String> verificationCardIds, final ImmutableList<String> startVotingKeys, final ImmutableList<VoterType> voters,
			final ImmutableList<String> extendedAuthenticationFactors) {
		final String electionEventId = precomputeContext.electionEventId();
		final String verificationCardSetId = precomputeContext.verificationCardSetId();

		final ImmutableList<String> voterIdentifications = voters.stream()
				.map(VoterType::getVoterIdentification)
				.collect(toImmutableList());
		final ImmutableList<String> ballotCastingKeys = genVerDatOutputs.stream()
				.map(GenVerDatOutput::getBallotCastingKeys)
				.flatMap(ImmutableList::stream)
				.collect(toImmutableList());

		final ImmutableList<VoterInitialCodes> voterInitialCodes = IntStream.range(0, voters.size())
				.parallel()
				.mapToObj(voter -> new VoterInitialCodes(voterIdentifications.get(voter), getVotingCardId(verificationCardIds.get(voter)),
						verificationCardIds.get(voter), startVotingKeys.get(voter), extendedAuthenticationFactors.get(voter),
						ballotCastingKeys.get(voter)))
				.collect(toImmutableList());
		final VoterInitialCodesPayload payload = new VoterInitialCodesPayload(electionEventId, verificationCardSetId, voterInitialCodes);

		voterInitialCodesPayloadService.save(payload, verificationCardSetId);
		LOGGER.info(
				"Successfully created and persisted the voter initial codes payload. [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId, verificationCardSetId);
	}

	/**
	 * Truncates the {@code verificationCardId} then add a suffix.
	 *
	 * @param verificationCardId the verification card id to truncate.
	 * @return the votingCardId based on the provided verificationCardId.
	 */
	@VisibleForTesting
	String getVotingCardId(final String verificationCardId) {
		final String votingCardId = verificationCardId.substring(0, ID_LENGTH - votingCardIdSuffix.length()) + votingCardIdSuffix;
		return validateUUID(votingCardId);
	}

	private ImmutableList<VoterType> getVotersFromConfigurationAnonymized(final VerificationCardSetEntity verificationCardSetEntity,
			final Configuration configuration) {

		final String authorizationIdentification = verificationCardSetEntity.getAlias()
				.substring(ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSet.PREFIX.length());

		// Get the voters for the corresponding authorization identification
		return configuration.getRegister().getVoter().stream()
				.filter(voter -> voter.getAuthorization().equals(authorizationIdentification))
				.collect(toImmutableList());
	}

	private String parseExtendedAuthenticationFactor(final ExtendedAuthenticationKeyType extendedAuthenticationKeyType) {
		final String extendedAuthenticationKeyTypeName = extendedAuthenticationKeyType.getName();

		final ExtendedAuthenticationFactor extendedAuthenticationFactor = EXTENDED_AUTHENTICATION_FACTORS.get(extendedAuthenticationKeyTypeName);
		checkState(Objects.nonNull(extendedAuthenticationFactor), "Unsupported extended authentication factor. [name: %s]",
				extendedAuthenticationKeyTypeName);

		final String extendedAuthenticationKeyTypeValue = extendedAuthenticationKeyType.getValue();

		checkState(extendedAuthenticationKeyTypeValue.matches(extendedAuthenticationFactor.regex()),
				"The extended authentication factor does not have the correct format. [value: %s]", extendedAuthenticationKeyTypeValue);

		if (extendedAuthenticationKeyTypeName.equals(BIRTH_DATE)) {
			final String[] extendedAuthenticationFactorSplit = extendedAuthenticationKeyTypeValue.split("-");
			return extendedAuthenticationFactorSplit[2] + extendedAuthenticationFactorSplit[1] + extendedAuthenticationFactorSplit[0];
		}

		return extendedAuthenticationKeyTypeValue;
	}

	private void persistSetupComponentTallyDataPayload(final PrecomputeContext precomputeContext, final GqGroup encryptionGroup,
			final ImmutableList<String> verificationCardIds, final ImmutableList<ElGamalMultiRecipientKeyPair> verificationCardKeyPairs,
			final String ballotBoxDescription) {
		final String electionEventId = precomputeContext.electionEventId();
		final String verificationCardSetId = precomputeContext.verificationCardSetId();

		final GroupVector<ElGamalMultiRecipientPublicKey, GqGroup> verificationCardPublicKeys = verificationCardKeyPairs.stream()
				.parallel()
				.map(ElGamalMultiRecipientKeyPair::getPublicKey)
				.collect(GroupVector.toGroupVector());

		final SetupComponentTallyDataPayload setupComponentTallyDataPayload = new SetupComponentTallyDataPayload(encryptionGroup, electionEventId,
				verificationCardSetId, verificationCardIds, ballotBoxDescription, verificationCardPublicKeys);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentTallyData(electionEventId, verificationCardSetId);
		setupComponentTallyDataPayload.setSignature(getPayloadSignature(setupComponentTallyDataPayload, additionalContextData));

		setupComponentTallyDataPayloadService.save(setupComponentTallyDataPayload);

		LOGGER.debug("Successfully created and persisted the setup component tally data payload. [electionEventId: {}, verificationCardSet: {}]",
				electionEventId, verificationCardSetId);
	}

	private void persistVoterAuthenticationData(final PrecomputeContext precomputeContext,
			final ElectionEventContextPayload electionEventContextPayload, final ImmutableList<String> verificationCardIds,
			final ImmutableList<String> startVotingKeys, final ImmutableList<String> extendedAuthenticationFactors,
			final Configuration configuration) {

		final String electionEventId = precomputeContext.electionEventId();
		final String verificationCardSetId = precomputeContext.verificationCardSetId();
		final String ballotBoxId = precomputeContext.ballotBoxId();

		checkArgument(verificationCardIds.size() == startVotingKeys.size(),
				"The number of start voting keys does not correspond to the number of verification card ids. [electionEventId: %s, verificationCardSetId: %s]",
				electionEventId, verificationCardSetId);

		final GetVoterAuthenticationDataOutput getVoterAuthenticationDataOutput = getVoterAuthenticationDataService.getVoterAuthenticationData(
				electionEventContextPayload, verificationCardSetId, configuration, startVotingKeys, extendedAuthenticationFactors);

		LOGGER.info("GetVoterAuthenticationData algorithm successfully performed. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		final ImmutableList<SetupComponentVoterAuthenticationData> setupComponentVoterAuthenticationData = IntStream.range(0,
						verificationCardIds.size())
				.parallel()
				.mapToObj(i -> {
					final String verificationCardId = verificationCardIds.get(i);
					final String votingCardId = getVotingCardId(verificationCardId);
					final String credentialId = getVoterAuthenticationDataOutput.derivedVoterIdentifiers().get(i);
					final String baseAuthenticationChallenge = getVoterAuthenticationDataOutput.baseAuthenticationChallenges().get(i);
					return new SetupComponentVoterAuthenticationData(electionEventId, verificationCardSetId, ballotBoxId, verificationCardId,
							votingCardId, credentialId, baseAuthenticationChallenge);
				})
				.collect(toImmutableList());
		final SetupComponentVoterAuthenticationDataPayload payload = new SetupComponentVoterAuthenticationDataPayload(electionEventId,
				verificationCardSetId, setupComponentVoterAuthenticationData);

		final Hashable additionalContextData = ChannelSecurityContextData.setupComponentVoterAuthenticationData(electionEventId,
				verificationCardSetId);
		payload.setSignature(getPayloadSignature(payload, additionalContextData));

		setupComponentVoterAuthenticationPayloadService.save(payload);

		LOGGER.info("Successfully created and persisted all voter authentication data. [electionEventId: {}, verificationCardSetId: {}]",
				electionEventId, verificationCardSetId);
	}

	private CryptoPrimitivesSignature getPayloadSignature(final SignedPayload payload, final Hashable additionalContextData) {

		final ImmutableByteArray signature;
		try {
			signature = signatureKeystoreService.generateSignature(payload, additionalContextData);
		} catch (final SignatureException e) {
			throw new IllegalStateException(String.format("Failed to generate payload signature. [name: %s]", payload.getClass().getName()));
		}

		return new CryptoPrimitivesSignature(signature);
	}
}

