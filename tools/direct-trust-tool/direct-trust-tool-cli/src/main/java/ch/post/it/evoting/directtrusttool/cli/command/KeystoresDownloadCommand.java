/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableByteArray;
import ch.post.it.evoting.directtrusttool.backend.process.downloadkeystores.KeystoresDownloadService;
import ch.post.it.evoting.directtrusttool.cli.FileService;

import picocli.CommandLine;

@Component
@CommandLine.Command(
		name = "keystores-download",
		description = "Download the generated keystores.",
		mixinStandardHelpOptions = true)
public class KeystoresDownloadCommand implements Runnable {

	@CommandLine.Option(
			names = { "--session-id" },
			description = "The UUID of the wanted session.",
			defaultValue = "00000000000000000000000000000000"
	)
	private String sessionId;

	@CommandLine.Option(
			names = { "--output" },
			description = "The path to save the generated keystores.",
			required = true
	)
	private Path output;

	private final KeystoresDownloadService keystoresDownloadService;
	private final FileService fileService;

	public KeystoresDownloadCommand(final KeystoresDownloadService keystoresDownloadService, final FileService fileService) {
		this.keystoresDownloadService = keystoresDownloadService;
		this.fileService = fileService;
	}

	@Override
	public void run() {
		final ImmutableByteArray downloadedKeystoresAsZip = keystoresDownloadService.downloadKeystores(sessionId);
		fileService.saveByteArrayAsZip(downloadedKeystoresAsZip, output);
	}
}

