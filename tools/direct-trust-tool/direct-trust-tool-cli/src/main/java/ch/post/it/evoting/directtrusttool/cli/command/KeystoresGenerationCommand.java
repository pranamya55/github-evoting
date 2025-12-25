/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Component;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableSet;
import ch.post.it.evoting.directtrusttool.backend.process.generatekeystores.KeystorePropertiesDto;
import ch.post.it.evoting.directtrusttool.backend.process.generatekeystores.KeystoresGenerationService;
import ch.post.it.evoting.directtrusttool.cli.command.converter.ComponentConverter;
import ch.post.it.evoting.evotinglibraries.domain.signature.Alias;

import picocli.CommandLine;

@Component
@CommandLine.Command(
		name = "keystores-generation",
		description = "Generate the wanted keystores.",
		mixinStandardHelpOptions = true)
public class KeystoresGenerationCommand implements Runnable {

	@CommandLine.Option(
			names = { "--session-id" },
			description = "The UUID of the wanted session.",
			defaultValue = "00000000000000000000000000000000"
	)
	private String sessionId;

	@CommandLine.Option(
			names = { "--components" },
			description = "The component(s) for which generate a keystore.",
			converter = ComponentConverter.class,
			required = true,
			split = ","
	)
	private Set<Alias> components;

	@CommandLine.Option(
			names = { "--valid-until" },
			description = "The date until which the keystore is valid. Format: yyyy-MM-dd.",
			required = true
	)
	private LocalDate validUntil;

	@CommandLine.Option(
			names = { "--country" },
			description = "The country for which generate a keystore.",
			required = true
	)
	private String country;

	@CommandLine.Option(
			names = { "--state" },
			description = "The state for which generate a keystore.",
			required = true
	)
	private String state;

	@CommandLine.Option(
			names = { "--locality" },
			description = "The locality for which generate a keystore.",
			required = true
	)
	private String locality;

	@CommandLine.Option(
			names = { "--organization" },
			description = "The organization for which generate a keystore.",
			required = true
	)
	private String organization;

	@CommandLine.Option(
			names = { "--platform" },
			description = "The name of the platform to tag the downloaded files.",
			defaultValue = ""
	)
	private String platform;

	private final KeystoresGenerationService keystoresGenerationService;

	public KeystoresGenerationCommand(final KeystoresGenerationService keystoresGenerationService) {
		this.keystoresGenerationService = keystoresGenerationService;
	}

	@Override
	public void run() {
		keystoresGenerationService.generateKeystores(
				sessionId,
				new KeystorePropertiesDto(
						validUntil,
						country,
						state,
						locality,
						organization,
						ImmutableSet.from(components),
						platform
				)
		);
	}
}

