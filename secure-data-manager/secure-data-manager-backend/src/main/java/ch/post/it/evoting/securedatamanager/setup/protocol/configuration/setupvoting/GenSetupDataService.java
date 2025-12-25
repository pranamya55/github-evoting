/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.protocol.configuration.setupvoting;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap.toImmutableMap;
import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.election.PartialPrimesMappingTableEntry;
import ch.post.it.evoting.evotinglibraries.xml.primesmappingtable.PartialPrimesMappingTableEntryBuilder;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetEntity;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

@Service
@ConditionalOnProperty("role.isSetup")
public class GenSetupDataService {

	private final EvotingConfigService evotingConfigService;
	private final GenSetupDataAlgorithm genSetupDataAlgorithm;
	private final VerificationCardSetService verificationCardSetService;

	public GenSetupDataService(
			final EvotingConfigService evotingConfigService,
			final GenSetupDataAlgorithm genSetupDataAlgorithm,
			final VerificationCardSetService verificationCardSetService) {
		this.evotingConfigService = evotingConfigService;
		this.genSetupDataAlgorithm = genSetupDataAlgorithm;
		this.verificationCardSetService = verificationCardSetService;
	}

	/**
	 * Invokes the GenSetupData algorithm.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @param seed            the encryption parameters seed. Must be non-null and a valid seed.
	 */
	public GenSetupDataOutput genSetupData(final String electionEventId, final String seed) {
		validateUUID(electionEventId);
		validateSeed(seed);

		final ImmutableList<VerificationCardSetEntity> verificationCardSetEntities = verificationCardSetService.getVerificationCardSets(
				electionEventId);
		final Configuration configuration = evotingConfigService.load();

		final ImmutableMap<String, ImmutableList<PartialPrimesMappingTableEntry>> optionsInformationPerVerificationCardSetId = verificationCardSetEntities.stream()
				.parallel()
				.collect(toImmutableMap(
						VerificationCardSetEntity::getVerificationCardSetId,
						verificationCardSet -> PartialPrimesMappingTableEntryBuilder.create(configuration,
								verificationCardSet.getDomainsOfInfluence())
				));

		final GenSetupDataContext genSetupDataContext = new GenSetupDataContext(optionsInformationPerVerificationCardSetId);
		return genSetupDataAlgorithm.genSetupData(genSetupDataContext, seed);
	}
}
