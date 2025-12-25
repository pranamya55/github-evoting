/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.compute;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.process.VerificationCardSetService;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.RetryBackoffSpec;

/**
 * This is an application service that deals with the computation of verification card data.
 */
@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class ComputeVerificationCardSetService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeVerificationCardSetService.class);

	private final long maxRequestBodySize;
	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;
	private final VerificationCardSetService verificationCardSetService;
	private final ComputeEncryptedLongReturnCodeSharesService computeEncryptedLongReturnCodeSharesService;
	private final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;

	public ComputeVerificationCardSetService(
			@Value("${sdm.process.compute.max-request-size}")
			final long maxRequestBodySize,
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec,
			final VerificationCardSetService verificationCardSetService,
			final ComputeEncryptedLongReturnCodeSharesService computeEncryptedLongReturnCodeSharesService,
			final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository) {
		this.maxRequestBodySize = maxRequestBodySize;
		this.retryBackoffSpec = retryBackoffSpec;
		this.webClientFactory = webClientFactory;
		this.verificationCardSetService = verificationCardSetService;
		this.computeEncryptedLongReturnCodeSharesService = computeEncryptedLongReturnCodeSharesService;
		this.setupComponentVerificationDataPayloadFileRepository = setupComponentVerificationDataPayloadFileRepository;
	}

	/**
	 * Computes the verification card sets.
	 */
	public void compute(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		LOGGER.debug("Starting computation of verification card set... [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
				verificationCardSetId);

		final int chunkCount = setupComponentVerificationDataPayloadFileRepository.getCount(electionEventId, verificationCardSetId);
		checkState(chunkCount > 0, "No chunk found for computation. [electionEventId: %s, verificationCardSetId: %s]", electionEventId,
				verificationCardSetId);

		final ImmutableList<Integer> queuedChunkIds = getQueuedComputeChunkIds(electionEventId, verificationCardSetId);
		if (!queuedChunkIds.isEmpty()) {
			final String queuedChunkIdsString = queuedChunkIds.stream().map(Object::toString).collect(Collectors.joining(","));
			LOGGER.warn(
					"Ignoring some chunks as they were already queued for computation. [electionEventId: {}, verificationCardSetId: {}, chunkIds: {}]",
					electionEventId, verificationCardSetId, queuedChunkIdsString);
		}

		final AtomicLong consumedBytes = new AtomicLong(0);

		// Send for processing.
		Flux.range(0, chunkCount)
				.filter(chunkId -> !queuedChunkIds.contains(chunkId))
				.transform(flux -> splitFluxBySize(flux, electionEventId, verificationCardSetId, consumedBytes, maxRequestBodySize))
				.publishOn(Schedulers.boundedElastic())
				.flatMap(chunkIdList ->
						Flux.fromIterable(chunkIdList)
								.publishOn(Schedulers.boundedElastic())
								.map(chunkId -> setupComponentVerificationDataPayloadFileRepository.retrieve(electionEventId, verificationCardSetId,
										chunkId))
								.collect(toImmutableList())
				)
				.doOnNext(
						list -> {
							computeEncryptedLongReturnCodeSharesService.compute(electionEventId, verificationCardSetId,
									list);
							LOGGER.info(
									"Computation of batch started. [electionEventId: {}, verificationCardSetId: {}, batchSize: {}]", electionEventId,
									verificationCardSetId, list.size());
						})
				.doOnComplete(() -> {
					// All chunks have been sent, update status.
					verificationCardSetService.updateStatus(verificationCardSetId, Status.COMPUTING);
					LOGGER.info("Computation of verification card set started. [electionEventId: {}, verificationCardSetId: {}]", electionEventId,
							verificationCardSetId);
				})
				.blockLast();
	}

	private ImmutableList<Integer> getQueuedComputeChunkIds(final String electionEventId, final String verificationCardSetId) {
		final ImmutableList<Integer> queuedComputeChunkIds = webClientFactory.getWebClient(
						String.format("Request for queued compute chunk ids unsuccessful. [electionEventId: %s, verificationCardSetId: %s]", electionEventId,
								verificationCardSetId))
				.get()
				.uri(uriBuilder -> uriBuilder.path(
								"api/v1/configuration/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/queuedcomputechunkids")
						.build(electionEventId, verificationCardSetId))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToFlux(Integer.class)
				.collect(toImmutableList())
				.retryWhen(retryBackoffSpec)
				.block();

		checkState(Objects.nonNull(queuedComputeChunkIds),
				"Queued compute chunk ids cannot be null. [electionEventId: %s, verificationCardSetId: %s]", electionEventId, verificationCardSetId);

		return queuedComputeChunkIds;
	}

	private Flux<ImmutableList<Integer>> splitFluxBySize(final Flux<Integer> flux,
			final String electionEventId, final String verificationCardSetId, final AtomicLong consumedBytes, final long maxSize) {
		return flux.bufferUntil(payload -> {
			final long payloadSize = setupComponentVerificationDataPayloadFileRepository.getPayloadSize(electionEventId,
					verificationCardSetId);
			final long actualConsumption = consumedBytes.addAndGet(payloadSize);
			if (actualConsumption > maxSize) {
				LOGGER.debug("size limit reached, cutting the flux after this element [actualConsumption: {}, limit: {}]", actualConsumption,
						maxSize);
				consumedBytes.set(0);
				return true;
			}
			return false;
		}, false).map(ImmutableList::from);
	}

}
