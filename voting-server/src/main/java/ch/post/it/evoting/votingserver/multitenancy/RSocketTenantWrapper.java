/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.multitenancy;

import static ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder.TENANT_ID;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import org.springframework.stereotype.Component;

import ch.post.it.evoting.evotinglibraries.domain.common.ContextHolder;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.Tenant;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
public class RSocketTenantWrapper {

	private final TenantLookupService tenantLookupService;
	private final ContextHolder contextHolder;

	public RSocketTenantWrapper(final TenantLookupService tenantLookupService,
			final ContextHolder contextHolder) {
		this.tenantLookupService = tenantLookupService;
		this.contextHolder = contextHolder;
	}

	public <T> Flux<T> wrapWithTenantContext(final Flux<T> flux, final String electionEventId) {
		final Tenant tenant = lookupTenant(validateUUID(electionEventId));
		contextHolder.setTenantId(tenant.id());
		return flux.contextWrite(Context.of(TENANT_ID, tenant.id())).contextCapture();
	}

	public <T> Mono<T> wrapWithTenantContext(final Mono<T> mono, final String electionEventId) {
		final Tenant tenant = lookupTenant(validateUUID(electionEventId));
		contextHolder.setTenantId(tenant.id());
		return mono.contextWrite(Context.of(TENANT_ID, tenant.id())).contextCapture();
	}

	private Tenant lookupTenant(final String electionEventId) {
		return tenantLookupService.lookupTenantFromElectionEventId(electionEventId).orElseThrow(() -> new IllegalArgumentException(
				String.format("Tenant not found for election event. [electionEventId: %s]", electionEventId)));
	}
}
