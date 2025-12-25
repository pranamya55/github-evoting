/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

import ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys.PublicKeysSharingService;
import ch.post.it.evoting.directtrusttool.cli.FileService;

import picocli.CommandLine;

@Component
@CommandLine.Command(
		name = "public-keys-sharing-import",
		description = "Add the public keys to the keystores.",
		mixinStandardHelpOptions = true)
public class PublicKeysSharingImportCommand implements Runnable {

	@CommandLine.Option(
			names = { "--session-id" },
			description = "The UUID of the wanted session.",
			defaultValue = "00000000000000000000000000000000"
	)
	private String sessionId;

	@CommandLine.Option(
			names = { "--public-key-path" },
			description = "The path where is the complete set of keys from all components to import.",
			required = true
	)
	private Path publicKeysPaths;

	private final PublicKeysSharingService publicKeysSharingService;
	private final FileService fileService;

	public PublicKeysSharingImportCommand(final PublicKeysSharingService publicKeysSharingService, final FileService fileService) {
		this.publicKeysSharingService = publicKeysSharingService;
		this.fileService = fileService;
	}

	@Override
	public void run() {
		publicKeysSharingService.importPublicKeys(sessionId, fileService.getAllFilesContentAsString(publicKeysPaths));
	}
}

