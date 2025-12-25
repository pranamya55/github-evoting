/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.configurefrontend;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.directtrusttool.backend.ResetMode;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

public record FrontendConfigurationDto(
		CertificateDefaultValueDto certificateDefaultValue,
		ImmutableSet<String> availableStates,
		ImmutableSet<Alias> availableComponents,
		ResetMode resetMode) {
}
