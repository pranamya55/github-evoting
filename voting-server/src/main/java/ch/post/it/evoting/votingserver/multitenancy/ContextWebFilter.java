/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.multitenancy;

import static ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder.TENANT_ID;
import static ch.post.it.evoting.evotinglibraries.domain.validations.TenantIdValidation.validateTenantId;
import static ch.post.it.evoting.votingserver.process.Constants.DUMMY_ELECTION_EVENT_ID;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.domain.mixnet.ElectionEventContextPayload;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.Tenant;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
public class ContextWebFilter implements WebFilter {

	public static final String HEADER_TENANT_ID = "x-tenant-id";

	private static final Logger LOGGER = LoggerFactory.getLogger(ContextWebFilter.class);
	private final ContextHolder contextHolder;
	private final TenantLookupService tenantLookupService;
	private final ObjectMapper objectMapper;
	private final TenantService tenantService;

	public ContextWebFilter(final ContextHolder contextHolder, final TenantLookupService tenantLookupService, final ObjectMapper objectMapper,
			final TenantService tenantService) {
		this.contextHolder = contextHolder;
		this.tenantLookupService = tenantLookupService;
		this.objectMapper = objectMapper;
		this.tenantService = tenantService;
	}

	@Override
	public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
		checkNotNull(exchange);
		checkNotNull(chain);
		final PathContainer requestPath = exchange.getRequest().getPath().pathWithinApplication();
		final Optional<String> electionEventId = getElectionEventIdFromRequest(requestPath);

