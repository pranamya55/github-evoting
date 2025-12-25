/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.UncheckedIOException;
import java.security.SignatureException;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.signing.SignatureKeystore;
import ch.post.it.evoting.domain.InvalidPayloadSignatureException;
import ch.post.it.evoting.evotinglibraries.domain.LocalDateTimeUtils;
import ch.post.it.evoting.evotinglibraries.domain.common.ChannelSecurityContextData;
import ch.post.it.evoting.evotinglibraries.domain.election.ElectionEventContext;
import ch.post.it.evoting.evotinglibraries.domain.election.VerificationCardSetContext;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.ElectionTexts;
import ch.post.it.evoting.evotinglibraries.domain.electoralmodel.VoteTexts;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.signature.CryptoPrimitivesSignature;
import ch.post.it.evoting.votingserver.process.votingcardmanagement.ElectionEventDto;

@Service
public class ElectionEventContextService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectionEventContextService.class);

	private final ElectionEventService electionEventService;
	private final VerificationCardSetService verificationCardSetService;
	private final BallotBoxService ballotBoxService;
	private final ElectionEventContextRepository electionEventContextRepository;
	private final SignatureKeystore<Alias> signatureKeystoreService;

	public ElectionEventContextService(
			final ElectionEventService electionEventService,
			final VerificationCardSetService verificationCardSetService,
			final BallotBoxService ballotBoxService,
			final ElectionEventContextRepository electionEventContextRepository,
			final SignatureKeystore<Alias> signatureKeystoreService) {
		this.electionEventService = electionEventService;
		this.verificationCardSetService = verificationCardSetService;
		this.ballotBoxService = ballotBoxService;
		this.electionEventContextRepository = electionEventContextRepository;
		this.signatureKeystoreService = signatureKeystoreService;
	}

	/**
	 * Saves the election event context and uploads it to the control components.
	 *
	 * @param electionEventContextPayload the request payload. Must be non null.
	 * @throws NullPointerException             if {@code electionEventContextPayload} is null.
	 * @throws IllegalStateException            if an error occurred while verifying the signature of the election event context payload.
	 * @throws IllegalArgumentException         if the election event finish date is in the past.
	 * @throws InvalidPayloadSignatureException if the signature of the election event context payload is invalid.
	 * @throws UncheckedIOException             if an error occurs while serializing the election event context.
	 */
	@Transactional
	public void saveElectionEventContext(final ElectionEventContextPayload electionEventContextPayload) {
		checkNotNull(electionEventContextPayload);
		verifyPayloadSignature(electionEventContextPayload);

		final ElectionEventContext electionEventContext = electionEventContextPayload.getElectionEventContext();
		final String electionEventId = electionEventContext.electionEventId();

		checkArgument(electionEventContext.finishTime().isAfter(LocalDateTimeUtils.now()),
				"The election event period should not be finished yet. [electionEventId: %s]", electionEventId);

		electionEventService.save(electionEventId, electionEventContextPayload.getEncryptionGroup());

		electionEventContextRepository.save(createElectionEventContextEntity(electionEventContext));
		final String startTime = LocalDateTimeUtils.format(electionEventContext.startTime());
		final String finishTime = LocalDateTimeUtils.format(electionEventContext.finishTime());
		LOGGER.info("Election event context successfully saved. [electionEventId: {}, alias: {}, startTime: {}, finishTime: {}]",
				electionEventId, electionEventContext.electionEventAlias(), startTime, finishTime);

		final ImmutableList<VerificationCardSetContext> verificationCardSetContexts = electionEventContext.verificationCardSetContexts();
		verificationCardSetService.saveAllFromContext(electionEventId, verificationCardSetContexts);
		ballotBoxService.saveAllFromContexts(verificationCardSetContexts);
	}

	public VotesElectionsTexts getVerificationCardSetTexts(final String electionEventId, final String verificationCardSetId) {
		final ElectionEventContextEntity electionEventContextEntity = electionEventContextRepository.findById(electionEventId)
				.orElseThrow(() -> new IllegalStateException(
						String.format("No election event context entity found. [electionEventId: %s]", electionEventId)));

		final VerificationCardSetEntity verificationCardSetEntity = verificationCardSetService.getVerificationCardSetEntity(verificationCardSetId);

		final ImmutableList<String> domainsOfInfluence = verificationCardSetEntity.getDomainsOfInfluence();

		final ImmutableList<VoteTexts> votesTexts = electionEventContextEntity.getVotesTexts().stream()
				.filter(voteTexts -> domainsOfInfluence.contains(voteTexts.domainOfInfluence()))
				.collect(toImmutableList());
		final ImmutableList<ElectionTexts> electionsTexts = electionEventContextEntity.getElectionsTexts().stream()
				.filter(electionTexts -> domainsOfInfluence.contains(electionTexts.domainOfInfluence()))
				.collect(toImmutableList());

		return new VotesElectionsTexts(votesTexts, electionsTexts);
	}

	/**
	 * Encapsulates the {@link VoteTexts} and {@link ElectionTexts} lists.
	 */
	public record VotesElectionsTexts(ImmutableList<VoteTexts> votesTexts, ImmutableList<ElectionTexts> electionsTexts) {}

	/**
	 * Gets the election event context entity.
	 *
	 * @param electionEventEntity the related election event entity. Must be non-null.
	 * @return the election event context entity.
	 * @throws NullPointerException  if {@code ElectionEventEntity} is null.
	 * @throws IllegalStateException if the election event context entity does not exist.
	 */
	public ElectionEventContextEntity getElectionEventContextEntity(final ElectionEventEntity electionEventEntity) {
		checkNotNull(electionEventEntity);

		return electionEventContextRepository.findById(electionEventEntity.getElectionEventId())
				.orElseThrow(() -> new IllegalStateException(
						String.format("No election event context entity found. [electionEventId: %s]", electionEventEntity.getElectionEventId())));
	}

	private ElectionEventContextEntity createElectionEventContextEntity(final ElectionEventContext electionEventContext) {
		final ElectionEventEntity electionEventEntity = electionEventService.retrieveElectionEventEntity(electionEventContext.electionEventId());

		return new ElectionEventContextEntity(electionEventEntity, electionEventContext.startTime(), electionEventContext.finishTime(),
				electionEventContext.electionEventAlias(), electionEventContext.electionEventDescription(), electionEventContext.votesTexts(),
				electionEventContext.electionsTexts());
	}

	/**
	 * Retrieves all election events.
	 */
	public ImmutableList<ElectionEventDto> retrieveAll() {
		return StreamSupport.stream(electionEventContextRepository.findAll().spliterator(), false)
				.map(electionEventContextEntity -> new ElectionEventDto(
						electionEventContextEntity.getElectionEventId(),
						electionEventContextEntity.getElectionEventAlias(),
						electionEventContextEntity.getElectionEventDescription(),
						electionEventContextEntity.getStartTime(),
						electionEventContextEntity.getFinishTime())
				).collect(toImmutableList());
	}

	/**
	 * Verifies the signature of the election event context payload.
	 *
	 * @param electionEventContextPayload the election event context payload to verify. Must be non-null.
	 * @throws NullPointerException             if {@code electionEventContextPayload} is null.
	 * @throws IllegalStateException            if an error occurred while verifying the signature of the election event context payload.
	 * @throws InvalidPayloadSignatureException if the signature of the election event context payload is invalid.
	 */
	public void verifyPayloadSignature(final ElectionEventContextPayload electionEventContextPayload) {
		final String electionEventId = electionEventContextPayload.getElectionEventContext().electionEventId();

		final CryptoPrimitivesSignature signature = electionEventContextPayload.getSignature();

		checkState(signature != null, "The signature of the election event context payload is null. [electionEventId: %s]", electionEventId);

		final Hashable additionalContextData = ChannelSecurityContextData.electionEventContext(electionEventId);

		final boolean isSignatureValid;
		try {
			isSignatureValid = signatureKeystoreService.verifySignature(Alias.SDM_CONFIG, electionEventContextPayload,
					additionalContextData, signature.signatureContents());
		} catch (final SignatureException e) {
			throw new IllegalStateException(
					String.format("Could not verify the signature of the election event context. [electionEventId: %s]", electionEventId));
		}

		if (!isSignatureValid) {
			throw new InvalidPayloadSignatureException(ElectionEventContextPayload.class, String.format("[electionEventId: %s]", electionEventId));
		}
	}

}
