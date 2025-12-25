/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.online.process;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.securedatamanager.shared.process.PathResolver;

@Service
@ConditionalOnProperty(prefix = "role", name = { "isSetup", "isTally" }, havingValue = "false")
public class OnlinePathResolver extends PathResolver {

	private final Path output;
	private final Path voterPortalConfiguration;

	public OnlinePathResolver(
			@Value("${sdm.path.workspace}")
			final Path workspace,
			@Value("${sdm.path.export}")
			final Path output,
			@Value("${sdm.path.voter-portal-configuration}")
			final Path voterPortalConfiguration) throws IOException {
		super(workspace);

		checkNotNull(output, "The output path is required.");
		checkNotNull(voterPortalConfiguration, "The voter portal configuration path is required.");

		this.output = output.toRealPath(LinkOption.NOFOLLOW_LINKS);
		this.voterPortalConfiguration = voterPortalConfiguration.toRealPath(LinkOption.NOFOLLOW_LINKS);

		checkArgument(Files.isDirectory(this.output), "The given output path is not a directory. [path: %s]", this.output);
		checkArgument(Files.isDirectory(this.voterPortalConfiguration),
				"The given voter portal configuration path is not a directory. [path: %s]", this.voterPortalConfiguration);
	}

	@Override
	public Path resolveOutputPath() {
		return output;
	}

	@Override
	public Path resolvePrintingOutputPath() {
		throw new UnsupportedOperationException("The printing output path is not available in the online SDM.");
	}

	@Override
	public Path resolveTallyOutputPath() {
		throw new UnsupportedOperationException("The tally output path is not available in the online SDM.");
	}

	@Override
	public Path resolveVerifierOutputPath() {
		throw new UnsupportedOperationException("The verifier output path is not available in the online SDM.");
	}

	@Override
	public Path resolveExternalConfigurationPath() {
		throw new UnsupportedOperationException("The external configuration path is not available in the online SDM.");
	}

	@Override
	public Path resolveVoterPortalConfigurationPath() {
		return voterPortalConfiguration;
	}

}
