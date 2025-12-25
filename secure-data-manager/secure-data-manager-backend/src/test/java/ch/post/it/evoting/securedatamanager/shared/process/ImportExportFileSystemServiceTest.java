/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.xml.XsdConstants.TALLY_COMPONENT_ECH_0222_VERSION;
import static ch.post.it.evoting.securedatamanager.shared.Constants.BALLOT_BOXES;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIGURATION;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_CONFIGURATION_ANONYMIZED;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_ELECTION_EVENT_CONTEXT_PAYLOAD;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_SETUP_COMPONENT_TALLY_DATA_PAYLOAD;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_FILE_NAME_SETUP_COMPONENT_VERIFICATION_CARD_KEYSTORES_PAYLOAD;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIG_SETUP_COMPONENT_ELECTORAL_BOARD_HASHES_PAYLOAD;
import static ch.post.it.evoting.securedatamanager.shared.Constants.SDM_CONFIG_FILE_NAME_ELECTIONS_CONFIG;
import static ch.post.it.evoting.securedatamanager.shared.Constants.TALLY_COMPONENT_ECH_0222_XML;
import static ch.post.it.evoting.securedatamanager.shared.Constants.VERIFICATION_CARD_SETS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.UUIDGenerator;
import ch.post.it.evoting.securedatamanager.setup.process.SetupPathResolver;
import ch.post.it.evoting.securedatamanager.shared.Constants;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ImportExportFileSystemServiceTest {

	private static final UUIDGenerator uuidGenerator = UUIDGenerator.getInstance();
	private static final String ELECTION_EVENT_SEED = "NE_20231124_TT05";
	private final String electionEventId = uuidGenerator.generate();
	private final ImmutableList<String> verificationCardSetIds = ImmutableList.of(
			uuidGenerator.generate(),
			uuidGenerator.generate());
	private final ImmutableList<String> ballotBoxIds = ImmutableList.of(
			uuidGenerator.generate(),
			uuidGenerator.generate());

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private Path workspace;
	@TempDir(cleanup = CleanupMode.ALWAYS)
	private Path output;
	@TempDir(cleanup = CleanupMode.ALWAYS)
	private Path externalConfiguration;
	@TempDir(cleanup = CleanupMode.ALWAYS)
	private Path verifierOutput;
	@TempDir(cleanup = CleanupMode.ALWAYS)
	private Path printingOutput;
	@TempDir(cleanup = CleanupMode.ALWAYS)
	private Path usbDirectory;
	private ImportExportFileSystemService importExportFilesystemServiceFullExport;
	private ImportExportFileSystemService importExportFilesystemServicePhaseExport;
	private ImmutableList<FileExportInformation> files;

	@BeforeEach
	void setUp() throws IOException {
		final PathResolver pathResolver = new SetupPathResolver(workspace, output, externalConfiguration, verifierOutput, printingOutput);
		final WhiteListService whiteListServiceFullExport = new WhiteListService(true);
		importExportFilesystemServiceFullExport = new ImportExportFileSystemService(pathResolver, whiteListServiceFullExport, ELECTION_EVENT_SEED);

		final WhiteListService whiteListServicePhaseExport = new WhiteListService(false);
		importExportFilesystemServicePhaseExport = new ImportExportFileSystemService(pathResolver, whiteListServicePhaseExport, ELECTION_EVENT_SEED);

		files = getFiles();
	}

	@Test
	void importFileSystem() {
		// given
		initializeSourceDirectory(usbDirectory);

		// when
		importExportFilesystemServiceFullExport.importFileSystem(usbDirectory);

		// then
		validateTargetDirectory(workspace);
	}

	@Test
	void exportFileSystemFullExport() {
		// given
		initializeSourceDirectory(workspace);

		// when
		importExportFilesystemServiceFullExport.exportFileSystem(electionEventId, usbDirectory, 1);

		// then
		validateTargetDirectory(usbDirectory);
	}

	@Test
	void export1() {
		// given
		initializeSourceDirectory(workspace, ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1));

		// when
		importExportFilesystemServicePhaseExport.exportFileSystem(electionEventId, usbDirectory, 1);

		// then
		validateTargetDirectory(usbDirectory, WorkflowStep.EXPORT_TO_ONLINE_1);
	}

	@Test
	void export2() {
		// given
		initializeSourceDirectory(workspace, ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_TO_SETUP_2));

		// when
		importExportFilesystemServicePhaseExport.exportFileSystem(electionEventId, usbDirectory, 2);

		// then
		validateTargetDirectory(usbDirectory, WorkflowStep.EXPORT_TO_SETUP_2);
	}

	@Test
	void export3() {
		// given
		initializeSourceDirectory(workspace,
				ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_TO_SETUP_2, WorkflowStep.EXPORT_TO_ONLINE_3));

		// when
		importExportFilesystemServicePhaseExport.exportFileSystem(electionEventId, usbDirectory, 3);

		// then
		validateTargetDirectory(usbDirectory, WorkflowStep.EXPORT_TO_ONLINE_3);
	}

	@Test
	void export4() {
		// given
		initializeSourceDirectory(workspace,
				ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_TO_SETUP_2, WorkflowStep.EXPORT_TO_ONLINE_3,
						WorkflowStep.EXPORT_TO_ONLINE_4));

		// when
		importExportFilesystemServicePhaseExport.exportFileSystem(electionEventId, usbDirectory, 4);

		// then
		validateTargetDirectory(usbDirectory, WorkflowStep.EXPORT_TO_ONLINE_4);
	}

	@Test
	void export5() {
		// given
		initializeSourceDirectory(workspace);

		// when
		importExportFilesystemServicePhaseExport.exportFileSystem(electionEventId, usbDirectory, 5);

		// then
		validateTargetDirectory(usbDirectory, WorkflowStep.EXPORT_TO_TALLY_5);
	}

	@Test
	void collectForVerifierContextZip() {
		// given
		initializeSourceDirectory(workspace);

		// when
		importExportFilesystemServiceFullExport.collectForVerifier(VerifierExportType.CONTEXT, electionEventId, usbDirectory);

		// then
		validateTargetDirectory(usbDirectory, VerifierExportType.CONTEXT);
	}

	@Test
	void collectForVerifierTallyZip() {
		// given
		initializeSourceDirectory(workspace);

		// when
		importExportFilesystemServiceFullExport.collectForVerifier(VerifierExportType.TALLY, electionEventId, usbDirectory);

		// then
		validateTargetDirectory(usbDirectory, VerifierExportType.TALLY);
	}

	private void initializeSourceDirectory(final Path baseDirectory) {
		initializeSourceDirectory(baseDirectory, ImmutableList.emptyList());
	}

	private void initializeSourceDirectory(final Path baseDirectory, final ImmutableList<WorkflowStep> workflowSteps) {
		files.stream()
				.filter(file -> file.workflowSteps() == null || workflowSteps.isEmpty() || workflowSteps.stream().anyMatch(
						workflowStep -> file.workflowSteps().contains(workflowStep)))
				.map(file -> file.sdmPath() + file.fileName())
				.forEach(pathName -> createTestFile(baseDirectory, pathName));
	}

	private void createTestFile(final Path baseDirectory, final String pathName) {
		try {
			final Path path = baseDirectory.resolve(pathName);
			Files.createDirectories(path.getParent());
			Files.createFile(path);
		} catch (final IOException e) {
			throw new UncheckedIOException("An error occurred creating test file.", e);
		}
	}

	private void validateTargetDirectory(final Path baseDirectory) {
		validateTargetDirectory(baseDirectory, (WorkflowStep) null);
	}

	private void validateTargetDirectory(final Path baseDirectory, final WorkflowStep workflowStep) {
		final Predicate<FileExportInformation> isInPhase = file -> workflowStep == null || file.workflowSteps().contains(workflowStep);
		final Map<Boolean, ImmutableList<FileExportInformation>> filesByExistence = files.stream()
				.collect(Collectors.partitioningBy(file -> file.workflowSteps() != null && isInPhase.test(file), toImmutableList()));

		final Function<FileExportInformation, String> getPathName = file -> file.sdmPath() + file.fileName();
		validateFileExistence(baseDirectory, filesByExistence, getPathName);
	}

	private void validateTargetDirectory(final Path baseDirectory, final VerifierExportType exportType) {
		final Map<Boolean, ImmutableList<FileExportInformation>> filesByExistence = files.stream()
				.collect(Collectors.partitioningBy(
						file -> file.verifierExportType() != null && file.verifierExportType().equals(exportType), toImmutableList()));

		final Function<FileExportInformation, String> getPathName = file -> file.verifierPath() + file.fileName();
		validateFileExistence(baseDirectory, filesByExistence, getPathName);
	}

	private void validateFileExistence(final Path baseDirectory, final Map<Boolean, ImmutableList<FileExportInformation>> filesByExistence,
			final Function<FileExportInformation, String> getPathName) {
		filesByExistence.get(true).stream()
				.map(getPathName)
				.forEach(pathName -> assertTrue(baseDirectory.resolve(pathName).toFile().exists()));

		filesByExistence.get(false).stream()
				.map(getPathName)
				.forEach(pathName -> assertFalse(baseDirectory.resolve(pathName).toFile().exists()));
	}

	private ImmutableList<FileExportInformation> getFiles() {
		final List<FileExportInformation> fileExportInformationList = new ArrayList<>();

		final String configurationDirectory = CONFIGURATION + "/";
		final String electionEventDirectory = String.format("%s/", electionEventId);
		final String context = "context/";
		final String verificationCardSets = Constants.VERIFICATION_CARD_SETS + "/";
		final String tally = "tally/";
		final String ballotBoxes = tally + Constants.BALLOT_BOXES + "/";

		fileExportInformationList.add(new FileExportInformation(configurationDirectory, SDM_CONFIG_FILE_NAME_ELECTIONS_CONFIG,
				ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_TO_TALLY_5)));

		fileExportInformationList.addAll(List.of(
				new FileExportInformation(configurationDirectory, CONFIG_FILE_NAME_CONFIGURATION_ANONYMIZED, context,
						ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_TO_TALLY_5), VerifierExportType.CONTEXT),
				// unwanted file
				new FileExportInformation(electionEventDirectory,
						String.format(TALLY_COMPONENT_ECH_0222_XML, TALLY_COMPONENT_ECH_0222_VERSION, ELECTION_EVENT_SEED), tally,
						VerifierExportType.TALLY),
				new FileExportInformation(electionEventDirectory,
						String.format(TALLY_COMPONENT_ECH_0222_XML, TALLY_COMPONENT_ECH_0222_VERSION, ELECTION_EVENT_SEED + "-wrong"))
				// unwanted file
		));

		fileExportInformationList.addAll(List.of(
				new FileExportInformation(electionEventDirectory, "controlComponentPublicKeysPayload.0.json"), // unwanted file
				new FileExportInformation(electionEventDirectory, "controlComponentPublicKeysPayload.1.json",
						context,
						ImmutableList.of(WorkflowStep.EXPORT_TO_SETUP_2),
						VerifierExportType.CONTEXT),
				new FileExportInformation(electionEventDirectory, "controlComponentPublicKeysPayload.2.json",
						context,
						ImmutableList.of(WorkflowStep.EXPORT_TO_SETUP_2),
						VerifierExportType.CONTEXT),
				new FileExportInformation(electionEventDirectory, "controlComponentPublicKeysPayload.3.json",
						context,
						ImmutableList.of(WorkflowStep.EXPORT_TO_SETUP_2),
						VerifierExportType.CONTEXT),
				new FileExportInformation(electionEventDirectory, "controlComponentPublicKeysPayload.4.json",
						context,
						ImmutableList.of(WorkflowStep.EXPORT_TO_SETUP_2),
						VerifierExportType.CONTEXT),
				new FileExportInformation(electionEventDirectory, "controlComponentPublicKeysPayload.5.json"), // unwanted file

				new FileExportInformation(electionEventDirectory, CONFIG_FILE_NAME_ELECTION_EVENT_CONTEXT_PAYLOAD,
						context,
						ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_TO_TALLY_5),
						VerifierExportType.CONTEXT)
		));

		fileExportInformationList.add(new FileExportInformation(electionEventDirectory, CONFIG_SETUP_COMPONENT_ELECTORAL_BOARD_HASHES_PAYLOAD,
				ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_4, WorkflowStep.EXPORT_TO_TALLY_5)));

		fileExportInformationList.addAll(verificationCardSetIds.stream()
				.flatMap(verificationCardSetId -> {
					final String sdmVerificationCardSetDirectory =
							electionEventDirectory + VERIFICATION_CARD_SETS + "/" + verificationCardSetId + "/";
					final String verifierVerificationCardSetDirectory = verificationCardSets + verificationCardSetId + "/";

					return Stream.of(
							new FileExportInformation(sdmVerificationCardSetDirectory, "controlComponentCodeSharesPayload.0.json",
									ImmutableList.of(WorkflowStep.EXPORT_TO_SETUP_2)
							),
							new FileExportInformation(sdmVerificationCardSetDirectory, "controlComponentCodeSharesPayload.1.json",
									ImmutableList.of(WorkflowStep.EXPORT_TO_SETUP_2)
							),
							new FileExportInformation(sdmVerificationCardSetDirectory, "setupComponentCMTablePayload.0.json",
									ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_3)
							),
							new FileExportInformation(sdmVerificationCardSetDirectory, "setupComponentCMTablePayload.1.json",
									ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_3)
							),
							new FileExportInformation(sdmVerificationCardSetDirectory, "setupComponentLVCCAllowListPayload.json",
									ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_3)
							),
							new FileExportInformation(sdmVerificationCardSetDirectory, CONFIG_FILE_NAME_SETUP_COMPONENT_TALLY_DATA_PAYLOAD,
									context + verifierVerificationCardSetDirectory,
									ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_TO_TALLY_5),
									VerifierExportType.CONTEXT),
							new FileExportInformation(sdmVerificationCardSetDirectory,
									CONFIG_FILE_NAME_SETUP_COMPONENT_VERIFICATION_CARD_KEYSTORES_PAYLOAD,
									ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_4)
							),
							new FileExportInformation(sdmVerificationCardSetDirectory, "setupComponentVerificationDataPayload.0.json",
									ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1)
							),
							new FileExportInformation(sdmVerificationCardSetDirectory, "setupComponentVerificationDataPayload.1.json",
									ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1)
							),
							new FileExportInformation(sdmVerificationCardSetDirectory, "setupComponentVoterAuthenticationDataPayload.json",
									ImmutableList.of(WorkflowStep.EXPORT_TO_ONLINE_1)
							)
					);
				}).toList());

		fileExportInformationList.addAll(ballotBoxIds.stream()
				.flatMap(ballotBoxId -> {
					final String sdmBallotBoxDirectory = electionEventDirectory + BALLOT_BOXES + "/" + ballotBoxId + "/";
					final String verifierBallotBoxDirectory = ballotBoxes + ballotBoxId + "/";

					return Stream.of(
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentBallotBoxPayload_0.json"), // unwanted file
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentBallotBoxPayload_1.json",
									verifierBallotBoxDirectory,
									ImmutableList.of(WorkflowStep.EXPORT_TO_TALLY_5),
									VerifierExportType.TALLY),
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentBallotBoxPayload_2.json",
									verifierBallotBoxDirectory,
									ImmutableList.of(WorkflowStep.EXPORT_TO_TALLY_5),
									VerifierExportType.TALLY),
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentBallotBoxPayload_3.json",
									verifierBallotBoxDirectory,
									ImmutableList.of(WorkflowStep.EXPORT_TO_TALLY_5),
									VerifierExportType.TALLY),
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentBallotBoxPayload_4.json",
									verifierBallotBoxDirectory,
									ImmutableList.of(WorkflowStep.EXPORT_TO_TALLY_5),
									VerifierExportType.TALLY),
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentBallotBoxPayload_5.json"), // unwanted file
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentShufflePayload_0.json"), // unwanted file
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentShufflePayload_1.json",
									verifierBallotBoxDirectory,
									ImmutableList.of(WorkflowStep.EXPORT_TO_TALLY_5),
									VerifierExportType.TALLY),
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentShufflePayload_2.json",
									verifierBallotBoxDirectory,
									ImmutableList.of(WorkflowStep.EXPORT_TO_TALLY_5),
									VerifierExportType.TALLY),
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentShufflePayload_3.json",
									verifierBallotBoxDirectory,
									ImmutableList.of(WorkflowStep.EXPORT_TO_TALLY_5),
									VerifierExportType.TALLY),
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentShufflePayload_4.json",
									verifierBallotBoxDirectory,
									ImmutableList.of(WorkflowStep.EXPORT_TO_TALLY_5),
									VerifierExportType.TALLY),
							new FileExportInformation(sdmBallotBoxDirectory, "controlComponentShufflePayload_5.json"), // unwanted file
							new FileExportInformation(sdmBallotBoxDirectory, "tallyComponentShufflePayload.json",
									verifierBallotBoxDirectory,
									VerifierExportType.TALLY),
							new FileExportInformation(sdmBallotBoxDirectory, "tallyComponentVotesPayload.json",
									verifierBallotBoxDirectory,
									VerifierExportType.TALLY)
					);
				}).toList());

		return ImmutableList.from(fileExportInformationList);
	}

	private record FileExportInformation(String sdmPath, String fileName, String verifierPath, ImmutableList<WorkflowStep> workflowSteps,
										 VerifierExportType verifierExportType) {
		public FileExportInformation(final String sdmPath, final String fileName) {
			this(sdmPath, fileName, null, null, null);
		}

		public FileExportInformation(final String sdmPath, final String fileName, final ImmutableList<WorkflowStep> workflowSteps) {
			this(sdmPath, fileName, null, workflowSteps, null);
		}

		public FileExportInformation(final String sdmPath, final String fileName, final String verifierPath,
				final VerifierExportType verifierExportType) {
			this(sdmPath, fileName, verifierPath, null, verifierExportType);
		}
	}
}
