/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.tally.process;

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
@ConditionalOnProperty("role.isTally")
public class TallyPathResolver extends PathResolver {

	private final Path verifierOutput;
	private final Path tallyOutput;

	public TallyPathResolver(
			@Value("${sdm.path.workspace}")
			final Path workspace,
			@Value("${sdm.path.verifier:}")
			final Path verifierOutput,
			@Value("${sdm.path.tally:}")
			final Path tallyOutput) throws IOException {
		super(workspace);

		checkNotNull(verifierOutput, "The verifier output path is required for tally.");
		checkNotNull(tallyOutput, "The tally output path is required for tally.");

		this.verifierOutput = verifierOutput.toRealPath(LinkOption.NOFOLLOW_LINKS);
		this.tallyOutput = tallyOutput.toRealPath(LinkOption.NOFOLLOW_LINKS);

		checkArgument(Files.isDirectory(this.verifierOutput), "The given verifier output path is not a directory. [path: %s]", this.verifierOutput);
		checkArgument(Files.isDirectory(this.tallyOutput), "The given tally output path is not a directory. [path: %s]", this.tallyOutput);
	}

	@Override
	public Path resolveOutputPath() {
		throw new UnsupportedOperationException("The output path is not available in the tally SDM.");
	}

	@Override
	public Path resolvePrintingOutputPath() {
		throw new UnsupportedOperationException("The printing output path is not available in the tally SDM.");
	}

	@Override
	public Path resolveTallyOutputPath() {
		return tallyOutput;
	}

	@Override
	public Path resolveVerifierOutputPath() {
		return verifierOutput;
	}

	@Override
	public Path resolveExternalConfigurationPath() {
		throw new UnsupportedOperationException("The external configuration path is not available in the tally SDM.");
	}

	@Override
	public Path resolveVoterPortalConfigurationPath() {
		throw new UnsupportedOperationException("The voter portal configuration path is not available in the tally SDM.");
	}

}