		final ExchangeType exchangeType = determineCase(requestPath, electionEventId);
		LOGGER.debug("Handling request. [requestPath: {}, exchangeType: {}]", requestPath, exchangeType);
		return switch (exchangeType) {
			case OUT_SCOPE, DUMMY_ELECTION_EVENT -> chain.filter(exchange);
			case INITIAL_REQUEST -> handleInitialRequest(exchange, chain, requestPath, electionEventId);
			case VOTING_CARD_MANAGER_API -> handleVotingCardManagerApi(exchange, chain, requestPath);
			case STANDARD_REQUEST -> handleStandardRequest(exchange, chain, requestPath, electionEventId);
		};
	}

	private Mono<Void> handleStandardRequest(final ServerWebExchange exchange, final WebFilterChain chain, final PathContainer requestPath,
			final Optional<String> optionalElectionEventId) {
		if (optionalElectionEventId.isEmpty()) {
			LOGGER.warn("Election event id missing in the request path. [requestPath: {}]", requestPath);
			return responseWithHttpError(exchange, HttpStatus.PRECONDITION_FAILED);
		}

		final String electionEventId = optionalElectionEventId.get();

		final Optional<Tenant> tenant = tenantLookupService.lookupTenantFromElectionEventId(electionEventId);
		if (tenant.isEmpty()) {
			LOGGER.warn("Election event id not found. [electionEventId: {}]", electionEventId);
			return responseWithHttpError(exchange, HttpStatus.NOT_FOUND);
		} else {
			final String tenantId = tenant.get().id();
			try {
				contextHolder.setTenantId(tenantId);
				return chain.filter(exchange).contextWrite(Context.of(TENANT_ID, tenantId));
			} finally {
				contextHolder.clear();
			}
		}
	}

	private Mono<Void> handleVotingCardManagerApi(final ServerWebExchange exchange, final WebFilterChain chain, final PathContainer requestPath) {
		final Optional<String> headerValue = exchange.getRequest().getHeaders().getValuesAsList(HEADER_TENANT_ID).stream().findFirst();
		if (headerValue.isEmpty()) {
			LOGGER.warn("The tenant id is missing in the request header. [requestPath: {}]", requestPath);
			return responseWithHttpError(exchange, HttpStatus.PRECONDITION_FAILED);
		} else {
			final String tenantId = headerValue.get();
			checkTenantValidity(tenantId);
			try {
				contextHolder.setTenantId(tenantId);
				return chain.filter(exchange).contextWrite(Context.of(TENANT_ID, tenantId));
			} finally {
				contextHolder.clear();
			}
		}
	}

	private Mono<Void> handleInitialRequest(final ServerWebExchange exchange, final WebFilterChain chain, final PathContainer requestPath,
			final Optional<String> electionEventId) {
		if (electionEventId.isEmpty()) {
			LOGGER.warn("The election event id is missing in the request path. [requestPath: {}]", requestPath);
			return responseWithHttpError(exchange, HttpStatus.PRECONDITION_FAILED);
		} else {
			try {
				final ServerHttpRequest request = exchange.getRequest();
				return DataBufferUtils.join(request.getBody())
						.flatMap(dataBuffer -> {
							final byte[] bytes = new byte[dataBuffer.readableByteCount()];
							dataBuffer.read(bytes);
							DataBufferUtils.release(dataBuffer);

							final String tenantId;
							try {
								final ElectionEventContextPayload electionEventContextPayload = objectMapper.readValue(bytes,
										ElectionEventContextPayload.class);
								tenantId = electionEventContextPayload.getTenantId();
							} catch (final IOException e) {
								LOGGER.error("Unable to read the election event context payload file", e);
								return Mono.error(new IllegalStateException("Unable to read the election event context payload file", e));
							}

							checkTenantValidity(tenantId);
							contextHolder.setTenantId(tenantId);

							final ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(request) {
								@Override
								public Flux<DataBuffer> getBody() {
									final DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
									final DataBuffer dataBuffer = factory.wrap(bytes);
									return Flux.just(dataBuffer);
								}
							};
							return chain.filter(exchange.mutate().request(requestDecorator).build()).contextWrite(Context.of(TENANT_ID, tenantId));
						});
			} finally {
				contextHolder.clear();
			}
		}
	}

	private void checkTenantValidity(final String tenantId) {
		validateTenantId(tenantId);
		if (!tenantService.existTenant(tenantId)) {
			final String message = String.format("Tenant non configured or invalid. [tenantId: %s]", tenantId);
			LOGGER.warn(message);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
	}

	private Optional<String> getElectionEventIdFromRequest(final PathContainer requestPath) {
		final int electionEventIndex = requestPath.elements().stream().map(PathContainer.Element::value).toList().indexOf("electionevent");
		if (electionEventIndex != -1 && requestPath.elements().size() > electionEventIndex + 2) {
			return Optional.of(requestPath.elements().get(electionEventIndex + 2).value());
		} else {
			return Optional.empty();
		}
	}

	private boolean containsPath(final PathContainer requestPath, final String subPath) {
		return requestPath.value().contains(subPath);
	}

	private Mono<Void> responseWithHttpError(final ServerWebExchange exchange, final HttpStatus status) {
		final ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(status);
		return response.setComplete();
	}

	private ExchangeType determineCase(final PathContainer requestPath, final Optional<String> electionEventId) {
		if (!containsPath(requestPath, "/api/")) {
			return ExchangeType.OUT_SCOPE;
		} else if (containsPath(requestPath, "/api/v1/configuration/setupvoting/keygeneration/electionevent/")) {
			return ExchangeType.INITIAL_REQUEST;
		} else if (containsPath(requestPath, "/api/v1/votingcardmanager/")) {
			return ExchangeType.VOTING_CARD_MANAGER_API;
		} else if (containsPath(requestPath, "api/v1/processor/voting/configurevoterportal/electionevent/") && electionEventId.isPresent()
				&& DUMMY_ELECTION_EVENT_ID.equals(electionEventId.get())) {
			// This is a special case for the dummy election event id where we don't set any tenant id.
			// This means no call to the database can be made as we don't have a tenant id to target the correct datasource.
			return ExchangeType.DUMMY_ELECTION_EVENT;
		} else {
			return ExchangeType.STANDARD_REQUEST;
		}
	}

	private enum ExchangeType {
		OUT_SCOPE,
		INITIAL_REQUEST,
		VOTING_CARD_MANAGER_API,
		STANDARD_REQUEST,
		DUMMY_ELECTION_EVENT
	}
}
