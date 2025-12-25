/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.securedatamanager.setup.process.constituteelectoralboard;

import static ch.post.it.evoting.cryptoprimitives.collection.ImmutableList.toImmutableList;
import static ch.post.it.evoting.evotinglibraries.domain.validations.Validations.validateUUID;
import static ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStep.CONSTITUTE_ELECTORAL_BOARD;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptoprimitives.collection.ImmutableList;
import ch.post.it.evoting.evotinglibraries.domain.common.SafePasswordHolder;
import ch.post.it.evoting.securedatamanager.shared.process.BoardMember;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoard;
import ch.post.it.evoting.securedatamanager.shared.process.ElectoralBoardService;
import ch.post.it.evoting.securedatamanager.shared.process.Status;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowStepRunner;
import ch.post.it.evoting.securedatamanager.shared.workflow.WorkflowTask;

@Service
@ConditionalOnProperty("role.isSetup")
public class ConstituteElectoralBoardService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConstituteElectoralBoardService.class);

	private final WorkflowStepRunner workflowStepRunner;
	private final ElectoralBoardService electoralBoardService;
	private final ElectoralBoardConfigService electoralBoardConfigService;

	public ConstituteElectoralBoardService(
			final WorkflowStepRunner workflowStepRunner,
			final ElectoralBoardService electoralBoardService,
			final ElectoralBoardConfigService electoralBoardConfigService) {
		this.workflowStepRunner = workflowStepRunner;
		this.electoralBoardService = electoralBoardService;
		this.electoralBoardConfigService = electoralBoardConfigService;
	}

	public void constitute(final String electionEventId, final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords) {
		validateUUID(electionEventId);

		// Create a safe copy of the passwords and clear the original ones.
		final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswordsCopy = checkNotNull(electoralBoardMembersPasswords).stream()
				.map(SafePasswordHolder::copy)
				.collect(toImmutableList());
		electoralBoardMembersPasswords.forEach(SafePasswordHolder::clear);
		checkArgument(electoralBoardMembersPasswordsCopy.size() >= 2, "There must be at least two passwords.");

		LOGGER.debug("Constituting Electoral Board... [electionEventId: {}]", electionEventId);

		final WorkflowTask workflowTask = new WorkflowTask(
				() -> performConstitute(electionEventId, electoralBoardMembersPasswordsCopy),
				() -> LOGGER.info("Constitution of the Electoral Board successful. [electionEventId: {}]", electionEventId),
				throwable -> LOGGER.error("Constitution of the Electoral Board failed. [electionEventId: {}]", electionEventId, throwable)
		);

		workflowStepRunner.run(CONSTITUTE_ELECTORAL_BOARD, workflowTask);
	}

	public ImmutableList<BoardMember> getElectoralBoardMembers() {
		return electoralBoardService.getElectoralBoard().members();
	}

	private void performConstitute(final String electionEventId, final ImmutableList<SafePasswordHolder> electoralBoardMembersPasswords) {
		LOGGER.debug("Constituting the Electoral Board... [electionEventId: {}]", electionEventId);

		final ElectoralBoard electoralBoard = electoralBoardService.getElectoralBoard();
		electoralBoardConfigService.constitute(electionEventId, electoralBoard.id(), electoralBoardMembersPasswords);
		electoralBoardService.updateStatus(electoralBoard.id(), Status.CONSTITUTED);

		LOGGER.info("Electoral Board successfully constituted. [electionEventId: {}]", electionEventId);
	}

}
