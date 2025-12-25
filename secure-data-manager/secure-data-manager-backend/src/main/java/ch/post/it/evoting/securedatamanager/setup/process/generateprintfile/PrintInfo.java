/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.generateprintfile;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public record PrintInfo(Path outputFolder, String filename) {

	public PrintInfo {
		checkNotNull(outputFolder);
		checkArgument(Files.exists(outputFolder), "Output folder does not exist. [outputFolder: %s]", outputFolder);
		checkArgument(Files.isDirectory(outputFolder), "Output folder is not a directory. [outputFolder: %s]", outputFolder);

		checkNotNull(filename);
		checkArgument(!filename.isBlank(), "Filename is blank. [filename: %s]", filename);
	}

}
