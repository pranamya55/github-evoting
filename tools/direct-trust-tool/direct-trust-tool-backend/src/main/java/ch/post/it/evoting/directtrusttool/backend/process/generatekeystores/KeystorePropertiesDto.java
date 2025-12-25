/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.generatekeystores;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.time.LocalDate;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.directtrusttool.backend.process.State;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;
import ch.post.it.evoting.evotinglibraries.domain.validations.KeystoreOrganisationValidation;

public record KeystorePropertiesDto(LocalDate validUntil, String country, String state, String locality,
									String organisation, ImmutableSet<Alias> wantedComponents, String platform) {

	public KeystorePropertiesDto {
		checkNotNull(validUntil);
		checkNotNull(country);
		checkNotNull(state);
		checkNotNull(locality);
		checkNotNull(organisation);
		checkNotNull(wantedComponents);
		checkNotNull(platform);
		checkState(!country.isBlank());
		State.isValidLabel(state);
		checkState(!locality.isBlank());
		KeystoreOrganisationValidation.validate(organisation);
		checkState(!wantedComponents.isEmpty());
		checkState(platform.matches("^[a-z]*$"));
	}
}
