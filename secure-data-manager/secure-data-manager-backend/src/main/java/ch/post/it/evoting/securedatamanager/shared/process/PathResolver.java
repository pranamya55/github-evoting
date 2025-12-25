/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.springframework.cache.annotation.Cacheable;

import ch.post.it.evoting.evotinglibraries.domain.validations.FailedValidationException;
import ch.post.it.evoting.securedatamanager.shared.Constants;

public abstract class PathResolver {

	private final Path workspace;

	protected PathResolver(final Path workspace) throws IOException {
		checkNotNull(workspace, "The workspace path is required.");

		this.workspace = workspace.toRealPath(LinkOption.NOFOLLOW_LINKS);

		checkArgument(Files.isDirectory(this.workspace), "The given workspace is not a directory. [path: %s]", this.workspace);
	}

	/**
	 * Provides the output path.
	 *
	 * @return the output directory path.
	 */
	public abstract Path resolveOutputPath();

	/**
	 * Provides the printing output path.
	 *
	 * @return the printing output directory path.
	 * @throws UnsupportedOperationException if the printing output path is not
	 */
	public abstract Path resolvePrintingOutputPath();

	/**
	 * Provides the tally output path.
	 *
	 * @return the tally output directory path.
	 * @throws UnsupportedOperationException if the tally output path is not available.
	 */
	public abstract Path resolveTallyOutputPath();

	/**
	 * Provides the verifier output path.
	 *
	 * @return the verifier output directory path.
	 * @throws UnsupportedOperationException if the verifier output path is not available.
	 */
	public abstract Path resolveVerifierOutputPath();

	/**
	 * Provides the configuration directory path in the workspace.
	 * <p>
	 * The path corresponds to the location {@value Constants#CONFIGURATION}.
	 *
	 * @return the configuration directory path.
	 */
	@Cacheable(value = "configurationPaths", sync = true)
	public Path resolveConfigurationPath() {
		final Path configurationPath = resolveWorkspacePath().resolve(Constants.CONFIGURATION);

		createFolderIfNotExists(configurationPath);

		return configurationPath;
	}

	/**
	 * Provides the external configuration path where the configuration-anonymized file lies.
	 *
	 * @return the external configuration directory path.
	 */
	public abstract Path resolveExternalConfigurationPath();

	/**
	 * Provides the voter portal configuration path where the logo, favicon and json config lie.
	 *
	 * @return the voter portal configuration path.
	 */
	public abstract Path resolveVoterPortalConfigurationPath();

	/**
	 * Provides the workspace path.
	 *
	 * @return the workspace directory path.
	 */
	public Path resolveWorkspacePath() {
		return workspace;
	}

	/**
	 * Provides the election event directory path in the workspace for the given election event.
	 * <p>
	 * The path corresponds to the location {@code electionEventId}.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the election event path in the workspace.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not valid.
	 */
	@Cacheable(value = "electionEventPaths", sync = true)
	public Path resolveElectionEventPath(final String electionEventId) {
		validateUUID(electionEventId);

		final Path electionEventPath = resolveWorkspacePath().resolve(electionEventId);

		createFolderIfNotExists(electionEventPath);

		return electionEventPath;
	}

	/**
	 * Provides the ballot box directory path in the workspace for the given election event and ballot box.
	 * <p>
	 * The path corresponds to the location {@code electionEventId}/{@value Constants#BALLOT_BOXES}/{@code ballotBoxId}.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @param ballotBoxId     the ballot box id. Must be non-null and a valid UUID.
	 * @return the ballot box path in the workspace.
	 * @throws NullPointerException      if any of the inputs is null.
	 * @throws FailedValidationException if any of the inputs is not valid.
	 */
	@Cacheable(value = "ballotBoxPaths", sync = true)
	public Path resolveBallotBoxPath(final String electionEventId, final String ballotBoxId) {
		validateUUID(electionEventId);
		validateUUID(ballotBoxId);

		final Path ballotBoxPath = resolveElectionEventPath(electionEventId)
				.resolve(Constants.BALLOT_BOXES)
				.resolve(ballotBoxId);

		createFolderIfNotExists(ballotBoxPath);

		return ballotBoxPath;
	}

	/**
	 * Provides the verification card sets directory path in the workspace for the given election event.
	 * <p>
	 * The path corresponds to the location {@code electionEventId}/{@value Constants#VERIFICATION_CARD_SETS}.
	 *
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the verification card sets path in the workspace.
	 * @throws NullPointerException      if {@code electionEventId} is null.
	 * @throws FailedValidationException if {@code electionEventId} is not valid.
	 */
	@Cacheable(value = "verificationCardSetsPaths", sync = true)
	public Path resolveVerificationCardSetsPath(final String electionEventId) {
		validateUUID(electionEventId);

		final Path verificationCardSetsPath = resolveElectionEventPath(electionEventId)
				.resolve(Constants.VERIFICATION_CARD_SETS);

		createFolderIfNotExists(verificationCardSetsPath);

		return verificationCardSetsPath;
	}

	/**
	 * Provides the verification card set directory path in the workspace for the given election event and verification card set.
	 * <p>
	 * The path corresponds to the location {@code electionEventId}/{@value Constants#VERIFICATION_CARD_SETS}/{@code verificationCardSetId}.
	 *
	 * @param electionEventId       the election event id. Must be non-null and a valid UUID.
	 * @param verificationCardSetId the verification card set id. Must be non-null and a valid UUID.
	 * @return the verification card set path in the workspace.
	 * @throws NullPointerException      if any of the inputs is null.
	 * @throws FailedValidationException if any of the inputs is not valid.
	 */
	@Cacheable(value = "verificationCardSetPaths", sync = true)
	public Path resolveVerificationCardSetPath(final String electionEventId, final String verificationCardSetId) {
		validateUUID(electionEventId);
		validateUUID(verificationCardSetId);

		final Path verificationCardSetPath = resolveVerificationCardSetsPath(electionEventId)
				.resolve(verificationCardSetId);

		createFolderIfNotExists(verificationCardSetPath);

		return verificationCardSetPath;
	}

	private void createFolderIfNotExists(final Path path) {
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
			} catch (final IOException e) {
				throw new UncheckedIOException(String.format("An error occurred while creating the folder. [folder: %s]", path), e);
			}
		}
	}
}
