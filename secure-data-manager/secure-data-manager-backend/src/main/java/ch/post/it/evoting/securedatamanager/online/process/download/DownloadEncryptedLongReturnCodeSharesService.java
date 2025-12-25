/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.download;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.DOWNLOAD_UNSUCCESSFUL_MESSAGE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.setupvoting.DownloadRequestPayload;
import ch.post.it.evoting.domain.reactor.Box;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;
import ch.post.it.evoting.evotinglibraries.domain.returncodes.ControlComponentCodeSharesPayload;
import ch.post.it.evoting.securedatamanager.online.WebClientFactory;
import ch.post.it.evoting.securedatamanager.shared.process.ControlComponentCodeSharesPayloadFileRepository;
import ch.post.it.evoting.securedatamanager.shared.process.SetupComponentVerificationDataPayloadFileRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.RetryBackoffSpec;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class DownloadEncryptedLongReturnCodeSharesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadEncryptedLongReturnCodeSharesService.class);

	private final int splitConcurrency;
	private final boolean downloadRSocket;
	private final long maxRequestBodySize;
	private final ObjectMapper objectMapper;
	private final boolean deleteControlComponentCodeShares;
	private final WebClientFactory webClientFactory;
	private final RetryBackoffSpec retryBackoffSpec;
	private final ControlComponentCodeSharesPayloadFileRepository controlComponentCodeSharesPayloadFileRepository;
	private final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository;

	public DownloadEncryptedLongReturnCodeSharesService(
			@Value("${sdm.process.download.split.split-concurrency}")
			final int splitConcurrency,
			@Value("${sdm.process.download.rsocket}")
			final boolean downloadRSocket,
			@Value("${sdm.process.download.split.max-request-size}")
			final long maxRequestBodySize,
			final ObjectMapper objectMapper,
			@Value("${sdm.process.download.delete}")
			final boolean deleteControlComponentCodeShares,
			final WebClientFactory webClientFactory,
			final RetryBackoffSpec retryBackoffSpec,
			final ControlComponentCodeSharesPayloadFileRepository controlComponentCodeSharesPayloadFileRepository,
			final SetupComponentVerificationDataPayloadFileRepository setupComponentVerificationDataPayloadFileRepository) {
		this.splitConcurrency = splitConcurrency;
		this.downloadRSocket = downloadRSocket;
		this.maxRequestBodySize = maxRequestBodySize;
		this.objectMapper = objectMapper;
		this.deleteControlComponentCodeShares = deleteControlComponentCodeShares;
		this.webClientFactory = webClientFactory;
		this.retryBackoffSpec = retryBackoffSpec;
		this.setupComponentVerificationDataPayloadFileRepository = setupComponentVerificationDataPayloadFileRepository;
		this.controlComponentCodeSharesPayloadFileRepository = controlComponentCodeSharesPayloadFileRepository;
	}

	/**
	 * Download the control components' encrypted long Choice Return codes and the encrypted long Vote Cast Return code shares.
	 */
	public ParallelFlux<ImmutableList<ControlComponentCodeSharesPayload>> download(final String electionEventId,
			final String verificationCardSetId, final int chunkCount) {

		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);
		checkArgument(chunkCount > 0);

		if (downloadRSocket) {
			return downloadEncryptedLongReturnCodeSharesRSocket(electionEventId, verificationCardSetId, chunkCount);
		} else {
			return downloadEncryptedLongReturnCodeSharesSplit(electionEventId, verificationCardSetId, chunkCount);
		}
	}

	private ParallelFlux<ImmutableList<ControlComponentCodeSharesPayload>> downloadEncryptedLongReturnCodeSharesRSocket(final String electionEventId,
			final String verificationCardSetId, final int chunkCount) {

		final ImmutableList<Integer> chunkIds = IntStream.range(0, chunkCount)
				.filter(chunkId ->
						deleteControlComponentCodeShares || !controlComponentCodeSharesPayloadFileRepository.exists(electionEventId,
								verificationCardSetId, chunkId))
				.boxed()
				.collect(toImmutableList());
		final DownloadRequestPayload downloadRequestPayload = new DownloadRequestPayload(electionEventId, verificationCardSetId, chunkIds);

		final RSocketDownloadClient rSocketClient = webClientFactory.createRSocketClient(RSocketDownloadClient.class);

		return rSocketClient.download(electionEventId, Mono.just(downloadRequestPayload))
				.limitRate(1)
				.parallel()
				.runOn(Schedulers.boundedElastic())
				.map(controlComponentCodeSharesPayloadsBytes ->
						controlComponentCodeSharesPayloadsBytes.stream()
								.map(this::deserializePayload)
								.collect(toImmutableList()))
				.map(controlComponentCodeSharesPayloads ->
						checkPayloadsConsistency(electionEventId, verificationCardSetId, controlComponentCodeSharesPayloads));
	}

	private ParallelFlux<ImmutableList<ControlComponentCodeSharesPayload>> downloadEncryptedLongReturnCodeSharesSplit(final String electionEventId,
			final String verificationCardSetId, final int chunkCount) {

		final AtomicLong consumedBytes = new AtomicLong(0);

		return Flux.range(0, chunkCount)
				.filter(chunkId -> deleteControlComponentCodeShares || !controlComponentCodeSharesPayloadFileRepository.exists(electionEventId,
						verificationCardSetId, chunkId))
				.transform(flux -> splitFluxBySize(flux, electionEventId, verificationCardSetId, consumedBytes, maxRequestBodySize))
				.flatMap(chunkIds -> downloadControlComponentCodeSharesPayloads(electionEventId, verificationCardSetId, chunkIds),
						splitConcurrency)
				.parallel()
				.runOn(Schedulers.boundedElastic())
				.map(controlComponentCodeSharesPayloadsBytes -> controlComponentCodeSharesPayloadsBytes.stream()
						.map(this::deserializePayload)
						.collect(toImmutableList()))
				.map(controlComponentCodeSharesPayloads -> checkPayloadsConsistency(electionEventId, verificationCardSetId,
						controlComponentCodeSharesPayloads));
	}

	private Flux<ImmutableList<Integer>> splitFluxBySize(final Flux<Integer> flux, final String electionEventId, final String verificationCardSetId,
			final AtomicLong consumedBytes, final long maxSize) {
		return flux.bufferUntil(payload -> {
					final long requestPayloadSize = setupComponentVerificationDataPayloadFileRepository.getPayloadSize(electionEventId,
							verificationCardSetId);
					final long responsePayloadSize = ControlComponentNode.ids().size() * requestPayloadSize;
					final long actualConsumption = consumedBytes.addAndGet(responsePayloadSize);
					if (actualConsumption > maxSize) {
						LOGGER.debug("size limit reached, cutting the flux after this element [actualConsumption: {}, limit: {}]", actualConsumption,
								maxSize);
						consumedBytes.set(0);
						return true;
					}
					return false;
				}, false)
				.map(ImmutableList::from);
	}

	private Flux<ImmutableList<ImmutableByteArray>> downloadControlComponentCodeSharesPayloads(final String electionEventId,
			final String verificationCardSetId, final ImmutableList<Integer> chunkIds) {
		return webClientFactory.getWebClient(
						String.format("%s [electionEventId: %s, verificationCardSetId: %s]",
								DOWNLOAD_UNSUCCESSFUL_MESSAGE, electionEventId, verificationCardSetId))
				.post()
				.uri(uriBuilder -> uriBuilder.path(
								"api/v1/configuration/electionevent/{electionEventId}/verificationcardset/{verificationCardSetId}/download")
						.build(electionEventId, verificationCardSetId))
				.contentType(MediaType.APPLICATION_NDJSON)
				.accept(MediaType.APPLICATION_NDJSON)
				.body(Flux.fromIterable(chunkIds), Integer.class)
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<Box<ImmutableList<ImmutableByteArray>>>() {
				})
				.retryWhen(retryBackoffSpec)
				.map(Box::boxed);
	}

	private ControlComponentCodeSharesPayload deserializePayload(final ImmutableByteArray bytes) {
		final ControlComponentCodeSharesPayload controlComponentCodeSharesPayload;
		try {
			controlComponentCodeSharesPayload = objectMapper.readValue(bytes.elements(), ControlComponentCodeSharesPayload.class);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}

		LOGGER.debug("Control Component Code Shares Payload deserialized. [electionEventId: {}, verificationCardSetId: {}, chunkId: {}, nodeId: {}]",
				controlComponentCodeSharesPayload.getElectionEventId(), controlComponentCodeSharesPayload.getVerificationCardSetId(),
				controlComponentCodeSharesPayload.getChunkId(), controlComponentCodeSharesPayload.getNodeId());

		return controlComponentCodeSharesPayload;
	}

	private static ImmutableList<ControlComponentCodeSharesPayload> checkPayloadsConsistency(final String electionEventId,
			final String verificationCardSetId, final ImmutableList<ControlComponentCodeSharesPayload> controlComponentCodeSharesPayloads) {
		final ImmutableList<ControlComponentCodeSharesPayload> result = controlComponentCodeSharesPayloads.stream()
				.sorted(Comparator.comparingInt(ControlComponentCodeSharesPayload::getNodeId))
				.collect(toImmutableList());

		checkState(controlComponentCodeSharesPayloads.stream().map(ControlComponentCodeSharesPayload::getChunkId).distinct().count() == 1,
				"All ControlComponentCodeSharesPayloads must have the same chunkId");

		checkState(ControlComponentNode.ids().equals(result.stream().parallel()
						.map(controlComponentCodeSharesPayload -> {

							checkState(controlComponentCodeSharesPayload.getElectionEventId().equals(electionEventId),
									"The ControlComponentCodeSharesPayload's election event id does not correspond to the election event id of the request");

							checkState(controlComponentCodeSharesPayload.getVerificationCardSetId().equals(verificationCardSetId),
									"The ControlComponentCodeSharesPayload's verification card set id does not correspond to the verification card set id of the request");

							return controlComponentCodeSharesPayload.getNodeId();
						})
						.collect(toImmutableSet())),
				"There must be exactly the expected number of ControlComponentCodeSharesPayloads with the expected node ids.");
		return result;
	}
}
