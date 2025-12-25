/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.evotinglibraries.domain.validations.EncryptionParametersSeedValidation.validateSeed;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.evotinglibraries.xml.XsdConstants.TALLY_COMPONENT_ECH_0222_VERSION;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIGURATION;
import static ch.post.it.evoting.securedatamanager.shared.Constants.TALLY_COMPONENT_ECH_0222_XML;
import static ch.post.it.evoting.securedatamanager.shared.process.VerifierExportType.CONTEXT;
import static ch.post.it.evoting.securedatamanager.shared.process.VerifierExportType.TALLY;

import java.nio.file.Path;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.shared.Constants;

public final class VerifierWhiteList {

	private VerifierWhiteList() {
		// static usage only
	}

	/**
	 * @param electionEventId the election event id. Must be non-null and a valid UUID.
	 * @return the list of entries needed to be exported for the Verifier.
	 */
	@SuppressWarnings("java:S1192") // Suppress duplication warning for patterns.
	public static ImmutableList<VerifierEntry> getList(final VerifierExportType verifierExportType, final String electionEventId,
			final String electionEventSeed) {
		validateUUID(electionEventId);
		validateSeed(electionEventSeed);

		final String folderRegex = "[a-zA-Z0-9]{32}";

		final String verificationCardSetsDirectory = join(electionEventId, Constants.VERIFICATION_CARD_SETS, folderRegex);
		final String ballotBoxesDirectory = join(electionEventId, Constants.BALLOT_BOXES, folderRegex);

		final Path contextPath = Path.of(CONTEXT.rootPath());

		final UnaryOperator<Path> verificationCardSetsPath = basePath -> basePath.resolve(Constants.VERIFICATION_CARD_SETS);

		final Path tallyPath = Path.of(TALLY.rootPath());
		final Path ballotBoxesPath = tallyPath.resolve(Constants.BALLOT_BOXES);

		switch (verifierExportType) {
		case CONTEXT -> {
			return ImmutableList.of(
					createEntry(join(electionEventId, "controlComponentPublicKeysPayload\\.[1-4]{1}\\.json"), contextPath, false),
					createEntry(join(electionEventId, "electionEventContextPayload\\.json"), contextPath, false),
					createEntry(join(electionEventId, "setupComponentPublicKeysPayload\\.json"), contextPath, false),

					createEntry(join(CONFIGURATION, "configuration-anonymized\\.xml"), contextPath, false),

					createEntry(join(verificationCardSetsDirectory, "setupComponentTallyDataPayload\\.json"),
							verificationCardSetsPath.apply(contextPath), true)
			);
		}
		case TALLY -> {
			return ImmutableList.of(
					createEntry(
							join(electionEventId, String.format(TALLY_COMPONENT_ECH_0222_XML, TALLY_COMPONENT_ECH_0222_VERSION, electionEventSeed)),
							tallyPath, false),

					createEntry(join(ballotBoxesDirectory, "controlComponentBallotBoxPayload_[1-4]{1}\\.json"), ballotBoxesPath, true),
					createEntry(join(ballotBoxesDirectory, "controlComponentShufflePayload_[1-4]{1}\\.json"), ballotBoxesPath, true),
					createEntry(join(ballotBoxesDirectory, "tallyComponent(Shuffle|Votes)Payload\\.json"), ballotBoxesPath, true)
			);
		}
		default -> throw new IllegalArgumentException("The verifier export type does not exist.");
		}
	}

	private static VerifierEntry createEntry(final String regex, final Path destinationPath, final boolean extendWithLastFolder) {
		return new VerifierEntry(Pattern.compile(regex), destinationPath, extendWithLastFolder);
	}

	private static String join(final String... params) {
		final String folderDelimiter = "/";
		return String.join(folderDelimiter, params);
	}

	/**
	 * Represents an entry to be exported.
	 *
	 * @param pattern                the pattern to match the entry.
	 * @param destinationPath        the destination path.
	 * @param extendWithParentFolder if true, the entry is exported with its parent (last) folder. If false, only the entry itself is exported.
	 */
	public record VerifierEntry(Pattern pattern, Path destinationPath, boolean extendWithParentFolder) {

	}
}
