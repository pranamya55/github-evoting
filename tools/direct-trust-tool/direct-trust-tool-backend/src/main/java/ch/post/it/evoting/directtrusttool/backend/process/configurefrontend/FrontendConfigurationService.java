/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.configurefrontend;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet.toImmutableSet;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.directtrusttool.backend.ResetMode;
import ch.post.it.evoting.directtrusttool.backend.process.State;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

@Service
public class FrontendConfigurationService {
	private final String defaultCountry;
	private final State defaultState;
	private final String defaultLocality;
	private final String defaultOrganization;
	private final ImmutableSet<Alias> availableComponents;
	private final ImmutableSet<String> availableStates;
	private final ResetMode resetMode;

	public FrontendConfigurationService(
			@Value("${app.certificate.country:}")
			final String defaultCountry,
			@Value("${app.certificate.state:AARGAU}")
			final String defaultState,
			@Value("${app.certificate.locality:}")
			final String defaultLocality,
			@Value("${app.certificate.organization:}")
			final String defaultOrganization,
			@Value("${app.component.available:}")
			final List<String> availableComponents,
			@Value("${app.resetMode:#{T(ch.post.it.evoting.directtrusttool.backend.ResetMode).getDefaultResetMode()}}")
			final ResetMode resetMode) {

		this.defaultCountry = defaultCountry;
		this.defaultState = State.valueOf(defaultState);
		this.defaultLocality = defaultLocality;
		this.defaultOrganization = defaultOrganization;
		this.availableComponents = availableComponents.stream()
				.map(Alias::getByComponentName)
				.collect(toImmutableSet());
		this.availableStates = State.getLabels();
		this.resetMode = resetMode;
	}

	public FrontendConfigurationDto getFrontendConfiguration() {
		return new FrontendConfigurationDto(
				new CertificateDefaultValueDto(
						defaultCountry,
						defaultState.getLabel(),
						defaultLocality,
						defaultOrganization
				),
				availableStates,
				availableComponents,
				resetMode
		);
	}
}
