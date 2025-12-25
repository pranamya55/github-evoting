/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.multitenancy;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.Tenant;
import ch.post.it.evoting.evotinglibraries.multitenancy.multitenancy.TenantService;

@Service
public class TenantLookupService {

	private final TenantService tenantService;

	public TenantLookupService(final TenantService tenantService) {
		this.tenantService = tenantService;
	}

	@Cacheable("lookupTenantFromElectionEventId")
	public Optional<Tenant> lookupTenantFromElectionEventId(final String electionEventId) {
		validateUUID(electionEventId);
		for (final Tenant tenant : tenantService.getTenants()) {
			final JdbcTemplate jdbc = new JdbcTemplate(tenant.dataSource());
			final Integer count = jdbc.queryForObject("select count(*) from ELECTION_EVENT where ELECTION_EVENT_ID = ?", Integer.class, electionEventId);
			final boolean exist = count != null && count > 0;
			if (exist) {
				return Optional.of(tenant);
			}
		}
		return Optional.empty();
	}
}
