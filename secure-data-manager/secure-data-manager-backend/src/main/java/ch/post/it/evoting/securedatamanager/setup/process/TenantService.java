/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.TenantIdValidation.validateTenantId;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantService {
	private final String tenantId;

	public TenantService(
			@Value("${sdm.election-event-seed}")
			final String seed,
			@Value("${tenant.id:}")
			final String tenantId) {
		this.tenantId = determineTenantId(tenantId, validateSeed(seed));
	}

	public String getTenantId() {
		return tenantId;
	}

	private String determineTenantId(final String tenantId, final String electionEventSeed) {
		final String result;
		if (StringUtils.hasText(tenantId)) {
			result = tenantId;
		} else {
			result = electionEventSeed.substring(0, 2).toLowerCase(Locale.ENGLISH);
		}

		return validateTenantId(result);
	}
}
