/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableMap;
import ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys.PublicKeysSharingService;

import picocli.CommandLine;

@Component
@CommandLine.Command(
		name = "public-keys-fingerprint",
		description = "Display the fingerprint of all the public keys.",
		mixinStandardHelpOptions = true)
public class PublicKeysFingerprintCommand implements Runnable {

	@CommandLine.Option(
			names = { "--session-id" },
			description = "The UUID of the wanted session.",
			defaultValue = "00000000000000000000000000000000"
	)
	private String sessionId;

	private final PublicKeysSharingService publicKeysSharingService;

	public PublicKeysFingerprintCommand(final PublicKeysSharingService publicKeysSharingService) {
		this.publicKeysSharingService = publicKeysSharingService;
	}

	@Override
	@SuppressWarnings({
			"java:S106", // As it is a CLI tool, this is its interface and it is not wanted to use a logger.
			"java:S3457" // Because of the formating of the printf, we do not want to use String.format
	})
	public void run() {
		final ImmutableMap<String, String> fingerprints = publicKeysSharingService.extractFingerprints(sessionId);

		System.out.println();
		System.out.println("FINGERPRINTS");
		System.out.println("============");
		System.out.println();

		if (fingerprints.isEmpty()) {
			System.out.println("There is no fingerprint.");
		} else {
			final Integer componentNameMaxLength = fingerprints.keySet().stream()
					.map(String::length)
					.reduce(Integer::max)
					.orElse(0);

			fingerprints.entrySet().stream().sorted(Comparator.comparing(ImmutableMap.Entry::key))
					.forEach(entry -> System.out.printf("%-" + componentNameMaxLength + "s | %s%n", entry.key(), entry.value()));
		}
		System.out.println();
	}
}

