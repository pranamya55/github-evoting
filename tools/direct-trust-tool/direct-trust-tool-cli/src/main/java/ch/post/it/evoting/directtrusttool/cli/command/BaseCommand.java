/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.cli.command;

import org.springframework.stereotype.Component;

import picocli.CommandLine;

@Component
@CommandLine.Command(subcommands = {
		KeystoresGenerationCommand.class,
		PublicKeysSharingDownloadCommand.class,
		PublicKeysSharingImportCommand.class,
		PublicKeysFingerprintCommand.class,
		KeystoresDownloadCommand.class,
		ClearCommand.class
})
public class BaseCommand {
}
