/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.process.configuration.download;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkState;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.setupvoting.DownloadRequestPayload;
import ch.post.it.evoting.votingserver.multitenancy.RSocketTenantWrapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

@Controller
public class DownloadEncryptedLongReturnCodeSharesRController {

	private static final int MEGA_BYTE = 1024 * 1024;
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadEncryptedLongReturnCodeSharesRController.class);

	private final DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService;
	private final RSocketTenantWrapper rSocketTenantWrapper;

	public DownloadEncryptedLongReturnCodeSharesRController(
			final DownloadEncryptedLongReturnCodeSharesService downloadEncryptedLongReturnCodeSharesService,
			final RSocketTenantWrapper rSocketTenantWrapper) {
		this.downloadEncryptedLongReturnCodeSharesService = downloadEncryptedLongReturnCodeSharesService;
		this.rSocketTenantWrapper = rSocketTenantWrapper;
	}

	@MessageMapping("electionEvents/{electionEventId}/downloadEncLongCodeShares")
	public Flux<ImmutableList<ImmutableByteArray>> download(
			@DestinationVariable("electionEventId")
			final String eeId,
			final Mono<DownloadRequestPayload> downloadRequestPayloadMono) {

		validateUUID(eeId);
		final AtomicInteger counter = new AtomicInteger(0);

		return rSocketTenantWrapper.wrapWithTenantContext(downloadRequestPayloadMono
				.flatMapMany(downloadRequestPayload ->
						Flux.fromStream(downloadRequestPayload.chunkIds().stream()
								.map(chunkId -> Tuples.of(downloadRequestPayload.electionEventId(),
										downloadRequestPayload.verificationCardSetId(),
										chunkId))))
				.publishOn(Schedulers.boundedElastic())
				.map(tuple -> {
					final String electionEventId = tuple.getT1();
					final String verificationCardSetId = tuple.getT2();
					final int chunkId = tuple.getT3();

					checkState(eeId.equals(electionEventId), "Election event id mismatch. [expected: %s, actual: %s]", eeId, electionEventId);

					LOGGER.debug(
							"Received request to download control component code shares payload. [electionEventId: {}, verificationCardSetId: {},  chunkId: {}]",
							electionEventId, verificationCardSetId, chunkId);

					final ImmutableList<ImmutableByteArray> controlComponentCodeSharesPayloadsBytes = downloadEncryptedLongReturnCodeSharesService.download(
							electionEventId, verificationCardSetId, chunkId);

					LOGGER.info(
							"Retrieved control component code shares payload for download. [electionEventId: {}, verificationCardSetId: {},  chunkId: {}]",
							electionEventId, verificationCardSetId, chunkId);

					return controlComponentCodeSharesPayloadsBytes;
				})
				.limitRate(1)
				.doOnNext(i -> LOGGER.debug("Memory ({}): {} MB", counter.incrementAndGet(),
						(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MEGA_BYTE))
				.doFinally(signal -> LOGGER.debug("Maximum memory used: {} MB", getPeakMemUsage() / MEGA_BYTE)), eeId);
	}

	private static long getPeakMemUsage() {
		final List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
		long total = 0;
		for (final MemoryPoolMXBean memoryPoolMXBean : pools) {
			if (memoryPoolMXBean.getType() == MemoryType.HEAP) {
				final long peakUsed = memoryPoolMXBean.getPeakUsage().getUsed();
				total += peakUsed;
			}
		}
		return total;
	}
}
