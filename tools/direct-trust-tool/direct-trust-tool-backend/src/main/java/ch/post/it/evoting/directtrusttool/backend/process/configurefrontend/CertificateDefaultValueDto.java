/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.configurefrontend;

import static com.google.common.base.Preconditions.checkNotNull;

import ch.post.it.evoting.directtrusttool.backend.process.State;

public record CertificateDefaultValueDto(String country, String state, String locality, String organisation) {

	public CertificateDefaultValueDto {
		checkNotNull(country);
		State.isValidLabel(state);
		checkNotNull(locality);
		checkNotNull(organisation);
	}
}
