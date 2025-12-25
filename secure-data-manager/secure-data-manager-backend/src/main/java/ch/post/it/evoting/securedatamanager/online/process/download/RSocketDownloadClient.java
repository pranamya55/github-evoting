/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process.download;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.rsocket.service.RSocketExchange;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.domain.configuration.setupvoting.DownloadRequestPayload;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RSocketDownloadClient {

	@RSocketExchange("electionEvents/{electionEventId}/downloadEncLongCodeShares")
	Flux<ImmutableList<ImmutableByteArray>> download(
			@DestinationVariable("electionEventId")
			final String electionEventId, final Mono<DownloadRequestPayload> downloadRequestPayloadMono);

}
