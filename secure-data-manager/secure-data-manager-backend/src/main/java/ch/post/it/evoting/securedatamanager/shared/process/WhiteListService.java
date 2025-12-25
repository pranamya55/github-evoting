/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.shared.process;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.Constants.BALLOT_BOXES;
import static ch.post.it.evoting.securedatamanager.shared.Constants.CONFIGURATION;
import static ch.post.it.evoting.securedatamanager.shared.Constants.VERIFICATION_CARD_SETS;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep;

/**
 * Define regexp patterns that will match the files needed to import/export the SDM files.
 * </p>
 * The import whitelist will match all the elections present, while the export whitelist will match only the given elections ID.
 */
@Component
public class WhiteListService {

	// enough to reach the end of SDM structure while preventing wasting time in incorrect deep tree.
	public static final int MAX_DEPTH = 10;

	/**
	 * To optimize the size of the export ZIP file, we handle only the relevant files for the specific export step. However, exporting all files can
	 * be enforced by setting this property.
	 */
	private final boolean fullExport;

	public WhiteListService(
			@Value("${sdm.process.data-exchange.forceFull}")
			final boolean fullExport) {
		this.fullExport = fullExport;
	}

	public ImmutableList<Pattern> getImportList() {
		return getList("[a-zA-Z0-9]{32}").stream()
				.map(Entry::pattern)
				.collect(toImmutableList());
	}

	public ImmutableList<Pattern> getExportList(final String electionEventId, final int exchangeIndex) {
		validateUUID(electionEventId);

		final WorkflowStep currentWorkflowStep = WorkflowStep.getExportStep(exchangeIndex);
		return getList(electionEventId).stream()
				.filter(pair -> fullExport || pair.steps().isEmpty() || pair.steps().stream().anyMatch(step -> step == currentWorkflowStep))
				.map(Entry::pattern)
				.collect(toImmutableList());
	}

	private ImmutableList<Entry> getList(final String electionEventId) {
		final String configurationDirectory = CONFIGURATION + "/";
		final String electionEventDirectory = String.format("%s/", electionEventId);
		final String verificationCardSetsDirectory = electionEventDirectory + VERIFICATION_CARD_SETS + "/";
		final String ballotBoxesDirectory = electionEventDirectory + BALLOT_BOXES + "/";

		return ImmutableList.of(
				// configuration root
				createEntry(configurationDirectory + "elections_config\\.json",
						WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_PARTIALLY_TO_TALLY_5, WorkflowStep.EXPORT_TO_TALLY_5),
				createEntry(configurationDirectory + "configuration-anonymized\\.xml",
						WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_PARTIALLY_TO_TALLY_5, WorkflowStep.EXPORT_TO_TALLY_5),

				// election root
				createEntry(electionEventDirectory + "controlComponentPublicKeysPayload\\.[1-4]{1}\\.json",
						WorkflowStep.EXPORT_TO_SETUP_2),
				createEntry(electionEventDirectory + "electionEventContextPayload\\.json",
						WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_PARTIALLY_TO_TALLY_5, WorkflowStep.EXPORT_TO_TALLY_5),
				createEntry(electionEventDirectory + "setupComponentPublicKeysPayload\\.json",
						WorkflowStep.EXPORT_TO_ONLINE_4, WorkflowStep.EXPORT_PARTIALLY_TO_TALLY_5, WorkflowStep.EXPORT_TO_TALLY_5),
				createEntry(electionEventDirectory + "setupComponentElectoralBoardHashesPayload\\.json",
						WorkflowStep.EXPORT_TO_ONLINE_4, WorkflowStep.EXPORT_PARTIALLY_TO_TALLY_5, WorkflowStep.EXPORT_TO_TALLY_5),

				// ballot boxes directory
				createEntry(ballotBoxesDirectory + "[a-zA-Z0-9]{32}/controlComponentBallotBoxPayload_[1-4]{1}\\.json",
						WorkflowStep.EXPORT_PARTIALLY_TO_TALLY_5, WorkflowStep.EXPORT_TO_TALLY_5),
				createEntry(ballotBoxesDirectory + "[a-zA-Z0-9]{32}/controlComponentShufflePayload_[1-4]{1}\\.json",
						WorkflowStep.EXPORT_PARTIALLY_TO_TALLY_5, WorkflowStep.EXPORT_TO_TALLY_5),

				// verification card sets directory
				createEntry(verificationCardSetsDirectory + "[a-zA-Z0-9]{32}/controlComponentCodeSharesPayload\\.[0-9]+\\.json",
						WorkflowStep.EXPORT_TO_SETUP_2),
				createEntry(verificationCardSetsDirectory + "[a-zA-Z0-9]{32}/setupComponentCMTablePayload\\.[0-9]+\\.json",
						WorkflowStep.EXPORT_TO_ONLINE_3),
				createEntry(verificationCardSetsDirectory + "[a-zA-Z0-9]{32}/setupComponentLVCCAllowListPayload\\.json",
						WorkflowStep.EXPORT_TO_ONLINE_3),
				createEntry(verificationCardSetsDirectory + "[a-zA-Z0-9]{32}/setupComponentTallyDataPayload\\.json",
						WorkflowStep.EXPORT_TO_ONLINE_1, WorkflowStep.EXPORT_PARTIALLY_TO_TALLY_5, WorkflowStep.EXPORT_TO_TALLY_5),
				createEntry(verificationCardSetsDirectory + "[a-zA-Z0-9]{32}/setupComponentVerificationCardKeystoresPayload\\.json",
						WorkflowStep.EXPORT_TO_ONLINE_4),
				createEntry(verificationCardSetsDirectory + "[a-zA-Z0-9]{32}/setupComponentVerificationDataPayload\\.[0-9]+\\.json",
						WorkflowStep.EXPORT_TO_ONLINE_1),
				createEntry(verificationCardSetsDirectory + "[a-zA-Z0-9]{32}/setupComponentVoterAuthenticationDataPayload.json",
						WorkflowStep.EXPORT_TO_ONLINE_1)
		);
	}

	private Entry createEntry(final String regex, final WorkflowStep... steps) {
		return new Entry(Pattern.compile(regex), ImmutableList.of(steps));
	}

	private record Entry(Pattern pattern, ImmutableList<WorkflowStep> steps) {
	}
}
