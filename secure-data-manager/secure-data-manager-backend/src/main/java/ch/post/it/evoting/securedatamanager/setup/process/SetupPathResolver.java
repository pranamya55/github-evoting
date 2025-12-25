/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Service
@ConditionalOnProperty("role.isSetup")
public class SetupPathResolver extends PathResolver {

	private final Path output;
	private final Path verifierOutput;
	private final Path printOutput;
	private final Path externalConfiguration;

	public SetupPathResolver(
			@Value("${sdm.path.workspace}")
			final Path workspace,
			@Value("${sdm.path.export}")
			final Path output,
			@Value("${sdm.path.external-configuration}")
			final Path externalConfiguration,
			@Value("${sdm.path.verifier:}")
			final Path verifierOutput,
			@Value("${sdm.path.print:}")
			final Path printOutput) throws IOException {
		super(workspace);

		checkNotNull(output, "The output path is required for setup.");
		checkNotNull(verifierOutput, "The verifier output path is required for setup.");
		checkNotNull(printOutput, "The print output path is required for setup.");

		this.output = output.toRealPath(LinkOption.NOFOLLOW_LINKS);
		this.verifierOutput = verifierOutput.toRealPath(LinkOption.NOFOLLOW_LINKS);
		this.printOutput = printOutput.toRealPath(LinkOption.NOFOLLOW_LINKS);
		this.externalConfiguration = externalConfiguration.toRealPath(LinkOption.NOFOLLOW_LINKS);

		checkArgument(Files.isDirectory(this.output), "The given output path is not a directory. [path: %s]", this.output);
		checkArgument(Files.isDirectory(this.externalConfiguration), "The given external configuration path is not a directory. [path: %s]",
				this.externalConfiguration);
		checkArgument(Files.isDirectory(this.verifierOutput), "The given verifier output path is not a directory. [path: %s]", this.verifierOutput);
		checkArgument(Files.isDirectory(this.printOutput), "The given print output path is not a directory. [path: %s]", this.printOutput);
	}

	@Override
	public Path resolveOutputPath() {
		return output;
	}

	/**
	 * Provides the printing output path.
	 *
	 * @return the printing output directory path.
	 * @throws UnsupportedOperationException if the printing output path is not available.
	 */
	public Path resolvePrintingOutputPath() {
		return printOutput;
	}

	@Override
	public Path resolveTallyOutputPath() {
		throw new UnsupportedOperationException("The tally output path is not available in the setup SDM.");
	}

	/**
	 * Provides the verifier output path.
	 *
	 * @return the verifier output directory path.
	 * @throws UnsupportedOperationException if the verifier output path is not available.
	 */
	@Override
	public Path resolveVerifierOutputPath() {
		return verifierOutput;
	}

	@Override
	public Path resolveVoterPortalConfigurationPath() {
		throw new UnsupportedOperationException("The voter portal configuration path is not available in the setup SDM.");
	}

	@Override
	public Path resolveExternalConfigurationPath() {
		return externalConfiguration.resolve(Constants.CONFIG_FILE_NAME_CONFIGURATION_ANONYMIZED);
	}

}
