/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process.collectdataverifier;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.ech.xmlns.ech_0222._3.Delivery;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.evotinglibraries.domain.tally.TallyComponentVotesPayload;
import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.evotinglibraries.xml.mapper.RawDataDeliveryMapper;
import ch.post.it.evoting.evotinglibraries.xml.xmlns.evotingconfig.Configuration;
import ch.post.it.evoting.securedatamanager.shared.process.EvotingConfigService;

@Service
@ConditionalOnProperty("role.isTally")
public class TallyComponentEch0222Service {

	private static final Logger LOGGER = LoggerFactory.getLogger(TallyComponentEch0222Service.class);

	private final EvotingConfigService evotingConfigService;
	private final TallyComponentEch0222FileRepository tallyComponentEch0222FileRepository;

	public TallyComponentEch0222Service(
			final EvotingConfigService evotingConfigService,
			final TallyComponentEch0222FileRepository tallyComponentEch0222FileRepository) {
		this.evotingConfigService = evotingConfigService;
		this.tallyComponentEch0222FileRepository = tallyComponentEch0222FileRepository;
	}

	/**
	 * Generates and persists the eCH-0222 tally file.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @throws NullPointerException      if the election event id is null.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 */
	public void generate(final String electionEventId,
			final ImmutableMap<String, TallyComponentVotesPayload> authorizationNameToTallyComponentVotesPayloadMap) {
		validateUUID(electionEventId);

		LOGGER.debug("Generating tally component eCH-0222 file... [electionEventId: {}]", electionEventId);

		final Configuration configuration = evotingConfigService.load();

		final Delivery delivery = RawDataDeliveryMapper.createECH0222(configuration,
				authorizationNameToTallyComponentVotesPayloadMap);

		tallyComponentEch0222FileRepository.save(delivery, electionEventId);

		LOGGER.info("Tally component eCH-0222 file successfully generated. [electionEventId: {}]", electionEventId);
	}

	/**
	 * Loads the tally component eCH-0222 for the given election event id and validates its signature.
	 * <p>
	 * The result of this method is cached.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the tally component eCH-0222 as {@link Delivery}.
	 * @throws NullPointerException      if the election event id is null.
	 * @throws IllegalArgumentException  if the contest identification is blank.
	 * @throws FailedValidationException if the election event id is not a valid UUID.
	 */
	public Delivery load(final String electionEventId) {
		validateUUID(electionEventId);

		return tallyComponentEch0222FileRepository.load(electionEventId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("Could not find the requested tally component eCH-0222 file. [electionEventId: %s]", electionEventId)));
	}
}
