/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys.PublicKeysSharingService;
import ch.post.it.evoting.directtrusttool.cli.FileService;

import picocli.CommandLine;

@Component
@CommandLine.Command(
		name = "public-keys-sharing-download",
		description = "Download the public keys of the generated keystores.",
		mixinStandardHelpOptions = true)
public class PublicKeysSharingDownloadCommand implements Runnable {

	@CommandLine.Option(
			names = { "--session-id" },
			description = "The UUID of the wanted session.",
			defaultValue = "00000000000000000000000000000000"
	)
	private String sessionId;

	@CommandLine.Option(
			names = { "--output" },
			description = "The path to save the exporter public keys.",
			required = true
	)
	private Path output;

	private final PublicKeysSharingService publicKeysSharingService;
	private final FileService fileService;

	public PublicKeysSharingDownloadCommand(final PublicKeysSharingService publicKeysSharingService, final FileService fileService) {
		this.publicKeysSharingService = publicKeysSharingService;
		this.fileService = fileService;
	}

	@Override
	public void run() {
		final ImmutableByteArray downloadedPublicKeysAsZip = publicKeysSharingService.downloadPublicKeys(sessionId);
		fileService.saveByteArrayAsZip(downloadedPublicKeysAsZip, output);
	}
}

