/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.tools.disputeresolver.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.domain.Constants.CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT;
import static ch.post.it.evoting.domain.Constants.CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT;
import static ch.post.it.evoting.domain.Constants.DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.ControlComponentNode;

@Service
public class PathService {

	private static final String CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_REGEX = String.join("|", ControlComponentNode.ids().stream()
			.map(nodeId -> String.format(CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_FORMAT, nodeId))
			.collect(toImmutableList()));

	private static final String CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_REGEX = String.join("|",
			ControlComponentNode.ids().stream()
					.map(nodeId -> String.format(CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_FORMAT, nodeId))
					.collect(toImmutableList()));

	private final Path disputeResolverResolvedConfirmedVotesPayloadPath;
	private final ImmutableList<Path> controlComponentExtractedElectionEventPayloadsPaths;
	private final ImmutableList<Path> controlComponentExtractedVerificationCardsPayloadsPaths;

	/**
	 * @param inputDirectory  the input directory path where the dispute resolver input files are stored. Must not be null and must be a valid
	 *                        directory.
	 * @param outputDirectory the output directory path where the dispute resolver output file will be stored. Must not be null and must be a valid
	 *                        directory.
	 * @throws NullPointerException     if the input or output directory path is null.
	 * @throws UncheckedIOException     if the input or output directory path does not exist or cannot be resolved to a real path.
	 * @throws IllegalArgumentException if the input or output directory path is not a valid directory.
	 * @throws IllegalStateException    if
	 *                                  <ul>
	 *                                  	<li>the input directory does not contain the expected control component extracted election event payloads.</li>
	 *                                  	<li>the output directory already contains the dispute resolver resolved confirmed votes payload file.</li>
	 *                                  </ul>
	 */
	public PathService(
			@Value("${input.directory}")
			final Path inputDirectory,
			@Value("${output.directory}")
			final Path outputDirectory) {

		final Path validatedInputDirectory = validatePath(inputDirectory);
		final Path validatedOutputDirectory = validatePath(outputDirectory);
		final ImmutableList<Path> inputDirectoryFiles = listFilesPaths(validatedInputDirectory);

		// Validate that the input directory contains the expected control component extracted election event payloads.
		controlComponentExtractedElectionEventPayloadsPaths = inputDirectoryFiles.stream()
				.filter(filePath -> filePath.getFileName().toString().matches(CONTROL_COMPONENT_EXTRACTED_ELECTION_EVENT_PAYLOAD_NAME_REGEX))
				.collect(toImmutableList());
		checkState(controlComponentExtractedElectionEventPayloadsPaths.size() == ControlComponentNode.ids().size(),
				"There must be exactly one control component extracted election event payload for each control component node. [expected: %s, found: %s]",
				ControlComponentNode.ids().size(), controlComponentExtractedElectionEventPayloadsPaths.size());

		// Validate that the input directory contains the expected control component extracted verification cards payloads.
		controlComponentExtractedVerificationCardsPayloadsPaths = inputDirectoryFiles.stream()
				.filter(filePath -> filePath.getFileName().toString().matches(CONTROL_COMPONENT_EXTRACTED_VERIFICATION_CARDS_PAYLOAD_NAME_REGEX))
				.collect(toImmutableList());
		checkState(controlComponentExtractedVerificationCardsPayloadsPaths.size() == ControlComponentNode.ids().size(),
				"There must be exactly one control component extracted verification cards payload for each control component node. [expected: %s, found: %s]",
				ControlComponentNode.ids().size(), controlComponentExtractedVerificationCardsPayloadsPaths.size());

		// Validate that the output directory does not already contain the dispute resolver resolved confirmed votes payload file.
		disputeResolverResolvedConfirmedVotesPayloadPath = validatedOutputDirectory.resolve(DISPUTE_RESOLVER_RESOLVED_CONFIRMED_VOTES_PAYLOAD_NAME);
		checkState(!Files.exists(disputeResolverResolvedConfirmedVotesPayloadPath, LinkOption.NOFOLLOW_LINKS),
				"The dispute resolver resolved confirmed votes payload file already exists. [path: %s]",
				disputeResolverResolvedConfirmedVotesPayloadPath);
	}

	public ImmutableList<Path> getControlComponentExtractedElectionEventPayloadsPaths() {
		return controlComponentExtractedElectionEventPayloadsPaths;
	}

	public ImmutableList<Path> getControlComponentExtractedVerificationCardsPayloadsPaths() {
		return controlComponentExtractedVerificationCardsPayloadsPaths;
	}

	public Path getDisputeResolverResolvedConfirmedVotesPayloadPath() {
		return disputeResolverResolvedConfirmedVotesPayloadPath;
	}

	private Path validatePath(final Path path) {
		checkNotNull(path, "The provided path must not be null.");

		final Path validatedPath;
		try {
			validatedPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("The provided path does not exist. [path: %s]", path), e);
		}

		checkArgument(Files.isDirectory(validatedPath, LinkOption.NOFOLLOW_LINKS), "The provided path is not a directory. [path: %s]", path);

		return validatedPath;
	}

	/**
	 * Extracts the node id from the provided path's file name.
	 *
	 * @param path the path of the file from which to extract the node id. Must not be null and must be a regular file.
	 * @return the extracted node id.
	 * @throws NullPointerException     if the path is null.
	 * @throws IllegalArgumentException if
	 *                                                          <ul>
	 *                                                           <li>the path is not a regular file.</li>
	 *                                                           <li>the path's file name does not contain a node id.</li>
	 *                                  </ul>
	 */
	public static int getNodeId(final Path path) {
		checkNotNull(path);
		checkArgument(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), "The provided path is not a regular file. [path: %s]", path);
		final String fileName = path.getFileName().toString();
		final String[] parts = fileName.split("\\.");
		checkArgument(parts.length > 2, "The provided file name does not contain a node id. [path: %s, fileName: %s]", path, fileName);

		try {
			final int parsedNodeId = Integer.parseInt(parts[parts.length - 2]);
			checkArgument(ControlComponentNode.ids().contains(parsedNodeId),
					"The provided file name contains an unknown node id. [path: %s, fileName: %s]", path, fileName);

			return parsedNodeId;
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException(
					String.format("The provided file name does not contain a valid node id. [path: %s, fileName: %s]", path, fileName), e);
		}
	}

	private ImmutableList<Path> listFilesPaths(final Path path) {
		final ImmutableList<Path> payloadsPaths;
		try (final var stream = Files.list(path)) {
			payloadsPaths = stream
					.filter(filePath -> Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS))
					.collect(toImmutableList());
		} catch (final IOException e) {
			throw new UncheckedIOException(String.format("Could not list files from path. [path: %s]", path), e);
		}
		return payloadsPaths;
	}
}
